package dev.panopt.autonomia.ui.anchors

import dev.panopt.autonomia.TargetPeriod

/**
 * Shared goal preset enumeration used across the app for configuring
 * activity goal targets in "Mis anclas".
 */
enum class GoalPreset(val label: String) {
    None("Sin meta"),

    // Weekly presets
    TwoPerWeek("2×/sem"),
    ThreePerWeek("3×/sem"),
    FourPerWeek("4×/sem"),
    FivePerWeek("5×/sem"),
    SixPerWeek("6×/sem"),
    SevenPerWeek("7×/sem"),

    // Monthly presets
    TwoPerMonth("2×/mes"),
    ThreePerMonth("3×/mes"),
    FourPerMonth("4×/mes"),
    SixPerMonth("6×/mes"),
    EightPerMonth("8×/mes"),
    TenPerMonth("10×/mes"),

    Custom("Personalizada");

    /**
     * Returns [count, period] for non-custom presets.
     * - [None] → (null, null)
     * - Weekly presets → (n, [TargetPeriod.Week])
     * - Monthly presets → (n, [TargetPeriod.Month])
     * - [Custom] → (null, null) — handled via customCount/customPeriod fields
     */
    fun toCountAndPeriod(): Pair<Int?, TargetPeriod?> = when (this) {
        None -> null to null

        // Weekly
        TwoPerWeek -> 2 to TargetPeriod.Week
        ThreePerWeek -> 3 to TargetPeriod.Week
        FourPerWeek -> 4 to TargetPeriod.Week
        FivePerWeek -> 5 to TargetPeriod.Week
        SixPerWeek -> 6 to TargetPeriod.Week
        SevenPerWeek -> 7 to TargetPeriod.Week

        // Monthly
        TwoPerMonth -> 2 to TargetPeriod.Month
        ThreePerMonth -> 3 to TargetPeriod.Month
        FourPerMonth -> 4 to TargetPeriod.Month
        SixPerMonth -> 6 to TargetPeriod.Month
        EightPerMonth -> 8 to TargetPeriod.Month
        TenPerMonth -> 10 to TargetPeriod.Month

        Custom -> null to null
    }
}
