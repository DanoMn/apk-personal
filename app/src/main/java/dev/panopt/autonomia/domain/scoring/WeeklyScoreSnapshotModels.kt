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
    // core-v2 (2026-06-16, PR-F): motor de núcleo v1 (pesos puros). El significado de
    // weeklyBaseScore/weeklyScore cambió a ESTADO ∈ [0,1.5] (antes 0–1) y visibleScore al rango
    // [650,1100]; invalida snapshots weekly-base-v1 (convención de score/banda distinta).
    // weekly-base-v1 (2026-06-01, legacy): cap de estado por ausencia de sueño (árbol §16.7).
    const val SCORING_VERSION = "core-v2"
}
