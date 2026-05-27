package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ScoreState

internal object BaseStatePolicy {
    fun stateFor(
        visibleScore: Int,
        weeklyBaseScore: Float,
        worstLayerScore: Float,
        stability: StabilityEvaluation,
    ): ScoreState =
        when {
            visibleScore < 750 -> ScoreState.Restoration
            visibleScore < 800 || worstLayerScore < 0.55f -> ScoreState.Attention
            stability.hasTemporalMemory &&
                weeklyBaseScore >= 0.90f &&
                worstLayerScore >= 0.80f &&
                (stability.stabilityScore ?: 0f) >= 0.90f -> ScoreState.Unbreakable
            weeklyBaseScore >= 0.85f && worstLayerScore >= 0.75f -> ScoreState.Plenitude
            else -> ScoreState.Motion
        }
}
