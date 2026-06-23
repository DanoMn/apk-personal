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
import dev.panopt.autonomia.SleepNight
import dev.panopt.autonomia.SleepSessionState
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.abstinence.AbstinencePolicy
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.activity.isGoal
import dev.panopt.autonomia.domain.activity.AnchorGraceRule
import dev.panopt.autonomia.domain.scoring.BuildScoreInputUseCase
import dev.panopt.autonomia.domain.scoring.ScoreEngine
import dev.panopt.autonomia.domain.scoring.PointsMappingPolicy
import dev.panopt.autonomia.domain.activity.ActivityTargetVersion
import dev.panopt.autonomia.domain.scoring.ScoreInputSource
import dev.panopt.autonomia.domain.scoring.StartupCounterPolicy
import dev.panopt.autonomia.domain.scoring.StartupDetectionRule
import dev.panopt.autonomia.domain.scoring.StartupProjectionUseCase
import dev.panopt.autonomia.domain.scoring.WeeklyScoreHistoryEntry
import java.time.temporal.ChronoUnit
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import dev.panopt.autonomia.domain.sleep.SleepNightScore
import dev.panopt.autonomia.domain.sleep.SleepPolicy
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
    /** phraseId resolved from the daily slot for the current day-phase. Null = no slot yet. */
    anchorPhrasePhraseId: String? = null,
    sleepNight: SleepNight?,
    sleepConfig: SleepConfig = SleepPolicy.defaultConfig(),
    sleepSession: SleepSessionState? = null,
    weeklyHistory: List<WeeklyScoreHistoryEntry> = emptyList(),
    focusSignalActivityId: String?,
    today: LocalDate = LocalDate.now(),
    targetVersions: Map<String, List<ActivityTargetVersion>> = emptyMap(),
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
        anchors = primaryActivities,
        weekLogs = weekActivityLogs,
        today = today,
    )

    // Source de hechos crudos del scoring: lo consume el camino maduro (ScoreEngine.calculate) y, en
    // arranque, la proyección (StartupProjectionUseCase). Se extrae a un val para reusarlo sin
    // duplicar el mapeo (refactor neutro: el camino maduro queda byte-idéntico).
    val scoreInputSource = ScoreInputSource(
        layers = layers,
        activities = activities,
        todayActivityLogs = todayActivityLogs,
        periodActivityLogs = periodActivityLogs,
        abstinenceTracks = abstinenceTracks,
        todayAbstinenceLogs = todayAbstinenceLogs,
        allAbstinenceLogs = allAbstinenceLogs,
        tasks = tasks,
        // WU-7: wire the today's SleepNight into the scoring path (design §7, PR3 carryover).
        // If sleepNight has a cached sleepScore (auto-materialized), pass it as a single-night
        // list. If null (NoData or manual entry), empty list → ADR-3: re-normalize Cuerpo.
        sleepNights = listOfNotNull(sleepNight?.toSleepNightScore()),
        today = today,
        weeklyHistory = weeklyHistory,
        targetVersions = targetVersions,
    )
    val scoreReport = ScoreEngine.calculate(BuildScoreInputUseCase(scoreInputSource))
    val scoreState = scoreReport.state

    // ARRANQUE (`scoring-arranque-cuenta`): si la cuenta es nueva (anclas en gracia → NoData real +
    // sin historial de score real + ≥3 capas con ancla), reemplazamos el blackout NoData por la barra
    // de arranque. El `scoreReport` real SIGUE NoData (presentación aparte, no estado del motor).
    val startup: StartupCardState? =
        if (StartupDetectionRule.isStartup(scoreReport, activities, layers, weeklyHistory, today)) {
            val anchorActivities = activities.filter {
                it.active && !it.archived && it.activityType == ActivitySurface.Anchor
            }
            val daysLived = startupDaysLived(anchorActivities, today)
            StartupProjectionUseCase(scoreInputSource, daysLived)?.let { projection ->
                StartupCounterPolicy.counter(projection.estado, daysLived).toStartupCardState()
            }
        } else {
            null
        }
    // NIVEL 7 (proyección): el número visible del dashboard es el mapeo E ESTADO→PUNTOS [650,1100].
    // Mismo cálculo de dominio puro que el motor usa para poblar el seam (PointsMappingPolicy), así
    // dashboard y snapshot semanal nunca divergen. NoData (visibleScore == null) → sin número.
    val visiblePoints: Int? = scoreReport.visibleScore?.let {
        PointsMappingPolicy.points(scoreReport.estado.toDouble())
    }
    val score = visiblePoints ?: 0
    // La lista de capas va COMPLETA en el estado (la consumen también los chips de filtro de las
    // pantallas de config de anclas/soportes/tasks). Solo MARCAMOS cuáles tienen ≥1 ancla
    // configurada con `hasAnchors`; "Capas de hoy" en el dashboard filtra por ese flag al renderizar.
    val anchorLayerIds = primaryActivities.map { it.layerId }.toSet()
    val layerStates = scoreReport.layerScores.map { layerScore ->
        DashboardLayerState(
            id = layerScore.layerId,
            name = layerScore.name,
            progress = layerScore.score,
            hasAnchors = layerScore.layerId in anchorLayerIds,
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
        headerDate = DashboardHeaderDate.format(today),
        status = DashboardStatusState(
            scoreState = scoreState,
            title = scoreTitle(scoreState),
            headline = scoreHeadline(scoreState),
            body = scoreBody(scoreState),
            score = score,
            scoreLabel = visiblePoints?.toString() ?: "--",
            progress = scoreReport.progress,
        ),
        dailyProgress = DashboardDailyProgressState(
            percent = (dailyProgress * 100f).roundToInt().coerceIn(0, 100),
            progress = dailyProgress,
            pendingLabel = pendingLabel(pendingCount),
            activeLayersLabel = "${activeLayerIds.size} de ${activeLayers.size} capas activas",
        ),
        anchorPhrase = selectAnchorPhrase(anchorPhrasePhraseId, anchorPhrases),
        layers = layerStates,
        signals = buildSignals(
            activities = timeActivities,
            logsByActivity = todayLogsByActivity,
            sleepNight = sleepNight,
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
        sleep = sleepNight.toSleepState(sleepConfig, sleepSession),
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
        startup = startup,
    )
}

/**
 * Días vividos desde la creación del ancla MÁS VIEJA (`createdAt` epoch millis), clampeado a `[1, 7]`.
 * El día de creación cuenta como día 1 (`+ 1`). Coherente con `AnchorGraceRule.GRACE_DAYS = 7`: día 8
 * ya sale de gracia y el motor maduro toma la posta. Sin anclas → 1 (defensivo).
 */
private fun startupDaysLived(anchorActivities: List<ActivityDefinition>, today: LocalDate): Int {
    val oldestCreatedAt = anchorActivities.minOfOrNull { it.createdAt } ?: return 1
    val createdDate = Instant.ofEpochMilli(oldestCreatedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val grace = AnchorGraceRule.GRACE_DAYS
    return (ChronoUnit.DAYS.between(createdDate, today) + 1).coerceIn(1L, grace).toInt()
}

/**
 * Mapea el [dev.panopt.autonomia.domain.scoring.StartupCounter] de dominio al value object de
 * presentación, armando el copy con tono adulto/compasivo (AGENTS.md): "Faltan N días para tu puntaje
 * real" (singular/plural), sin términos prohibidos ("fallaste", "deberías", tono clínico).
 */
private fun dev.panopt.autonomia.domain.scoring.StartupCounter.toStartupCardState(): StartupCardState {
    val remaining = daysRemaining
    val daysLabel = when {
        remaining <= 0 -> "Mañana llega tu puntaje real"
        remaining == 1 -> "Falta 1 día para tu puntaje real"
        else -> "Faltan $remaining días para tu puntaje real"
    }
    return StartupCardState(
        counterLabel = counterPoints.toString(),
        counterPoints = counterPoints,
        windowProgress = windowProgress,
        daysRemaining = remaining,
        daysRemainingLabel = daysLabel,
        headline = "La base está cargando",
        body = "Tu puntaje se está formando con cada acción. Sin apuro: esto recién empieza.",
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
    val visiblePoints = visibleScore?.let { PointsMappingPolicy.points(estado.toDouble()) }
    return DashboardScoreReportState(
        stateTitle = scoreTitle(state),
        headline = scoreHeadline(state),
        scoreLabel = visiblePoints?.toString() ?: "--",
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

/**
 * Pure lookup: given the phraseId resolved by [AnchorPhraseResolver] for the current
 * day-phase, find the phrase in the in-memory catalog and map it to UI state.
 *
 * This function performs NO selection logic — selection already happened in the resolver.
 * It only maps (phraseId, catalog) → (text, authorReference).
 *
 * Returns empty/neutral state when:
 * - [phraseId] is null (no slot written yet for this phase)
 * - [phraseId] is not found in [catalog] (e.g., catalog not yet loaded)
 */
private fun selectAnchorPhrase(
    phraseId: String?,
    catalog: List<AnchorPhrase>,
): DashboardAnchorPhraseState {
    if (phraseId == null) return DashboardAnchorPhraseState()
    val phrase = catalog.firstOrNull { it.id == phraseId } ?: return DashboardAnchorPhraseState()
    return DashboardAnchorPhraseState(
        text = phrase.text,
        authorReference = phrase.authorReference.orEmpty(),
    )
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
    sleepNight: SleepNight?,
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
            value = sleepNight.sleepValue(sleepConfig, sleepSession),
            meta = sleepNight.sleepMeta(sleepConfig, sleepSession),
            status = sleepNight.sleepStatus(sleepSession),
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

private fun SleepNight?.sleepValue(
    sleepConfig: SleepConfig,
    sleepSession: SleepSessionState?,
): String {
    if (sleepSession != null) return "desde ${sleepSession.startedAt}"
    val night = this ?: return "--"
    // For auto-mode nights: use onset→wake epoch millis if available.
    val onsetMs = night.sleepOnsetAt
    val wakeMs = night.definitiveWakeAt
    return if (onsetMs != null && wakeMs != null && wakeMs > onsetMs) {
        val actualMinutes = ((wakeMs - onsetMs) / 60_000L).toInt()
        val target = sleepConfig.targetMinutes()
        "${SleepPolicy.formatDuration(actualMinutes)} de ${SleepPolicy.formatDuration(target)}"
    } else {
        // Manual entry: sleptAt/wokeAt are stored in targetSleepAt/targetWakeAt fields for now.
        // Show the target window as fallback.
        val target = sleepConfig.targetMinutes()
        "${SleepPolicy.formatDuration(target)} objetivo"
    }
}

private fun SleepNight?.sleepMeta(
    sleepConfig: SleepConfig,
    sleepSession: SleepSessionState?,
): String {
    if (sleepSession != null) return "en descanso"
    val night = this ?: return "toca registrar"
    val score = night.sleepScore
    return when {
        score == null -> if (night.source == "manual") "registrado" else "sin lectura"
        score >= 0.90f -> "base cubierta"
        score >= 0.70f -> "descanso aceptable"
        else -> "descanso bajo"
    }
}

private fun SleepNight?.sleepStatus(sleepSession: SleepSessionState?): DashboardDimensionStatus =
    when {
        sleepSession != null -> DashboardDimensionStatus.Motion
        this == null -> DashboardDimensionStatus.Unknown
        else -> {
            val score = sleepScore
            when {
                score == null -> DashboardDimensionStatus.Unknown // NoData — no score available
                score >= 0.90f -> DashboardDimensionStatus.Stable
                score >= 0.70f -> DashboardDimensionStatus.Motion
                else -> DashboardDimensionStatus.Attention
            }
        }
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

private fun SleepNight?.toSleepState(
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
            sleptAt = targetSleepAt,   // best-effort display for manual entries
            wokeAt = targetWakeAt,
            note = note,
        )
    }

/**
 * Converts a [SleepNight] header (with cached sub-scores) to [SleepNightScore] for scoring path.
 * Returns null when the night has no scored data (NoData confidence or null sleepScore).
 * This is the bridge used by [buildDashboardState] to wire today's night into the daily
 * scoring path (WU-7, design §7 — connect day score to dashboard).
 */
private fun SleepNight.toSleepNightScore(): SleepNightScore? {
    val score = sleepScore ?: return null
    return SleepNightScore(
        duration = durationScore ?: 0f,
        continuity = continuityScore ?: 0f,
        alignment = alignmentScore ?: 0f,
        digitalInterruption = digitalInterruptionScore ?: 0f,
        sleepScore = score,
        confidence = runCatching {
            dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence.valueOf(confidenceLevel)
        }.getOrElse { dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence.NoData },
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

/**
 * Filas de "Semana": UNA por ancla configurada (no por capa), porque la meta de frecuencia vive por
 * actividad. Cada fila cuenta los DÍAS DISTINTOS de la semana en que esa ancla se completó, contra
 * TU meta. Dos fases:
 *  - Construyendo el hábito (activeDays < meta): denominador = meta. La barra se llena hacia tu
 *    objetivo (ej. `2/3`).
 *  - Superhábit (activeDays >= meta): cumpliste, el denominador pasa a 7 (ej. `3/7`) para ver cuántos
 *    días extra sobre la meta estás sumando hacia una semana perfecta.
 * La fila conserva el `layerId` del ancla solo para heredar el color de su capa.
 */
private fun buildWeekRows(
    anchors: List<ActivityDefinition>,
    weekLogs: List<ActivityLog>,
    today: LocalDate,
): List<DashboardWeekRowState> {
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekDates = (0L..6L).map { start.plusDays(it).toString() }.toSet()
    val completedDatesByActivity = weekLogs
        .filter { it.completed && it.date in weekDates }
        .groupBy { it.activityId }
        .mapValues { (_, logs) -> logs.map { it.date }.toSet() }

    return anchors.map { anchor ->
        val activeDays = completedDatesByActivity[anchor.id].orEmpty().size
        val target = anchor.weeklyFrequencyTarget?.coerceIn(1, 7) ?: 7
        val metGoal = activeDays >= target
        val denominator = if (metGoal) 7 else target
        DashboardWeekRowState(
            layerId = anchor.layerId,
            name = anchor.name,
            score = "$activeDays/$denominator",
            progress = if (denominator == 0) 0f else activeDays.toFloat() / denominator,
        )
    }
}
