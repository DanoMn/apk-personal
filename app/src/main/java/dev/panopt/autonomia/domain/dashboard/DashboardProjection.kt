package dev.panopt.autonomia.domain.dashboard

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.RiskEvent
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.SleepQuality
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.activity.isGoal
import dev.panopt.autonomia.domain.scoring.ScoreEngine
import dev.panopt.autonomia.domain.scoring.ScoreInput
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

internal fun buildDashboardState(
    layers: List<Layer>,
    activities: List<ActivityDefinition>,
    catalogActivities: List<ActivityDefinition> = activities,
    todayActivityLogs: List<ActivityLog>,
    weekActivityLogs: List<ActivityLog>,
    periodActivityLogs: List<ActivityLog> = weekActivityLogs,
    abstinenceTracks: List<AbstinenceTrack>,
    todayAbstinenceLogs: List<AbstinenceLog>,
    allAbstinenceLogs: List<AbstinenceLog>,
    riskEvents: List<RiskEvent>,
    tasks: List<Task>,
    anchorPhrases: List<AnchorPhrase>,
    sleepLog: SleepLog?,
    focusSignalActivityId: String?,
    today: LocalDate = LocalDate.now(),
): DashboardState {
    val activeLayers = layers.filter { it.active }.sortedBy { it.sortOrder }
    val layerById = activeLayers.associateBy { it.id }
    val visibleActivities = activities
        .filter { it.active && !it.archived && it.activityType != ActivitySurface.Task }
        .sortedBy { it.sortOrder }
    val goalActivities = visibleActivities.filter { it.isGoal() }
    val dashboardActivities = visibleActivities.filterNot { it.isGoal() }
    val todayLogsByActivity = todayActivityLogs.associateBy { it.activityId }
    val completedActivities = dashboardActivities.filter { activity ->
        activity.isCompletedBy(todayLogsByActivity[activity.id])
    }
    val completedActivityIds = completedActivities.map { it.id }.toSet()

    val primaryActivities = dashboardActivities.filter { it.activityType == ActivitySurface.Anchor }
    val selfCareActivities = dashboardActivities.filter {
        it.activityType == ActivitySurface.Support
    }
    val secondaryActivities = selfCareActivities
    val coreActivities = dashboardActivities.filter { it.contributionRole == ContributionRole.Core }
    val timeActivities = dashboardActivities.filter { it.unit == ActivityUnit.Minutes }

    val completedCount = completedActivities.size
    val dailyProgress = ratio(completedCount, dashboardActivities.size)
    val activeLayerIds = completedActivities.map { it.layerId }.toSet()

    val activeTracks = abstinenceTracks.filter { it.active }.sortedBy { it.sortOrder }
    val todayLogsByTrack = todayAbstinenceLogs.associateBy { it.trackId }
    val cleanTrackCount = activeTracks.count { todayLogsByTrack[it.id]?.status == AbstinenceStatus.Clean }
    val criticalRelapse = activeTracks.any { track ->
        track.severity == AbstinenceSeverity.Critical &&
            todayLogsByTrack[track.id]?.status == AbstinenceStatus.Relapse
    }
    val anyRelapse = activeTracks.any { todayLogsByTrack[it.id]?.status == AbstinenceStatus.Relapse }
    val completedCoreCount = coreActivities.count { it.id in completedActivityIds }
    val completedSelfCareCount = selfCareActivities.count { it.id in completedActivityIds }
    val weekRows = buildWeekRows(
        layers = activeLayers,
        activities = dashboardActivities,
        weekLogs = weekActivityLogs,
        today = today,
    )

    val scoreReport = ScoreEngine.calculate(
        ScoreInput(
            layers = activeLayers,
            activities = visibleActivities,
            todayActivityLogs = todayActivityLogs,
            periodActivityLogs = periodActivityLogs,
            abstinenceTracks = activeTracks,
            todayAbstinenceLogs = todayAbstinenceLogs,
            allAbstinenceLogs = allAbstinenceLogs,
            tasks = tasks,
            sleepLog = sleepLog,
            today = today,
        ),
    )
    val scoreState = scoreReport.state
    val score = scoreReport.visibleScore ?: 0
    val layerStates = scoreReport.layerScores.map { layerScore ->
        DashboardLayerState(
            id = layerScore.layerId,
            name = layerScore.name,
            progress = layerScore.score,
        )
    }
    val pendingCount = (dashboardActivities.size - completedCount).coerceAtLeast(0)
    val dimensions = buildDimensions(
        cleanTrackCount = cleanTrackCount,
        activeTrackCount = activeTracks.size,
        anyRelapse = anyRelapse,
        criticalRelapse = criticalRelapse,
        completedSelfCareCount = completedSelfCareCount,
        selfCareCount = selfCareActivities.size,
        completedCoreCount = completedCoreCount,
        coreCount = coreActivities.size,
        riskEvents = riskEvents,
    )

    return DashboardState(
        isLoading = false,
        status = DashboardStatusState(
            scoreState = scoreState,
            title = scoreTitle(scoreState),
            headline = scoreHeadline(scoreState),
            body = scoreBody(scoreState),
            score = score,
            scoreLabel = scoreReport.visibleScore?.toString() ?: "--",
            progress = scoreReport.progress,
        ),
        dailyProgress = DashboardDailyProgressState(
            percent = (dailyProgress * 100f).roundToInt().coerceIn(0, 100),
            progress = dailyProgress,
            pendingLabel = pendingLabel(pendingCount),
            activeLayersLabel = "${activeLayerIds.size} de ${activeLayers.size} capas activas",
        ),
        anchorPhrase = selectAnchorPhrase(anchorPhrases),
        layers = layerStates,
        signals = buildSignals(
            activities = timeActivities,
            logsByActivity = todayLogsByActivity,
            sleepLog = sleepLog,
            focusSignalActivityId = focusSignalActivityId,
        ),
        sobrietyTracks = activeTracks.map { track ->
            val status = todayLogsByTrack[track.id]?.status ?: AbstinenceStatus.Unknown
            DashboardSobrietyTrackState(
                id = track.id,
                label = track.name,
                days = streakDays(track.id, allAbstinenceLogs, today),
                meta = sobrietyMeta(status),
                status = sobrietyStatus(status, track.severity),
                isRelapseToday = status == AbstinenceStatus.Relapse,
                isMarkedCleanToday = status == AbstinenceStatus.Clean,
            )
        },
        checklistItems = primaryActivities.map { activity ->
            DashboardChecklistItemState(
                id = activity.id,
                title = activity.name,
                layerId = activity.layerId,
                layerName = layerById[activity.layerId]?.name.orEmpty(),
                value = activity.valueLabel(),
                completed = activity.id in completedActivityIds,
                activityType = activity.activityType.name,
            )
        },
        supports = buildSupports(
            secondaryActivities = secondaryActivities,
            completedActivities = completedActivities,
            layerById = layerById,
            tasks = tasks,
        ),
        weekRows = weekRows,
        dimensions = dimensions,
        sleep = sleepLog.toSleepState(),
        activityOptions = catalogActivities.map { activity ->
            val log = todayLogsByActivity[activity.id]
            DashboardActivityOptionState(
                id = activity.id,
                title = activity.name,
                layerId = activity.layerId,
                layerName = layerById[activity.layerId]?.name.orEmpty(),
                targetValue = activity.targetValue ?: activity.minimumValue ?: 1,
                actualValue = log?.actualValue ?: activity.targetValue ?: activity.minimumValue ?: 0,
                isCompletedToday = activity.isCompletedBy(log),
                isFocusSignal = activity.id == focusSignalActivityId,
                displaySurface = activity.displaySurface.name, // deprecated, keep for now
                activityType = activity.activityType.name,
                isGoal = activity.isGoal(),
            )
        },
        secondaryChecklistItems = secondaryActivities.map { activity ->
            DashboardChecklistItemState(
                id = activity.id,
                title = activity.name,
                layerId = activity.layerId,
                layerName = layerById[activity.layerId]?.name.orEmpty(),
                value = activity.valueLabel(),
                completed = activity.id in completedActivityIds,
                activityType = activity.activityType.name,
            )
        },
        pendingTasks = tasks
            .filter { it.status == TaskStatus.Pending }
            .sortedBy { it.dueDate ?: "9999-99-99" }
            .map { DashboardTaskState(id = it.id, title = it.title, layerId = it.layerId) },
    )
}

