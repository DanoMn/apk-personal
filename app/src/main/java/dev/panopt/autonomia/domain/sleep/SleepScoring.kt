package dev.panopt.autonomia.domain.sleep

import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.domain.sleep.interpretation.NightTimeline
import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
import dev.panopt.autonomia.domain.sleep.interpretation.SleepSegmentKind
import kotlin.math.exp

/**
 * Computes the 4-component sleep score from a [NightTimeline].
 *
 * Sealed formula (arbol-scoring-vocal-v1.md §11.2, design §2):
 *   SleepWeeklyScore = 0.40·DurationScore
 *                    + 0.25·ContinuityScore
 *                    + 0.20·ScheduleAlignmentScore
 *                    + 0.15·DigitalInterruptionScore
 *
 * DO NOT change weights — they are sealed per the spec.
 *
 * Key fixes over the legacy score(log) method:
 * - Sleeping MORE than target = 1.0 NEUTRAL (no decay — Bug §10 fix)
 * - 4 components instead of 2
 * - NoData → returns null (not 0) — ADR-3
 * - Ambiguous → score is computed but attenuated by [SleepScoringParams.ambiguousConfidenceFactor]
 */
object SleepScoring {

    // ─── Sealed component weights (DO NOT modify) ─────────────────────────────

    private const val WEIGHT_DURATION = 0.40f
    private const val WEIGHT_CONTINUITY = 0.25f
    private const val WEIGHT_ALIGNMENT = 0.20f
    private const val WEIGHT_DIGITAL_INTERRUPTION = 0.15f

    /**
     * Scores a single night from its [NightTimeline] and configured [target] window.
     *
     * @return [SleepNightScore] or null when [timeline].confidence == [SleepConfidence.NoData].
     *   NEVER coerce null to 0 — that is the bug this method was rewritten to fix (ADR-3).
     */
    fun scoreNight(timeline: NightTimeline, target: SleepTargetWindow): SleepNightScore? {
        // NoData → do not compute, return null (propagate absence, not 0)
        if (timeline.confidence == SleepConfidence.NoData) return null

        val duration = computeDurationScore(timeline, target)
        val continuity = computeContinuityScore(timeline)
        val alignment = computeAlignmentScore(timeline, target)
        val digitalInterruption = computeDigitalInterruptionScore(timeline)

        val rawScore = WEIGHT_DURATION * duration +
            WEIGHT_CONTINUITY * continuity +
            WEIGHT_ALIGNMENT * alignment +
            WEIGHT_DIGITAL_INTERRUPTION * digitalInterruption

        // Attenuation for Ambiguous confidence (design §2.5)
        val finalScore = when (timeline.confidence) {
            SleepConfidence.Ambiguous -> (rawScore * SleepScoringParams.ambiguousConfidenceFactor).coerceIn(0f, 1f)
            else -> rawScore.coerceIn(0f, 1f)
        }

        return SleepNightScore(
            duration = duration,
            continuity = continuity,
            alignment = alignment,
            digitalInterruption = digitalInterruption,
            sleepScore = finalScore,
            confidence = timeline.confidence,
        )
    }

    // ─── 2.1 DurationScore (0.40) ─────────────────────────────────────────────

    /**
     * DurationScore = clamp(actualSleepMinutes / targetSleepMinutes, 0, 1).
     * Sleeping MORE than target = 1.0 NEUTRAL — no decay. (Bug §10 fix: removes
     * the old coerceIn(0.50f, 1f) with decay from SleepScoring.score().)
     */
    private fun computeDurationScore(timeline: NightTimeline, target: SleepTargetWindow): Float {
        val targetMinutes = SleepPolicy.plannedWindowMinutes(
            plannedSleepAt = target.targetSleepAt,
            plannedWakeAt = target.targetWakeAt,
        ) ?: SleepPolicy.DEFAULT_SLEEP_WINDOW_MINUTES

        val asleepMinutes = timeline.segments
            .filter { it.kind == SleepSegmentKind.Asleep }
            .sumOf { seg ->
                (seg.endAt.toEpochMilli() - seg.startAt.toEpochMilli()).coerceAtLeast(0L)
            }
            .let { it / 60_000L } // millis → minutes

        return if (targetMinutes <= 0) 0f
        else (asleepMinutes.toFloat() / targetMinutes.toFloat()).coerceIn(0f, 1f)
    }

    // ─── 2.2 ContinuityScore (0.25) ───────────────────────────────────────────

    /**
     * ContinuityScore = clamp(0.5·exp(-awakeCount/k) + 0.5·longestAsleepRatio, 0, 1).
     * k = [SleepScoringParams.continuityDecayFactor] ≈ 2 (same pattern as TaskMomentumRaw).
     * 0 despertares + single Asleep block → 1.0.
     */
    private fun computeContinuityScore(timeline: NightTimeline): Float {
        val awakeUseSegments = timeline.segments.filter { it.kind == SleepSegmentKind.AwakeUse }
        val asleepSegments = timeline.segments.filter { it.kind == SleepSegmentKind.Asleep }

        val awakeCount = awakeUseSegments.size
        val totalAsleepMs = asleepSegments.sumOf {
            (it.endAt.toEpochMilli() - it.startAt.toEpochMilli()).coerceAtLeast(0L)
        }
        val longestAsleepMs = asleepSegments.maxOfOrNull {
            (it.endAt.toEpochMilli() - it.startAt.toEpochMilli()).coerceAtLeast(0L)
        } ?: 0L

        val longestAsleepRatio = if (totalAsleepMs <= 0L) 1f
        else (longestAsleepMs.toFloat() / totalAsleepMs.toFloat()).coerceIn(0f, 1f)

        val k = SleepScoringParams.continuityDecayFactor
        val fragmentationPenalty = exp(-awakeCount.toFloat() / k)

        return (0.5f * fragmentationPenalty + 0.5f * longestAsleepRatio).coerceIn(0f, 1f)
    }

