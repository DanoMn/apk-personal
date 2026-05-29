package dev.panopt.autonomia.domain.scoring

import kotlin.math.roundToInt

internal object VisibleScorePolicy {
    fun visibleScore(internalScore: Float): Int =
        (700 + internalScore.coerceIn(0f, 1f) * 300f).roundToInt().coerceIn(700, 1000)
}