internal fun weekStartKey(today: LocalDate = LocalDate.now()): String =
    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

private fun ActivityDefinition.isCompletedBy(log: ActivityLog?): Boolean {
    if (log == null) return false
    if (log.completed) return true
    val actual = log.actualValue ?: return false
    val minimum = minimumValue ?: targetValue ?: return false
    return actual >= minimum
}

private fun ActivityDefinition.valueLabel(): String =
    when (unit) {
        ActivityUnit.Minutes -> "${targetValue ?: minimumValue ?: 0} min"
        ActivityUnit.Count -> "${targetValue ?: minimumValue ?: 1}x"
        ActivityUnit.Boolean -> "hoy"
        ActivityUnit.Time -> "hora"
        ActivityUnit.Text -> "nota"
    }

private fun ratio(numerator: Int, denominator: Int): Float =
    if (denominator <= 0) 0f else numerator.toFloat() / denominator.toFloat()

private fun pendingLabel(count: Int): String =
    when (count) {
        0 -> "Base completa"
        1 -> "1 pendiente"
        else -> "$count pendientes"
    }

private fun scoreTitle(state: ScoreState): String =
    when (state) {
        ScoreState.NoData -> "Sin datos"
        ScoreState.Restoration -> "Restauración"
        ScoreState.Attention -> "Atención"
        ScoreState.Motion -> "En marcha"
        ScoreState.Plenitude -> "Plenitud"
        ScoreState.Unbreakable -> "Inquebrantable"
    }

