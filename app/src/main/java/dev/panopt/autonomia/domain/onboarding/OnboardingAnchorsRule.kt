package dev.panopt.autonomia.domain.onboarding

import dev.panopt.autonomia.domain.scoring.ScoringConstants

/**
 * Regla de avance del Bloque Anclas del onboarding.
 *
 * El motor exige al menos [ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR] capas
 * distintas con ≥1 ancla para salir de "Sin datos" (§7.4 del árbol de scoring). El
 * onboarding hace cumplir esa compuerta: no se avanza del Bloque Anclas hasta tenerla.
 * Referencia la constante del motor para que ambos umbrales no diverjan.
 */
object OnboardingAnchorsRule {

    /** Mínimo de capas distintas con ancla para avanzar (espejo del umbral del motor). */
    val minLayers: Int = ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR

    /** Capas distintas que tienen al menos un ancla configurada. */
    fun distinctLayersWithAnchor(anchorLayerIds: List<String>): Int =
        anchorLayerIds.toSet().size

    /** true cuando hay anclas en al menos [ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR] capas distintas. */
    fun canAdvance(anchorLayerIds: List<String>): Boolean =
        distinctLayersWithAnchor(anchorLayerIds) >= ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR
}
