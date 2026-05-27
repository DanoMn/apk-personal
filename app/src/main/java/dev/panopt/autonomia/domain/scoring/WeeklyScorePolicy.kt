package dev.panopt.autonomia.domain.scoring

internal object WeeklyScorePolicy {
    fun summarize(layerEvaluations: List<LayerEvaluation>): WeeklyScoreSummary {
        val averageLayerScore = layerEvaluations.map { it.rawScore }.averageOrZero()
        val worstLayer = layerEvaluations.minByOrNull { it.baseScore }
        val worstLayerScore = worstLayer?.baseScore ?: 0f
        val weeklyBaseScore = (
            ScoringConstants.WEEKLY_AVERAGE_WEIGHT * averageLayerScore +
                ScoringConstants.WEEKLY_WORST_WEIGHT * worstLayerScore
            ).coerceAtLeast(0f)
        return WeeklyScoreSummary(
            weeklyBaseScore = weeklyBaseScore,
            averageLayerScore = averageLayerScore,
            worstLayerScore = worstLayerScore,
            worstLayer = worstLayer,
        )
    }
}
