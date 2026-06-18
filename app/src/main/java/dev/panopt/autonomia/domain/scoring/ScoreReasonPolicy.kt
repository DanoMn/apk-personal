package dev.panopt.autonomia.domain.scoring

/**
 * Traduce las señales del reporte semanal a razones en lenguaje del usuario (lo que se renderiza en
 * la sección "Razones" de `ScoringScreen`). Dominio puro: no toca Room ni Compose.
 *
 * Reconstrucción adaptada al motor núcleo (PR-F borró la policy vieja). Las señales de entrada son
 * las que el motor nuevo SÍ produce (`base_eff` por capa, opt-in de sueño, opt-in de sobriedad),
 * pero se preservan los 3 mensajes y sus umbrales del modelo viejo:
 * - peor capa con `base_eff < 0.60`
 * - señal de sueño `M < 0.70`
 * - señal de sobriedad `M < 0.70` (solo si hay tracks de sobriedad activos)
 *
 * Tono: adulto compasivo (AGENTS.md) — sin "fallaste"/"deberías", solo una señal serena.
 */
internal object ScoreReasonPolicy {

    /** Umbral de "capa en pie": por debajo, la capa se nombra como la más baja. */
    private const val WORST_LAYER_THRESHOLD = 0.60

    /** Umbral del opt-in de sueño: por debajo, el descanso arrastra a Cuerpo. */
    private const val SLEEP_THRESHOLD = 0.70

    /** Umbral del opt-in de sobriedad: por debajo (con tracks activos), reduce Conducta. */
    private const val SOBRIETY_THRESHOLD = 0.70

    /**
     * @param worstLayerName nombre de la peor capa (de `activeLayers` por id), o `null` si no hay.
     * @param worstLayerBaseEff `base_eff ∈ [0,1]` de la peor capa, o `null`.
     * @param sleepSignal señal M de sueño ∈ [0,1] (opt-in NIVEL 4), o `null` si no hay sueño.
     * @param sobrietySignal señal M de sobriedad ∈ [0,1] (opt-in NIVEL 4), o `null`.
     * @param hasActiveSobriety si hay al menos un track de sobriedad activo en la ventana.
     */
    fun build(
        worstLayerName: String?,
        worstLayerBaseEff: Double?,
        sleepSignal: Double?,
        sobrietySignal: Double?,
        hasActiveSobriety: Boolean,
    ): List<String> {
        val reasons = mutableListOf<String>()

        if (worstLayerName != null &&
            worstLayerBaseEff != null &&
            worstLayerBaseEff < WORST_LAYER_THRESHOLD
        ) {
            reasons += "La capa más baja es $worstLayerName."
        }

        if (sleepSignal != null && sleepSignal < SLEEP_THRESHOLD) {
            reasons += "El descanso bajo está afectando Cuerpo."
        }

        if (hasActiveSobriety && sobrietySignal != null && sobrietySignal < SOBRIETY_THRESHOLD) {
            reasons += "Sobriedad está reduciendo Conducta esta semana."
        }

        return reasons
    }
}
