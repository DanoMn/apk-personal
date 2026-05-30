package dev.panopt.autonomia

import android.content.Context
import dev.panopt.autonomia.data.AbstinenceLogEntity
import dev.panopt.autonomia.data.AbstinenceRelapseEventEntity
import dev.panopt.autonomia.data.AbstinenceTrackEntity
import dev.panopt.autonomia.data.ActivityDefinitionEntity
import dev.panopt.autonomia.data.AutonomiaDatabase
import dev.panopt.autonomia.data.DailyActivityLogEntity
import dev.panopt.autonomia.data.DailyClosureEntity
import dev.panopt.autonomia.data.RiskEventEntity
import dev.panopt.autonomia.data.SleepConfigEntity
import dev.panopt.autonomia.data.SleepNightEntity
import dev.panopt.autonomia.data.SleepSessionStateEntity
import dev.panopt.autonomia.data.TaskEntity
import dev.panopt.autonomia.data.UserActivityConfigEntity
import dev.panopt.autonomia.data.scoring.WeeklyScoreSnapshotWriter
import dev.panopt.autonomia.data.scoring.toHistoryEntry
import dev.panopt.autonomia.data.local.mapper.toDomain
import dev.panopt.autonomia.domain.activity.normalizeAnchorSessionTargetMinutes
import dev.panopt.autonomia.domain.activity.normalizeAnchorWeeklyFrequencyTarget
import dev.panopt.autonomia.data.local.mapper.mergeToDomain
import dev.panopt.autonomia.data.local.seed.DefaultSeeds
import dev.panopt.autonomia.domain.abstinence.AbstinencePolicy
import dev.panopt.autonomia.domain.abstinence.AbstinenceRelapseMaterializationPolicy
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.activity.defaultActualValue
import dev.panopt.autonomia.data.SleepSegmentEntity
import dev.panopt.autonomia.data.repository.TelemetryRepository
import dev.panopt.autonomia.platform.telemetry.DeviceTelemetryWorkScheduler
import dev.panopt.autonomia.platform.telemetry.TelemetryPermissionState
import dev.panopt.autonomia.domain.sleep.SleepAutoModeResult
import dev.panopt.autonomia.domain.sleep.SleepConfigValidation
import dev.panopt.autonomia.domain.sleep.SleepNightScore
import dev.panopt.autonomia.domain.sleep.SleepPolicy
import dev.panopt.autonomia.domain.sleep.SleepScoring
import dev.panopt.autonomia.domain.sleep.SleepTargetWindow
import dev.panopt.autonomia.domain.sleep.interpretation.InterpretationParams
import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
import dev.panopt.autonomia.domain.sleep.interpretation.SleepInterpreter
import dev.panopt.autonomia.domain.sleep.interpretation.SleepSegmentKind
import dev.panopt.autonomia.domain.task.TaskPolicy
import dev.panopt.autonomia.domain.scoring.WeeklyScoreHistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

class AutonomiaRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("autonomia_prefs", Context.MODE_PRIVATE)
    private val dao = AutonomiaDatabase.getInstance(appContext).autonomiaDao()
    private val weeklyScoreSnapshotWriter = WeeklyScoreSnapshotWriter(dao)
    private val telemetryRepository = TelemetryRepository(appContext)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    private val _focusSignalActivityId = MutableStateFlow(prefs.getString("focus_signal_activity_id", null))
    private val _isInitialConfigurationComplete = MutableStateFlow(
        prefs.getBoolean("initial_configuration_complete", false),
    )
    private val _isSleepAutoModeEnabled = MutableStateFlow(prefs.getBoolean("sleep_auto_mode_enabled", false))

    fun isDarkModeFlow(): StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun isSleepAutoModeEnabledFlow(): StateFlow<Boolean> = _isSleepAutoModeEnabled.asStateFlow()

    fun focusSignalActivityIdFlow(): StateFlow<String?> = _focusSignalActivityId.asStateFlow()

    fun isInitialConfigurationCompleteFlow(): StateFlow<Boolean> =
        _isInitialConfigurationComplete.asStateFlow()

    suspend fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    suspend fun setFocusSignalActivity(activityId: String?) {
        prefs.edit().putString("focus_signal_activity_id", activityId).apply()
        _focusSignalActivityId.value = activityId
    }

    suspend fun setInitialConfigurationComplete(completed: Boolean) {
        prefs.edit().putBoolean("initial_configuration_complete", completed).apply()
        _isInitialConfigurationComplete.value = completed
    }

    fun allActivityLogsFlow(): Flow<List<ActivityLog>> =
        dao.observeAllActivityLogs().map { logs -> logs.map { it.toDomain() } }

    fun layersFlow(): Flow<List<Layer>> =
        dao.observeLayers().map { layers -> layers.map { it.toDomain() } }

    fun activityLogsForDateFlow(date: String): Flow<List<ActivityLog>> =
        dao.observeActivityLogsForDate(date).map { logs -> logs.map { it.toDomain() } }

    fun activityLogsBetweenFlow(startDate: String, endDate: String): Flow<List<ActivityLog>> =
        dao.observeActivityLogsBetween(startDate, endDate).map { logs -> logs.map { it.toDomain() } }

    fun abstinenceTracksFlow(): Flow<List<AbstinenceTrack>> =
        dao.observeAbstinenceTracks().map { tracks -> tracks.map { it.toDomain() } }

    fun abstinenceLogsForDateFlow(date: String): Flow<List<AbstinenceLog>> =
        dao.observeAbstinenceLogsForDate(date).map { logs -> logs.map { it.toDomain() } }

    fun allAbstinenceLogsFlow(): Flow<List<AbstinenceLog>> =
        dao.observeAllAbstinenceLogs().map { logs -> logs.map { it.toDomain() } }

    fun riskEventsForDateFlow(date: String): Flow<List<RiskEvent>> =
        dao.observeRiskEventsForDate(date).map { events -> events.map { it.toDomain() } }

    fun tasksFlow(): Flow<List<Task>> =
        dao.observeTasks().map { tasks -> tasks.map { it.toDomain() } }

    fun anchorPhrasesFlow(): Flow<List<AnchorPhrase>> =
        dao.observeAnchorPhrases().map { phrases -> phrases.map { it.toDomain() } }

    // v12+: uses sleep_nights table. Returns SleepNight (null if no night recorded).
    fun sleepNightForDateFlow(date: String): Flow<SleepNight?> =
        dao.observeSleepNightForDate(date).map { it?.toDomain() }

    // Legacy alias kept for UI compatibility — maps SleepNight to SleepLog shape.
    // TODO(WU-6): remove when all UI is updated to SleepNight.
    @Deprecated("Use sleepNightForDateFlow — SleepLog maps to dropped sleep_logs table (v12).")
    fun sleepLogForDateFlow(date: String): Flow<SleepLog?> = kotlinx.coroutines.flow.flowOf(null)

    fun sleepConfigFlow(): Flow<SleepConfig> =
        dao.observeSleepConfig(SleepPolicy.DEFAULT_CONFIG_ID)
            .map { it?.toDomain() ?: SleepPolicy.defaultConfig() }

    fun sleepSessionStateFlow(): Flow<SleepSessionState?> =
        dao.observeSleepSessionState(SleepPolicy.DEFAULT_SESSION_ID)
            .map { it?.toDomain() }

    fun weeklyScoreHistoryFlow(): Flow<List<WeeklyScoreHistoryEntry>> =
        dao.observeWeeklyScoreSnapshots()
            .map { snapshots -> snapshots.map { it.toHistoryEntry() } }

    suspend fun ensureSeeded() {
        // Layers: only insert on first run (stable, user-agnostic)
        if (dao.layerCount() == 0) {
            dao.upsertLayers(DefaultSeeds.layers)
        }

        // Activities and abstinence tracks: always upsert so new seeds
        // reach existing installations without losing user-configured ones.
        dao.upsertActivityDefinitions(DefaultSeeds.activityDefinitions)
        dao.upsertAbstinenceTracks(DefaultSeeds.abstinenceTracks)

        if (dao.getSleepConfig(SleepPolicy.DEFAULT_CONFIG_ID) == null) {
            val config = SleepPolicy.defaultConfig()
            dao.upsertSleepConfig(
                SleepConfigEntity(
                    id = SleepPolicy.DEFAULT_CONFIG_ID,
                    targetSleepAt = config.targetSleepAt,
                    targetWakeAt = config.targetWakeAt,
                    digitalWindDownMinutes = config.digitalWindDownMinutes,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun closeElapsedActivityDays(
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        source: String = "app_open",
    ) {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val yesterday = today.minusDays(1)
        if (yesterday.isBefore(weekStart)) return

        var cursor = weekStart
        while (!cursor.isAfter(yesterday)) {
            closeActivityDay(date = cursor, zoneId = zoneId, source = source)
            cursor = cursor.plusDays(1)
        }
    }

    suspend fun closeActivityDay(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        source: String = "manual",
    ) {
        val dateKey = date.toString()
        if (dao.getDailyClosure(dateKey) != null) return

        val definitionsById = dao.getActivityDefinitionsSnapshot().associateBy { it.id }
        val activeConfigs = dao.getActiveUserActivityConfigs()
        val existingActivityIds = dao.getActivityLogsForDate(dateKey)
            .map { it.subjectId }
            .toSet()
        val now = System.currentTimeMillis()
        val closureLogs = activeConfigs
            .filter { config ->
                config.activityType == ActivitySurface.Anchor.name ||
                    config.activityType == ActivitySurface.Support.name
            }
            .filter { config -> definitionsById.containsKey(config.activityId) }
            .filter { config -> config.activityId !in existingActivityIds }
            .filter { config -> date >= config.createdLocalDate(zoneId) }
            .map { config ->
                val status = if (config.activityType == ActivitySurface.Support.name) {
                    DailyActivityStatus.Done
                } else {
                    DailyActivityStatus.NotDone
                }
                DailyActivityLogEntity(
                    date = dateKey,
                    timezoneId = zoneId.id,
                    subjectType = config.activityType,
                    subjectId = config.activityId,
                    layerId = definitionsById[config.activityId]?.layerId,
                    status = status.name,
                    actualValue = if (status == DailyActivityStatus.NotDone) 0 else null,
                    createdAt = now,
                    updatedAt = now,
                )
            }

        if (closureLogs.isNotEmpty()) {
            dao.upsertActivityLogs(closureLogs)
        }
        dao.upsertDailyClosure(
            DailyClosureEntity(
                date = dateKey,
                timezoneId = zoneId.id,
                closedAt = now,
                source = source,
                closureVersion = DAILY_CLOSURE_VERSION,
            ),
        )
    }

    suspend fun refreshCurrentWeeklyScoreSnapshot(
        today: LocalDate = LocalDate.now(),
    ) {
        weeklyScoreSnapshotWriter.refreshCurrentWeek(today = today)
    }

    suspend fun materializeAssumedAbstinenceRelapses(
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val tracks = dao.getAbstinenceTracksSnapshot().map { it.toDomain() }
        val logs = dao.getAllAbstinenceLogsSnapshot().map { it.toDomain() }
        val ranges = AbstinenceRelapseMaterializationPolicy.assumedRanges(
            tracks = tracks,
            logs = logs,
            today = today,
            zoneId = zoneId,
        )
        if (ranges.isEmpty()) return

        val now = System.currentTimeMillis()
        val relapseLogs = ranges.flatMap { range ->
            range.dates.map { date ->
                AbstinenceLogEntity(
                    trackId = range.trackId,
                    date = date.toString(),
                    status = AbstinenceStatus.Relapse.name,
                    note = ASSUMED_RELAPSE_NOTE,
                    updatedAt = now,
                )
            }
        }
        val events = ranges.map { range ->
            AbstinenceRelapseEventEntity(
                id = assumedRelapseEventId(range.trackId, range.startDate),
                trackId = range.trackId,
                startDate = range.startDate.toString(),
                endDate = range.endDate.toString(),
                source = ASSUMED_RELAPSE_SOURCE,
                userAdjusted = false,
                note = ASSUMED_RELAPSE_NOTE,
                createdAt = now,
                updatedAt = now,
            )
        }

        dao.upsertAbstinenceLogs(relapseLogs)
        dao.upsertAbstinenceRelapseEvents(events)
    }

    suspend fun setActivityCompleted(
        activity: ActivityDefinition,
        completed: Boolean,
        date: String = todayKey(),
    ) {
        if (!completed) {
            dao.deleteActivityLog(activity.id, date)
            return
        }

        val now = System.currentTimeMillis()
        val status = if (activity.activityType == ActivitySurface.Support) {
            DailyActivityStatus.Omitted
        } else {
            DailyActivityStatus.Done
        }
        dao.upsertActivityLog(
            DailyActivityLogEntity(
                date = date,
                timezoneId = ZoneId.systemDefault().id,
                subjectType = activity.activityType.name,
                subjectId = activity.id,
                layerId = activity.layerId,
                status = status.name,
                actualValue = activity.defaultActualValue(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun setActivityValue(
        activity: ActivityDefinition,
        actualValue: Int,
        date: String = todayKey(),
    ) {
        val now = System.currentTimeMillis()
        dao.upsertActivityLog(
            DailyActivityLogEntity(
                date = date,
                timezoneId = ZoneId.systemDefault().id,
                subjectType = activity.activityType.name,
                subjectId = activity.id,
                layerId = activity.layerId,
                status = DailyActivityStatus.Done.name,
                actualValue = actualValue.coerceAtLeast(0),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun markAbstinenceClean(track: AbstinenceTrack, date: String = todayKey()) {
        markAbstinenceClean(track.id, date)
    }

    suspend fun markAbstinenceClean(trackId: String, date: String = todayKey()) {
        val track = dao.getAbstinenceTrack(trackId)?.toDomain() ?: return
        if (!AbstinencePolicy.canRecordDailyLog(track)) return

        dao.upsertAbstinenceLog(
            AbstinenceLogEntity(
                trackId = trackId,
                date = date,
                status = AbstinenceStatus.Clean.name,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markAbstinenceRelapse(track: AbstinenceTrack, date: String = todayKey()) {
        markAbstinenceRelapse(track.id, date)
    }

    suspend fun markAbstinenceRelapse(trackId: String, date: String = todayKey()) {
        val track = dao.getAbstinenceTrack(trackId)?.toDomain() ?: return
        if (!AbstinencePolicy.canRecordDailyLog(track)) return

        dao.upsertAbstinenceLog(
            AbstinenceLogEntity(
                trackId = trackId,
                date = date,
                status = AbstinenceStatus.Relapse.name,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun clearAbstinenceLog(trackId: String, date: String = todayKey()) {
        dao.deleteAbstinenceLog(trackId, date)
    }

    suspend fun setAbstinenceTrackActive(trackId: String, active: Boolean) {
        val track = dao.getAbstinenceTrack(trackId) ?: return
        dao.setAbstinenceTrackActive(
            trackId = track.id,
            active = active,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun createCustomAbstinenceTrack(name: String) {
        val draft = AbstinencePolicy.createCustomDraft(name) ?: return
        val now = System.currentTimeMillis()
        dao.upsertAbstinenceTrack(
            AbstinenceTrackEntity(
                id = "trk_custom_${UUID.randomUUID()}",
                name = draft.name,
                substanceLabel = draft.substanceLabel,
                severity = draft.severity.name,
                contributionRole = draft.contributionRole.name,
                importanceTier = draft.importanceTier.name,
                active = draft.active,
                sortOrder = now.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun deleteCustomAbstinenceTrack(trackId: String) {
        val track = dao.getAbstinenceTrack(trackId)?.toDomain() ?: return
        if (!AbstinencePolicy.canDelete(track)) return

        dao.deleteAbstinenceLogsForTrack(trackId)
        dao.deleteAbstinenceTrack(trackId)
    }

    suspend fun recordDashboardRiskEvent(date: String = todayKey()) {
        val now = System.currentTimeMillis()
        dao.upsertRiskEvent(
            RiskEventEntity(
                id = UUID.randomUUID().toString(),
                date = date,
                createdAt = now,
                intensity = 6,
                trigger = "dashboard",
                actionTaken = "Registrar evento desde dashboard",
                actedOnImpulse = false,
                note = "",
            ),
        )
    }

    /**
     * Materializes the sleep night for [nightDate] by:
     *   1. Reading telemetry events for the detection window (20:00 D-1 → 12:00 D).
     *   2. Running [SleepInterpreter] → [NightTimeline].
     *   3. Scoring via [SleepScoring.scoreNight] → [SleepNightScore?].
     *   4. Persisting [SleepNightEntity] + [SleepSegmentEntity]s (idempotent: overwrites on re-run).
     *
     * Convivencia: if a night with source="manual" already exists, this method does NOT overwrite
     * it — manual entries take precedence (design §6.3).
     *
     * @return true if the night was materialized (or already existed as manual), false on error.
     */
    suspend fun materializeSleepNight(
        nightDate: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        // Convivencia: if manual night exists, skip auto materialization
        val existing = dao.getSleepNight(nightDate.toString())
        if (existing != null && existing.source == "manual") return true

        // 1. Detection window: 20:00 of D-1 → 12:00 of D (epoch millis)
        val from = nightDate.minusDays(1)
            .atTime(20, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val to = nightDate
            .atTime(12, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        // 2. Pull telemetry events
        val events = telemetryRepository.eventsInRange(from, to)

        // 3. Get sleep config for the target window
        val config = currentSleepConfig()
        val target = SleepTargetWindow(
            targetSleepAt = config.targetSleepAt,
            targetWakeAt = config.targetWakeAt,
        )

        // 4. Interpret timeline (pure)
        val timeline = SleepInterpreter.interpret(events, target, InterpretationParams.DEFAULT)

        // 5. Score the night (null for NoData)
        val score: SleepNightScore? = SleepScoring.scoreNight(timeline, target)

        val now = System.currentTimeMillis()

        // 6. Persist night header (upsert — idempotent)
        dao.upsertSleepNight(
            SleepNightEntity(
                nightDate = nightDate.toString(),
                targetSleepAt = config.targetSleepAt,
                targetWakeAt = config.targetWakeAt,
                sleepOnsetAt = timeline.sleepOnsetAt?.toEpochMilli(),
                definitiveWakeAt = timeline.definitiveWakeAt?.toEpochMilli(),
                confidenceLevel = timeline.confidence.name,
                durationScore = score?.duration,
                continuityScore = score?.continuity,
                alignmentScore = score?.alignment,
                digitalInterruptionScore = score?.digitalInterruption,
                sleepScore = score?.sleepScore,
                note = "",
                source = "auto",
                updatedAt = now,
            ),
        )

        // 7. Replace segments (delete + insert — idempotent)
        dao.deleteSleepSegmentsForNight(nightDate.toString())
        val segmentEntities = timeline.segments.map { seg ->
            SleepSegmentEntity(
                nightDate = nightDate.toString(),
                startAt = seg.startAt.toEpochMilli(),
                endAt = seg.endAt.toEpochMilli(),
                kind = seg.kind.name,
            )
        }
        if (segmentEntities.isNotEmpty()) {
            dao.insertSleepSegments(segmentEntities)
        }

        return true
    }

    // v12+: writes a SleepNightEntity with source="manual" plus a single Asleep segment.
    // SleepQuality.Acceptable removed (bug §10) — quality is not stored; scoring uses 4-component pipeline.
    // Manual entries use a single Asleep segment spanning sleptAt→wokeAt (WU-7 completion).
    suspend fun saveSleepLog(
        sleptAt: String,
        wokeAt: String,
        note: String,
        date: String = todayKey(),
    ): Boolean {
        val config = currentSleepConfig()
        val resolvedSleptAt = sleptAt.ifBlank { config.targetSleepAt }
        val resolvedWokeAt = wokeAt.ifBlank { config.targetWakeAt }
        SleepPolicy.minutesBetween(resolvedSleptAt, resolvedWokeAt) ?: return false

        val now = System.currentTimeMillis()
        dao.upsertSleepNight(
            SleepNightEntity(
                nightDate = date,
                targetSleepAt = config.targetSleepAt,
                targetWakeAt = config.targetWakeAt,
                sleepOnsetAt = null,       // manual entry — no telemetry onset
                definitiveWakeAt = null,   // manual entry — no telemetry wake detection
                confidenceLevel = "NoData", // manual entry = no telemetry signal
                durationScore = null,
                continuityScore = null,
                alignmentScore = null,
                digitalInterruptionScore = null,
                sleepScore = null,         // manual entries have no scored timeline
                note = note,
                source = "manual",
                updatedAt = now,
            ),
        )

        // Write a single Asleep segment spanning sleptAt → wokeAt (design §6.3, WU-7).
        // sleptAt/wokeAt are "HH:mm" strings — convert to epoch millis anchored to `date`.
        val zone = ZoneId.systemDefault()
        val sleepMillis = SleepPolicy.timeStringToEpochMillis(resolvedSleptAt, date, zone)
        val wakeMillis = SleepPolicy.timeStringToEpochMillis(resolvedWokeAt, date, zone)
        if (sleepMillis != null && wakeMillis != null) {
            val wakeAdjusted = if (wakeMillis <= sleepMillis) wakeMillis + 86_400_000L else wakeMillis
            dao.deleteSleepSegmentsForNight(date)
            dao.insertSleepSegments(
                listOf(
                    SleepSegmentEntity(
                        nightDate = date,
                        startAt = sleepMillis,
                        endAt = wakeAdjusted,
                        kind = SleepSegmentKind.Asleep.name,
                    ),
                ),
            )
        }
        return true
    }

    suspend fun startSleepSession(
        date: String = todayKey(),
        startedAt: String = currentTimeKey(),
    ): Boolean {
        SleepPolicy.minutesBetween(startedAt, startedAt) ?: return false
        dao.upsertSleepSessionState(
            SleepSessionStateEntity(
                id = SleepPolicy.DEFAULT_SESSION_ID,
                date = date,
                startedAt = startedAt,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return true
    }

    suspend fun finishSleepSession(note: String = ""): Boolean {
        val session = dao.getSleepSessionState(SleepPolicy.DEFAULT_SESSION_ID)?.toDomain() ?: return false
        val saved = saveSleepLog(
            sleptAt = session.startedAt,
            wokeAt = currentTimeKey(),
            note = note,
            date = session.date,
        )
        if (saved) {
            dao.deleteSleepSessionState(SleepPolicy.DEFAULT_SESSION_ID)
        }
        return saved
    }

    /**
     * Toggles the automatic sleep detection mode.
     *
     * When [enabled] = true:
     *   - If permission is GRANTED → registers the "sleep" telemetry consumer (starts drain if needed).
     *   - If permission is MISSING → returns [SleepAutoModeResult.PermissionRequired] without activating.
     * When [enabled] = false → unregisters the "sleep" consumer (stops drain when no other consumers).
     *
     * The enabled state is persisted in SharedPreferences (survives app restart).
     * Manual mode (startSleepSession/finishSleepSession) is unaffected by this toggle.
     */
    suspend fun toggleSleepAutoMode(enabled: Boolean): SleepAutoModeResult {
        if (enabled) {
            val permState = telemetryRepository.permissionState()
            if (permState == TelemetryPermissionState.MISSING) {
                return SleepAutoModeResult.PermissionRequired
            }
            DeviceTelemetryWorkScheduler.register(appContext, "sleep")
        } else {
            DeviceTelemetryWorkScheduler.unregister(appContext, "sleep")
        }
        prefs.edit().putBoolean("sleep_auto_mode_enabled", enabled).apply()
        _isSleepAutoModeEnabled.value = enabled
        return SleepAutoModeResult.Success(enabled)
    }

    suspend fun saveSleepConfig(
        targetSleepAt: String,
        targetWakeAt: String,
        digitalWindDownMinutes: Int,
    ): Boolean {
        val validation = SleepPolicy.validateConfig(
            targetSleepAt = targetSleepAt,
            targetWakeAt = targetWakeAt,
            digitalWindDownMinutes = digitalWindDownMinutes,
        )
        val config = when (validation) {
            is SleepConfigValidation.Valid -> validation.config
            is SleepConfigValidation.Invalid -> return false
        }
        dao.upsertSleepConfig(
            SleepConfigEntity(
                id = SleepPolicy.DEFAULT_CONFIG_ID,
                targetSleepAt = config.targetSleepAt,
                targetWakeAt = config.targetWakeAt,
                digitalWindDownMinutes = config.digitalWindDownMinutes,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return true
    }

    private suspend fun currentSleepConfig(): SleepConfig =
        dao.getSleepConfig(SleepPolicy.DEFAULT_CONFIG_ID)?.toDomain() ?: SleepPolicy.defaultConfig()

    suspend fun createTask(
        title: String,
        layerId: String?,
    ) {
        val draft = TaskPolicy.createDraft(title = title, layerId = layerId) ?: return
        if (draft.layerId != null && dao.getLayer(draft.layerId) == null) return

        val now = System.currentTimeMillis()
        dao.upsertTask(
            TaskEntity(
                id = "task_${UUID.randomUUID()}",
                title = draft.title,
                description = "",
                layerId = draft.layerId,
                projectId = null,
                status = TaskStatus.Pending.name,
                contributionRole = draft.contributionRole.name,
                importanceTier = draft.importanceTier.name,
                dueDate = null,
                completedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun completeTask(taskId: String) {
        val task = dao.getTask(taskId)?.toDomain() ?: return
        if (!TaskPolicy.canComplete(task)) return

        val now = System.currentTimeMillis()
        dao.updateTaskStatus(
            taskId = taskId,
            status = TaskStatus.Done.name,
            completedAt = now,
            updatedAt = now,
        )
        dao.upsertActivityLog(
            DailyActivityLogEntity(
                date = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                timezoneId = ZoneId.systemDefault().id,
                subjectType = ActivitySurface.Task.name,
                subjectId = taskId,
                layerId = task.layerId,
                status = DailyActivityStatus.Done.name,
                actualValue = 1,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun reactivateTask(taskId: String) {
        val task = dao.getTask(taskId)?.toDomain() ?: return
        if (!TaskPolicy.canReactivate(task)) return
        task.completedAt
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString() }
            ?.let { completedDate -> dao.deleteActivityLog(taskId, completedDate) }

        dao.updateTaskStatus(
            taskId = taskId,
            status = TaskStatus.Pending.name,
            completedAt = null,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun addActivityAsAnchor(
        activityId: String,
        sessionTargetMinutes: Int,
        weeklyFrequencyTarget: Int,
        commitmentDurationMonths: Int? = null,
    ) {
        val normalizedSessionTarget = normalizeAnchorSessionTargetMinutes(sessionTargetMinutes)
        val normalizedWeeklyTarget = normalizeAnchorWeeklyFrequencyTarget(weeklyFrequencyTarget)
        configureActivity(
            activityId = activityId,
            activityType = ActivitySurface.Anchor,
            cadence = ActivityCadence.Weekly,
            targetValue = normalizedSessionTarget,
            targetCount = normalizedWeeklyTarget,
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = normalizedWeeklyTarget,
            sessionTargetMinutes = normalizedSessionTarget,
            commitmentDurationMonths = commitmentDurationMonths,
        )
    }

    suspend fun removeActivityAsAnchor(activityId: String) {
        dao.deleteUserActivityConfig(activityId)
    }

    // --- New repository methods for v4 entity split ---

    fun observeActivityDefinitions(): Flow<List<ActivityDefinitionEntity>> =
        dao.observeActivityDefinitions()

    fun observeUserActivityConfigs(): Flow<List<UserActivityConfigEntity>> =
        dao.observeUserActivityConfigs()

    fun observeConfiguredActivities(): Flow<List<ActivityDefinition>> =
        dao.observeUserActivityConfigs().combine(dao.observeActivityDefinitions()) { configs, definitions ->
            val definitionMap = definitions.associateBy { it.id }
            configs.mapNotNull { config ->
                definitionMap[config.activityId]?.let { def ->
                    mergeToDomain(def, config)
                }
            }
        }

    fun observeCatalogActivities(): Flow<List<ActivityDefinition>> =
        dao.observeActivityDefinitions().map { definitions ->
            definitions.map { it.toDomain() }
        }

    suspend fun configureActivity(
        activityId: String,
        activityType: ActivitySurface,
        cadence: ActivityCadence? = null,
        targetValue: Int? = null,
        minimumValue: Int? = null,
        targetCount: Int? = null,
        targetPeriod: TargetPeriod? = null,
        weeklyFrequencyTarget: Int? = null,
        sessionTargetMinutes: Int? = null,
        commitmentDurationMonths: Int? = null,
        customName: String? = null,
        customDescription: String? = null,
    ) {
        val now = System.currentTimeMillis()
        dao.upsertUserActivityConfig(
            UserActivityConfigEntity(
                activityId = activityId,
                activityType = activityType.name,
                cadence = cadence?.name,
                targetValue = targetValue,
                minimumValue = minimumValue,
                targetCount = targetCount,
                targetPeriod = targetPeriod?.name,
                weeklyFrequencyTarget = weeklyFrequencyTarget,
                sessionTargetMinutes = sessionTargetMinutes,
                commitmentDurationMonths = commitmentDurationMonths,
                customName = customName?.trim()?.takeIf { it.isNotBlank() },
                customDescription = customDescription?.trim()?.takeIf { it.isNotBlank() },
                sortOrder = now.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun toggleActivityArchive(activityId: String, archived: Boolean) {
        dao.toggleUserActivityConfigActive(
            activityId = activityId,
            active = !archived,
            archived = archived,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun deleteUserActivityConfig(activityId: String) {
        dao.deleteUserActivityConfig(activityId)
    }

    suspend fun deleteCustomActivity(activityId: String) {
        if (!isCustomActivityId(activityId)) return

        dao.deleteActivityLogsForActivity(activityId)
        dao.deleteActivityDefinition(activityId)
    }

    suspend fun upsertActivityDefinition(definition: ActivityDefinitionEntity) {
        dao.upsertActivityDefinition(definition)
    }

    suspend fun upsertUserActivityConfig(config: UserActivityConfigEntity) {
        dao.upsertUserActivityConfig(config)
    }

    // ── Support-specific methods (validated, no targets) ──

    suspend fun addSupport(activityId: String) {
        val definition = dao.getActivityDefinition(activityId) ?: return
        val layer = dao.getLayer(definition.layerId) ?: return
        val now = System.currentTimeMillis()
        dao.upsertUserActivityConfig(
            UserActivityConfigEntity(
                activityId = activityId,
                activityType = ActivitySurface.Support.name,
                active = true,
                archived = false,
                // Support has no targets by domain design
                weeklyFrequencyTarget = null,
                sessionTargetMinutes = null,
                commitmentDurationMonths = null,
                cadence = null,
                targetValue = null,
                minimumValue = null,
                targetCount = null,
                targetPeriod = null,
                sortOrder = now.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun removeSupport(activityId: String) {
        val config = dao.getUserActivityConfig(activityId)
        if (config != null && config.activityType == ActivitySurface.Support.name) {
            dao.deleteUserActivityConfig(activityId)
        }
    }
}

private fun isCustomActivityId(activityId: String): Boolean =
    activityId.startsWith("act_custom_") || (!activityId.startsWith("act_") && !activityId.startsWith("sup_"))

private const val DAILY_CLOSURE_VERSION = 1
private const val ASSUMED_RELAPSE_SOURCE = "AssumedAfterMissingTracking"
private const val ASSUMED_RELAPSE_NOTE = "recaida_asumida_por_falta_de_tracking"

private fun assumedRelapseEventId(trackId: String, startDate: LocalDate): String =
    "relapse_assumed_${trackId}_${startDate}"

private fun UserActivityConfigEntity.createdLocalDate(zoneId: ZoneId): LocalDate =
    Instant.ofEpochMilli(createdAt)
        .atZone(zoneId)
        .toLocalDate()

private fun currentTimeKey(): String =
    LocalTime.now()
        .truncatedTo(ChronoUnit.MINUTES)
        .format(DateTimeFormatter.ofPattern("HH:mm"))
