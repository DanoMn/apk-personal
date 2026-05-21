package dev.panopt.autonomia

import android.content.Context
import dev.panopt.autonomia.data.AbstinenceLogEntity
import dev.panopt.autonomia.data.AbstinenceTrackEntity
import dev.panopt.autonomia.data.ActivityEntity
import dev.panopt.autonomia.data.ActivityLogEntity
import dev.panopt.autonomia.data.AutonomiaDatabase
import dev.panopt.autonomia.data.LayerEntity
import dev.panopt.autonomia.data.RiskEventEntity
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutonomiaRepository(context: Context) {
    private val prefs = context.getSharedPreferences("autonomia_prefs", Context.MODE_PRIVATE)
    private val dao = AutonomiaDatabase.getInstance(context).autonomiaDao()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))

    fun isDarkModeFlow(): StateFlow<Boolean> = _isDarkMode.asStateFlow()

    suspend fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    fun dashboardFlow(date: String = todayKey()): Flow<DashboardState> =
        combine(
            combine(
                dao.observeLayers(),
                dao.observeActivities(),
                dao.observeActivityLogsForDate(date),
            ) { layers, activities, logsToday ->
                DashboardCore(layers, activities, logsToday)
            },
            combine(
                dao.observeAbstinenceTracks(),
                dao.observeAbstinenceLogsForDate(date),
                dao.observeRiskEventsForDate(date),
                dao.observeActivityLogsBetween(
                    startDate = LocalDate.parse(date).minusDays(6).toString(),
                    endDate = date,
                ),
            ) { tracks, abstinenceToday, riskEvents, weekLogs ->
                DashboardSignals(tracks, abstinenceToday, riskEvents, weekLogs)
            },
        ) { core, signals ->
            val domainLayers = core.layers.map { it.toDomain() }
            val domainActivities = core.activities.map { it.toDomain() }
            val activityLogMap = core.logsToday.associate { it.activityId to it.toDomain() }
            val abstinenceLogMap = signals.abstinenceToday.associate { it.trackId to it.toDomain() }
            val domainTracks = signals.tracks.map { it.toDomain() }

            buildDashboardState(
                date = date,
                layers = domainLayers,
                activities = domainActivities,
                activityLogsToday = activityLogMap,
                abstinenceTracks = domainTracks,
                abstinenceLogsToday = abstinenceLogMap,
                weeklyGymDone = signals.weekLogs.count { it.activityId == ACTIVITY_GYM && it.completed },
                riskEventsToday = signals.riskEvents.size,
            )
        }

    fun allActivityLogsFlow(): Flow<List<ActivityLog>> =
        dao.observeAllActivityLogs().map { logs -> logs.map { it.toDomain() } }

    fun allAbstinenceLogsFlow(): Flow<List<AbstinenceLog>> =
        dao.observeAllAbstinenceLogs().map { logs -> logs.map { it.toDomain() } }

    suspend fun ensureSeeded() {
        if (dao.layerCount() > 0) return

        dao.upsertLayers(DefaultLayers)
        dao.upsertActivities(DefaultActivities)
        dao.upsertAbstinenceTracks(DefaultAbstinenceTracks)
    }

    suspend fun toggleActivity(activity: TrackedActivity, existingLog: ActivityLog?, date: String = todayKey()) {
        if (existingLog?.completed == true) {
            dao.deleteActivityLog(activity.id, date)
            return
        }

        val value = when (activity.type) {
            ActivityType.Time,
            ActivityType.Weekly -> activity.targetValue
            else -> 1
        }

        dao.upsertActivityLog(
            ActivityLogEntity(
                activityId = activity.id,
                date = date,
                completed = true,
                actualValue = value,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setActivityValue(
        activity: TrackedActivity,
        actualValue: Int,
        date: String = todayKey(),
    ) {
        dao.upsertActivityLog(
            ActivityLogEntity(
                activityId = activity.id,
                date = date,
                completed = actualValue >= activity.minimumValue,
                actualValue = actualValue.coerceAtLeast(0),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markAbstinenceClean(track: AbstinenceTrack, date: String = todayKey()) {
        dao.upsertAbstinenceLog(
            AbstinenceLogEntity(
                trackId = track.id,
                date = date,
                status = AbstinenceStatus.Clean.name,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markAbstinenceRelapse(track: AbstinenceTrack, date: String = todayKey()) {
        dao.upsertAbstinenceLog(
            AbstinenceLogEntity(
                trackId = track.id,
                date = date,
                status = AbstinenceStatus.Relapse.name,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun recordRiskEvent() {
        dao.upsertRiskEvent(
            RiskEventEntity(
                id = UUID.randomUUID().toString(),
                date = todayKey(),
                createdAt = System.currentTimeMillis(),
                intensity = 6,
                trigger = "modo riesgo abierto",
                actionTaken = "protocolo de 20 minutos",
                actedOnImpulse = false,
                note = "",
            ),
        )
    }
}

private data class DashboardCore(
    val layers: List<LayerEntity>,
    val activities: List<ActivityEntity>,
    val logsToday: List<ActivityLogEntity>,
)

private data class DashboardSignals(
    val tracks: List<AbstinenceTrackEntity>,
    val abstinenceToday: List<AbstinenceLogEntity>,
    val riskEvents: List<RiskEventEntity>,
    val weekLogs: List<ActivityLogEntity>,
)

private fun buildDashboardState(
    date: String,
    layers: List<Layer>,
    activities: List<TrackedActivity>,
    activityLogsToday: Map<String, ActivityLog>,
    abstinenceTracks: List<AbstinenceTrack>,
    abstinenceLogsToday: Map<String, AbstinenceLog>,
    weeklyGymDone: Int,
    riskEventsToday: Int,
): DashboardState {
    val selfCareActivities = activities.filter { it.type == ActivityType.SelfCare }
    val selfCareDone = selfCareActivities.count { activityLogsToday[it.id]?.completed == true }
    val practiceActivities = activities.filter { it.type == ActivityType.Time || it.type == ActivityType.Weekly }
    val practiceDone = practiceActivities.count { activityLogsToday[it.id]?.completed == true }
    val relapseCount = abstinenceTracks.count {
        abstinenceLogsToday[it.id]?.status == AbstinenceStatus.Relapse
    }
    val cleanCount = abstinenceTracks.count {
        abstinenceLogsToday[it.id]?.status == AbstinenceStatus.Clean
    }

    val dimensions = listOf(
        DashboardDimension(
            name = "Sobriedad",
            status = when {
                relapseCount > 0 -> DimensionStatus.Alert
                cleanCount == abstinenceTracks.size && abstinenceTracks.isNotEmpty() -> DimensionStatus.Stable
                cleanCount > 0 -> DimensionStatus.InMotion
                else -> DimensionStatus.Unknown
            },
            message = when {
                relapseCount > 0 -> "Esto es una senal, no una condena."
                cleanCount > 0 -> "Rachas activas marcadas hoy."
                else -> "Marca tus rachas cuando puedas."
            },
        ),
        DashboardDimension(
            name = "Cuidado basico",
            status = when {
                selfCareActivities.isEmpty() -> DimensionStatus.Unknown
                selfCareDone == selfCareActivities.size -> DimensionStatus.Stable
                selfCareDone > 0 -> DimensionStatus.InMotion
                else -> DimensionStatus.Low
            },
            message = if (selfCareDone > 0) "Volviste al cuerpo." else "Una accion basica basta para empezar.",
        ),
        DashboardDimension(
            name = "Practica",
            status = when {
                practiceDone >= 3 -> DimensionStatus.Stable
                practiceDone > 0 -> DimensionStatus.InMotion
                else -> DimensionStatus.Low
            },
            message = if (practiceDone > 0) "Hay practica real registrada." else "Elige una practica minima.",
        ),
        DashboardDimension(
            name = "Riesgo",
            status = if (riskEventsToday > 0) DimensionStatus.Alert else DimensionStatus.Stable,
            message = if (riskEventsToday > 0) "Protocolo usado hoy." else "Sin eventos de riesgo hoy.",
        ),
    )

    val globalState = when {
        relapseCount > 0 -> GlobalState.Crisis
        riskEventsToday > 0 -> GlobalState.Risk
        selfCareDone == 0 && practiceDone == 0 && cleanCount == 0 -> GlobalState.NoData
        selfCareDone == 0 && practiceDone == 0 -> GlobalState.LowMotion
        selfCareDone > 0 && practiceDone > 0 && cleanCount > 0 -> GlobalState.Stable
        else -> GlobalState.InMotion
    }

    val message = when (globalState) {
        GlobalState.NoData -> "Todavia no hay senales de hoy. Una accion minima abre el dia."
        GlobalState.InMotion -> "La base ya empezo a moverse."
        GlobalState.Stable -> "Hoy la base esta sostenida."
        GlobalState.LowMotion -> "La base esta baja. Volvamos al cuerpo."
        GlobalState.Risk -> "Hay senal de riesgo. Gana 20 minutos."
        GlobalState.Crisis -> "Esto ya paso. Ahora el objetivo es no empeorar."
        GlobalState.Recovery -> "Recuperacion: estructura minima, sin castigo."
    }

    return DashboardState(
        today = date,
        globalState = globalState,
        globalMessage = message,
        dimensions = dimensions,
        layers = layers,
        activities = activities,
        activityLogsToday = activityLogsToday,
        abstinenceTracks = abstinenceTracks,
        abstinenceLogsToday = abstinenceLogsToday,
        weeklyGymDone = weeklyGymDone,
        riskEventsToday = riskEventsToday,
    )
}

private fun LayerEntity.toDomain(): Layer =
    Layer(id = id, name = name, description = description, sortOrder = sortOrder)

private fun ActivityEntity.toDomain(): TrackedActivity =
    TrackedActivity(
        id = id,
        layerId = layerId,
        name = name,
        description = description,
        type = runCatching { ActivityType.valueOf(type) }.getOrDefault(ActivityType.Check),
        targetValue = targetValue,
        minimumValue = minimumValue,
        unit = runCatching { ActivityUnit.valueOf(unit) }.getOrDefault(ActivityUnit.Boolean),
        weeklyTarget = weeklyTarget,
        importance = importance,
        active = active,
        sortOrder = sortOrder,
    )

private fun ActivityLogEntity.toDomain(): ActivityLog =
    ActivityLog(
        activityId = activityId,
        date = date,
        completed = completed,
        actualValue = actualValue,
        note = note,
    )

private fun AbstinenceTrackEntity.toDomain(): AbstinenceTrack =
    AbstinenceTrack(
        id = id,
        name = name,
        substanceLabel = substanceLabel,
        severity = runCatching { AbstinenceSeverity.valueOf(severity) }.getOrDefault(AbstinenceSeverity.Moderate),
        active = active,
        sortOrder = sortOrder,
    )

private fun AbstinenceLogEntity.toDomain(): AbstinenceLog =
    AbstinenceLog(
        trackId = trackId,
        date = date,
        status = runCatching { AbstinenceStatus.valueOf(status) }.getOrDefault(AbstinenceStatus.Unknown),
        urge = urge,
        urgeIntensity = urgeIntensity,
        note = note,
    )

private const val LAYER_INTERIOR = "layer_interior"
private const val LAYER_BODY = "layer_body"
private const val LAYER_CONDUCT = "layer_conduct"
private const val LAYER_FOOD_HOME = "layer_food_home"
private const val LAYER_SOCIAL = "layer_social"
private const val LAYER_PROJECT = "layer_project"

private const val ACTIVITY_GYM = "activity_gym"

private val DefaultLayers = listOf(
    LayerEntity(LAYER_INTERIOR, "Interior", "Meditacion, escritura y respeto interno.", 10),
    LayerEntity(LAYER_BODY, "Cuerpo", "Movimiento, sueno y cuidado fisico.", 20),
    LayerEntity(LAYER_CONDUCT, "Conducta", "Autocontrol y limites que protegen la base.", 30),
    LayerEntity(LAYER_FOOD_HOME, "Casa/comida", "Alimentacion, orden y entorno.", 40),
    LayerEntity(LAYER_SOCIAL, "Vinculos", "Relacion con otros y aislamiento.", 50),
    LayerEntity(LAYER_PROJECT, "Proyecto", "Digitaliza, musica e identidad creativa.", 60),
)

private val DefaultActivities = listOf(
    ActivityEntity("activity_meditation", LAYER_INTERIOR, "Meditar antes de dormir", "Tap = 5 min. Mantener = editar minutos de hoy.", ActivityType.Time.name, 5, 1, ActivityUnit.Minutes.name, 0, 2, true, 10),
    ActivityEntity("activity_honest_line", LAYER_INTERIOR, "Escribir una linea honesta", "Una frase basta. No tiene que ser perfecta.", ActivityType.Note.name, 1, 1, ActivityUnit.Text.name, 0, 1, true, 20),
    ActivityEntity(ACTIVITY_GYM, LAYER_BODY, "Gimnasio / caminar", "Objetivo diario si toca moverse.", ActivityType.Time.name, 40, 10, ActivityUnit.Minutes.name, 3, 2, true, 30),
    ActivityEntity("activity_sleep_early", LAYER_BODY, "Dormir con algo de orden", "Por ahora check manual; luego sera hora real.", ActivityType.Check.name, 1, 1, ActivityUnit.Boolean.name, 0, 2, true, 40),
    ActivityEntity("activity_shower", LAYER_BODY, "Banarse", "Cuidado basico. Volver al cuerpo.", ActivityType.SelfCare.name, 1, 1, ActivityUnit.Boolean.name, 0, 2, true, 50),
    ActivityEntity("activity_teeth", LAYER_BODY, "Cepillarse dientes", "Basico no significa menor.", ActivityType.SelfCare.name, 1, 1, ActivityUnit.Boolean.name, 0, 2, true, 60),
    ActivityEntity("activity_no_phone_bed", LAYER_CONDUCT, "Celular fuera de la cama", "Una frontera pequena que protege el sueno.", ActivityType.AbstinenceSupport.name, 1, 1, ActivityUnit.Boolean.name, 0, 2, true, 70),
    ActivityEntity("activity_home_meal", LAYER_FOOD_HOME, "Comida hecha en casa", "Cuidar alimentacion sin obsesion.", ActivityType.Check.name, 1, 1, ActivityUnit.Boolean.name, 0, 1, true, 80),
    ActivityEntity("activity_order", LAYER_FOOD_HOME, "Orden minimo de casa", "Tap = 15 min. Mantener = editar minutos.", ActivityType.Time.name, 15, 5, ActivityUnit.Minutes.name, 0, 1, true, 90),
    ActivityEntity("activity_clean_interaction", LAYER_SOCIAL, "Interaccion limpia", "No aislarme destructivamente.", ActivityType.Note.name, 1, 1, ActivityUnit.Text.name, 0, 1, true, 100),
    ActivityEntity("activity_digitaliza", LAYER_PROJECT, "Avance en Digitaliza", "Un avance concreto, sin sacrificar la base.", ActivityType.Time.name, 30, 10, ActivityUnit.Minutes.name, 0, 2, true, 110),
    ActivityEntity("activity_music", LAYER_PROJECT, "Musica / cuaderno", "Anatomia de la ausencia: crear sin destruirme.", ActivityType.Time.name, 20, 5, ActivityUnit.Minutes.name, 0, 2, true, 120),
    ActivityEntity("activity_read", LAYER_PROJECT, "Leer", "20 minutos para alimentar la mente.", ActivityType.Time.name, 20, 5, ActivityUnit.Minutes.name, 5, 1, true, 130),
)

private val DefaultAbstinenceTracks = listOf(
    AbstinenceTrackEntity("abstinence_alcohol", "Alcohol", "no beber", AbstinenceSeverity.Critical.name, true, 10),
    AbstinenceTrackEntity("abstinence_porn", "Conducta sexual", "no usar como escape", AbstinenceSeverity.Moderate.name, true, 20),
    AbstinenceTrackEntity("abstinence_marihuana", "Marihuana", "no consumir", AbstinenceSeverity.Moderate.name, false, 30),
)
