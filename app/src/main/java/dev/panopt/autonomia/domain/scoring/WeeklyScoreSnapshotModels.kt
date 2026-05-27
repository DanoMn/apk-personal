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
    const val SCORING_VERSION = "weekly-base-v0"
}