    // ─── 2.3 ScheduleAlignmentScore (0.20) ────────────────────────────────────

    /**
     * ScheduleAlignmentScore = average(
     *   closeness(sleepOnsetAt, targetSleepAt),
     *   closeness(definitiveWakeAt, targetWakeAt),
     * )
     *
     * Reuses [SleepPolicy.scheduleCloseness] which computes circular minute distance
     * with a 120-minute tolerance. This is the ONLY component that uses the target window
     * for its calculation (design §1.2, §2.3).
     */
    private fun computeAlignmentScore(timeline: NightTimeline, target: SleepTargetWindow): Float {
        val onset = timeline.sleepOnsetAt ?: return 0f
        val wake = timeline.definitiveWakeAt ?: return 0f

        // Convert Instant to "HH:mm" string in UTC for SleepPolicy.scheduleCloseness
        val onsetTime = formatHHmm(onset.toEpochMilli())
        val wakeTime = formatHHmm(wake.toEpochMilli())

        val onsetCloseness = SleepPolicy.scheduleCloseness(onsetTime, target.targetSleepAt)
        val wakeCloseness = SleepPolicy.scheduleCloseness(wakeTime, target.targetWakeAt)

        return ((onsetCloseness + wakeCloseness) / 2f).coerceIn(0f, 1f)
    }

    // ─── 2.4 DigitalInterruptionScore (0.15) ──────────────────────────────────

    /**
     * DigitalInterruptionScore = exp(-awakeUseMinutes / m).
     * m = [SleepScoringParams.digitalInterruptionDecayFactor] ≈ 30 (calibrable).
     * 0 AwakeUse → 1.0. More nocturnal use → lower score, saturating softly.
     *
     * D3: digitalWindDownMinutes is INERT a propósito — not used here.
     * This component measures only real use DURING detected sleep, not the config.
     * See also: ScoreInputSource.digitalWindDownMinutes (documented as inert).
     */
    private fun computeDigitalInterruptionScore(timeline: NightTimeline): Float {
        val awakeUseMinutes = timeline.segments
            .filter { it.kind == SleepSegmentKind.AwakeUse }
            .sumOf { seg ->
                (seg.endAt.toEpochMilli() - seg.startAt.toEpochMilli()).coerceAtLeast(0L)
            }
            .let { it.toFloat() / 60_000f } // millis → minutes

        val m = SleepScoringParams.digitalInterruptionDecayFactor
        return exp(-awakeUseMinutes / m).coerceIn(0f, 1f)
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    /** Converts epoch millis to "HH:mm" in UTC, for use with [SleepPolicy.scheduleCloseness]. */
    private fun formatHHmm(epochMillis: Long): String {
        val totalSeconds = epochMillis / 1000L
        val totalMinutes = totalSeconds / 60
        val hour = (totalMinutes / 60 % 24).toInt()
        val minute = (totalMinutes % 60).toInt()
        return "%02d:%02d".format(hour, minute)
    }

    // ─── Legacy API (kept for compilation; deprecated — use scoreNight) ───────

    /**
     * Legacy 2-component score from [SleepLog].
     * @deprecated Use [scoreNight] with [NightTimeline]. This method remains only
     *   for backwards compatibility until WU-5/WU-6 remove the last callers.
     */
    @Deprecated("Use scoreNight(NightTimeline, SleepTargetWindow) — this is the 2-component legacy scorer")
    fun score(log: SleepLog): Float {
        val plannedMinutes = SleepPolicy.plannedWindowMinutes(
            plannedSleepAt = log.plannedSleepAt,
            plannedWakeAt = log.plannedWakeAt,
        ) ?: SleepPolicy.DEFAULT_SLEEP_WINDOW_MINUTES
        val actualMinutes = SleepPolicy.minutesBetween(log.sleptAt, log.wokeAt) ?: 0
        val durationScore = when {
            actualMinutes <= 0 -> 0f
            actualMinutes <= plannedMinutes -> actualMinutes.toFloat() / plannedMinutes.toFloat()
            actualMinutes <= plannedMinutes + 90 -> 1f
            else -> (1f - ((actualMinutes - plannedMinutes - 90).toFloat() / 240f)).coerceIn(0.50f, 1f)
        }
        val scheduleScore = (
            SleepPolicy.scheduleCloseness(log.sleptAt, log.plannedSleepAt) +
                SleepPolicy.scheduleCloseness(log.wokeAt, log.plannedWakeAt)
            ) / 2f
        return (durationScore * 0.70f + scheduleScore * 0.30f).coerceIn(0f, 1f)
    }
}
