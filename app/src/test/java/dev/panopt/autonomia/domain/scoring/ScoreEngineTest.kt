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
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.sleep.SleepNightScore
import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
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
    fun bodyLayerUsesSleepAsThirtyPercentOfTheLayerWhenDataPresent() {
        // ADR-3 fix: when sleep is absent (NoData), Cuerpo = anchorBase (re-normalized, NOT 0.70*base).
        // When sleep score = 1.0, Cuerpo = 0.70*anchorBase + 0.30*1.0.
        // With 4/4 days + value=20, anchorBase ≈ 1.0 → both cases produce 1.0.
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
            sleepNights = listOf(sleepNight(score = 1.0f)),
        )
        val withPoorSleep = calculate(
            layers = listOf(layer("layer_cuerpo", "Cuerpo")),
            activities = listOf(activity),
            activityLogs = logs,
            sleepNights = listOf(sleepNight(score = 0.0f)),
        )

        // NoData: Cuerpo = anchorBase (not penalized — ADR-3)
        // Perfect sleep: same (formula: 0.70*1.0 + 0.30*1.0 = 1.0)
        assertEquals(noSleep.layerScores.single().baseScore, withSleep.layerScores.single().baseScore, 0.001f)
        // Poor sleep (0.0): Cuerpo = 0.70*1.0 + 0.30*0.0 = 0.70 — LOWER than NoData
        assertEquals(0.70f, withPoorSleep.layerScores.single().baseScore, 0.001f)
        // NoData (1.0) is strictly better than poor sleep (0.70): absence ≠ poor sleep
        assertTrue(
            "NoData Cuerpo should be > poor-sleep Cuerpo",
            noSleep.layerScores.single().baseScore > withPoorSleep.layerScores.single().baseScore,
        )
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
            sleepNights = listOf(sleepNight(score = 1.0f)),
        )

        assertEquals(1000, report.visibleScore)
        assertEquals(ScoreState.Plenitude, report.state)
        assertNull(report.stabilityScore)
        assertEquals(1, report.stabilityWeeks)
    }

    @Test
    fun unbreakableRequiresTemporalMemoryFromPreviousWeeks() {
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
            sleepNights = listOf(sleepNight(score = 1.0f)),
            weeklyHistory = highHistory(),
        )

        assertEquals(1000, report.visibleScore)
        assertEquals(ScoreState.Unbreakable, report.state)
        assertTrue((report.stabilityScore ?: 0f) >= 0.90f)
        assertEquals(6, report.stabilityWeeks)
    }

    private fun calculate(
        today: LocalDate = this.today,
        layers: List<Layer>,
        activities: List<ActivityDefinition> = emptyList(),
        activityLogs: List<ActivityLog> = emptyList(),
        abstinenceTracks: List<AbstinenceTrack> = emptyList(),
        abstinenceLogs: List<AbstinenceLog> = emptyList(),
        tasks: List<Task> = emptyList(),
        sleepNights: List<SleepNightScore> = emptyList(),
        weeklyHistory: List<WeeklyScoreHistoryEntry> = emptyList(),
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
                sleepNights = sleepNights,
                today = today,
                weeklyHistory = weeklyHistory,
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

    private fun sleepNight(score: Float = 1.0f): SleepNightScore =
        SleepNightScore(
            duration = score,
            continuity = score,
            alignment = score,
            digitalInterruption = score,
            sleepScore = score,
            confidence = SleepConfidence.High,
        )

    @Test
    fun previousStateFromWeeklyHistoryPropagatesHysteresisDamping() {
        // Single layer, 3 out of 4 days with actualValue=15 (target=20):
        // frequencyRatio = 3/4 = 0.75, valueRatio = 45/80 = 0.5625
        // anchorScore = 0.70*0.75 + 0.30*0.5625 = 0.69375
        // weeklyBaseScore = 0.69375, worstLayerScore = 0.69375 (single layer, >= 0.55, no cap)
        // Raw band = Attention (0.69375 < 0.70), margin = 0.70 - 0.69375 = 0.00625 <= 0.03.
        // With previousState=Motion from history → hysteresis holds at Motion.
        // Without previousState (no history) → falls to Attention (confirms derivation path).
        val activity = anchor("act_motion", "layer_motion")

        val prevWeekStart = today
            .minusWeeks(1)
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

        val historyEntry = WeeklyScoreHistoryEntry(
            weekStart = prevWeekStart.toString(),
            weekEnd = prevWeekStart.plusDays(6).toString(),
            scoringVersion = WeeklyScoreSnapshotConstants.SCORING_VERSION,
            weeklyBaseScore = 0.80f,
            weeklyScore = 0.80f,
            state = ScoreState.Motion,
        )

        val withHistory = calculate(
            layers = listOf(layer("layer_motion", "Motion")),
            activities = listOf(activity),
            activityLogs = weekDates.take(3).map { log(activity.id, it, actualValue = 15) },
            weeklyHistory = listOf(historyEntry),
        )

        val withoutHistory = calculate(
            layers = listOf(layer("layer_motion", "Motion")),
            activities = listOf(activity),
            activityLogs = weekDates.take(3).map { log(activity.id, it, actualValue = 15) },
            weeklyHistory = emptyList(),
        )

        // previousState=Motion from history + margin within 0.03 → hysteresis holds at Motion
        assertEquals(ScoreState.Motion, withHistory.state)
        assertEquals(0.69375f, withHistory.weeklyBaseScore, 0.001f)
        // No history → no previousState → no damping → falls to Attention
        assertEquals(ScoreState.Attention, withoutHistory.state)
    }

    private fun highHistory(): List<WeeklyScoreHistoryEntry> =
        (1L..5L).map { index ->
            val weekStart = today
                .minusWeeks(index)
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            WeeklyScoreHistoryEntry(
                weekStart = weekStart.toString(),
                weekEnd = weekStart.plusDays(6).toString(),
                scoringVersion = WeeklyScoreSnapshotConstants.SCORING_VERSION,
                weeklyBaseScore = 0.95f,
                weeklyScore = 0.95f,
                state = ScoreState.Plenitude,
            )
        }
}
