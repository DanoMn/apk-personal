package dev.panopt.autonomia.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.panopt.autonomia.AutonomiaRepository
import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.app.AppGraph
import dev.panopt.autonomia.data.ActivityDefinitionEntity
import dev.panopt.autonomia.data.UserActivityConfigEntity
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.activity.normalizeAnchorSessionTargetMinutes
import dev.panopt.autonomia.domain.activity.normalizeAnchorWeeklyFrequencyTarget
import dev.panopt.autonomia.domain.dashboard.DashboardEngine
import dev.panopt.autonomia.domain.dashboard.DashboardState
import dev.panopt.autonomia.domain.dashboard.weekStartKey
import dev.panopt.autonomia.todayKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

internal class DashboardViewModel(
    private val repository: AutonomiaRepository,
) : ViewModel() {
    private val today = LocalDate.now()
    private val dateKey = todayKey()
    private val weekStartDateKey = weekStartKey(today)
    private val monthStartDateKey = today.withDayOfMonth(1).toString()

    private val activities: StateFlow<List<ActivityDefinition>> =
        repository.observeConfiguredActivities()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val catalogActivities: StateFlow<List<ActivityDefinition>> =
        repository.observeCatalogActivities()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val activityLogs =
        combine(
            repository.activityLogsForDateFlow(dateKey),
            repository.activityLogsBetweenFlow(weekStartDateKey, dateKey),
            repository.activityLogsBetweenFlow(monthStartDateKey, dateKey),
        ) { todayActivityLogs, weekActivityLogs, periodActivityLogs ->
            DashboardActivityLogSnapshot(
                todayActivityLogs = todayActivityLogs,
                weekActivityLogs = weekActivityLogs,
                periodActivityLogs = periodActivityLogs,
            )
        }

    private val sleepSnapshot =
        combine(
            repository.sleepLogForDateFlow(dateKey),
            repository.sleepConfigFlow(),
            repository.sleepSessionStateFlow(),
        ) { sleepLog, sleepConfig, sleepSession ->
            DashboardSleepSnapshot(log = sleepLog, config = sleepConfig, session = sleepSession)
        }

    val isDarkMode: StateFlow<Boolean> = repository.isDarkModeFlow()

    val dashboardState: StateFlow<DashboardState> =
        combine(
            repository.layersFlow(),
            activities,
            activityLogs,
            repository.abstinenceTracksFlow(),
        ) { layers, activities, activityLogs, abstinenceTracks ->
            DashboardCoreSnapshot(
                layers = layers,
                activities = activities,
                todayActivityLogs = activityLogs.todayActivityLogs,
                weekActivityLogs = activityLogs.weekActivityLogs,
                periodActivityLogs = activityLogs.periodActivityLogs,
                abstinenceTracks = abstinenceTracks,
            )
        }.let { coreFlow ->
            combine(
                coreFlow,
                repository.abstinenceLogsForDateFlow(dateKey),
                repository.allAbstinenceLogsFlow(),
                repository.riskEventsForDateFlow(dateKey),
                repository.tasksFlow(),
            ) { core, todayAbstinenceLogs, allAbstinenceLogs, riskEvents, tasks ->
                DashboardFactSnapshot(
                    core = core,
                    todayAbstinenceLogs = todayAbstinenceLogs,
                    allAbstinenceLogs = allAbstinenceLogs,
                    riskEvents = riskEvents,
                    tasks = tasks,
                )
            }
        }.let { factFlow ->
            combine(factFlow, repository.weeklyScoreHistoryFlow()) { facts, weeklyHistory ->
                facts.copy(weeklyHistory = weeklyHistory)
            }
        }.let { factFlow ->
            combine(
                factFlow,
                catalogActivities,
                repository.anchorPhrasesFlow(),
                sleepSnapshot,
                repository.focusSignalActivityIdFlow(),
            ) { facts, catalogActivities, anchorPhrases, sleepSnapshot, focusSignalActivityId ->
                DashboardEngine.buildState(
                    layers = facts.core.layers,
                    activityDefinitions = facts.core.activities,
                    catalogDefinitions = catalogActivities,
                    todayActivityLogs = facts.core.todayActivityLogs,
                    weekActivityLogs = facts.core.weekActivityLogs,
                    periodActivityLogs = facts.core.periodActivityLogs,
                    abstinenceTracks = facts.core.abstinenceTracks,
                    todayAbstinenceLogs = facts.todayAbstinenceLogs,
                    allAbstinenceLogs = facts.allAbstinenceLogs,
                    riskEvents = facts.riskEvents,
                    tasks = facts.tasks,
                    anchorPhrases = anchorPhrases,
                    sleepLog = sleepSnapshot.log,
                    sleepConfig = sleepSnapshot.config,
                    sleepSession = sleepSnapshot.session,
                    weeklyHistory = facts.weeklyHistory,
                    focusSignalActivityId = focusSignalActivityId,
                    today = today,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardState(),
        )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            repository.materializeAssumedAbstinenceRelapses(today = today)
            repository.closeElapsedActivityDays(today = today)
            repository.refreshCurrentWeeklyScoreSnapshot(today = today)
        }
    }

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDarkMode(enabled)
        }
    }

    fun toggleActivity(activityId: String, completed: Boolean) {
        val activity = activities.value.firstOrNull { it.id == activityId } ?: return
        viewModelScope.launch {
            repository.setActivityCompleted(activity = activity, completed = completed, date = dateKey)
        }
    }

    fun saveActivityValue(activityId: String, actualValue: Int) {
        val activity = activities.value.firstOrNull { it.id == activityId } ?: return
        viewModelScope.launch {
            repository.setActivityValue(activity = activity, actualValue = actualValue, date = dateKey)
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            repository.deleteCustomActivity(activityId)
        }
    }

    fun toggleAbstinenceClean(trackId: String, isMarkedCleanToday: Boolean) {
        viewModelScope.launch {
            if (isMarkedCleanToday) {
                repository.clearAbstinenceLog(trackId = trackId, date = dateKey)
            } else {
                repository.markAbstinenceClean(trackId = trackId, date = dateKey)
            }
        }
    }

    fun toggleAbstinenceRelapse(trackId: String, isRelapseToday: Boolean) {
        viewModelScope.launch {
            if (isRelapseToday) {
                repository.clearAbstinenceLog(trackId = trackId, date = dateKey)
            } else {
                repository.markAbstinenceRelapse(trackId = trackId, date = dateKey)
            }
        }
    }

    fun setAbstinenceTrackActive(trackId: String, active: Boolean) {
        viewModelScope.launch {
            repository.setAbstinenceTrackActive(trackId = trackId, active = active)
        }
    }

    fun createCustomAbstinenceTrack(name: String) {
        viewModelScope.launch {
            repository.createCustomAbstinenceTrack(name)
        }
    }

    fun deleteCustomAbstinenceTrack(trackId: String) {
        viewModelScope.launch {
            repository.deleteCustomAbstinenceTrack(trackId)
        }
    }

    fun startSleepSession(onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            if (repository.startSleepSession()) {
                onStarted()
            }
        }
    }

    fun finishSleepSession(note: String = "") {
        viewModelScope.launch {
            repository.finishSleepSession(note)
        }
    }

    fun saveSleepConfig(
        targetSleepAt: String,
        targetWakeAt: String,
        digitalWindDownMinutes: Int,
    ) {
        viewModelScope.launch {
            repository.saveSleepConfig(
                targetSleepAt = targetSleepAt,
                targetWakeAt = targetWakeAt,
                digitalWindDownMinutes = digitalWindDownMinutes,
            )
        }
    }

    fun setFocusSignalActivity(activityId: String) {
        viewModelScope.launch {
            repository.setFocusSignalActivity(activityId)
        }
    }

    fun createActivity(
        name: String,
        layerId: String,
        sessionTargetMinutes: Int,
        isSecondary: Boolean,
        weeklyFrequencyTarget: Int? = null,
        commitmentDurationMonths: Int? = null,
    ) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) return@launch
            val now = System.currentTimeMillis()
            val activityId = "act_custom_${UUID.randomUUID()}"
            val activityType = if (isSecondary) ActivitySurface.Support else ActivitySurface.Anchor
            val isProject = layerId == "layer_proyecto"
            val normalizedSessionTarget = if (isSecondary) {
                null
            } else {
                normalizeAnchorSessionTargetMinutes(sessionTargetMinutes)
            }
            val normalizedWeeklyTarget = if (isSecondary) {
                null
            } else {
                normalizeAnchorWeeklyFrequencyTarget(weeklyFrequencyTarget)
            }
            // Insert definition
            repository.upsertActivityDefinition(
                ActivityDefinitionEntity(
                    id = activityId,
                    layerId = layerId,
                    name = trimmedName,
                    description = "",
                    type = if (isSecondary) ActivityType.Check.name else ActivityType.Time.name,
                    role = if (isProject) ActivityRole.ProjectWork.name else ActivityRole.Practice.name,
                    unit = if (isSecondary) ActivityUnit.Boolean.name else ActivityUnit.Minutes.name,
                    contributionRole = if (isSecondary) ContributionRole.Support.name else ContributionRole.Core.name,
                    importanceTier = ImportanceTier.Medium.name,
                    presetCategory = if (isSecondary) "support" else "anchor",
                    sortOrder = now.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    createdAt = now,
                    updatedAt = now,
                )
            )
            // Insert config
            repository.upsertUserActivityConfig(
                UserActivityConfigEntity(
                    activityId = activityId,
                    activityType = activityType.name,
                    cadence = if (isSecondary) null else ActivityCadence.Weekly.name,
                    targetValue = normalizedSessionTarget,
                    minimumValue = if (isSecondary) null else 1,
                    targetCount = normalizedWeeklyTarget,
                    targetPeriod = if (isSecondary) null else TargetPeriod.Week.name,
                    weeklyFrequencyTarget = normalizedWeeklyTarget,
                    sessionTargetMinutes = normalizedSessionTarget,
                    commitmentDurationMonths = if (isSecondary) null else commitmentDurationMonths,
                    sortOrder = now.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    fun createTask(title: String, layerId: String?) {
        viewModelScope.launch {
            repository.createTask(title, layerId)
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            repository.completeTask(taskId)
        }
    }

    fun reactivateTask(taskId: String) {
        viewModelScope.launch {
            repository.reactivateTask(taskId)
        }
    }

    fun addActivityAsAnchor(
        activityId: String,
        sessionTargetMinutes: Int,
        weeklyFrequencyTarget: Int,
        commitmentDurationMonths: Int?,
    ) {
        viewModelScope.launch {
            repository.addActivityAsAnchor(
                activityId = activityId,
                sessionTargetMinutes = sessionTargetMinutes,
                weeklyFrequencyTarget = weeklyFrequencyTarget,
                commitmentDurationMonths = commitmentDurationMonths,
            )
        }
    }

    fun removeActivityAsAnchor(activityId: String) {
        viewModelScope.launch {
            repository.removeActivityAsAnchor(activityId)
        }
    }

    // --- Support activity methods (inverted semantics) ---

    fun onToggleSupport(activityId: String) {
        viewModelScope.launch {
            val activity = activities.value.firstOrNull { it.id == activityId } ?: return@launch
            // Get current completed state from today's logs
            val todayLogs = repository.activityLogsForDateFlow(dateKey).first()
            val log = todayLogs.firstOrNull { it.activityId == activityId }
            val currentlyCompleted = log?.completed == true
            // INVERTED: flip the completed flag
            repository.setActivityCompleted(
                activity = activity,
                completed = !currentlyCompleted,
                date = dateKey,
            )
        }
    }

    fun resetSupportOmissions() {
        viewModelScope.launch {
            activities.value
                .filter { it.activityType == ActivitySurface.Support }
                .forEach { activity ->
                    repository.setActivityCompleted(
                        activity = activity,
                        completed = false,
                        date = dateKey,
                    )
                }
        }
    }

    fun addToSupports(activityId: String) {
        viewModelScope.launch {
            repository.addSupport(activityId)
        }
    }

    fun removeFromSupports(activityId: String) {
        viewModelScope.launch {
            repository.removeSupport(activityId)
        }
    }

    fun toggleAllSupports() {
        viewModelScope.launch {
            val supports = activities.value.filter {
                it.activityType == ActivitySurface.Support
            }
            if (supports.isEmpty()) return@launch

            val todayLogs = repository.activityLogsForDateFlow(dateKey).first()
            val supportLogs = todayLogs.filter { log ->
                supports.any { it.id == log.activityId }
            }
            // Check if any support currently has an omission log (completed=true = NOT done)
            val hasOmissions = supports.any { support ->
                supportLogs.any { it.activityId == support.id && it.completed }
            }

            supports.forEach { support ->
                repository.setActivityCompleted(
                    activity = support,
                    completed = !hasOmissions, // flip: if any omitted, mark all as done; else mark all as omitted
                    date = dateKey,
                )
            }
        }
    }

    fun saveSupportChecklist() {
        // Individual toggles already persist via onToggleSupport.
        // This method exists as a UX confirmation trigger. Data is already saved.
    }

    class Factory(
        private val context: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(AppGraph.autonomiaRepository(context)) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

private data class DashboardCoreSnapshot(
    val layers: List<dev.panopt.autonomia.Layer>,
    val activities: List<dev.panopt.autonomia.domain.activity.ActivityDefinition>,
    val todayActivityLogs: List<dev.panopt.autonomia.ActivityLog>,
    val weekActivityLogs: List<dev.panopt.autonomia.ActivityLog>,
    val periodActivityLogs: List<dev.panopt.autonomia.ActivityLog>,
    val abstinenceTracks: List<dev.panopt.autonomia.AbstinenceTrack>,
)

private data class DashboardActivityLogSnapshot(
    val todayActivityLogs: List<dev.panopt.autonomia.ActivityLog>,
    val weekActivityLogs: List<dev.panopt.autonomia.ActivityLog>,
    val periodActivityLogs: List<dev.panopt.autonomia.ActivityLog>,
)

private data class DashboardSleepSnapshot(
    val log: dev.panopt.autonomia.SleepLog?,
    val config: dev.panopt.autonomia.SleepConfig,
    val session: dev.panopt.autonomia.SleepSessionState?,
)

private data class DashboardFactSnapshot(
    val core: DashboardCoreSnapshot,
    val todayAbstinenceLogs: List<dev.panopt.autonomia.AbstinenceLog>,
    val allAbstinenceLogs: List<dev.panopt.autonomia.AbstinenceLog>,
    val riskEvents: List<dev.panopt.autonomia.RiskEvent>,
    val tasks: List<dev.panopt.autonomia.Task>,
    val weeklyHistory: List<dev.panopt.autonomia.domain.scoring.WeeklyScoreHistoryEntry> = emptyList(),
)
