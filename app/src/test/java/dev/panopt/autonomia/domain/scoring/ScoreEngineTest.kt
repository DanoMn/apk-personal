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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreEngineTest {
    private val today = LocalDate.of(2026, 5, 21)
    private val weekDates = (0L..3L).map { LocalDate.of(2026, 5, 18).plusDays(it) }

    @Test
    fun noDataReturnsNoDataWithoutVisibleScore() {
        val report = calculate(
            layers = listOf(layer("layer_interior", "Interior")),
            activities = listOf(anchor("act_meditation", "layer_interior")),
        )

        assertEquals(ScoreState.NoData, report.state)
        assertNull(report.visibleScore)
        assertEquals(0f, report.weeklyBaseScore, 0.001f)
    }

    @Test
    fun anchorsUseSeventyPercentFrequencyAndThirtyPercentValue() {
        val activity = anchor("act_meditation", "layer_interior")
        val report = calculate(
            layers = listOf(layer("layer_interior", "Interior")),
            activities = listOf(activity),
            activityLogs = weekDates.take(2).map { log(activity.id, it, actualValue = 20) },
        )

        val layer = report.layerScores.single()
        assertEquals(0.50f, layer.anchorScore ?: 0f, 0.001f)
        assertEquals(0.50f, report.weeklyBaseScore, 0.001f)
        assertEquals(850, report.visibleScore)
    }

    @Test
    fun anchorSurplusAddsCappedPositiveMarginWithoutChangingBase() {
        val sunday = LocalDate.of(2026, 5, 24)
        val dates = (0L..5L).map { LocalDate.of(2026, 5, 18).plusDays(it) }
        val activity = anchor("act_meditation", "layer_interior")
        val report = calculate(
            today = sunday,
            layers = listOf(layer("layer_interior", "Interior")),
            activities = listOf(activity),
            activityLogs = dates.map { log(activity.id, it, actualValue = 50) },
        )

        val layer = report.layerScores.single()
        assertEquals(1.0f, layer.baseScore, 0.001f)
        assertTrue(layer.anchorSurplusBonus > 0.03f)
        assertTrue(layer.rawScore > 1.0f)
        assertEquals(1000, report.visibleScore)
    }

    @Test
    fun supportsAreOptInAndOnlyReduceTheirConfiguredLayerWhenOmitted() {
        val anchor = anchor("act_meditation", "layer_interior")
        val support = support("sup_phone", "layer_interior")
        val fullAnchorLogs = weekDates.map { log(anchor.id, it, actualValue = 20) }
        val withoutSupports = calculate(
            layers = listOf(layer("layer_interior", "Interior")),
            activities = listOf(anchor),
            activityLogs = fullAnchorLogs,
        )
        val withOneOmission = calculate(
            layers = listOf(layer("layer_interior", "Interior")),
            activities = listOf(anchor, support),
            activityLogs = fullAnchorLogs + log(support.id, weekDates.first(), actualValue = 1),
        )

        assertEquals(1.0f, withoutSupports.layerScores.single().baseScore, 0.001f)
        assertEquals(0.75f, withOneOmission.layerScores.single().supportScore ?: 0f, 0.001f)
        assertEquals(0.95f, withOneOmission.layerScores.single().baseScore, 0.001f)
    }

    @Test
    fun completedLayerTasksAddMomentumButPendingAndNeutralTasksDoNot() {
        val activity = anchor("act_meditation", "layer_interior")
        val logs = weekDates.map { log(activity.id, it, actualValue = 20) }
        val neutral = calculate(
            layers = listOf(layer("layer_interior", "Interior")),
            activities = listOf(activity),
            activityLogs = logs,
            tasks = listOf(task("task_neutral", "layer_interior", ContributionRole.Neutral, TaskStatus.Done)),
        )
        val pending = calculate(
            layers = listOf(layer("layer_interior", "Interior")),
            activities = listOf(activity),
            activityLogs = logs,
            tasks = listOf(task("task_pending", "layer_interior", ContributionRole.Support, TaskStatus.Pending)),
        )
        val completed = calculate(
            layers = listOf(layer("layer_interior", "Interior")),
            activities = listOf(activity),
            activityLogs = logs,
            tasks = listOf(task("task_done", "layer_interior", ContributionRole.Support, TaskStatus.Done)),
        )

        assertEquals(0f, neutral.layerScores.single().taskMomentumBonus, 0.001f)
        assertEquals(0f, pending.layerScores.single().taskMomentumBonus, 0.001f)
        assertTrue(completed.layerScores.single().taskMomentumBonus > 0.0f)
        assertTrue(completed.layerScores.single().rawScore > neutral.layerScores.single().rawScore)
    }

    @Test
    fun bodyLayerUsesSleepAsThirtyPercentOfTheLayer() {
        val activity = anchor("act_body", "layer_cuerpo")
        val logs = weekDates.map { log(activity.id, it, actualValue = 20) }
        val noSleep = calculate(
            layers = listOf(layer("layer_cuerpo", "Cuerpo")),
            activities = listOf(activity),
            activityLogs = logs,
        )
        val withSleep = calculate(
            layers = listOf(layer("layer_cuerpo", "Cuerpo")),
            activities = listOf(activity),
            activityLogs = logs,
            sleepLog = sleep(),
        )

        assertEquals(0.70f, noSleep.layerScores.single().baseScore, 0.001f)
        assertEquals(1.0f, withSleep.layerScores.single().baseScore, 0.001f)
    }

    @Test
    fun inactiveSobrietyDoesNotAffectConduct() {
        val activity = anchor("act_conduct", "layer_conducta")
        val inactiveTrack = abstinenceTrack(active = false)
        val report = calculate(
            layers = listOf(layer("layer_conducta", "Conducta")),
            activities = listOf(activity),
            activityLogs = weekDates.map { log(activity.id, it, actualValue = 20) },
            abstinenceTracks = listOf(inactiveTrack),
            abstinenceLogs = listOf(abstinenceLog(inactiveTrack.id, today, AbstinenceStatus.Relapse)),
        )

        assertEquals(1.0f, report.layerScores.single().baseScore, 0.001f)
        assertNull(report.layerScores.single().sobrietyScore)
    }

    @Test
    fun activeSobrietyEntersConductAsThirtyPercentAndRelapseLowersIt() {
        val activity = anchor("act_conduct", "layer_conducta")
        val track = abstinenceTrack()
        val cleanReport = calculate(
            layers = listOf(layer("layer_conducta", "Conducta")),
            activities = listOf(activity),
            activityLogs = weekDates.map { log(activity.id, it, actualValue = 20) },
            abstinenceTracks = listOf(track),
            abstinenceLogs = weekDates.map { abstinenceLog(track.id, it, AbstinenceStatus.Clean) },
        )
        val relapseReport = calculate(
            layers = listOf(layer("layer_conducta", "Conducta")),
            activities = listOf(activity),
            activityLogs = weekDates.map { log(activity.id, it, actualValue = 20) },
            abstinenceTracks = listOf(track),
            abstinenceLogs = weekDates.take(3).map { abstinenceLog(track.id, it, AbstinenceStatus.Clean) } +
                abstinenceLog(track.id, today, AbstinenceStatus.Relapse),
        )

        assertEquals(1.0f, cleanReport.layerScores.single().baseScore, 0.001f)
        assertTrue((relapseReport.layerScores.single().sobrietyScore ?: 1f) < 0.40f)
        assertTrue(relapseReport.layerScores.single().baseScore < cleanReport.layerScores.single().baseScore)
    }

    @Test
    fun pendingSobrietyWithinFiveDaysCountsAsDampenedContext() {
        val activity = anchor("act_conduct", "layer_conducta")
        val track = abstinenceTrack()
        val report = calculate(
            layers = listOf(layer("layer_conducta", "Conducta")),
            activities = listOf(activity),
            activityLogs = weekDates.map { log(activity.id, it, actualValue = 20) },
            abstinenceTracks = listOf(track),
        )

        assertEquals(0.425f, report.layerScores.single().sobrietyScore ?: 0f, 0.001f)
        assertEquals(0.8275f, report.layerScores.single().baseScore, 0.001f)
    }

    @Test
    fun worstLayerDragsWeeklyScoreByTwentyFivePercent() {
        val strong = anchor("act_interior", "layer_interior")
        val weak = anchor("act_project", "layer_proyecto")
        val report = calculate(
            layers = listOf(layer("layer_interior", "Interior"), layer("layer_proyecto", "Proyecto")),
            activities = listOf(strong, weak),
            activityLogs = weekDates.map { log(strong.id, it, actualValue = 20) },
        )

        assertEquals("layer_proyecto", report.worstLayerId)
        assertEquals(0.375f, report.weeklyBaseScore, 0.001f)
        assertEquals(813, report.visibleScore)
    }

    @Test
    fun perfectSingleWeekDoesNotBecomeUnbreakableWithoutTemporalMemory() {
        val layers = coreLayers()
        val activities = layers.map { anchor("act_${it.id}", it.id) }
        val logs = activities.flatMap { activity ->
            weekDates.map { date -> log(activity.id, date, actualValue = 20) }
        }
        val track = abstinenceTrack()
        val report = calculate(
            layers = layers,
            activities = activities,
            activityLogs = logs,
            abstinenceTracks = listOf(track),
            abstinenceLogs = weekDates.map { abstinenceLog(track.id, it, AbstinenceStatus.Clean) },
            sleepLog = sleep(),
        )

        assertEquals(1000, report.visibleScore)
        assertEquals(ScoreState.Plenitude, report.state)
    }

    private fun calculate(
        today: LocalDate = this.today,
        layers: List<Layer>,
        activities: List<ActivityDefinition> = emptyList(),
        activityLogs: List<ActivityLog> = emptyList(),
        abstinenceTracks: List<AbstinenceTrack> = emptyList(),
        abstinenceLogs: List<AbstinenceLog> = emptyList(),
        tasks: List<Task> = emptyList(),
        sleepLog: SleepLog? = null,
    ): ScoreReport =
        ScoreEngine.calculate(
            ScoreInput(
                layers = layers,
                activities = activities,
                todayActivityLogs = activityLogs.filter { it.date == today.toString() },
                periodActivityLogs = activityLogs,
                abstinenceTracks = abstinenceTracks,
                todayAbstinenceLogs = abstinenceLogs.filter { it.date == today.toString() },
                allAbstinenceLogs = abstinenceLogs,
                tasks = tasks,
                sleepLog = sleepLog,
                today = today,
            ),
        )

    private fun coreLayers(): List<Layer> =
        listOf(
            layer("layer_interior", "Interior", 10),
            layer("layer_cuerpo", "Cuerpo", 20),
            layer("layer_conducta", "Conducta", 30),
            layer("layer_vinculos", "Vinculos", 40),
            layer("layer_proyecto", "Proyecto", 50),
        )

    private fun layer(id: String, name: String, sortOrder: Int = 10): Layer =
        Layer(id = id, name = name, description = "", sortOrder = sortOrder)

    private fun anchor(id: String, layerId: String): ActivityDefinition =
        ActivityDefinition(
            id = id,
            layerId = layerId,
            name = id,
            description = "",
            type = ActivityType.Time,
            role = ActivityRole.Practice,
            activityType = ActivitySurface.Anchor,
            contributionRole = ContributionRole.Core,
            importanceTier = ImportanceTier.Medium,
            cadence = ActivityCadence.Daily,
            targetValue = null,
            minimumValue = 1,
            targetCount = null,
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = 4,
            sessionTargetMinutes = 20,
            unit = ActivityUnit.Minutes,
            sortOrder = 10,
        )

    private fun support(id: String, layerId: String): ActivityDefinition =
        anchor(id, layerId).copy(
            activityType = ActivitySurface.Support,
            contributionRole = ContributionRole.Support,
            unit = ActivityUnit.Boolean,
            sessionTargetMinutes = null,
            targetValue = 1,
        )

    private fun log(activityId: String, date: LocalDate, actualValue: Int): ActivityLog =
        ActivityLog(
            activityId = activityId,
            date = date.toString(),
            completed = true,
            actualValue = actualValue,
            updatedAt = 0L,
        )

    private fun abstinenceTrack(
        id: String = "trk_alcohol",
        active: Boolean = true,
    ): AbstinenceTrack =
        AbstinenceTrack(
            id = id,
            name = id,
            substanceLabel = id,
            severity = AbstinenceSeverity.Critical,
            contributionRole = ContributionRole.Protective,
            importanceTier = ImportanceTier.Critical,
            active = active,
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

    private fun task(
        id: String,
        layerId: String?,
        contributionRole: ContributionRole,
        status: TaskStatus,
    ): Task =
        Task(
            id = id,
            title = id,
            description = "",
            layerId = layerId,
            projectId = null,
            status = status,
            contributionRole = contributionRole,
            importanceTier = ImportanceTier.Medium,
            dueDate = today.toString(),
            completedAt = if (status == TaskStatus.Done) {
                today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                null
            },
            createdAt = 0L,
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
