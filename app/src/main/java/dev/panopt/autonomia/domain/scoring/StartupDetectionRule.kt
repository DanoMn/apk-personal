package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.LocalDate

/**
 * Regla de DETECCIÓN de arranque de cuenta (`scoring-arranque-cuenta`, Lote 2). Dominio puro JVM.
 *
 * `true` si la cuenta está en ARRANQUE: su motor real todavía dice `NoData` porque sus anclas
 * están en período de gracia, PERO tiene la cobertura mínima de anclas configurada y NUNCA tuvo
 * un score real (todo su historial semanal es `NoData` o vacío). En ese caso el blackout `NoData`
 * se reemplaza, en presentación, por la barra de arranque (canal `DashboardState.startup`).
 *
 * El motor NUNCA se entera del arranque: el `ScoreReport` real sigue siendo `NoData`. Esto es solo
 * una decisión de presentación sobre cómo mostrar el período de gracia inicial.
 */
object StartupDetectionRule {

    /**
     * @param report veredicto REAL del motor ([ScoreEngine.calculate]); arranque solo aplica si es `NoData`.
     * @param activities config de actividades del usuario (catálogo resuelto a config).
     * @param layers capas activas/inactivas.
     * @param weeklyHistory historial semanal (ya fluye a `ScoreInput.weeklyHistory`).
     * @param today fecha de hoy (presente por simetría con el resto del dominio; el predicado de
     *   cobertura NO filtra la gracia, justamente para "ver" las anclas nuevas).
     * @return `true` si la cuenta está en arranque.
     */
    fun isStartup(
        report: ScoreReport,
        activities: List<ActivityDefinition>,
        layers: List<Layer>,
        weeklyHistory: List<WeeklyScoreHistoryEntry>,
        today: LocalDate,
        minLayersGate: Int = ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR,
    ): Boolean {
        // 1. Solo sobre NoData real: si el motor ya emitió un veredicto, no es arranque.
        if (report.state != ScoreState.NoData) return false

        // 2. Sin historial de score real: una sola semana con score real ⇒ la cuenta ya arrancó.
        if (weeklyHistory.any { it.state != ScoreState.NoData }) return false

        // 3. Cobertura mínima (el GATE manda): capas activas con ≥1 ancla configurada (activa, no
        //    archivada), SIN filtrar la gracia (justo lo que el arranque quiere ver). < MIN → NoData
        //    real ("configurá tu base"), NUNCA arranque.
        return activeLayersWithAnchor(activities, layers) >= minLayersGate
    }

    /**
     * Cuenta capas ACTIVAS con al menos 1 ancla configurada (activa, no archivada), sin filtrar la
     * gracia. Espejo de `ScoreEngine.activeLayersWithAnchor` pero ignorando el filtro de gracia.
     */
    private fun activeLayersWithAnchor(activities: List<ActivityDefinition>, layers: List<Layer>): Int {
        val layerIdsWithAnchor = activities
            .filter { it.active && !it.archived && it.activityType == ActivitySurface.Anchor }
            .map { it.layerId }
            .toSet()
        return layers.count { it.active && it.id in layerIdsWithAnchor }
    }
}
