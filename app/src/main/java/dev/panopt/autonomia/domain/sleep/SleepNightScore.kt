package dev.panopt.autonomia.domain.sleep

import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence

/**
 * The 4-component sleep score for a single night.
 *
 * Sealed formula (arbol-scoring-vocal-v1.md §11.2):
 *   sleepScore = 0.40·duration + 0.25·continuity + 0.20·alignment + 0.15·digitalInterruption
 *
 * sleepScore is null when confidence == NoData (do NOT coerce to 0 — see ADR-3).
 * When confidence == Ambiguous, sleepScore is attenuated by SleepScoringParams.ambiguousConfidenceFactor.
 */
data class SleepNightScore(
    /** DurationScore (weight 0.40): clamp(actual/target, 0, 1), no surplus decay. */
    val duration: Float,

    /** ContinuityScore (weight 0.25): fragmentation penalty + compactness ratio. */
    val continuity: Float,

    /** ScheduleAlignmentScore (weight 0.20): how close onset/wake are to the target window. */
    val alignment: Float,

    /** DigitalInterruptionScore (weight 0.15): exp(-awakeUseMinutes/m) decay. */
    val digitalInterruption: Float,

    /**
     * Combined score [0,1] or null when confidence=NoData.
     * NEVER coerce null to 0 (that's the bug this model fixes — §10).
     */
    val sleepScore: Float?,

    /** Confidence level of the underlying timeline. */
    val confidence: SleepConfidence,
)

/**
 * The configured sleep objective window for a night.
 * Used only by ScheduleAlignmentScore — not by the detection/segmentation logic.
 */
data class SleepTargetWindow(
    val targetSleepAt: String,  // "HH:mm" local time
    val targetWakeAt: String,   // "HH:mm" local time
)
