package dev.panopt.autonomia.domain.scoring

/**
 * Applies "special" layer modifiers (sleep for Cuerpo, sobriety for Conducta).
 *
 * ADR-3 / Bug §10 fix: when sleepScore == null (NoData), Cuerpo is computed
 * WITHOUT the sleep term — re-normalized to baseWithoutSpecial only. This
 * avoids the old `sleepScore ?: 0f` coercion that fabricated a zero and sank
 * Cuerpo to 30% of base even when no sleep data existed.
 *
 * Rule: absence of data ≠ evidence of poor sleep. When sleep is absent, Cuerpo
 * is the full body-base score; it is NOT penalized.
 */
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
                if (sleepScore == null) {
                    // NoData: re-normalize — Cuerpo = base without sleep term (ADR-3)
                    baseWithoutSpecial
                } else {
                    (1f - ScoringConstants.SLEEP_WEIGHT_IN_BODY) * baseWithoutSpecial +
                        ScoringConstants.SLEEP_WEIGHT_IN_BODY * sleepScore
                }
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
                if (sleepScore == null) {
                    // NoData: re-normalize — Cuerpo = base without sleep term (ADR-3)
                    baseWithPositiveMargin
                } else {
                    (1f - ScoringConstants.SLEEP_WEIGHT_IN_BODY) * baseWithPositiveMargin +
                        ScoringConstants.SLEEP_WEIGHT_IN_BODY * sleepScore
                }
            }
            layerId == ScoringConstants.CONDUCT_LAYER_ID && hasActiveSobriety -> {
                val sobriety = sobrietyScore ?: 0f
                (1f - ScoringConstants.SOBRIETY_WEIGHT_IN_CONDUCT) * baseWithPositiveMargin +
                    ScoringConstants.SOBRIETY_WEIGHT_IN_CONDUCT * sobriety
            }
            else -> baseWithPositiveMargin
        }.coerceIn(0f, 1.20f)
}
