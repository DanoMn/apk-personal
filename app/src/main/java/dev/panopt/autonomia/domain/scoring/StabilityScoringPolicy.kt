package dev.panopt.autonomia.domain.scoring

internal object StabilityScoringPolicy {
    private const val REQUIRED_PREVIOUS_WEEKS = 5

    fun evaluate(
        currentWeekStart: String,
        currentWeeklyBaseScore: Float,
        history: List<WeeklyScoreHistoryEntry>,
    ): StabilityEvaluation {
        val previousWeeks = history
            .filter { entry ->
                entry.scoringVersion == WeeklyScoreSnapshotConstants.SCORING_VERSION &&
                    entry.weekStart != currentWeekStart
            }
            .sortedByDescending { it.weekStart }
            .take(REQUIRED_PREVIOUS_WEEKS)

        if (previousWeeks.size < REQUIRED_PREVIOUS_WEEKS) {
            return StabilityEvaluation(
                stabilityScore = null,
                evaluatedWeeks = previousWeeks.size + 1,
                hasTemporalMemory = false,
            )
        }

        val scores = previousWeeks.map { it.weeklyBaseScore } + currentWeeklyBaseScore
        val average = scores.averageOrZero()
        val worst = scores.minOrNull() ?: 0f
        val stabilityScore = (
            ScoringConstants.WEEKLY_AVERAGE_WEIGHT * average +
                ScoringConstants.WEEKLY_WORST_WEIGHT * worst
            ).coerceIn(0f, 1f)

        return StabilityEvaluation(
            stabilityScore = stabilityScore,
            evaluatedWeeks = scores.size,
            hasTemporalMemory = true,
        )
    }
}
