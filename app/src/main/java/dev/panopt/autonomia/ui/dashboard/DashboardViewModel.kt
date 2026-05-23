package dev.panopt.autonomia.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.panopt.autonomia.AutonomiaRepository
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.DisplaySurface
import dev.panopt.autonomia.SleepQuality
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.app.AppGraph
import dev.panopt.autonomia.domain.activity.ActivityDefinition
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
            combine(
                factFlow,
                repository.anchorPhrasesFlow(),
                repository.sleepLogForDateFlow(dateKey),
                repository.focusSignalActivityIdFlow(),
            ) { facts, anchorPhrases, sleepLog, focusSignalActivityId ->
                DashboardEngine.buildState(
                    layers = facts.core.layers,
                    activityDefinitions = facts.core.activities,
                    todayActivityLogs = facts.core.todayActivityLogs,
                    weekActivityLogs = facts.core.weekActivityLogs,
                    periodActivityLogs = facts.core.periodActivityLogs,
                    abstinenceTracks = facts.core.abstinenceTracks,
                    todayAbstinenceLogs = facts.todayAbstinenceLogs,
                    allAbstinenceLogs = facts.allAbstinenceLogs,
                    riskEvents = facts.riskEvents,
                    tasks = facts.tasks,
                    anchorPhrases = anchorPhrases,
                    sleepLog = sleepLog,
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
            repository.deleteActivity(activityId)
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

    fun saveSleep(
        plannedSleepAt: String,
        plannedWakeAt: String,
        sleptAt: String,
        wokeAt: String,
        quality: SleepQuality,
        note: String,
    ) {
        viewModelScope.launch {
            repository.saveSleepLog(
                plannedSleepAt = plannedSleepAt,
                plannedWakeAt = plannedWakeAt,
                sleptAt = sleptAt,
                wokeAt = wokeAt,
                quality = quality,
                note = note,
                date = dateKey,
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
        targetMinutes: Int,
        isSecondary: Boolean,
        isGoal: Boolean,
        isMonthlyGoal: Boolean,
        targetCount: Int? = null,
        targetPeriod: TargetPeriod? = null,
    ) {
        viewModelScope.launch {
            repository.createActivity(
                name = name,
                layerId = layerId,
                targetMinutes = targetMinutes,
                displaySurface = if (isSecondary) {
                    DisplaySurface.SecondaryChecklist
                } else {
                    DisplaySurface.PrimaryChecklist
                },
                activityType = if (isSecondary) ActivitySurface.Support else ActivitySurface.Anchor,
                isGoal = isGoal,
                isMonthlyGoal = isMonthlyGoal,
                targetCount = targetCount,
                targetPeriod = targetPeriod,
            )
        }
    }

    fun createTask(title: String, layerId: String?, contributesToCore: Boolean) {
        viewModelScope.launch {
            repository.createTask(title, layerId, contributesToCore)
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            repository.completeTask(taskId)
        }
    }

    fun addActivityToChecklist(
        activityId: String,
        targetValue: Int?,
        targetCount: Int?,
        targetPeriod: TargetPeriod?,
    ) {
        viewModelScope.launch {
            repository.addActivityToChecklist(
                activityId = activityId,
                targetValue = targetValue,
                targetCount = targetCount,
                targetPeriod = targetPeriod,
            )
        }
    }

    fun removeActivityFromChecklist(activityId: String) {
        viewModelScope.launch {
            repository.removeActivityFromChecklist(activityId)
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

    fun addToSupports(activityId: String) {
        viewModelScope.launch {
            repository.configureActivity(
                activityId = activityId,
                activityType = ActivitySurface.Support,
            )
        }
    }

    fun removeFromSupports(activityId: String) {
        viewModelScope.launch {
            repository.deleteUserActivityConfig(activityId)
        }
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

private data class DashboardFactSnapshot(
    val core: DashboardCoreSnapshot,
    val todayAbstinenceLogs: List<dev.panopt.autonomia.AbstinenceLog>,
    val allAbstinenceLogs: List<dev.panopt.autonomia.AbstinenceLog>,
    val riskEvents: List<dev.panopt.autonomia.RiskEvent>,
    val tasks: List<dev.panopt.autonomia.Task>,
)
