package dev.panopt.autonomia.ui.anchors

import dev.panopt.autonomia.domain.activity.MAX_ANCHOR_WEEKLY_FREQUENCY
import dev.panopt.autonomia.domain.activity.MIN_ANCHOR_WEEKLY_FREQUENCY
import dev.panopt.autonomia.domain.activity.hasRequiredAnchorTargets
import dev.panopt.autonomia.domain.activity.normalizeAnchorWeeklyFrequencyTarget

internal enum class WeeklyFrequencyPreset(
    val count: Int,
    val label: String,
) {
    TwoPerWeek(2, "2 veces/semana"),
    ThreePerWeek(3, "3 veces/semana"),
    FourPerWeek(4, "4 veces/semana"),
    FivePerWeek(5, "5 veces/semana"),
    SixPerWeek(6, "6 veces/semana"),
    SevenPerWeek(7, "7 veces/semana");
}

internal val weeklyFrequencyPresets: List<WeeklyFrequencyPreset> =
    WeeklyFrequencyPreset.entries.toList()

internal enum class CommitmentDurationPreset(
    val months: Int?,
    val label: String,
) {
    Indefinite(null, "Indefinido"),
    ThreeMonths(3, "3 meses"),
    FiveMonths(5, "5 meses"),
    SevenMonths(7, "7 meses"),
    NineMonths(9, "9 meses"),
    ElevenMonths(11, "11 meses"),
    ThirteenMonths(13, "13 meses"),
    Custom(null, "Personalizado");
}

internal val commitmentDurationPresets: List<CommitmentDurationPreset> =
    CommitmentDurationPreset.entries.toList()

internal fun weeklyFrequencyTargetFromPreset(preset: WeeklyFrequencyPreset): Int =
    preset.count.coerceIn(MIN_ANCHOR_WEEKLY_FREQUENCY, MAX_ANCHOR_WEEKLY_FREQUENCY)

internal fun normalizeWeeklyFrequencyTarget(value: Int?): Int =
    normalizeAnchorWeeklyFrequencyTarget(value)

internal fun isValidAnchorTargetContract(
    sessionTargetMinutes: Int?,
    weeklyFrequencyTarget: Int?,
): Boolean =
    hasRequiredAnchorTargets(sessionTargetMinutes, weeklyFrequencyTarget)

internal fun commitmentDurationLabel(months: Int?): String =
    months?.let { "$it meses" } ?: "Indefinido"

internal fun normalizeCustomCommitmentMonths(input: String): Int? =
    input.toIntOrNull()?.coerceIn(1, 120)