private fun scoreHeadline(state: ScoreState): String =
    when (state) {
        ScoreState.NoData -> "Todavia no hay lectura suficiente."
        ScoreState.Restoration -> "La base necesita cuidado minimo."
        ScoreState.Attention -> "Hay margen, pero estamos cediendo."
        ScoreState.Motion -> "La base esta activa y sosteniendose."
        ScoreState.Plenitude -> "Las bases forman una unidad estable."
        ScoreState.Unbreakable -> "Nucleo protegido por consistencia."
    }

private fun scoreBody(state: ScoreState): String =
    when (state) {
        ScoreState.NoData -> "Registra algunas acciones para empezar a leer la base."
        ScoreState.Restoration -> "Hoy no toca exigirte. Vuelve a cuerpo, descanso o cuidado basico."
        ScoreState.Attention -> "Una accion protectora ahora puede recuperar traccion."
        ScoreState.Motion -> "Hay practica real en tu dia. Mantener tambien cuenta."
        ScoreState.Plenitude -> "La consistencia ya sostiene la estructura sin apretar de mas."
        ScoreState.Unbreakable -> "Tu cuerpo, conducta y proyectos estan alineados por continuidad."
    }

private fun selectAnchorPhrase(phrases: List<AnchorPhrase>): DashboardAnchorPhraseState {
    val phrase = phrases
        .filter { it.active && !it.authorReference.isNullOrBlank() }
        .sortedBy { it.sortOrder }
        .firstOrNull()

    return if (phrase == null) {
        DashboardAnchorPhraseState()
    } else {
        DashboardAnchorPhraseState(
            text = phrase.text,
            authorReference = phrase.authorReference.orEmpty(),
        )
    }
}

private fun buildDimensions(
    cleanTrackCount: Int,
    activeTrackCount: Int,
    anyRelapse: Boolean,
    criticalRelapse: Boolean,
    completedSelfCareCount: Int,
    selfCareCount: Int,
    completedCoreCount: Int,
    coreCount: Int,
    riskEvents: List<RiskEvent>,
): List<DashboardDimensionState> {
    val sobrietyStatus = when {
        criticalRelapse -> DashboardDimensionStatus.Restoration
        anyRelapse -> DashboardDimensionStatus.Attention
        activeTrackCount == 0 -> DashboardDimensionStatus.Unknown
        cleanTrackCount == activeTrackCount -> DashboardDimensionStatus.Stable
        cleanTrackCount > 0 -> DashboardDimensionStatus.Motion
        else -> DashboardDimensionStatus.Unknown
    }
    val basicCareStatus = when {
        selfCareCount == 0 -> DashboardDimensionStatus.Unknown
        completedSelfCareCount >= selfCareCount -> DashboardDimensionStatus.Stable
        completedSelfCareCount > 0 -> DashboardDimensionStatus.Motion
        else -> DashboardDimensionStatus.Attention
    }
    val practiceStatus = when {
        coreCount == 0 -> DashboardDimensionStatus.Unknown
        completedCoreCount >= coreCount -> DashboardDimensionStatus.Stable
        completedCoreCount > 0 -> DashboardDimensionStatus.Motion
        else -> DashboardDimensionStatus.Attention
    }
    val riskStatus = when {
        riskEvents.any { it.actedOnImpulse || it.intensity >= 8 } -> DashboardDimensionStatus.Restoration
        riskEvents.isNotEmpty() -> DashboardDimensionStatus.Attention
        else -> DashboardDimensionStatus.Stable
    }

    return listOf(
        DashboardDimensionState("Sobriedad", "$cleanTrackCount/$activeTrackCount", sobrietyStatus),
        DashboardDimensionState("Cuidado", "$completedSelfCareCount/$selfCareCount", basicCareStatus),
        DashboardDimensionState("Practica", "$completedCoreCount/$coreCount", practiceStatus),
        DashboardDimensionState("Eventos", riskEvents.size.toString(), riskStatus),
    )
}

