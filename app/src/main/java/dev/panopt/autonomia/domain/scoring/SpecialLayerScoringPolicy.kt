package dev.panopt.autonomia.domain.scoring

internal object SpecialLayerScoringPolicy {
    fun baseScore(
        layerId: String,
        baseWithoutSpecial: Float,
        sleepScore: Float?,
        sobrietyScore: Float?,
        hasActiveSobriety: Boolean,
    ): Float =
        when {
            layerId == ScoringConstants.BODY_LAYER_ID -> {
                val sleep = sleepScore ?: 0f
                (1f - ScoringConstants.SLEEP_WEIGHT_IN_BODY) * baseWithoutSpecial +
                    ScoringConstants.SLEEP_WEIGHT_IN_BODY * sleep
            }
            layerId == ScoringConstants.CONDUCT_LAYER_ID && hasActiveSobriety -> {
                val sobriety = sobrietyScore ?: 0f
                (1f - ScoringConstants.SOBRIETY_WEIGHT_IN_CONDUCT) * baseWithoutSpecial +
                    ScoringConstants.SOBRIETY_WEIGHT_IN_CONDUCT * sobriety
            }
            else -> baseWithoutSpecial
        }.coerceIn(0f, 1f)

    fun rawScore(
        layerId: String,
        baseWithPositiveMargin: Float,
        sleepScore: Float?,
        sobrietyScore: Float?,
        hasActiveSobriety: Boolean,
    ): Float =
        when {
            layerId == ScoringConstants.BODY_LAYER_ID -> {
                val sleep = sleepScore ?: 0f
                ((1f - ScoringConstants.SLEEP_WEIGHT_IN_BODY) * baseWithPositiveMargin +
                    ScoringConstants.SLEEP_WEIGHT_IN_BODY * sleep)
            }
            layerId == ScoringConstants.CONDUCT_LAYER_ID && hasActiveSobriety -> {
                val sobriety = sobrietyScore ?: 0f
                ((1f - ScoringConstants.SOBRIETY_WEIGHT_IN_CONDUCT) * baseWithPositiveMargin +
                    ScoringConstants.SOBRIETY_WEIGHT_IN_CONDUCT * sobriety)
            }
            else -> baseWithPositiveMargin
        }.coerceIn(0f, 1.20f)
}
