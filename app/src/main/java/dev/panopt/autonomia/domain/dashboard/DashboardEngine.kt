package dev.panopt.autonomia.domain.dashboard

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.RiskEvent
import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.LocalDate

internal object DashboardEngine {
    fun buildState(
        layers: List<Layer>,
        activityDefinitions: List<ActivityDefinition>,
        catalogDefinitions: List<ActivityDefinition> = activityDefinitions,
        todayActivityLogs: List<ActivityLog>,
        weekActivityLogs: List<ActivityLog>,
        periodActivityLogs: List<ActivityLog>,
        abstinenceTracks: List<AbstinenceTrack>,
        todayAbstinenceLogs: List<AbstinenceLog>,
        allAbstinenceLogs: List<AbstinenceLog>,
        riskEvents: List<RiskEvent>,
        tasks: List<Task>,
        anchorPhrases: List<AnchorPhrase>,
        sleepLog: SleepLog?,
        focusSignalActivityId: String?,
        today: LocalDate,
    ): DashboardState =
        buildDashboardState(
            layers = layers,
            activities = activityDefinitions,
            catalogActivities = catalogDefinitions,
            todayActivityLogs = todayActivityLogs,
            weekActivityLogs = weekActivityLogs,
            periodActivityLogs = periodActivityLogs,
            abstinenceTracks = abstinenceTracks,
            todayAbstinenceLogs = todayAbstinenceLogs,
            allAbstinenceLogs = allAbstinenceLogs,
            riskEvents = riskEvents,
            tasks = tasks,
            anchorPhrases = anchorPhrases,
            sleepLog = sleepLog,
            focusSignalActivityId = focusSignalActivityId,
            today = today,
        )
}