private fun buildSignals(
    activities: List<ActivityDefinition>,
    logsByActivity: Map<String, ActivityLog>,
    sleepLog: SleepLog?,
    focusSignalActivityId: String?,
): List<DashboardSignalState> {
    val projectActivities = activities.filter {
        it.role == ActivityRole.ProjectWork || it.layerId == "layer_proyecto"
    }
    val primaryProject = projectActivities.maxByOrNull { activity ->
        logsByActivity[activity.id]?.actualValue ?: 0
    } ?: projectActivities.firstOrNull()
    val focusActivity = activities.firstOrNull { it.id == focusSignalActivityId }
        ?: projectActivities.firstOrNull { it.id != primaryProject?.id }
        ?: activities.firstOrNull { it.id != primaryProject?.id }

    return listOf(
        DashboardSignalState(
            kind = DashboardSignalKind.Sleep,
            label = "Sueno",
            value = sleepLog.sleepValue(),
            meta = sleepLog.sleepMeta(),
            status = sleepLog.sleepStatus(),
        ),
        DashboardSignalState(
            kind = DashboardSignalKind.Project,
            label = "Proyecto",
            value = primaryProject.activityValue(logsByActivity),
            meta = primaryProject?.name?.projectMeta() ?: "sin proyecto",
            status = primaryProject.activityStatus(logsByActivity),
        ),
        DashboardSignalState(
            kind = DashboardSignalKind.Focus,
            label = focusActivity?.shortLabel() ?: "Foco",
            value = focusActivity.activityValue(logsByActivity),
            meta = if (focusActivity == null) "configurable" else "elegida",
            status = focusActivity.activityStatus(logsByActivity),
        ),
    )
}

private fun DashboardDimensionStatus.metaLabel(): String =
    when (this) {
        DashboardDimensionStatus.Stable -> "estable"
        DashboardDimensionStatus.Motion -> "activa"
        DashboardDimensionStatus.Attention -> "atencion"
        DashboardDimensionStatus.Restoration -> "cuidado"
        DashboardDimensionStatus.Unknown -> "sin marcar"
    }

private fun SleepLog?.sleepValue(): String {
    val log = this ?: return "--"
    val minutes = minutesBetween(log.sleptAt, log.wokeAt) ?: return "--"
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) "${hours}h ${rest}m" else "${rest}m"
}

private fun SleepLog?.sleepMeta(): String =
    when (this?.quality) {
        SleepQuality.Low -> "baja"
        SleepQuality.Acceptable -> "aceptable"
        SleepQuality.Good -> "buena"
        null -> "toca registrar"
    }

private fun SleepLog?.sleepStatus(): DashboardDimensionStatus =
    when (this?.quality) {
        SleepQuality.Low -> DashboardDimensionStatus.Attention
        SleepQuality.Acceptable -> DashboardDimensionStatus.Motion
        SleepQuality.Good -> DashboardDimensionStatus.Stable
        null -> DashboardDimensionStatus.Unknown
    }

private fun ActivityDefinition?.activityValue(logsByActivity: Map<String, ActivityLog>): String {
    val activity = this ?: return "--"
    val value = logsByActivity[activity.id]?.actualValue ?: 0
    return "${value}m"
}

private fun ActivityDefinition?.activityStatus(logsByActivity: Map<String, ActivityLog>): DashboardDimensionStatus {
    val activity = this ?: return DashboardDimensionStatus.Unknown
    val log = logsByActivity[activity.id]
    return if (activity.isCompletedBy(log)) DashboardDimensionStatus.Motion else DashboardDimensionStatus.Unknown
}

private fun ActivityDefinition.shortLabel(): String =
    name.replace("Proyecto ", "").substringBefore("/").trim().ifBlank { name }

private fun String.projectMeta(): String =
    replace("Proyecto ", "").substringBefore("/").trim().lowercase().ifBlank { "proyecto" }

private fun SleepLog?.toSleepState(): DashboardSleepState =
    if (this == null) {
        DashboardSleepState()
    } else {
        DashboardSleepState(
            plannedSleepAt = plannedSleepAt,
            plannedWakeAt = plannedWakeAt,
            sleptAt = sleptAt,
            wokeAt = wokeAt,
            quality = quality,
            note = note,
        )
    }

