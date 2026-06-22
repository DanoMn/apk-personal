package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.activity.ActivityTargetVersion
import dev.panopt.autonomia.domain.sleep.SleepNightScore
import java.time.LocalDate

data class ScoreInput(
    val layers: List<Layer>,
    val activities: List<ActivityDefinition>,
    val todayActivityLogs: List<ActivityLog>,
    val periodActivityLogs: List<ActivityLog>,
    val abstinenceTracks: List<AbstinenceTrack>,
    val todayAbstinenceLogs: List<AbstinenceLog>,
    val allAbstinenceLogs: List<AbstinenceLog>,
    val tasks: List<Task>,
    /**
     * Sleep nights for the current week (already scored).
     * NoData nights are excluded — only nights with a computed sleepScore are relevant.
     * Weekly sleep score = average of nights with data. Empty → null (no sleep data this week).
     *
     * Replaces the deprecated single `sleepLog: SleepLog?` (WU-5 / design §5).
     */
    val sleepNights: List<SleepNightScore> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val weeklyHistory: List<WeeklyScoreHistoryEntry> = emptyList(),
    /**
     * FASE 2 — versiones de la vara por ancla (`activityId → versiones`). Vacío = sin versionado
     * (camino legacy: el motor usa la config actual para todos los días). Cuando un ancla tiene
     * versiones, el motor evalúa cada día con la meta que regía ese día (no reescribe el pasado).
     */
    val targetVersions: Map<String, List<ActivityTargetVersion>> = emptyMap(),
)

data class ScoreReport(
    val state: ScoreState,
    val visibleScore: Int?,
    val baseScore: Int?,
    val goalBonus: Int,
    val progress: Float,
    val layerScores: List<LayerScore>,
    val featureContributions: List<FeatureContribution>,
    val gates: List<ScoreGate>,
    /**
     * ESTADO crudo del NIVEL 5 (`∈ [0, 1.5]`), motor de núcleo v1. `Float` SOLO en esta frontera
     * de salida (el cálculo interno es `Double`). `state` = `BandPolicy.band(estado)`;
     * `weeklyBaseScore`/`weeklyScore` reflejan este ESTADO (no el rango 0–1 del modelo viejo).
     */
    val estado: Float = 0f,
    val weeklyBaseScore: Float = 0f,
    val weeklyScore: Float = 0f,
    val averageLayerScore: Float = 0f,
    val worstLayerScore: Float = 0f,
    val worstLayerId: String? = null,
    val reasons: List<String> = emptyList(),
    val stabilityScore: Float? = null,
    val stabilityWeeks: Int = 0,
)

data class WeeklyScoreHistoryEntry(
    val weekStart: String,
    val weekEnd: String,
    val scoringVersion: String,
    val weeklyBaseScore: Float,
    val weeklyScore: Float,
    val state: ScoreState,
)

data class LayerScore(
    val layerId: String,
    val name: String,
    val score: Float,
    val configured: Boolean,
    val baseScore: Float = 0f,
    val rawScore: Float = 0f,
    val anchorScore: Float? = null,
    val supportScore: Float? = null,
    val anchorSurplusBonus: Float = 0f,
    val taskMomentumBonus: Float = 0f,
    val sleepScore: Float? = null,
    val sobrietyScore: Float? = null,
)

data class FeatureContribution(
    val feature: ScoreFeature,
    val layerId: String?,
    val label: String,
    val value: Float,
    val maxValue: Float,
)

data class ScoreGate(
    val kind: ScoreGateKind,
    val active: Boolean,
    val maxScore: Int,
    val message: String,
)

enum class ScoreFeature {
    Anchor,
    Support,
    Task,
    Sleep,
    Sobriety,
    Goal,
}

enum class ScoreGateKind {
    SleepMissing,
    SleepLow,
    RelapseToday,
    CleanStreak,
    FoundationLayerLow,
    GoalMissing,
    GoalPartial,
}

/**
 * Forma cruda de UN ancla en la ventana semanal, tal como la produce [ScoringFactsAdapter]
 * ANTES de resolverla a su `R`-value (eso lo hace [AnchorScoringPolicy.r]).
 *
 * Invariante "ancla = solo Minutes" (PR-D): `mins[i] = actualValue` (minutos) — NO hay conversión
 * multi-unidad. La lista contiene solo los días CON actividad (`> 0`) de la ventana; su longitud =
 * nº de días con actividad (no fija en 7). `f`/`t` salen de la config de la actividad.
 */
data class AnchorWindow(
    val f: Int,
    val t: Int,
    val mins: List<Int>,
    /**
     * FASE 2: ratios `m_i / T_i` por día, calculados con la meta de minutos VIGENTE cada día
     * (vara versionada por fecha). `null` = camino legacy (sin versiones): el motor usa `(f, t, mins)`
     * vía [AnchorScoringPolicy.r]; si no es `null`, usa [AnchorScoringPolicy.rFromRatios].
     */
    val dayRatios: List<Double>? = null,
)

/**
 * Forma cruda de UNA capa en la ventana semanal (salida de [ScoringFactsAdapter]). Es el insumo
 * que, una vez resueltas las anclas a `R`-values, se convierte en
 * [StateAggregationPolicy.LayerInput] para el motor.
 *
 * @param anchors anclas de la capa con su forma cruda `(f, t, mins)`.
 * @param supportDays días sostenidos de cada soporte de la capa (ventana 4d, UX inversa).
 *   Lista vacía = capa sin soportes.
 * @param nTasksToday tasks completadas HOY con esta capa (conteo efímero, se resetea cada día).
 * @param optIn señal del opt-in de la capa (`M ∈ [0, 1]`), o `null` si la capa no tiene opt-in.
 */
data class LayerFacts(
    val anchors: List<AnchorWindow> = emptyList(),
    val supportDays: List<Int> = emptyList(),
    val nTasksToday: Int = 0,
    val optIn: Double? = null,
)

internal data class WeeklyScoringContext(
    val weekStart: LocalDate,
    val weekDates: List<LocalDate>,
    val activeLayers: List<Layer>,
    val visibleActivities: List<ActivityDefinition>,
    val weeklyLogsByActivity: Map<String, List<ActivityLog>>,
    val activeSobrietyTracks: List<AbstinenceTrack>,
    val sleepScore: Float?,
    val sobrietyScore: Float?,
    val completedTasksByLayer: Map<String, List<Task>>,
    /**
     * Logs de abstinencia de la ventana semanal por track activo (PR-E). Forma cruda que
     * [ScoringFactsAdapter.relapseDaysByTrack] consume para derivar `días_recaída` → `M_sobr`
     * en el motor nuevo. Reemplaza al `sobrietyScore` pre-computado (policy vieja eliminada en PR-F).
     */
    val weeklyAbstinenceLogsByTrack: Map<String, List<AbstinenceLog>> = emptyMap(),
)
