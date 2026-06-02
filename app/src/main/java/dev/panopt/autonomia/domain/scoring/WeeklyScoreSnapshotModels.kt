package dev.panopt.autonomia.domain.scoring

import java.time.LocalDate

data class WeeklyScoreSnapshotInput(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val calculatedAt: Long,
    val scoreInput: ScoreInput,
    val scoreReport: ScoreReport,
    val scoringVersion: String = WeeklyScoreSnapshotConstants.SCORING_VERSION,
)

data class WeeklyScoreSnapshotDraft(
    val weekStart: String,
    val weekEnd: String,
    val scoringVersion: String,
    val calculatedAt: Long,
    val configHash: String,
    val factsHash: String,
    val weeklyBaseScore: Float,
    val weeklyScore: Float,
    val stabilityScore: Float?,
    val stabilityWeeks: Int,
    val state: String,
    val visibleScore: Int,
    val worstLayerId: String?,
    val layerSummariesJson: String,
    val reasonsJson: String,
)

object WeeklyScoreSnapshotConstants {
    // v1 (2026-06-01): cap de estado por ausencia de registro de sueño (árbol §16.7).
    // El estado se topea en Motion sin sueño; invalida snapshots v0 (regla de estado distinta).
    const val SCORING_VERSION = "weekly-base-v1"
}
