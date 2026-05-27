package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ScoreState
import kotlin.math.roundToInt

internal object VisibleScorePolicy {
    fun visibleScore(internalScore: Float): Int =
        (700 + internalScore.coerceIn(0f, 1f) * 300f).roundToInt().coerceIn(700, 1000)

    fun stateFor(visibleScore: Int): ScoreState =
        when {
            visibleScore < 750 -> ScoreState.Restoration
            visibleScore < 800 -> ScoreState.Attention
            visibleScore < 900 -> ScoreState.Motion
            else -> ScoreState.Plenitude
        }
}
