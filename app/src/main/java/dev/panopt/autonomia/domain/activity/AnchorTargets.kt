package dev.panopt.autonomia.domain.activity

internal const val MIN_ANCHOR_WEEKLY_FREQUENCY = 2
internal const val MAX_ANCHOR_WEEKLY_FREQUENCY = 7
internal const val DEFAULT_ANCHOR_WEEKLY_FREQUENCY = 3
internal const val MIN_ANCHOR_SESSION_MINUTES = 1
internal const val MAX_ANCHOR_SESSION_MINUTES = 900

internal fun normalizeAnchorWeeklyFrequencyTarget(value: Int?): Int =
    (value ?: DEFAULT_ANCHOR_WEEKLY_FREQUENCY)
        .coerceIn(MIN_ANCHOR_WEEKLY_FREQUENCY, MAX_ANCHOR_WEEKLY_FREQUENCY)

internal fun normalizeAnchorSessionTargetMinutes(value: Int?): Int =
    (value ?: MIN_ANCHOR_SESSION_MINUTES)
        .coerceIn(MIN_ANCHOR_SESSION_MINUTES, MAX_ANCHOR_SESSION_MINUTES)

internal fun hasRequiredAnchorTargets(
    sessionTargetMinutes: Int?,
    weeklyFrequencyTarget: Int?,
): Boolean =
    sessionTargetMinutes != null &&
        sessionTargetMinutes in MIN_ANCHOR_SESSION_MINUTES..MAX_ANCHOR_SESSION_MINUTES &&
        weeklyFrequencyTarget != null &&
        weeklyFrequencyTarget in MIN_ANCHOR_WEEKLY_FREQUENCY..MAX_ANCHOR_WEEKLY_FREQUENCY
