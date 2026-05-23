package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.DisplaySurface
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.SleepQuality
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreEngineTest {
    private val today: LocalDate = LocalDate.of(2026, 5, 21)
    private val coreLayers = listOf(
        layer("layer_interior", "Interior", 10),
        layer("layer_cuerpo", "Cuerpo", 20),
        layer("layer_conducta", "Conducta", 30),
        layer("layer_vinculos", "Vinculos", 40),
        layer("layer_proyecto", "Proyecto", 50),
    )
    private val track = abstinenceTrack("trk_alcohol", AbstinenceSeverity.Critical)

    @Test
    fun noDataReturnsNoDataWithoutVisibleScore() {
        val report = ScoreEngine.calculate(
            baseInput(
                activities = coreActivities(),
                todayActivityLogs = emptyList(),
                periodActivityLogs = emptyList(),
                abstinenceTracks = listOf(track),
                todayAbstinenceLogs = emptyList(),
                allAbstinenceLogs = emptyList(),
                sleepLog = null,
            ),
        )

        assertEquals(ScoreState.NoData, report.state)
        assertEquals(null, report.visibleScore)
    }

    @Test
    fun visibleScoreUsesDefinedRanges() {
        val plenitude = fullBaseReport(goalActual = 50)
        val unbreakable = fullBaseReport(goalActual = 100)

        assertEquals(ScoreState.Plenitude, plenitude.state)
        assertTrue(plenitude.visibleScore in 900..949)
        assertEquals(ScoreState.Unbreakable, unbreakable.state)
        assertTrue(unbreakable.visibleScore in 950..1000)
    }

    @Test
    fun lowSleepPreventsHighStates() {
        val report = fullBaseReport(
            goalActual = 100,
            sleepLog = sleep("03:00", "07:00", SleepQuality.Low),
        )

        assertEquals(ScoreState.Motion, report.state)
        assertTrue(report.visibleScore in 800..899)
    }

    @Test
    fun relapseTodayCapsStateAndScore() {
        val report = fullBaseReport(
            goalActual = 100,
            todayAbstinenceLogs = listOf(abstinenceLog(track.id, today, AbstinenceStatus.Relapse)),
            allAbstinenceLogs = cleanLogs(track.id, 14),
        )

        assertEquals(ScoreState.Restoration, report.state)
        assertTrue(report.visibleScore in 700..749)
    }

    @Test
    fun shortCleanStreakBlocksHighStates() {
        val report = fullBaseReport(
            goalActual = 100,
            todayAbstinenceLogs = listOf(abstinenceLog(track.id, today, AbstinenceStatus.Clean)),
            allAbstinenceLogs = cleanLogs(track.id, 3),
        )

        assertEquals(ScoreState.Motion, report.state)
        assertTrue(report.visibleScore in 800..899)
    }

    @Test
    fun inactiveAbstinenceDoesNotLimitStateOrScore() {
        val inactiveTrack = track.copy(active = false)
        val report = fullBaseReport(
            goalActual = 100,
            todayAbstinenceLogs = listOf(abstinenceLog(track.id, today, AbstinenceStatus.Relapse)),
            allAbstinenceLogs = emptyList(),
        ).let {
            ScoreEngine.calculate(
                baseInput(
                    activities = coreActivities() + goalActivity("act_goal", "layer_proyecto"),
                    todayActivityLogs = coreActivities().map { activity -> log(activity.id) },
                    periodActivityLogs = coreActivities().map { activity -> log(activity.id) } + log("act_goal", actualValue = 100),
                    abstinenceTracks = listOf(inactiveTrack),
                    todayAbstinenceLogs = listOf(abstinenceLog(track.id, today, AbstinenceStatus.Relapse)),
                    allAbstinenceLogs = emptyList(),
                    sleepLog = sleep("23:30", "07:30", SleepQuality.Good),
                ),
            )
        }

        assertEquals(ScoreState.Unbreakable, report.state)
        assertTrue(checkNotNull(report.visibleScore) >= 950)
    }

    @Test
    fun primaryChecklistWeighsMoreThanSecondaryAndTasks() {
        val primary = singleLayerReport(
            activity = activity("act_primary", "layer_interior", DisplaySurface.PrimaryChecklist),
            activityLog = log("act_primary"),
        )
        val secondary = singleLayerReport(
            activity = activity("act_secondary", "layer_interior", DisplaySurface.SecondaryChecklist),
            activityLog = log("act_secondary"),
        )
        val task = singleLayerReport(
            tasks = listOf(task("task_support", "layer_interior", ContributionRole.Support)),
        )

        assertTrue(checkNotNull(primary.visibleScore) > checkNotNull(secondary.visibleScore))
        assertTrue(checkNotNull(secondary.visibleScore) > checkNotNull(task.visibleScore))
    }

    @Test
    fun neutralTasksDoNotAddScore() {
        val blockingActivity = activity("act_primary", "layer_interior", DisplaySurface.PrimaryChecklist)
        val neutral = singleLayerReport(
            activity = blockingActivity,
            tasks = listOf(task("task_neutral", "layer_interior", ContributionRole.Neutral)),
        )
        val support = singleLayerReport(
            activity = blockingActivity,
            tasks = listOf(task("task_support", "layer_interior", ContributionRole.Support)),
        )

        assertTrue(checkNotNull(support.visibleScore) > checkNotNull(neutral.visibleScore))
    }

    @Test
    fun goalsElevateFromMotionToUnbreakable() {
        val withoutGoals = fullBaseReport(goalActual = 0)
        val withGoals = fullBaseReport(goalActual = 100)

        assertNotEquals(ScoreState.Unbreakable, withoutGoals.state)
        assertEquals(ScoreState.Unbreakable, withGoals.state)
    }

    @Test
    fun goalsDoNotFillLayerBaseByThemselves() {
        val goal = goalActivity("act_goal", "layer_proyecto")
        val report = ScoreEngine.calculate(
            ScoreInput(
                layers = listOf(layer("layer_proyecto", "Proyecto", 10)),
                activities = listOf(goal),
                todayActivityLogs = listOf(log(goal.id, actualValue = 100)),
                periodActivityLogs = listOf(log(goal.id, actualValue = 100)),
                abstinenceTracks = emptyList(),
                todayAbstinenceLogs = emptyList(),
                allAbstinenceLogs = emptyList(),
                tasks = emptyList(),
                sleepLog = null,
                today = today,
            ),
        )

        assertEquals(100, report.goalBonus)
        assertTrue(checkNotNull(report.layerScores.firstOrNull()).score < 0.80f)
        assertTrue(checkNotNull(report.visibleScore) <= 899)
    }

    private fun fullBaseReport(
        goalActual: Int,
        sleepLog: SleepLog = sleep("23:30", "07:30", SleepQuality.Good),
        todayAbstinenceLogs: List<AbstinenceLog> = listOf(abstinenceLog(track.id, today, AbstinenceStatus.Clean)),
        allAbstinenceLogs: List<AbstinenceLog> = cleanLogs(track.id, 14),
    ): ScoreReport {
        val activities = coreActivities() + goalActivity("act_goal", "layer_proyecto")
        val todayLogs = coreActivities().map { log(it.id) }
        val periodLogs = todayLogs + log("act_goal", actualValue = goalActual)
        return ScoreEngine.calculate(
            baseInput(
                activities = activities,
                todayActivityLogs = todayLogs,
                periodActivityLogs = periodLogs,
                abstinenceTracks = listOf(track),
                todayAbstinenceLogs = todayAbstinenceLogs,
                allAbstinenceLogs = allAbstinenceLogs,
                sleepLog = sleepLog,
            ),
        )
    }

    private fun singleLayerReport(
        activity: ActivityDefinition? = null,
        activityLog: ActivityLog? = null,
        tasks: List<Task> = emptyList(),
    ): ScoreReport {
        val activities = listOfNotNull(activity)
        val logs = listOfNotNull(activityLog)
        return ScoreEngine.calculate(
            ScoreInput(
                layers = listOf(layer("layer_interior", "Interior", 10)),
                activities = activities,
                todayActivityLogs = logs,
                periodActivityLogs = logs,
                abstinenceTracks = emptyList(),
                todayAbstinenceLogs = emptyList(),
                allAbstinenceLogs = emptyList(),
                tasks = tasks,
                sleepLog = null,
                today = today,
            ),
        )
    }

    private fun baseInput(
        activities: List<ActivityDefinition>,
        todayActivityLogs: List<ActivityLog>,
        periodActivityLogs: List<ActivityLog>,
        abstinenceTracks: List<AbstinenceTrack>,
        todayAbstinenceLogs: List<AbstinenceLog>,
        allAbstinenceLogs: List<AbstinenceLog>,
        sleepLog: SleepLog?,
    ): ScoreInput =
        ScoreInput(
            layers = coreLayers,
            activities = activities,
            todayActivityLogs = todayActivityLogs,
            periodActivityLogs = periodActivityLogs,
            abstinenceTracks = abstinenceTracks,
            todayAbstinenceLogs = todayAbstinenceLogs,
            allAbstinenceLogs = allAbstinenceLogs,
            tasks = emptyList(),
            sleepLog = sleepLog,
            today = today,
        )

    private fun coreActivities(): List<ActivityDefinition> =
        listOf(
            activity("act_interior", "layer_interior", DisplaySurface.PrimaryChecklist),
            activity("act_body", "layer_cuerpo", DisplaySurface.PrimaryChecklist),
            activity("act_conduct", "layer_conducta", DisplaySurface.PrimaryChecklist),
            activity("act_vinculos", "layer_vinculos", DisplaySurface.PrimaryChecklist),
            activity("act_project", "layer_proyecto", DisplaySurface.PrimaryChecklist),
        )

    private fun layer(id: String, name: String, sortOrder: Int): Layer =
        Layer(id = id, name = name, description = "", sortOrder = sortOrder)

    private fun activity(
        id: String,
        layerId: String,
        displaySurface: DisplaySurface,
    ): ActivityDefinition =
        ActivityDefinition(
            id = id,
            layerId = layerId,
            name = id,
            description = "",
            type = ActivityType.Time,
            role = ActivityRole.Practice,
            displaySurface = displaySurface,
            activityType = when (displaySurface) {
                DisplaySurface.PrimaryChecklist -> ActivitySurface.Anchor
                DisplaySurface.SecondaryChecklist -> ActivitySurface.Support
                else -> ActivitySurface.Anchor
            },
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

    private fun goalActivity(id: String, layerId: String): ActivityDefinition =
        activity(id, layerId, DisplaySurface.PrimaryChecklist).copy(
            cadence = ActivityCadence.Weekly,
            targetValue = 100,
            targetCount = 1,
            targetPeriod = TargetPeriod.Week,
            importanceTier = ImportanceTier.High,
        )

    private fun log(
        activityId: String,
        actualValue: Int = 30,
    ): ActivityLog =
        ActivityLog(
            activityId = activityId,
            date = today.toString(),
            completed = true,
            actualValue = actualValue,
            updatedAt = 0L,
        )

    private fun abstinenceTrack(id: String, severity: AbstinenceSeverity): AbstinenceTrack =
        AbstinenceTrack(
            id = id,
            name = id,
            substanceLabel = id,
            severity = severity,
            contributionRole = ContributionRole.Protective,
            importanceTier = ImportanceTier.Critical,
            sortOrder = 10,
        )

    private fun abstinenceLog(
        trackId: String,
        date: LocalDate,
        status: AbstinenceStatus,
    ): AbstinenceLog =
        AbstinenceLog(
            trackId = trackId,
            date = date.toString(),
            status = status,
            updatedAt = 0L,
        )

    private fun cleanLogs(trackId: String, days: Int): List<AbstinenceLog> =
        (0 until days).map { offset ->
            abstinenceLog(trackId, today.minusDays(offset.toLong()), AbstinenceStatus.Clean)
        }

    private fun sleep(
        sleptAt: String,
        wokeAt: String,
        quality: SleepQuality,
    ): SleepLog =
        SleepLog(
            date = today.toString(),
            plannedSleepAt = "23:30",
            plannedWakeAt = "07:30",
            sleptAt = sleptAt,
            wokeAt = wokeAt,
            quality = quality,
        )

    private fun task(id: String, layerId: String, contributionRole: ContributionRole): Task =
        Task(
            id = id,
            title = id,
            description = "",
            layerId = layerId,
            projectId = null,
            status = TaskStatus.Done,
            contributionRole = contributionRole,
            importanceTier = ImportanceTier.Medium,
            dueDate = today.toString(),
            completedAt = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            createdAt = 0L,
            updatedAt = 0L,
        )
}
