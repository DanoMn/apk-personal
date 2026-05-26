package dev.panopt.autonomia.domain.dashboard

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.SleepQuality
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardEngineTest {
    private val today = LocalDate.of(2026, 5, 21)

    @Test
    fun noDataKeepsScoreCardPlaceholder() {
        val state = state(
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            sleepLog = null,
        )

        assertEquals(ScoreState.NoData, state.status.scoreState)
        assertEquals("--", state.status.scoreLabel)
        assertEquals(0, state.status.score)
    }

    @Test
    fun importantSignalsStaySleepProjectFocus() {
        val project = activity(
            id = "act_project",
            layerId = "layer_proyecto",
            role = ActivityRole.ProjectWork,
        )
        val focus = activity(
            id = "act_focus",
            layerId = "layer_interior",
            role = ActivityRole.Practice,
        )
        val state = state(
            activities = listOf(project, focus),
            todayActivityLogs = listOf(log(project.id, 40), log(focus.id, 12)),
            sleepLog = sleep(),
            focusSignalActivityId = focus.id,
        )

        assertEquals(
            listOf(DashboardSignalKind.Sleep, DashboardSignalKind.Project, DashboardSignalKind.Focus),
            state.signals.map { it.kind },
        )
    }

    private fun state(
        activities: List<ActivityDefinition>,
        todayActivityLogs: List<ActivityLog>,
        sleepLog: SleepLog?,
        focusSignalActivityId: String? = null,
        abstinenceTracks: List<AbstinenceTrack> = emptyList(),
        abstinenceLogs: List<AbstinenceLog> = emptyList(),
    ): DashboardState =
        DashboardEngine.buildState(
            layers = layers(),
            activityDefinitions = activities,
            todayActivityLogs = todayActivityLogs,
            weekActivityLogs = todayActivityLogs,
            periodActivityLogs = todayActivityLogs,
            abstinenceTracks = abstinenceTracks,
            todayAbstinenceLogs = abstinenceLogs,
            allAbstinenceLogs = abstinenceLogs,
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepLog = sleepLog,
            focusSignalActivityId = focusSignalActivityId,
            today = today,
        )

    private fun layers(): List<Layer> =
        listOf(
            Layer("layer_interior", "Interior", "", 10),
            Layer("layer_cuerpo", "Cuerpo", "", 20),
            Layer("layer_conducta", "Conducta", "", 30),
            Layer("layer_vinculos", "Vinculos", "", 40),
            Layer("layer_proyecto", "Proyecto", "", 50),
        )

    private fun activity(
        id: String,
        layerId: String,
        role: ActivityRole,
    ): ActivityDefinition =
        ActivityDefinition(
            id = id,
            layerId = layerId,
            name = id,
            description = "",
            type = ActivityType.Time,
            role = role,
            activityType = ActivitySurface.Anchor,
            contributionRole = ContributionRole.Core,
            importanceTier = ImportanceTier.Medium,
            cadence = ActivityCadence.Daily,
            targetValue = 30,
            minimumValue = 1,
            targetCount = null,
            targetPeriod = TargetPeriod.Day,
            unit = ActivityUnit.Minutes,
            sortOrder = 10,
        )

    private fun log(activityId: String, actualValue: Int): ActivityLog =
        ActivityLog(
            activityId = activityId,
            date = today.toString(),
            completed = true,
            actualValue = actualValue,
            updatedAt = 0L,
        )

    private fun sleep(): SleepLog =
        SleepLog(
            date = today.toString(),
            plannedSleepAt = "23:30",
            plannedWakeAt = "07:30",
            sleptAt = "23:30",
            wokeAt = "07:30",
            quality = SleepQuality.Good,
        )
}
