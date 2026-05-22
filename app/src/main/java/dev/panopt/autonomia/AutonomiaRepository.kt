package dev.panopt.autonomia

import android.content.Context
import dev.panopt.autonomia.data.AbstinenceLogEntity
import dev.panopt.autonomia.data.ActivityEntity
import dev.panopt.autonomia.data.ActivityLogEntity
import dev.panopt.autonomia.data.AutonomiaDatabase
import dev.panopt.autonomia.data.RiskEventEntity
import dev.panopt.autonomia.data.SleepLogEntity
import dev.panopt.autonomia.data.TaskEntity
import dev.panopt.autonomia.data.local.mapper.toDomain
import dev.panopt.autonomia.data.local.seed.DefaultSeeds
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.activity.defaultActualValue
import dev.panopt.autonomia.domain.sleep.SleepPolicy
import dev.panopt.autonomia.domain.sleep.SleepWindowValidation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AutonomiaRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("autonomia_prefs", Context.MODE_PRIVATE)
    private val dao = AutonomiaDatabase.getInstance(appContext).autonomiaDao()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", true))
    private val _focusSignalActivityId = MutableStateFlow(prefs.getString("focus_signal_activity_id", null))
    private val _isInitialConfigurationComplete = MutableStateFlow(
        prefs.getBoolean("initial_configuration_complete", false),
    )

    fun isDarkModeFlow(): StateFlow<Boolean> = _isDarkMode.asStateFlow()

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

    fun activityDefinitionsFlow(): Flow<List<ActivityDefinition>> =
        dao.observeActivities().map { activities -> activities.map { it.toDomain() } }

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

    fun sleepLogForDateFlow(date: String): Flow<SleepLog?> =
        dao.observeSleepLogForDate(date).map { it?.toDomain() }

    suspend fun ensureSeeded() {
        if (dao.layerCount() > 0) return

        dao.upsertLayers(DefaultSeeds.layers)
        dao.upsertActivities(DefaultSeeds.activities)
        dao.upsertAbstinenceTracks(DefaultSeeds.abstinenceTracks)
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

        dao.upsertActivityLog(
            ActivityLogEntity(
                activityId = activity.id,
                date = date,
                completed = true,
                actualValue = activity.defaultActualValue(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setActivityValue(
        activity: ActivityDefinition,
        actualValue: Int,
        date: String = todayKey(),
    ) {
        dao.upsertActivityLog(
            ActivityLogEntity(
                activityId = activity.id,
                date = date,
                completed = true,
                actualValue = actualValue.coerceAtLeast(0),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun createActivity(
        name: String,
        layerId: String,
        targetMinutes: Int,
        displaySurface: DisplaySurface,
        isGoal: Boolean = false,
        isMonthlyGoal: Boolean = false,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        val now = System.currentTimeMillis()
        val isProject = layerId == "layer_proyecto"
        val targetPeriod = when {
            !isGoal -> TargetPeriod.Day
            isMonthlyGoal -> TargetPeriod.Month
            else -> TargetPeriod.Week
        }
        dao.upsertActivity(
            ActivityEntity(
                id = "act_custom_${UUID.randomUUID()}",
                layerId = layerId,
                name = trimmedName,
                description = "",
                type = ActivityType.Time.name,
                role = if (isProject) ActivityRole.ProjectWork.name else ActivityRole.Practice.name,
                displaySurface = if (isGoal) DisplaySurface.Contextual.name else displaySurface.name,
                contributionRole = ContributionRole.Core.name,
                importanceTier = ImportanceTier.Medium.name,
                cadence = when (targetPeriod) {
                    TargetPeriod.Day -> ActivityCadence.Daily
                    TargetPeriod.Week -> ActivityCadence.Weekly
                    TargetPeriod.Month -> ActivityCadence.Monthly
                }.name,
                targetValue = targetMinutes.coerceAtLeast(1),
                minimumValue = 1,
                targetCount = if (isGoal) 1 else null,
                targetPeriod = targetPeriod.name,
                unit = ActivityUnit.Minutes.name,
                sortOrder = now.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun markAbstinenceClean(track: AbstinenceTrack, date: String = todayKey()) {
        markAbstinenceClean(track.id, date)
    }

    suspend fun markAbstinenceClean(trackId: String, date: String = todayKey()) {
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

    suspend fun saveSleepLog(
        plannedSleepAt: String,
        plannedWakeAt: String,
        sleptAt: String,
        wokeAt: String,
        quality: SleepQuality,
        note: String,
        date: String = todayKey(),
    ): Boolean {
        val resolvedPlannedSleepAt = plannedSleepAt.ifBlank { "23:30" }
        val resolvedPlannedWakeAt = plannedWakeAt.ifBlank { "07:30" }
        val validation = SleepPolicy.validatePlannedWindow(
            plannedSleepAt = resolvedPlannedSleepAt,
            plannedWakeAt = resolvedPlannedWakeAt,
        )
        if (validation is SleepWindowValidation.Invalid) return false

        dao.upsertSleepLog(
            SleepLogEntity(
                date = date,
                plannedSleepAt = resolvedPlannedSleepAt,
                plannedWakeAt = resolvedPlannedWakeAt,
                sleptAt = sleptAt.ifBlank { "00:00" },
                wokeAt = wokeAt.ifBlank { "07:00" },
                quality = quality.name,
                note = note,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return true
    }

    suspend fun createTask(
        title: String,
        layerId: String?,
        contributesToCore: Boolean,
    ) {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isBlank()) return

        val now = System.currentTimeMillis()
        val scoringLayerId = layerId?.takeIf { contributesToCore && it.isNotBlank() }
        dao.upsertTask(
            TaskEntity(
                id = "task_${UUID.randomUUID()}",
                title = trimmedTitle,
                description = "",
                layerId = scoringLayerId,
                projectId = null,
                status = TaskStatus.Pending.name,
                contributionRole = if (scoringLayerId == null) {
                    ContributionRole.Neutral
                } else {
                    ContributionRole.Support
                }.name,
                importanceTier = ImportanceTier.Medium.name,
                dueDate = null,
                completedAt = null,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun completeTask(taskId: String) {
        val now = System.currentTimeMillis()
        dao.updateTaskStatus(
            taskId = taskId,
            status = TaskStatus.Done.name,
            completedAt = now,
            updatedAt = now,
        )
    }
}
