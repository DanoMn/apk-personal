package dev.panopt.autonomia.domain.sleep.interpretation

import dev.panopt.autonomia.domain.sleep.SleepTargetWindow
import dev.panopt.autonomia.platform.telemetry.DeviceActivityEvent
import dev.panopt.autonomia.platform.telemetry.DeviceActivityEventType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.abs

/**
 * Converts a list of raw [DeviceActivityEvent]s from the detection window
 * (20:00 day D-1 → 12:00 day D) into a [NightTimeline] with segment breakdown
 * and confidence level.
 *
 * This is pure JVM domain logic — no Room, no Android types.
 *
 * Design: sdd/sleep-consumer/design.md §1
 * Spec: openspec/changes/sleep-consumer/specs/sleep-interpretation/spec.md
 *
 * ADR-1: imports DeviceActivityEvent directly (value object, already pure JVM).
 */
object SleepInterpreter {

    /**
     * Interprets raw device activity events into a [NightTimeline].
     *
     * @param events All events the caller has fetched for the biological window.
     *   The interpreter will filter to 20:00–12:00; callers need not pre-filter.
     * @param target The user's configured sleep objective window (HH:mm strings).
     *   Used only for: (a) nap disambiguation and (b) nightDate assignment.
     * @param params Calibration thresholds — use [InterpretationParams.DEFAULT] normally.
     */
    fun interpret(
        events: List<DeviceActivityEvent>,
        target: SleepTargetWindow,
        params: InterpretationParams = InterpretationParams.DEFAULT,
    ): NightTimeline {
        // Determine the wake date from context: if we have events, derive from latest event.
        // For the biological window 20:00 D-1 → 12:00 D we use the day of the latest event.
        // The nightDate is assigned to the wake day (day D), not the onset day.
        val wakeDate = deriveProbableWakeDate(events)

        // 1. Filter events to the biological detection window: 20:00 D-1 → 12:00 D
        val windowStart = windowStartEpochMillis(wakeDate)
        val windowEnd = windowEndEpochMillis(wakeDate)
        val windowEvents = events
            .filter { it.timestamp in windowStart..windowEnd }
            .sortedBy { it.timestamp }

        // 2. No events at all → NoData
        if (windowEvents.isEmpty()) {
            return NightTimeline(
                nightDate = wakeDate,
                segments = emptyList(),
                sleepOnsetAt = null,
                definitiveWakeAt = null,
                confidence = SleepConfidence.NoData,
            )
        }

        // 3. Group real-use events into AwakeUse episodes
        val awakeEpisodes = groupAwakeUseEpisodes(windowEvents, params.quietGapMillis)

        // 4. Check: if there are no real-use events at all (only glances/quiet events)
        //    → High confidence (quiet phone is signature of good sleep)
        if (awakeEpisodes.isEmpty()) {
            // No real use detected: determine if this is a clean quiet night or truly no data
            // Having SCREEN_ON events means the device existed but was not used → quiet = High
            val hasAnySignal = windowEvents.isNotEmpty()
            return if (hasAnySignal) {
                // Quiet phone with some minimal signal (e.g., glances) → High confidence
                // We need an onset anchor: use windowStart as proxy onset
                val targetOnset = targetOnsetEpochMillis(target, wakeDate)
                NightTimeline(
                    nightDate = wakeDate,
                    segments = listOf(
                        SleepSegment(
                            startAt = Instant.ofEpochMilli(targetOnset),
                            endAt = Instant.ofEpochMilli(windowEnd),
                            kind = SleepSegmentKind.Asleep,
                        ),
                    ),
                    sleepOnsetAt = Instant.ofEpochMilli(targetOnset),
                    definitiveWakeAt = Instant.ofEpochMilli(windowEnd),
                    confidence = SleepConfidence.High,
                )
            } else {
                NightTimeline(
                    nightDate = wakeDate,
                    segments = emptyList(),
                    sleepOnsetAt = null,
                    definitiveWakeAt = null,
                    confidence = SleepConfidence.NoData,
                )
            }
        }

        // 5. Build timeline segments: alternate Asleep/AwakeUse
        val allSegments = buildSegments(awakeEpisodes, windowStart, windowEnd)

        // 6. Determine sleepOnsetAt = end of the last AwakeUse before the main sleep block
        val sleepOnsetAt = deriveSleepOnset(awakeEpisodes, allSegments)

        // 7. Determine definitiveWakeAt
        val definitiveWakeResult = detectDefinitiveWake(
            awakeEpisodes = awakeEpisodes,
            target = target,
            wakeDate = wakeDate,
            windowEnd = windowEnd,
            params = params,
        )
        val definitiveWakeAt = definitiveWakeResult.definitiveWakeAt
        val cappedAtWindow = definitiveWakeResult.cappedAtWindow

        // 8. Select main sleep block (discard naps far from objective)
        val mainSegments = selectMainSleepBlock(
            allSegments = allSegments,
            target = target,
            wakeDate = wakeDate,
            params = params,
            definitiveWakeAt = definitiveWakeAt,
        )

        // 9. Assign confidence
        val confidence = assignConfidence(
            awakeEpisodes = awakeEpisodes,
            sleepOnsetAt = sleepOnsetAt,
            definitiveWakeAt = definitiveWakeAt,
            cappedAtWindow = cappedAtWindow,
        )

        return NightTimeline(
            nightDate = wakeDate,
            segments = mainSegments,
            sleepOnsetAt = sleepOnsetAt,
            definitiveWakeAt = definitiveWakeAt,
            confidence = confidence,
        )
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Determines the probable wake date (day D) from the event list.
     * If no events, defaults to today.
     * The wake date is the calendar day on which the user woke up:
     * any event after midnight belongs to day D.
     */
    private fun deriveProbableWakeDate(events: List<DeviceActivityEvent>): LocalDate {
        if (events.isEmpty()) return LocalDate.now(ZoneOffset.UTC)
        // Use the day of the latest event as our reference for the wake date
        val latestMs = events.maxOf { it.timestamp }
        return Instant.ofEpochMilli(latestMs).atZone(ZoneOffset.UTC).toLocalDate()
    }

    /**
     * Detection window start: 20:00 of day D-1 (in UTC for test determinism).
     * In production, the caller should pass events already in local time millis.
     */
    private fun windowStartEpochMillis(wakeDate: LocalDate): Long {
        val prevDay = wakeDate.minusDays(1)
        return prevDay.atTime(20, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    /**
     * Detection window end: 12:00 of day D (wake day).
     */
    private fun windowEndEpochMillis(wakeDate: LocalDate): Long =
        wakeDate.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    /**
     * Returns the epoch millis of the configured target sleep time.
     * If the target sleep hour is in the afternoon/evening (≥ 14:00), it falls on day D-1.
     * If the target sleep hour is early morning or midnight (< 14:00), it falls on day D
     * (user means "I go to sleep just after midnight").
     */
    private fun targetOnsetEpochMillis(target: SleepTargetWindow, wakeDate: LocalDate): Long {
        val sleepTime = parseTime(target.targetSleepAt) ?: LocalTime.of(23, 0)
        // Sleep times in the evening (≥ 14:00 local) → previous calendar day
        // Sleep times around/after midnight (< 14:00) → wake day itself
        val sleepDate = if (sleepTime.hour >= 14) wakeDate.minusDays(1) else wakeDate
        return sleepDate.atTime(sleepTime).toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    /**
     * Returns the epoch millis of the configured target wake time on wakeDate.
     */
    private fun targetWakeEpochMillis(target: SleepTargetWindow, wakeDate: LocalDate): Long {
        val wakeTime = parseTime(target.targetWakeAt) ?: LocalTime.of(7, 0)
        return wakeDate.atTime(wakeTime).toInstant(ZoneOffset.UTC).toEpochMilli()
    }

    private fun parseTime(value: String): LocalTime? =
        runCatching { LocalTime.parse(value) }.getOrNull()

    /**
     * Groups consecutive real-use events (USER_INTERACTION, APP_FOREGROUND) into
     * AwakeUse episodes. A new episode starts on the first real-use event; it closes
     * when no event (of any type) arrives within [quietGapMillis].
     *
     * SCREEN_ON / UNLOCK alone (without real-use within [quietGapMillis]) = glance → ignored.
     *
     * Returns: list of AwakeUse segments (startAt = first real-use event, endAt = last event
     * of the episode).
     */
    private fun groupAwakeUseEpisodes(
        events: List<DeviceActivityEvent>,
        quietGapMillis: Long,
    ): List<SleepSegment> {
        val realUseTypes = setOf(
            DeviceActivityEventType.USER_INTERACTION,
            DeviceActivityEventType.APP_FOREGROUND,
        )

        val episodes = mutableListOf<SleepSegment>()
        var episodeStart: Long? = null
        var lastEventTs: Long = Long.MIN_VALUE
        var hasRealUseInEpisode = false

        for (event in events) {
            val ts = event.timestamp
            val isRealUse = event.eventType in realUseTypes

            if (episodeStart == null) {
                // Not in an episode yet
                if (isRealUse) {
                    // Start new episode
                    episodeStart = ts
                    lastEventTs = ts
                    hasRealUseInEpisode = true
                }
                // SCREEN_ON/UNLOCK alone: ignore (glance)
            } else {
                // We are in an episode
                val gap = ts - lastEventTs
                if (gap > quietGapMillis) {
                    // Close the current episode
                    if (hasRealUseInEpisode) {
                        episodes.add(
                            SleepSegment(
                                startAt = Instant.ofEpochMilli(episodeStart),
                                endAt = Instant.ofEpochMilli(lastEventTs),
                                kind = SleepSegmentKind.AwakeUse,
                            ),
                        )
                    }
                    // Start a new episode only if this event is real use
                    if (isRealUse) {
                        episodeStart = ts
                        lastEventTs = ts
                        hasRealUseInEpisode = true
                    } else {
                        episodeStart = null
                        hasRealUseInEpisode = false
                    }
                } else {
                    // Within the gap: extend the episode
                    lastEventTs = ts
                    if (isRealUse) hasRealUseInEpisode = true
                }
            }
        }

        // Close the final episode if still open
        if (episodeStart != null && hasRealUseInEpisode) {
            episodes.add(
                SleepSegment(
                    startAt = Instant.ofEpochMilli(episodeStart),
                    endAt = Instant.ofEpochMilli(lastEventTs),
                    kind = SleepSegmentKind.AwakeUse,
                ),
            )
        }

        return episodes
    }

    /**
     * Builds the alternating Asleep/AwakeUse segment list from the wake episodes.
     * Gaps between wake episodes (and before first / after last) are Asleep segments.
     */
    private fun buildSegments(
        awakeEpisodes: List<SleepSegment>,
        windowStart: Long,
        windowEnd: Long,
    ): List<SleepSegment> {
        val segments = mutableListOf<SleepSegment>()
        var cursor = windowStart

        for (episode in awakeEpisodes) {
            val episodeStart = episode.startAt.toEpochMilli()
            val episodeEnd = episode.endAt.toEpochMilli()
            if (episodeStart > cursor) {
                // Gap before this episode = Asleep
                segments.add(
                    SleepSegment(
                        startAt = Instant.ofEpochMilli(cursor),
                        endAt = Instant.ofEpochMilli(episodeStart),
                        kind = SleepSegmentKind.Asleep,
                    ),
                )
            }
            segments.add(episode)
            cursor = episodeEnd
        }

        // Trailing Asleep segment after last episode (up to window end)
        if (cursor < windowEnd) {
            segments.add(
                SleepSegment(
                    startAt = Instant.ofEpochMilli(cursor),
                    endAt = Instant.ofEpochMilli(windowEnd),
                    kind = SleepSegmentKind.Asleep,
                ),
            )
        }

        return segments
    }

    /**
     * Derives sleepOnsetAt = end of the last AwakeUse episode before the main sleep block.
     * If there is no AwakeUse before the sleep block, onset = windowStart.
     *
     * "Detox" (APP_FOREGROUND late at night) IS a real-use episode, so onset is placed
     * AFTER it — this correctly prevents detox from being counted as sleep onset anchor
     * (design §1.4).
     */
    private fun deriveSleepOnset(
        awakeEpisodes: List<SleepSegment>,
        allSegments: List<SleepSegment>,
    ): Instant? {
        if (allSegments.isEmpty()) return null

        // Find the longest Asleep block — that's the main sleep block candidate
        val longestAsleep = allSegments
            .filter { it.kind == SleepSegmentKind.Asleep }
            .maxByOrNull { it.endAt.toEpochMilli() - it.startAt.toEpochMilli() }
            ?: return null

        // sleepOnsetAt = start of the longest Asleep block
        // (it's the point of quiet after the last real use before the main sleep period)
        return longestAsleep.startAt
    }

    /**
     * Data class returned by [detectDefinitiveWake].
     */
    private data class DefinitiveWakeResult(
        val definitiveWakeAt: Instant?,
        val cappedAtWindow: Boolean,
    )

    /**
     * Detects the definitive wakeup: the last AwakeUse episode that:
     * (a) starts at or after targetWakeAt,
     * (b) has duration >= definitiveWakeMinMinutes,
     * (c) is not followed by an Asleep block >= returnToSleepMinMinutes.
     *
     * Falls back to the last AwakeUse if none qualifies after target.
     * If no candidate at all → window end (capped, Ambiguous).
     */
    private fun detectDefinitiveWake(
        awakeEpisodes: List<SleepSegment>,
        target: SleepTargetWindow,
        wakeDate: LocalDate,
        windowEnd: Long,
        params: InterpretationParams,
    ): DefinitiveWakeResult {
        val targetWakeMs = targetWakeEpochMillis(target, wakeDate)
        val definitiveMinMs = params.definitiveWakeMinMinutes * 60_000L
        val returnToSleepMinMs = params.returnToSleepMinMinutes * 60_000L

        // Look for the last qualifying episode
        val candidatesAfterTarget = awakeEpisodes.filter { ep ->
            val epStartMs = ep.startAt.toEpochMilli()
            val epDurationMs = ep.endAt.toEpochMilli() - epStartMs
            epStartMs >= targetWakeMs && epDurationMs >= definitiveMinMs
        }

        // From candidates, pick the last one that is not followed by a significant Asleep block
        val candidate = candidatesAfterTarget.lastOrNull { ep ->
            val epEndMs = ep.endAt.toEpochMilli()
            val nextSleepMs = awakeEpisodes
                .filter { it.startAt.toEpochMilli() > epEndMs }
                .minByOrNull { it.startAt.toEpochMilli() }
                ?.startAt?.toEpochMilli()
            // If there's no later awake episode, or the gap to next awake < returnToSleepMinMs
            nextSleepMs == null || (nextSleepMs - epEndMs) < returnToSleepMinMs
        }

        return if (candidate != null) {
            DefinitiveWakeResult(
                definitiveWakeAt = candidate.startAt,
                cappedAtWindow = false,
            )
        } else {
            // Fall back: last awake episode of any kind
            val lastEpisode = awakeEpisodes.lastOrNull()
            if (lastEpisode != null && lastEpisode.startAt.toEpochMilli() >= targetWakeMs) {
                DefinitiveWakeResult(
                    definitiveWakeAt = lastEpisode.startAt,
                    cappedAtWindow = false,
                )
            } else {
                // No qualifying episode → cap at window end, mark as capped
                DefinitiveWakeResult(
                    definitiveWakeAt = Instant.ofEpochMilli(windowEnd),
                    cappedAtWindow = true,
                )
            }
        }
    }

    /**
     * Selects the segments that belong to the main sleep block, discarding naps
     * that are far from the configured objective window.
     *
     * Algorithm (design §1.5):
     * 1. Group Asleep segments into sleep blocks (separated by AwakeUse > napSeparationMillis).
     * 2. For each block, measure overlap/closeness to the target window.
     * 3. Winner = longest block that overlaps or is within napAnchorWindowMinutes of the target.
     * 4. If none overlaps: winner = longest block overall.
     * 5. Exclude all other isolated blocks (naps).
     *
     * Returns segments from sleepOnsetAt to definitiveWakeAt, keeping only the main block.
     */
    private fun selectMainSleepBlock(
        allSegments: List<SleepSegment>,
        target: SleepTargetWindow,
        wakeDate: LocalDate,
        params: InterpretationParams,
        definitiveWakeAt: Instant?,
    ): List<SleepSegment> {
        if (allSegments.isEmpty()) return emptyList()

        val targetOnsetMs = targetOnsetEpochMillis(target, wakeDate)
        val targetWakeMs = targetWakeEpochMillis(target, wakeDate)
        val napAnchorMs = params.napAnchorWindowMinutes * 60_000L
        val napSeparationMs = params.napSeparationMillis

        // Group contiguous Asleep segments into blocks separated by long AwakeUse gaps
        val sleepBlocks = groupSleepBlocks(allSegments, napSeparationMs)

        if (sleepBlocks.size <= 1) {
            // Only one block — it's the main block regardless
            return allSegments
        }

        // Score each block by closeness to the target window
        data class BlockScore(val block: List<SleepSegment>, val score: Long, val duration: Long)

        val scored = sleepBlocks.map { block ->
            val blockStart = block.minOf { it.startAt.toEpochMilli() }
            val blockEnd = block.maxOf { it.endAt.toEpochMilli() }
            val blockDuration = blockEnd - blockStart

            // Closeness: how much does the block overlap with or approach [targetOnset, targetWake]?
            val overlapStart = maxOf(blockStart, targetOnsetMs)
            val overlapEnd = minOf(blockEnd, targetWakeMs)
            val overlap = maxOf(0L, overlapEnd - overlapStart)

            // Distance from closest endpoint to target window
            val distanceToTarget = if (overlap > 0) 0L else minOf(
                abs(blockStart - targetOnsetMs),
                abs(blockEnd - targetWakeMs),
                abs(blockStart - targetWakeMs),
                abs(blockEnd - targetOnsetMs),
            )

            BlockScore(
                block = block,
                score = if (overlap > 0 || distanceToTarget <= napAnchorMs) blockDuration else 0L,
                duration = blockDuration,
            )
        }

        // Winner: longest among blocks that overlap/are close; fallback = longest overall
        val winner = scored.filter { it.score > 0 }.maxByOrNull { it.duration }
            ?: scored.maxByOrNull { it.duration }
            ?: return allSegments

        // Return only segments from the winning block (and AwakeUse episodes interspersed)
        val winnerStart = winner.block.minOf { it.startAt.toEpochMilli() }
        val winnerEnd = definitiveWakeAt?.toEpochMilli()
            ?: winner.block.maxOf { it.endAt.toEpochMilli() }

        return allSegments.filter { seg ->
            val segStart = seg.startAt.toEpochMilli()
            val segEnd = seg.endAt.toEpochMilli()
            segStart >= winnerStart && segEnd <= winnerEnd + 1
        }
    }

    /**
     * Groups Asleep segments into distinct sleep blocks.
     * Two Asleep segments belong to the same block if the AwakeUse gap between them
     * is shorter than [napSeparationMs].
     */
    private fun groupSleepBlocks(
        allSegments: List<SleepSegment>,
        napSeparationMs: Long,
    ): List<List<SleepSegment>> {
        val asleepSegments = allSegments.filter { it.kind == SleepSegmentKind.Asleep }
        if (asleepSegments.isEmpty()) return emptyList()

        val blocks = mutableListOf<MutableList<SleepSegment>>()
        var currentBlock = mutableListOf(asleepSegments.first())

        for (i in 1 until asleepSegments.size) {
            val prev = asleepSegments[i - 1]
            val curr = asleepSegments[i]
            val gap = curr.startAt.toEpochMilli() - prev.endAt.toEpochMilli()
            if (gap <= napSeparationMs) {
                currentBlock.add(curr)
            } else {
                blocks.add(currentBlock)
                currentBlock = mutableListOf(curr)
            }
        }
        blocks.add(currentBlock)
        return blocks
    }

    /**
     * Assigns confidence level based on signal quality:
     *
     * - High: clean onset and wake detected, no capping.
     * - NoData: no real-use episodes at all (handled earlier in interpret()).
     * - Ambiguous: signal exists but contradictory (capped at window, no clear onset, etc.).
     *
     * "Poca señal" (teléfono quieto) = High, NOT NoData (design §1.7 golden rule).
     * Only genuinely contradictory signal → Ambiguous.
     */
    private fun assignConfidence(
        awakeEpisodes: List<SleepSegment>,
        sleepOnsetAt: Instant?,
        definitiveWakeAt: Instant?,
        cappedAtWindow: Boolean,
    ): SleepConfidence {
        return when {
            // No real-use at all: handled by early return in interpret() as NoData/High
            awakeEpisodes.isEmpty() -> SleepConfidence.High

            // Onset and wake detected cleanly, not capped at window boundary
            sleepOnsetAt != null && definitiveWakeAt != null && !cappedAtWindow ->
                SleepConfidence.High

            // Night closed at 12:00 safety cap — signal was ambiguous
            cappedAtWindow -> SleepConfidence.Ambiguous

            // Has episodes but no clean onset (very fragmented signal)
            sleepOnsetAt == null -> SleepConfidence.Ambiguous

            // Default: treat as ambiguous when pattern is unclear
            else -> SleepConfidence.Ambiguous
        }
    }
}
