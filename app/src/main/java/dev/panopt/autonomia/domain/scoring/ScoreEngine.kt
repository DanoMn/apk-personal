package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.ScoreState

object ScoreEngine {
    fun calculate(input: ScoreInput): ScoreReport {
        val activeLayers = input.layers.filter { it.active }.sortedBy { it.sortOrder }
        // Gate de configuración mínima (árbol §7.4): se exigen ≥3 capas activas con
        // ≥1 ancla. Sin datos suficientes (sin capas, sin hechos, o config insuficiente) → NoData.
        if (activeLayers.isEmpty() ||
            !WeeklyScoringContextBuilder.hasAnyFact(input) ||
            activeLayersWithAnchor(input, activeLayers) < ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR
        ) {
            return noDataReport(activeLayers)
        }

        val context = WeeklyScoringContextBuilder.build(input)
        val layerResults = context.activeLayers.map { layer ->
            LayerScoringPolicy.evaluate(
                layer = layer,
                activities = context.visibleActivities.filter { it.layerId == layer.id },
                context = context,
                completedTasks = context.completedTasksByLayer[layer.id].orEmpty(),
            )
        }
        val layerEvaluations = layerResults.map { it.evaluation }
        val weeklySummary = WeeklyScorePolicy.summarize(layerEvaluations)
        val visibleScore = VisibleScorePolicy.visibleScore(weeklySummary.weeklyBaseScore)
        val stability = StabilityScoringPolicy.evaluate(
            currentWeekStart = context.weekStart.toString(),
            currentWeeklyBaseScore = weeklySummary.weeklyBaseScore,
            history = input.weeklyHistory,
        )

        val previousState = input.weeklyHistory
            .filter { it.scoringVersion == WeeklyScoreSnapshotConstants.SCORING_VERSION &&
                      it.weekStart != context.weekStart.toString() }
            .maxByOrNull { it.weekStart }
            ?.state

        return ScoreReport(
            state = BaseStatePolicy.stateFor(
                weeklyBaseScore = weeklySummary.weeklyBaseScore,
                worstLayerScore = weeklySummary.worstLayerScore,
                stability = stability,
                previousState = previousState,
                hasSleepData = context.sleepScore != null,
            ),
            visibleScore = visibleScore,
            baseScore = visibleScore,
            goalBonus = 0,
            progress = visibleScore / 1000f,
            layerScores = layerEvaluations.map { it.toLayerScore() },
            featureContributions = layerResults.flatMap { it.contributions },
            gates = emptyList(),
            weeklyBaseScore = weeklySummary.weeklyBaseScore,
            weeklyScore = weeklySummary.weeklyBaseScore,
            averageLayerScore = weeklySummary.averageLayerScore,
            worstLayerScore = weeklySummary.worstLayerScore,
            worstLayerId = weeklySummary.worstLayer?.layerId,
            reasons = ScoreReasonPolicy.build(
                layerEvaluations = layerEvaluations,
                hasActiveSobriety = context.activeSobrietyTracks.isNotEmpty(),
                sleepScore = context.sleepScore,
            ),
            stabilityScore = stability.stabilityScore,
            stabilityWeeks = stability.evaluatedWeeks,
        )
    }

    /** Cuenta capas activas que tienen al menos 1 ancla configurada (activa, no archivada). */
    private fun activeLayersWithAnchor(input: ScoreInput, activeLayers: List<Layer>): Int {
        val layerIdsWithAnchor = input.activities
            .filter { it.active && !it.archived && it.activityType == ActivitySurface.Anchor }
            .map { it.layerId }
            .toSet()
        return activeLayers.count { it.id in layerIdsWithAnchor }
    }

    private fun noDataReport(activeLayers: List<Layer>): ScoreReport =
        ScoreReport(
            state = ScoreState.NoData,
            visibleScore = null,
            baseScore = null,
            goalBonus = 0,
            progress = 0f,
            layerScores = activeLayers.map {
                LayerScore(layerId = it.id, name = it.name, score = 0f, configured = false)
            },
            featureContributions = emptyList(),
            gates = emptyList(),
        )
}