private fun minutesBetween(start: String, end: String): Int? {
    val startTime = runCatching { LocalTime.parse(start) }.getOrNull() ?: return null
    val endTime = runCatching { LocalTime.parse(end) }.getOrNull() ?: return null
    val startMinutes = startTime.hour * 60 + startTime.minute
    val endMinutes = endTime.hour * 60 + endTime.minute
    val raw = endMinutes - startMinutes
    return if (raw >= 0) raw else raw + 24 * 60
}

private fun sobrietyMeta(status: AbstinenceStatus): String =
    when (status) {
        AbstinenceStatus.Clean -> "hoy limpio"
        AbstinenceStatus.Relapse -> "senal registrada"
        AbstinenceStatus.Unknown -> "toca marcar"
    }

private fun sobrietyStatus(
    status: AbstinenceStatus,
    severity: AbstinenceSeverity,
): DashboardDimensionStatus =
    when (status) {
        AbstinenceStatus.Clean -> DashboardDimensionStatus.Stable
        AbstinenceStatus.Relapse -> if (severity == AbstinenceSeverity.Critical) {
            DashboardDimensionStatus.Restoration
        } else {
            DashboardDimensionStatus.Attention
        }
        AbstinenceStatus.Unknown -> DashboardDimensionStatus.Unknown
    }

private fun streakDays(
    trackId: String,
    logs: List<AbstinenceLog>,
    today: LocalDate,
): Int {
    val logsByDate = logs
        .filter { it.trackId == trackId }
        .mapNotNull { log -> runCatching { LocalDate.parse(log.date) }.getOrNull()?.let { it to log } }
        .toMap()

    var cursor = if (logsByDate[today]?.status == AbstinenceStatus.Clean) {
        today
    } else {
        today.minusDays(1)
    }
    var days = 0
    while (logsByDate[cursor]?.status == AbstinenceStatus.Clean) {
        days += 1
        cursor = cursor.minusDays(1)
    }
    return days
}

private fun buildSupports(
    secondaryActivities: List<ActivityDefinition>,
    completedActivities: List<ActivityDefinition>,
    layerById: Map<String, Layer>,
    tasks: List<Task>,
): List<DashboardSupportState> {
    val completedIds = completedActivities.map { it.id }.toSet()
    val completedSecondaryCount = secondaryActivities.count { it.id in completedIds }
    val pendingSecondary = secondaryActivities.filterNot { it.id in completedIds }
    val firstSecondary = secondaryActivities.firstOrNull()
    val secondSecondary = pendingSecondary.firstOrNull { it.id != firstSecondary?.id }
        ?: secondaryActivities.getOrNull(1)
    val pendingTasks = tasks
        .filter { it.status == TaskStatus.Pending }
        .sortedBy { it.dueDate ?: "9999-99-99" }

    return listOf(
        DashboardSupportState(
            kind = DashboardSupportKind.SecondaryChecklist,
            title = "Checklist secundaria",
            value = "$completedSecondaryCount/${secondaryActivities.size}",
            copy = "cuidado basico",
            first = firstSecondary?.name ?: "Sin cuidado base",
            firstChecked = firstSecondary?.id?.let { it in completedIds } == true,
            second = secondSecondary?.name ?: layerById.values.firstOrNull()?.name.orEmpty(),
            secondChecked = secondSecondary?.id?.let { it in completedIds } == true,
        ),
        DashboardSupportState(
            kind = DashboardSupportKind.Tasks,
            title = "Pendientes",
            value = pendingTasks.size.toString(),
            copy = "tareas abiertas",
            first = pendingTasks.getOrNull(0)?.title ?: "Sin pendientes",
            firstChecked = false,
            second = pendingTasks.getOrNull(1)?.title ?: "Agregar despues",
            secondChecked = false,
        ),
    )
}

private fun buildWeekRows(
    layers: List<Layer>,
    activities: List<ActivityDefinition>,
    weekLogs: List<ActivityLog>,
    today: LocalDate,
): List<DashboardWeekRowState> {
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val completedActivityIdsByDate = weekLogs
        .filter { it.completed }
        .groupBy { it.date }
        .mapValues { (_, logs) -> logs.map { it.activityId }.toSet() }
    val activitiesByLayer = activities.groupBy { it.layerId }

    return layers.map { layer ->
        val layerActivityIds = activitiesByLayer[layer.id].orEmpty().map { it.id }.toSet()
        val activeDays = (0L..6L).count { offset ->
            val date = start.plusDays(offset).toString()
            completedActivityIdsByDate[date].orEmpty().any { it in layerActivityIds }
        }
        DashboardWeekRowState(
            layerId = layer.id,
            name = layer.name,
            score = "$activeDays/7",
            progress = activeDays / 7f,
        )
    }
}
