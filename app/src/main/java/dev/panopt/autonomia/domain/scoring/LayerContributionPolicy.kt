package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.domain.activity.ActivityDefinition

internal object LayerContributionPolicy {
    fun build(
        layer: Layer,
        anchors: List<ActivityDefinition>,
        supports: List<ActivityDefinition>,
        completedTasks: List<Task>,
        anchorLayerScore: Float?,
        supportLayerScore: Float?,
        taskMomentumBonus: Float,
        sleepScore: Float?,
        sobrietyScore: Float?,
        hasActiveSobriety: Boolean,
    ): List<FeatureContribution> =
        listOfNotNull(
            contribution(ScoreFeature.Anchor, layer, "Anclas", anchorLayerScore, anchors.isNotEmpty()),
            contribution(ScoreFeature.Support, layer, "Soportes", supportLayerScore, supports.isNotEmpty()),
            contribution(
                feature = ScoreFeature.Task,
                layer = layer,
                label = "Pendientes",
                value = taskMomentumBonus,
                enabled = completedTasks.isNotEmpty(),
                maxValue = ScoringConstants.TASK_MOMENTUM_MAX,
            ),
            contribution(
                feature = ScoreFeature.Sleep,
                layer = layer,
                label = "Sueno",
                value = sleepScore,
                enabled = layer.id == ScoringConstants.BODY_LAYER_ID && sleepScore != null,
            ),
            contribution(
                feature = ScoreFeature.Sobriety,
                layer = layer,
                label = "Sobriedad",
                value = sobrietyScore,
                enabled = layer.id == ScoringConstants.CONDUCT_LAYER_ID && hasActiveSobriety,
            ),
        )

    private fun contribution(
        feature: ScoreFeature,
        layer: Layer,
        label: String,
        value: Float?,
        enabled: Boolean,
        maxValue: Float = 1f,
    ): FeatureContribution? {
        if (!enabled || value == null || maxValue <= 0f) return null
        return FeatureContribution(
            feature = feature,
            layerId = layer.id,
            label = label,
            value = value.coerceIn(0f, maxValue),
            maxValue = maxValue,
        )
    }
}
