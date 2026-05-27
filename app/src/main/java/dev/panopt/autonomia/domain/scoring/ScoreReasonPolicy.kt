package dev.panopt.autonomia.domain.scoring

internal object ScoreReasonPolicy {
    fun build(
        layerEvaluations: List<LayerEvaluation>,
        hasActiveSobriety: Boolean,
        sleepScore: Float?,
    ): List<String> {
        val reasons = mutableListOf<String>()
        val weakestLayer = layerEvaluations.minByOrNull { it.baseScore }
        if (weakestLayer != null && weakestLayer.baseScore < 0.60f) {
            reasons += "La capa mas baja es ${weakestLayer.name}."
        }
        if (sleepScore != null && sleepScore < 0.70f) {
            reasons += "El descanso bajo esta afectando Cuerpo."
        }
        val conduct = layerEvaluations.firstOrNull { it.layerId == ScoringConstants.CONDUCT_LAYER_ID }
        if (hasActiveSobriety && conduct?.sobrietyScore != null && conduct.sobrietyScore < 0.70f) {
            reasons += "Sobriedad esta reduciendo Conducta esta semana."
        }
        return reasons
    }
}
