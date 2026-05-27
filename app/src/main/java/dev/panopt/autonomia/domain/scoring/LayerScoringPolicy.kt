package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.domain.activity.ActivityDefinition

internal object LayerScoringPolicy {
    fun evaluate(
        layer: Layer,
        activities: List<ActivityDefinition>,
        context: WeeklyScoringContext,
        completedTasks: List<Task>,
    ): LayerScoringResult {
        val anchors = activities.filter { it.activityType == ActivitySurface.Anchor }
        val supports = activities.filter { it.activityType == ActivitySurface.Support }
        val anchorEvaluations = anchors.map { activity ->
            AnchorScoringPolicy.evaluate(activity, context.weeklyLogsByActivity[activity.id].orEmpty())
        }
        val anchorLayerScore = anchorEvaluations.map { it.baseScore }.averageOrNull()
        val anchorSurplusBonus = anchorEvaluations.map { it.surplusBonus }.averageOrZero()
        val supportLayerScore = SupportScoringPolicy.evaluate(supports, context.weeklyLogsByActivity, context.weekDates)
        val baseWithoutSpecial = baseWithoutSpecial(anchorLayerScore, supportLayerScore)
        val taskMomentumBonus = TaskMomentumPolicy.bonus(completedTasks.size)
        val baseWithPositiveMargin = (baseWithoutSpecial + anchorSurplusBonus + taskMomentumBonus)
            .coerceIn(0f, 1.20f)
        val hasActiveSobriety = layer.id == ScoringConstants.CONDUCT_LAYER_ID &&
            context.activeSobrietyTracks.isNotEmpty()
        val sleepScore = if (layer.id == ScoringConstants.BODY_LAYER_ID) context.sleepScore else null
        val sobrietyScore = if (layer.id == ScoringConstants.CONDUCT_LAYER_ID) context.sobrietyScore else null

        val layerBase = SpecialLayerScoringPolicy.baseScore(
            layerId = layer.id,
            baseWithoutSpecial = baseWithoutSpecial,
            sleepScore = sleepScore,
            sobrietyScore = sobrietyScore,
            hasActiveSobriety = hasActiveSobriety,
        )
        val rawLayerScore = SpecialLayerScoringPolicy.rawScore(
            layerId = layer.id,
            baseWithPositiveMargin = baseWithPositiveMargin,
            sleepScore = sleepScore,
            sobrietyScore = sobrietyScore,
            hasActiveSobriety = hasActiveSobriety,
        )

        val evaluation = LayerEvaluation(
            layerId = layer.id,
            name = layer.name,
            configured = anchors.isNotEmpty() ||
                supports.isNotEmpty() ||
                completedTasks.isNotEmpty() ||
                sleepScore != null ||
                hasActiveSobriety,
            baseScore = layerBase,
            rawScore = rawLayerScore,
            anchorScore = anchorLayerScore,
            supportScore = supportLayerScore,
            anchorSurplusBonus = anchorSurplusBonus,
            taskMomentumBonus = taskMomentumBonus,
            sleepScore = sleepScore,
            sobrietyScore = sobrietyScore,
        )
        return LayerScoringResult(
            evaluation = evaluation,
            contributions = LayerContributionPolicy.build(
                layer = layer,
                anchors = anchors,
                supports = supports,
                completedTasks = completedTasks,
                anchorLayerScore = anchorLayerScore,
                supportLayerScore = supportLayerScore,
                taskMomentumBonus = taskMomentumBonus,
                sleepScore = sleepScore,
                sobrietyScore = sobrietyScore,
                hasActiveSobriety = hasActiveSobriety,
            ),
        )
    }

    private fun baseWithoutSpecial(anchorLayerScore: Float?, supportLayerScore: Float?): Float =
        when {
            anchorLayerScore == null && supportLayerScore == null -> 0f
            supportLayerScore == null -> anchorLayerScore ?: 0f
            else -> ScoringConstants.ANCHOR_WITH_SUPPORT_WEIGHT * (anchorLayerScore ?: 0f) +
                ScoringConstants.SUPPORT_WEIGHT * supportLayerScore
        }.coerceIn(0f, 1f)
}
