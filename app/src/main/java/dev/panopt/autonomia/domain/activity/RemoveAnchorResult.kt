package dev.panopt.autonomia.domain.activity

/**
 * Resultado de quitar/eliminar un ancla — le dice a la UI qué pasó sin que conozca la regla de
 * cobertura. Mismo patrón que `SleepAutoModeResult`.
 *
 * La UI debe:
 *   - [Removed]          → reflejar que el ancla se quitó.
 *   - [BlockedByMinimum] → NO quitar nada; mostrar el mensaje compasivo (tono AGENTS.md): la app
 *     necesita al menos [AnchorCoverageRule.minLayers] capas con un ancla activa para acompañar.
 */
sealed interface RemoveAnchorResult {
    /** El ancla se quitó. */
    object Removed : RemoveAnchorResult

    /** Quitar este ancla dejaría menos de [AnchorCoverageRule.minLayers] capas con ancla: se bloquea. */
    object BlockedByMinimum : RemoveAnchorResult
}
