package dev.panopt.autonomia.domain.scoring

object BuildWeeklyScoreSnapshotUseCase {
    operator fun invoke(input: WeeklyScoreSnapshotInput): WeeklyScoreSnapshotDraft =
        WeeklyScoreSnapshotDraft(
            weekStart = input.weekStart.toString(),
            weekEnd = input.weekEnd.toString(),
            scoringVersion = input.scoringVersion,
            calculatedAt = input.calculatedAt,
            configHash = ScoreSnapshotHashPolicy.configHash(input.scoreInput),
            factsHash = ScoreSnapshotHashPolicy.factsHash(input.scoreInput),
            weeklyBaseScore = input.scoreReport.weeklyBaseScore,
            weeklyScore = input.scoreReport.weeklyScore,
            stabilityScore = null,
            state = input.scoreReport.state.name,
            visibleScore = input.scoreReport.visibleScore ?: 0,
            worstLayerId = input.scoreReport.worstLayerId,
            layerSummariesJson = ScoreSnapshotJson.layersJson(input.scoreReport.layerScores),
            reasonsJson = ScoreSnapshotJson.reasonsJson(input.scoreReport.reasons),
        )
}
