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
import dev.panopt.autonomia.SleepConfig
import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.SleepSessionState
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.abstinence.AbstinencePolicy
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.activity.isGoal
import dev.panopt.autonomia.domain.scoring.BuildScoreInputUseCase
import dev.panopt.autonomia.domain.scoring.ScoreEngine
import dev.panopt.autonomia.domain.scoring.ScoreInputSource
import dev.panopt.autonomia.domain.scoring.WeeklyScoreHistoryEntry
import java.util.Locale
import dev.panopt.autonomia.domain.sleep.SleepPolicy
import dev.panopt.autonomia.domain.sleep.SleepScoring
import java.time.DayOfWeek
import java.time.LocalDate
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
    sleepConfig: SleepConfig = SleepPolicy.defaultConfig(),
    sleepSession: SleepSessionState? = null,
    weeklyHistory: List<WeeklyScoreHistoryEntry> = emptyList(),
    focusSignalActivityId: String?,
    today: LocalDate = LocalDate.now(),
): DashboardState {
    val activeLayers = layers.filter { it.active }.sortedBy { it.sortOrder }
    val layerById = activeLayers.associateBy { it.id }
    val visibleActivities = activities
        .filter { it.active && !it.archived && it.activityType != ActivitySurface.Task }
        .sortedBy { it.sortOrder }
    val dashboardActivities = visibleActivities
    val todayLogsByActivity = todayActivityLogs.associateBy { it.activityId }
    val completedActivities = dashboardActivities.filter { activity ->
        activity.isCompletedBy(todayLogsByActivity[activity.id])
    }
    val completedActivityIds = completedActivities.map { it.id }.toSet()

    val primaryActivities = dashboardActivities.filter { it.activityType == ActivitySurface.Anchor }
    val selfCareActivities = dashboardActivities.filter {
        it.activityType == ActivitySurface.Support
    }
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
        BuildScoreInputUseCase(
            ScoreInputSource(
                layers = layers,
                activities = activities,
                todayActivityLogs = todayActivityLogs,
                periodActivityLogs = periodActivityLogs,
                abstinenceTracks = abstinenceTracks,
                todayAbstinenceLogs = todayAbstinenceLogs,
                allAbstinenceLogs = allAbstinenceLogs,
                tasks = tasks,
                sleepLog = sleepLog,
                today = today,
                weeklyHistory = weeklyHistory,
            ),
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
    val configuredById = activities.associateBy { it.id }

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
            sleepConfig = sleepConfig,
            sleepSession = sleepSession,
            focusSignalActivityId = focusSignalActivityId,
        ),
        sobrietyTracks = activeTracks.map { track ->
            track.toDashboardSobrietyTrack(
                todayLogsByTrack = todayLogsByTrack,
                allAbstinenceLogs = allAbstinenceLogs,
                today = today,
            )
        },
        sobrietyOptions = abstinenceTracks
            .sortedBy { it.sortOrder }
            .map { track ->
                track.toDashboardSobrietyTrack(
                    todayLogsByTrack = todayLogsByTrack,
                    allAbstinenceLogs = allAbstinenceLogs,
                    today = today,
                )
            },
        anchorItems = primaryActivities.map { activity ->
            DashboardCheckItemState(
                id = activity.id,
                title = activity.name,
                layerId = activity.layerId,
                layerName = layerById[activity.layerId]?.name.orEmpty(),
                value = activity.valueLabel(),
                completed = activity.id in completedActivityIds,
                activityType = activity.activityType.name,
            )
        },
        weekRows = weekRows,
        dimensions = dimensions,
        sleep = sleepLog.toSleepState(sleepConfig, sleepSession),
        activityOptions = catalogActivities.map { activity ->
            val log = todayLogsByActivity[activity.id]
            val configured = configuredById[activity.id]
            val effective = configured ?: activity
            DashboardActivityOptionState(
                id = activity.id,
                title = effective.name,
                layerId = activity.layerId,
                layerName = layerById[activity.layerId]?.name.orEmpty(),
                targetValue = configured?.sessionTargetMinutes
                    ?: configured?.targetValue
                    ?: activity.sessionTargetMinutes
                    ?: activity.targetValue
                    ?: activity.minimumValue
                    ?: 1,
                actualValue = log?.actualValue
                    ?: configured?.sessionTargetMinutes
                    ?: configured?.targetValue
                    ?: activity.sessionTargetMinutes
                    ?: activity.targetValue
                    ?: activity.minimumValue
                    ?: 0,
                weeklyFrequencyTarget = effective.weeklyFrequencyTarget,
                sessionTargetMinutes = effective.sessionTargetMinutes,
                commitmentDurationMonths = effective.commitmentDurationMonths,
                isCompletedToday = effective.isCompletedBy(log),
                isFocusSignal = activity.id == focusSignalActivityId,
                activityType = effective.activityType.name,
                isGoal = effective.isGoal(),
                isConfigured = configured != null,
            )
        },
        supportItems = selfCareActivities.map { activity ->
            DashboardCheckItemState(
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
            .sortedWith(
                compareBy<Task> { it.dueDate ?: "9999-99-99" }
                    .thenByDescending { it.createdAt },
            )
            .map { DashboardTaskState(id = it.id, title = it.title, layerId = it.layerId) },
        completedTasks = tasks
            .filter { it.status == TaskStatus.Done }
            .sortedByDescending { it.completedAt ?: it.updatedAt }
            .map { DashboardTaskState(id = it.id, title = it.title, layerId = it.layerId) },
        scoreReport = scoreReport.toDashboardScoreReportState(),
    )
}

internal fun weekStartKey(today: LocalDate = LocalDate.now()): String =
    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()

private fun ActivityDefinition.isCompletedBy(log: ActivityLog?): Boolean {
    if (log == null) return false
    if (log.completed) return true
    val actual = log.actualValue ?: return false
    val minimum = minimumValue ?: sessionTargetMinutes ?: targetValue ?: return false
    return actual >= minimum
}

private fun ActivityDefinition.valueLabel(): String =
    when (unit) {
        ActivityUnit.Minutes -> "${sessionTargetMinutes ?: targetValue ?: minimumValue ?: 0} min"
        ActivityUnit.Count -> "${sessionTargetMinutes ?: targetValue ?: minimumValue ?: 1}x"
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

private fun dev.panopt.autonomia.domain.scoring.ScoreReport.toDashboardScoreReportState(): DashboardScoreReportState {
    val worstLayerName = worstLayerId
        ?.let { id -> layerScores.firstOrNull { it.layerId == id }?.name }
        ?: "Sin capa baja"
    return DashboardScoreReportState(
        stateTitle = scoreTitle(state),
        headline = scoreHeadline(state),
        scoreLabel = visibleScore?.toString() ?: "--",
        progress = progress,
        weeklyBaseLabel = scoreRatioLabel(weeklyBaseScore),
        weeklyScoreLabel = scoreRatioLabel(weeklyScore),
        averageLayerLabel = scoreRatioLabel(averageLayerScore),
        worstLayerLabel = worstLayerName,
        stabilityLabel = stabilityScore?.let {
            "${scoreRatioLabel(it)} / $stabilityWeeks semanas"
        } ?: "Sin memoria suficiente",
        reasons = reasons.ifEmpty { listOf(scoreBody(state)) },
        layers = layerScores.map { layer ->
            DashboardScoreLayerReportState(
                layerId = layer.layerId,
                name = layer.name,
                scoreLabel = scoreRatioLabel(layer.rawScore),
                baseLabel = scoreRatioLabel(layer.baseScore),
                progress = layer.score,
                anchorLabel = layer.anchorScore.scoreOrDash(),
                supportLabel = layer.supportScore.scoreOrDash(),
                surplusLabel = if (layer.anchorSurplusBonus > 0f) {
                    "+${scoreRatioLabel(layer.anchorSurplusBonus)}"
                } else {
                    "--"
                },
                taskMomentumLabel = if (layer.taskMomentumBonus > 0f) {
                    "+${scoreRatioLabel(layer.taskMomentumBonus)}"
                } else {
                    "--"
                },
                sleepLabel = layer.sleepScore.scoreOrDash(),
                sobrietyLabel = layer.sobrietyScore.scoreOrDash(),
            )
        },
    )
}

private fun Float?.scoreOrDash(): String =
    this?.let(::scoreRatioLabel) ?: "--"

private fun scoreRatioLabel(value: Float): String =
    String.format(Locale.US, "%.3f", value.coerceAtLeast(0f))

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
    sleepConfig: SleepConfig,
    sleepSession: SleepSessionState?,
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
            value = sleepLog.sleepValue(sleepConfig, sleepSession),
            meta = sleepLog.sleepMeta(sleepConfig, sleepSession),
            status = sleepLog.sleepStatus(sleepSession),
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

private fun SleepLog?.sleepValue(
    sleepConfig: SleepConfig,
    sleepSession: SleepSessionState?,
): String {
    if (sleepSession != null) return "desde ${sleepSession.startedAt}"
    val log = this ?: return "--"
    val minutes = SleepPolicy.minutesBetween(log.sleptAt, log.wokeAt) ?: return "--"
    val target = sleepConfig.targetMinutes()
    return "${SleepPolicy.formatDuration(minutes)} de ${SleepPolicy.formatDuration(target)}"
}

private fun SleepLog?.sleepMeta(
    sleepConfig: SleepConfig,
    sleepSession: SleepSessionState?,
): String {
    if (sleepSession != null) return "en descanso"
    val log = this ?: return "toca registrar"
    val actual = SleepPolicy.minutesBetween(log.sleptAt, log.wokeAt) ?: return "toca registrar"
    val target = sleepConfig.targetMinutes()
    return if (actual >= target) "base cubierta" else "descanso bajo"
}

private fun SleepLog?.sleepStatus(sleepSession: SleepSessionState?): DashboardDimensionStatus =
    when {
        sleepSession != null -> DashboardDimensionStatus.Motion
        this == null -> DashboardDimensionStatus.Unknown
        SleepScoring.score(this) >= 0.90f -> DashboardDimensionStatus.Stable
        SleepScoring.score(this) >= 0.70f -> DashboardDimensionStatus.Motion
        else -> DashboardDimensionStatus.Attention
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

private fun SleepLog?.toSleepState(
    sleepConfig: SleepConfig,
    sleepSession: SleepSessionState?,
): DashboardSleepState =
    if (this == null) {
        DashboardSleepState(
            targetSleepAt = sleepConfig.targetSleepAt,
            targetWakeAt = sleepConfig.targetWakeAt,
            targetMinutes = sleepConfig.targetMinutes(),
            digitalWindDownMinutes = sleepConfig.digitalWindDownMinutes,
            pendingStartedAt = sleepSession?.startedAt.orEmpty(),
            pendingDate = sleepSession?.date.orEmpty(),
        )
    } else {
        DashboardSleepState(
            targetSleepAt = sleepConfig.targetSleepAt,
            targetWakeAt = sleepConfig.targetWakeAt,
            targetMinutes = sleepConfig.targetMinutes(),
            digitalWindDownMinutes = sleepConfig.digitalWindDownMinutes,
            pendingStartedAt = sleepSession?.startedAt.orEmpty(),
            pendingDate = sleepSession?.date.orEmpty(),
            sleptAt = sleptAt,
            wokeAt = wokeAt,
            note = note,
        )
    }

private fun SleepConfig.targetMinutes(): Int =
    SleepPolicy.plannedWindowMinutes(targetSleepAt, targetWakeAt)
        ?: SleepPolicy.DEFAULT_SLEEP_WINDOW_MINUTES

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

private fun AbstinenceTrack.toDashboardSobrietyTrack(
    todayLogsByTrack: Map<String, AbstinenceLog>,
    allAbstinenceLogs: List<AbstinenceLog>,
    today: LocalDate,
): DashboardSobrietyTrackState {
    val status = todayLogsByTrack[id]?.status ?: AbstinenceStatus.Unknown
    return DashboardSobrietyTrackState(
        id = id,
        label = name,
        days = streakDays(id, allAbstinenceLogs, today),
        meta = sobrietyMeta(status),
        status = sobrietyStatus(status, severity),
        active = active,
        isCustom = AbstinencePolicy.isCustomTrack(this),
        severity = severity.name,
        isRelapseToday = status == AbstinenceStatus.Relapse,
        isMarkedCleanToday = status == AbstinenceStatus.Clean,
    )
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
