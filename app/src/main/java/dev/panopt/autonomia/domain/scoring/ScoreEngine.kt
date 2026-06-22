package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.domain.activity.AnchorGraceRule

/**
 * Orquestador del motor de scoring de núcleo v1 (PR-F). Conecta el adapter de hechos con los
 * niveles 1–6 del modelo de pesos puros y emite un [ScoreReport] con el `ESTADO ∈ [0, 1.5]` crudo
 * y su banda.
 *
 * Pipeline (dominio puro JVM, cálculo en `Double`):
 * ```
 *   WeeklyScoringContextBuilder           recolecta/dedup la ventana de 7 días
 *     → ScoringFactsAdapter               (f, t, mins) por ancla · días sostenidos · n_tasks_hoy
 *     → AnchorScoringPolicy.r           NIVEL 1: cada ancla → R
 *     → cableado de opt-ins (PR-F)        sueño M → capa Cuerpo · sobriedad M → capa Conducta
 *     → StateAggregationPolicy.estado     NIVELES 2–5: bolsa-global → ESTADO
 *     → BandPolicy.band                   NIVEL 6: banda(ESTADO)
 * ```
 *
 * El gate de **NoData** (sin hechos, o < [ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR] capas con
 * ancla) vive AQUÍ, en el orquestador: `BandPolicy` es puro y no lo conoce. El mapeo a puntos
 * visibles (NIVEL 7) es dominio puro reutilizable ([PointsMappingPolicy]): lo consume tanto la
 * proyección (`DashboardProjection`) para el dashboard como este orquestador al poblar
 * `visibleScore` para el seam de persistencia semanal.
 */
object ScoreEngine {

    /** Capa que recibe el opt-in de sueño (NIVEL 4): el sueño arrastra a Cuerpo. */
    private const val SLEEP_OPT_IN_LAYER = ScoringConstants.BODY_LAYER_ID

    /** Capa que recibe el opt-in de sobriedad (NIVEL 4): la sobriedad arrastra a Conducta. */
    private const val SOBRIETY_OPT_IN_LAYER = ScoringConstants.CONDUCT_LAYER_ID

    /**
     * Camino MADURO (firma pública intacta): corre el pipeline con la ventana semanal completa
     * (`windowDays = 7`). Byte-idéntico al comportamiento previo a `scoring-arranque-cuenta`.
     */
    fun calculate(input: ScoreInput): ScoreReport =
        calculateInternal(input, windowDays = AnchorGraceRule.GRACE_DAYS.toInt())

    /**
     * Punto de entrada de la PROYECCIÓN de arranque ([StartupProjectionUseCase]): mismo pipeline
     * que [calculate] pero con una ventana parcial de `windowDays` días vividos. NO persiste nada
     * ni altera el `ScoreReport` real (que el camino maduro sigue produciendo `NoData`). La única
     * diferencia interna es propagar `windowDays` al resolver cada ancla (NIVEL 1).
     */
    internal fun calculateProjection(input: ScoreInput, windowDays: Int): ScoreReport =
        calculateInternal(input, windowDays)

