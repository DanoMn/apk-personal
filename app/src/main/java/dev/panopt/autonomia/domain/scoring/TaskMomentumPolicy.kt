package dev.panopt.autonomia.domain.scoring

import kotlin.math.exp

internal object TaskMomentumPolicy {
    fun bonus(completedLayerTasks: Int): Float {
        if (completedLayerTasks <= 0) return 0f
        return (ScoringConstants.TASK_MOMENTUM_MAX * (1f - exp(-completedLayerTasks.toFloat() / 2f)))
            .coerceIn(0f, ScoringConstants.TASK_MOMENTUM_MAX)
    }
}
