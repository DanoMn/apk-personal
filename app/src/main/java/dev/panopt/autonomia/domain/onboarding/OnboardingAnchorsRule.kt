package dev.panopt.autonomia.domain.onboarding

import dev.panopt.autonomia.domain.activity.AnchorCoverageRule

/**
 * Regla de avance del Bloque Anclas del onboarding.
 *
 * El motor exige al menos [AnchorCoverageRule.minLayers] capas distintas con ≥1 ancla para salir de
 * "Sin datos" (§7.4 del árbol de scoring). El onboarding hace cumplir esa compuerta: no se avanza del
 * Bloque Anclas hasta tenerla. **Delega en [AnchorCoverageRule]** (fuente única del umbral) para que el
 * onboarding y el candado de configuración nunca diverjan.
 */
object OnboardingAnchorsRule {

    /** Mínimo de capas distintas con ancla para avanzar (fuente única: [AnchorCoverageRule]). */
    val minLayers: Int = AnchorCoverageRule.minLayers

    /** Capas distintas que tienen al menos un ancla configurada. */
    fun distinctLayersWithAnchor(anchorLayerIds: List<String>): Int =
        AnchorCoverageRule.distinctLayersWithAnchor(anchorLayerIds)

    /** true cuando hay anclas en al menos [AnchorCoverageRule.minLayers] capas distintas. */
    fun canAdvance(anchorLayerIds: List<String>): Boolean =
        AnchorCoverageRule.meetsMinimum(anchorLayerIds)
}