    private fun calculateInternal(input: ScoreInput, windowDays: Int): ScoreReport {
        val activeLayers = input.layers.filter { it.active }.sortedBy { it.sortOrder }
        // Gate de configuración mínima (árbol §7.4): ≥3 capas activas con ≥1 ancla, y al menos un
        // hecho. Sin datos suficientes → NoData (lo decide el orquestador, no la banda pura).
        if (activeLayers.isEmpty() ||
            !WeeklyScoringContextBuilder.hasAnyFact(input) ||
            activeLayersWithAnchor(input, activeLayers) < ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR
        ) {
            return noDataReport(activeLayers)
        }

        val context = WeeklyScoringContextBuilder.build(input)

        // Opt-ins (NIVEL 4): una señal M ∈ [0,1] por feature, cableada a su capa.
        val sleepOptIn: Double? = ScoringFactsAdapter.sleepSignal(input.sleepNights)
        val sobrietyOptIn: Double? = OptInPolicy.sobrietySignal(
            context.activeSobrietyTracks.map { track ->
                ScoringFactsAdapter.relapseDaysByTrack(
                    track = track,
                    logs = context.weeklyAbstinenceLogsByTrack[track.id].orEmpty(),
                    weekDates = context.weekDates,
                )
            },
        )

        // NIVELES 1–4 (forma): cada capa activa → LayerInput con sus anclas resueltas a R, sus
        // días de soporte, sus tasks de hoy, y su opt-in (sueño/sobriedad) si corresponde.
        val activitiesByLayer = context.visibleActivities.groupBy { it.layerId }
        val tasksToday = ScoringFactsAdapter.tasksTodayByLayer(input.tasks, input.today)
        val layerInputs = context.activeLayers.map { layer ->
            val layerActivities = activitiesByLayer[layer.id].orEmpty()
            val anchorRs = layerActivities
                .filter { it.activityType == ActivitySurface.Anchor }
                .map { def ->
                    val window = ScoringFactsAdapter.anchorWindow(
                        def,
                        context.weeklyLogsByActivity[def.id].orEmpty(),
                        input.targetVersions[def.id].orEmpty(),
                        context.weekStart,
                    )
                    val ratios = window.dayRatios
                    if (ratios != null) {
                        AnchorScoringPolicy.rFromRatios(window.f, ratios, windowDays)
                    } else {
                        AnchorScoringPolicy.r(window.f, window.t, window.mins)
                    }
                }
            val supportDays = layerActivities
                .filter { it.activityType == ActivitySurface.Support }
                .map { def ->
                    ScoringFactsAdapter.sustainedSupportDays(
                        context.weeklyLogsByActivity[def.id].orEmpty(),
                        input.today,
                    )
                }
                .ifEmpty { null }
            StateAggregationPolicy.LayerInput(
                anchors = anchorRs,
                supportDays = supportDays,
                nTasksToday = tasksToday[layer.id] ?: 0,
                optIn = optInFor(layer.id, sleepOptIn, sobrietyOptIn),
                layerId = layer.id,
            )
        }

        // NIVEL 5: bolsa-global → ESTADO ∈ [0, 1.5] + detalle por capa (UNA pasada, sin recalcular).
        // NIVEL 6: banda(ESTADO).
        val aggregation = StateAggregationPolicy.aggregate(layerInputs)
        val estado: Double = aggregation.estado
        val band = BandPolicy.band(estado)
        val estadoFloat = estado.toFloat()
        val visiblePoints = PointsMappingPolicy.points(estado)

        // Detalle por-capa para el dashboard: `score = base_eff` (la barra "¿está en pie?" ∈ [0,1]);
        // `anchorSurplusBonus = extra_final` (el canal "se destacó", superhabit + tasks). `baseScore`
        // y `rawScore` reflejan el mismo base_eff (el modelo nuevo no separa raw/base por capa).
        // Campos del modelo viejo que el motor núcleo no produce (anchorScore/supportScore/
        // taskMomentumBonus/sleepScore/sobrietyScore) quedan en su default coherente, no inventados.
        // Derivado del detalle por-capa (NO recalculado): promedio de base_eff y peor capa.
        // base_eff ∈ [0,1] ("¿la capa está en pie?"), coherente con LayerScore.score del dashboard.
        // Lista vacía → defensivo (0f / null); el modelo solo llega aquí con ≥1 término de capa.
        val layerResults = aggregation.layerResults
        val averageLayerScore =
            if (layerResults.isEmpty()) 0f
            else layerResults.map { it.baseEff }.average().toFloat()
        val worstLayer = layerResults.minByOrNull { it.baseEff }

        // Razones (señales en lenguaje del usuario): peor capa, sueño y sobriedad bajos.
        // Reusa las señales ya calculadas (worstLayer / sleepOptIn / sobrietyOptIn / tracks activos).
        val worstLayerName = worstLayer?.layerId?.let { id -> activeLayers.firstOrNull { it.id == id }?.name }
        val reasons = ScoreReasonPolicy.build(
            worstLayerName = worstLayerName,
            worstLayerBaseEff = worstLayer?.baseEff,
            sleepSignal = sleepOptIn,
            sobrietySignal = sobrietyOptIn,
            hasActiveSobriety = context.activeSobrietyTracks.isNotEmpty(),
        )

        val resultsByLayer = aggregation.layerResults.associateBy { it.layerId }
        val layerScores = activeLayers.map { layer ->
            val result = resultsByLayer[layer.id]
            val baseEff = result?.baseEff?.toFloat() ?: 0f
            LayerScore(
                layerId = layer.id,
                name = layer.name,
                score = baseEff,
                configured = true,
                baseScore = baseEff,
                rawScore = baseEff,
                anchorSurplusBonus = result?.extra?.toFloat() ?: 0f,
            )
        }

        return ScoreReport(
            state = band,
            visibleScore = visiblePoints,
            baseScore = visiblePoints,
            goalBonus = 0,
            progress = (visiblePoints.toFloat() / ScoringConstants.POINTS_CEILING.toFloat()).coerceIn(0f, 1f),
            layerScores = layerScores,
            featureContributions = emptyList(),
            gates = emptyList(),
            estado = estadoFloat,
            // Seam de persistencia (design §"Mapeo seam"): weeklyBaseScore/weeklyScore = ESTADO.
            // average/worst-layer derivados de aggregation.layerResults (base_eff por capa);
            // stability* = null (estabilidad multi-semana fuera de alcance).
            weeklyBaseScore = estadoFloat,
            weeklyScore = estadoFloat,
            averageLayerScore = averageLayerScore,
            worstLayerScore = worstLayer?.baseEff?.toFloat() ?: 0f,
            worstLayerId = worstLayer?.layerId,
            reasons = reasons,
            stabilityScore = null,
            stabilityWeeks = 0,
        )
    }

    /** El opt-in de la capa, si la capa es la cableada para sueño o sobriedad; si no, `null`. */
    private fun optInFor(layerId: String, sleepOptIn: Double?, sobrietyOptIn: Double?): Double? =
        when (layerId) {
            SLEEP_OPT_IN_LAYER -> sleepOptIn
            SOBRIETY_OPT_IN_LAYER -> sobrietyOptIn
            else -> null
        }

    /** Cuenta capas activas que tienen al menos 1 ancla configurada (activa, no archivada). */
    private fun activeLayersWithAnchor(input: ScoreInput, activeLayers: List<Layer>): Int {
        val layerIdsWithAnchor = input.activities
            .filter { it.active && !it.archived && it.activityType == ActivitySurface.Anchor }
            .map { it.layerId }
            .toSet()
        return activeLayers.count { it.id in layerIdsWithAnchor }
    }

    private fun noDataReport(activeLayers: List<Layer>): ScoreReport =
        ScoreReport(
            state = ScoreState.NoData,
            visibleScore = null,
            baseScore = null,
            goalBonus = 0,
            progress = 0f,
            layerScores = activeLayers.map {
                LayerScore(layerId = it.id, name = it.name, score = 0f, configured = false)
            },
            featureContributions = emptyList(),
            gates = emptyList(),
            estado = 0f,
        )
}
