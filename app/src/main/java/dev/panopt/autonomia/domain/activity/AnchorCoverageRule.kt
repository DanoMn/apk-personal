package dev.panopt.autonomia.domain.activity

import dev.panopt.autonomia.domain.scoring.ScoringConstants

/** Referencia mínima de un ancla activa para evaluar cobertura: su id y la capa a la que pertenece. */
data class AnchorRef(val anchorId: String, val layerId: String)

/**
 * Regla canónica de **cobertura de anclas** — FUENTE ÚNICA del umbral del motor.
 *
 * El motor exige ≥ [minLayers] capas distintas con ≥1 ancla para emitir estado (deja de ser
 * `NoData`, §7.4 del árbol de scoring; gate en `ScoreEngine`/[ScoringConstants]). Esta regla
 * centraliza ese umbral y la cuenta de capas-con-ancla para que NINGÚN consumidor lo duplique:
 *
 * - **onboarding** lo usa para "no avanzar hasta tener cobertura" (`OnboardingAnchorsRule.canAdvance`);
 * - **configuración** lo usa como CANDADO para "no quitar un ancla si dejaría la app sin cobertura"
 *   ([canRemoveAnchor]).
 *
 * Referencia [ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR] para que el umbral nunca diverja.
 * Dominio puro: sin Room ni Compose.
 */
object AnchorCoverageRule {

    /** Mínimo de capas distintas con ancla (espejo del umbral del motor). */
    val minLayers: Int = ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR

    /** Capas distintas que tienen al menos un ancla. */
    fun distinctLayersWithAnchor(anchorLayerIds: List<String>): Int =
        anchorLayerIds.toSet().size

    /** true cuando hay anclas en al menos [minLayers] capas distintas. */
    fun meetsMinimum(anchorLayerIds: List<String>): Boolean =
        distinctLayersWithAnchor(anchorLayerIds) >= minLayers

    /**
     * CANDADO: ¿se puede quitar/desactivar el ancla [anchorIdToRemove] **sin** caer bajo el mínimo
     * de capas con ancla? Simula la remoción sobre [activeAnchors] (las anclas activas con su capa)
     * y verifica que las capas con ancla restantes sigan cumpliendo el mínimo. Quitar un ancla cuya
     * capa conserva otra ancla NO reduce la cobertura; quitar la última de su capa, sí.
     */
    fun canRemoveAnchor(activeAnchors: List<AnchorRef>, anchorIdToRemove: String): Boolean {
        val remainingLayers = activeAnchors
            .filterNot { it.anchorId == anchorIdToRemove }
            .map { it.layerId }
        return meetsMinimum(remainingLayers)
    }

    /**
     * Decisión del CANDADO para operaciones de ciclo de vida (eliminar / archivar) sobre
     * [activityId], expresada como [RemoveAnchorResult] para que el Repository delegue sin
     * conocer el umbral.
     *
     * El candado SOLO aplica cuando [isActiveAnchor] es `true`: un soporte, una task o una
     * actividad no-ancla no afectan la cobertura de capas, así que su operación **siempre**
     * procede ([RemoveAnchorResult.Removed]) aunque el sistema esté justo en el mínimo. Para un
     * ancla activa, se consulta [canRemoveAnchor]: si su remoción bajaría del mínimo de capas con
     * ancla, devuelve [RemoveAnchorResult.BlockedByMinimum] (no se debe tocar nada).
     */
    fun resolveAnchorOperation(
        activeAnchors: List<AnchorRef>,
        activityId: String,
        isActiveAnchor: Boolean,
    ): RemoveAnchorResult {
        if (!isActiveAnchor) return RemoveAnchorResult.Removed
        return if (canRemoveAnchor(activeAnchors, activityId)) {
            RemoveAnchorResult.Removed
        } else {
            RemoveAnchorResult.BlockedByMinimum
        }
    }
}
