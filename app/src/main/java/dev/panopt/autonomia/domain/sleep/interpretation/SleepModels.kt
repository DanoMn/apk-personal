package dev.panopt.autonomia.domain.sleep.interpretation

import java.time.Instant
import java.time.LocalDate

/**
 * A discrete activity period during a sleep night.
 * The sleep domain uses two kinds: quiet Asleep blocks and active AwakeUse episodes.
 */
data class SleepSegment(
    val startAt: Instant,
    val endAt: Instant,
    val kind: SleepSegmentKind,
)

/** Kind of activity within a sleep segment. */
enum class SleepSegmentKind {
    /** Phone was quiet — inferred sleep period. */
    Asleep,

    /** Real device use detected (USER_INTERACTION or APP_FOREGROUND). */
    AwakeUse,
}

/**
 * Confidence spectrum for a detected NightTimeline.
 * "Poca señal" (teléfono quieto) = High, NOT NoData. NoData = total absence of basis.
 */
enum class SleepConfidence {
    /** Onset and wake detected cleanly; main sleep block is unambiguous. */
    High,

    /**
     * Signal exists but is contradictory: onset unclear, definitiveWake fell to the
     * 12:00 safety cap, overlapping blocks, or API 26/27 proxy-only signal.
     * Sleep score is computed but attenuated by ambiguousConfidenceFactor.
     */
    Ambiguous,

    /**
     * Total absence of signal: no events in the detection window, or only noise
     * with no identifiable sleep block. sleepScore = null (not computed).
     */
    NoData,
}

/**
 * The interpreted timeline for one sleep night.
 * nightDate = the date the user woke up (date of the WAKE, not the date of sleep onset).
 * All Instant values are in epoch millis semantics; LocalDate is zone-local.
 */
data class NightTimeline(
    /** ISO date of wake-up (e.g., 2026-05-29 for a night that started 2026-05-28). */
    val nightDate: LocalDate,

    /** Alternating Asleep/AwakeUse segments, ordered by startAt. */
    val segments: List<SleepSegment>,

    /** Start of the main sleep block — null if NoData. */
    val sleepOnsetAt: Instant?,

    /** Start of the sustained wake episode that closes the night — null if NoData. */
    val definitiveWakeAt: Instant?,

    /** How much trust to assign to this timeline's measurements. */
    val confidence: SleepConfidence,
)
