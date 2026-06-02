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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
        // 3 capas idénticas (§7.4 gate); cada una 2/4 días → anchorScore 0.50, global 0.50.
        val layers = listOf(
            layer("layer_interior", "Interior", 10),
            layer("layer_cuerpo", "Cuerpo", 20),
            layer("layer_conducta", "Conducta", 30),
        )
        val activities = layers.map { anchor("act_${it.id}", it.id) }
        val report = calculate(
            layers = layers,
            activities = activities,
            activityLogs = activities.flatMap { a -> weekDates.take(2).map { log(a.id, it, actualValue = 20) } },
        )

        val layer = report.layerScores.first()
        assertEquals(0.50f, layer.anchorScore ?: 0f, 0.001f)
        assertEquals(0.50f, report.weeklyBaseScore, 0.001f)
        assertEquals(850, report.visibleScore)
    }

    @Test
    fun anchorSurplusAddsCappedPositiveMarginWithoutChangingBase() {
        val sunday = LocalDate.of(2026, 5, 24)
        val dates = (0L..5L).map { LocalDate.of(2026, 5, 18).plusDays(it) }
        val layers = listOf(
            layer("layer_interior", "Interior", 10),
            layer("layer_cuerpo", "Cuerpo", 20),
            layer("layer_conducta", "Conducta", 30),
        )
        val activities = layers.map { anchor("act_${it.id}", it.id) }
        val report = calculate(
            today = sunday,
            layers = layers,
            activities = activities,
            activityLogs = activities.flatMap { a -> dates.map { log(a.id, it, actualValue = 50) } },
        )

        val layer = report.layerScores.first()
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
            layers = listOf(layer("layer_interior", "Interior")) + fillerLayers(),
            activities = listOf(anchor) + fillerActivities(),
            activityLogs = fullAnchorLogs + fillerLogs(),
        )
        val withOneOmission = calculate(
            layers = listOf(layer("layer_interior", "Interior")) + fillerLayers(),
            activities = listOf(anchor, support) + fillerActivities(),
            activityLogs = fullAnchorLogs + log(support.id, weekDates.first(), actualValue = 1) + fillerLogs(),
        )

        val interiorWithout = withoutSupports.layerScores.first { it.layerId == "layer_interior" }
        val interiorOmission = withOneOmission.layerScores.first { it.layerId == "layer_interior" }
        assertEquals(1.0f, interiorWithout.baseScore, 0.001f)
        assertEquals(0.75f, interiorOmission.supportScore ?: 0f, 0.001f)
        assertEquals(0.95f, interiorOmission.baseScore, 0.001f)
    }

    @Test
    fun completedLayerTasksAddMomentumButPendingAndNeutralTasksDoNot() {
        val activity = anchor("act_meditation", "layer_interior")
        val logs = weekDates.map { log(activity.id, it, actualValue = 20) }
        val neutral = calculate(
            layers = listOf(layer("layer_interior", "Interior")) + fillerLayers(),
            activities = listOf(activity) + fillerActivities(),
            activityLogs = logs + fillerLogs(),
            tasks = listOf(task("task_neutral", "layer_interior", ContributionRole.Neutral, TaskStatus.Done)),
        )
        val pending = calculate(
            layers = listOf(layer("layer_interior", "Interior")) + fillerLayers(),
            activities = listOf(activity) + fillerActivities(),
            activityLogs = logs + fillerLogs(),
            tasks = listOf(task("task_pending", "layer_interior", ContributionRole.Support, TaskStatus.Pending)),
        )
        val completed = calculate(
            layers = listOf(layer("layer_interior", "Interior")) + fillerLayers(),
            activities = listOf(activity) + fillerActivities(),
            activityLogs = logs + fillerLogs(),
            tasks = listOf(task("task_done", "layer_interior", ContributionRole.Support, TaskStatus.Done)),
        )

        val neutralInterior = neutral.layerScores.first { it.layerId == "layer_interior" }
        val pendingInterior = pending.layerScores.first { it.layerId == "layer_interior" }
        val completedInterior = completed.layerScores.first { it.layerId == "layer_interior" }
        assertEquals(0f, neutralInterior.taskMomentumBonus, 0.001f)
        assertEquals(0f, pendingInterior.taskMomentumBonus, 0.001f)
        assertTrue(completedInterior.taskMomentumBonus > 0.0f)
        assertTrue(completedInterior.rawScore > neutralInterior.rawScore)
    }

    @Test
    fun bodyLayerUsesSleepAsThirtyPercentOfTheLayerWhenDataPresent() {
        // ADR-3 fix: when sleep is absent (NoData), Cuerpo = anchorBase (re-normalized, NOT 0.70*base).
        // When sleep score = 1.0, Cuerpo = 0.70*anchorBase + 0.30*1.0.
        // With 4/4 days + value=20, anchorBase ≈ 1.0 → both cases produce 1.0.
        val activity = anchor("act_body", "layer_cuerpo")
        val logs = weekDates.map { log(activity.id, it, actualValue = 20) }
        val noSleep = calculate(
            layers = listOf(layer("layer_cuerpo", "Cuerpo")) + fillerLayers(),
            activities = listOf(activity) + fillerActivities(),
            activityLogs = logs + fillerLogs(),
        )
        val withSleep = calculate(
            layers = listOf(layer("layer_cuerpo", "Cuerpo")) + fillerLayers(),
            activities = listOf(activity) + fillerActivities(),
            activityLogs = logs + fillerLogs(),
            sleepNights = listOf(sleepNight(score = 1.0f)),
        )
        val withPoorSleep = calculate(
            layers = listOf(layer("layer_cuerpo", "Cuerpo")) + fillerLayers(),
            activities = listOf(activity) + fillerActivities(),
            activityLogs = logs + fillerLogs(),
            sleepNights = listOf(sleepNight(score = 0.0f)),
        )

        val noSleepBody = noSleep.layerScores.first { it.layerId == "layer_cuerpo" }.baseScore
        val withSleepBody = withSleep.layerScores.first { it.layerId == "layer_cuerpo" }.baseScore
        val poorSleepBody = withPoorSleep.layerScores.first { it.layerId == "layer_cuerpo" }.baseScore
        // NoData: Cuerpo = anchorBase (not penalized — ADR-3). Perfect sleep: 0.70*1.0 + 0.30*1.0 = 1.0
        assertEquals(noSleepBody, withSleepBody, 0.001f)
        // Poor sleep (0.0): Cuerpo = 0.70*1.0 + 0.30*0.0 = 0.70 — LOWER than NoData
        assertEquals(0.70f, poorSleepBody, 0.001f)
        // NoData (1.0) is strictly better than poor sleep (0.70): absence ≠ poor sleep
        assertTrue("NoData Cuerpo should be > poor-sleep Cuerpo", noSleepBody > poorSleepBody)
    }

    @Test
    fun inactiveSobrietyDoesNotAffectConduct() {
        val activity = anchor("act_conduct", "layer_conducta")
        val inactiveTrack = abstinenceTrack(active = false)
        val report = calculate(
            layers = listOf(layer("layer_conducta", "Conducta")) + fillerLayers(),
            activities = listOf(activity) + fillerActivities(),
            activityLogs = weekDates.map { log(activity.id, it, actualValue = 20) } + fillerLogs(),
            abstinenceTracks = listOf(inactiveTrack),
            abstinenceLogs = listOf(abstinenceLog(inactiveTrack.id, today, AbstinenceStatus.Relapse)),
        )

        val conduct = report.layerScores.first { it.layerId == "layer_conducta" }
        assertEquals(1.0f, conduct.baseScore, 0.001f)
        assertNull(conduct.sobrietyScore)
    }

    @Test
    fun activeSobrietyEntersConductAsThirtyPercentAndRelapseLowersIt() {
        val activity = anchor("act_conduct", "layer_conducta")
        val track = abstinenceTrack()
        val cleanReport = calculate(
            layers = listOf(layer("layer_conducta", "Conducta")) + fillerLayers(),
            activities = listOf(activity) + fillerActivities(),
            activityLogs = weekDates.map { log(activity.id, it, actualValue = 20) } + fillerLogs(),
            abstinenceTracks = listOf(track),
            abstinenceLogs = weekDates.map { abstinenceLog(track.id, it, AbstinenceStatus.Clean) },
        )
        val relapseReport = calculate(
            layers = listOf(layer("layer_conducta", "Conducta")) + fillerLayers(),
            activities = listOf(activity) + fillerActivities(),
            activityLogs = weekDates.map { log(activity.id, it, actualValue = 20) } + fillerLogs(),
            abstinenceTracks = listOf(track),
            abstinenceLogs = weekDates.take(3).map { abstinenceLog(track.id, it, AbstinenceStatus.Clean) } +
                abstinenceLog(track.id, today, AbstinenceStatus.Relapse),
        )

        val cleanConduct = cleanReport.layerScores.first { it.layerId == "layer_conducta" }
        val relapseConduct = relapseReport.layerScores.first { it.layerId == "layer_conducta" }
        assertEquals(1.0f, cleanConduct.baseScore, 0.001f)
        assertTrue((relapseConduct.sobrietyScore ?: 1f) < 0.40f)
        assertTrue(relapseConduct.baseScore < cleanConduct.baseScore)
    }

    @Test
    fun pendingSobrietyWithinFiveDaysCountsAsDampenedContext() {
        val activity = anchor("act_conduct", "layer_conducta")
        val track = abstinenceTrack()
        val report = calculate(
            layers = listOf(layer("layer_conducta", "Conducta")) + fillerLayers(),
            activities = listOf(activity) + fillerActivities(),
            activityLogs = weekDates.map { log(activity.id, it, actualValue = 20) } + fillerLogs(),
            abstinenceTracks = listOf(track),
        )

        val conduct = report.layerScores.first { it.layerId == "layer_conducta" }
        assertEquals(0.425f, conduct.sobrietyScore ?: 0f, 0.001f)
        assertEquals(0.8275f, conduct.baseScore, 0.001f)
    }

    @Test
    fun worstLayerDragsWeeklyScoreByTwentyFivePercent() {
        // 3 capas con ancla (§7.4 gate): dos fuertes (1.0) + una débil sin logs (0.0).
        // avg = (1+1+0)/3 = 0.6667 ; worst = 0.0 → weeklyBase = 0.75*0.6667 + 0.25*0 = 0.50.
        val strong1 = anchor("act_interior", "layer_interior")
        val strong2 = anchor("act_vinculos", "layer_vinculos")
        val weak = anchor("act_project", "layer_proyecto")
        val report = calculate(
            layers = listOf(
                layer("layer_interior", "Interior", 10),
                layer("layer_vinculos", "Vinculos", 40),
                layer("layer_proyecto", "Proyecto", 50),
            ),
            activities = listOf(strong1, strong2, weak),
            activityLogs = weekDates.map { log(strong1.id, it, actualValue = 20) } +
                weekDates.map { log(strong2.id, it, actualValue = 20) },
        )

        assertEquals("layer_proyecto", report.worstLayerId)
        assertEquals(0.50f, report.weeklyBaseScore, 0.001f)
        assertEquals(850, report.visibleScore)
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

    @Test
    fun missingSleepRegistrationCapsStateAtMotionWithoutTouchingTheNumber() {
        // §16.7: sin registro de sueño el estado se topea en Motion (En marcha),
        // pero weeklyBaseScore/visibleScore quedan CRUDOS. ADR-3 intacto: el número
        // de Cuerpo no se penaliza (ausencia ≠ sueño malo), solo se topea el estado.
        // Con sueño registrado, las mismas anclas perfectas sí suben a Plenitud.
        val layers = coreLayers()
        val activities = layers.map { anchor("act_${it.id}", it.id) }
        val logs = activities.flatMap { activity ->
            weekDates.map { date -> log(activity.id, date, actualValue = 20) }
        }
        val track = abstinenceTrack()
        val cleanSobriety = weekDates.map { abstinenceLog(track.id, it, AbstinenceStatus.Clean) }

        val withoutSleep = calculate(
            layers = layers,
            activities = activities,
            activityLogs = logs,
            abstinenceTracks = listOf(track),
            abstinenceLogs = cleanSobriety,
            sleepNights = emptyList(),
        )
        val withSleep = calculate(
            layers = layers,
            activities = activities,
            activityLogs = logs,
            abstinenceTracks = listOf(track),
            abstinenceLogs = cleanSobriety,
            sleepNights = listOf(sleepNight(score = 1.0f)),
        )

        // Con sueño registrado: llega a Plenitud.
        assertEquals(ScoreState.Plenitude, withSleep.state)
        // Sin registro de sueño: el estado se topea en Motion...
        assertEquals(ScoreState.Motion, withoutSleep.state)
        // ...pero el número es IDÉNTICO (no se penaliza, solo se topea el estado).
        assertEquals(withSleep.visibleScore, withoutSleep.visibleScore)
        assertEquals(withSleep.weeklyBaseScore, withoutSleep.weeklyBaseScore, 0.001f)
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

    @Test
    fun fewerThanThreeActiveLayersWithAnchorReturnsNoData() {
        // §7.4: se exigen ≥3 capas activas con ancla. Con solo 2 válidas → NoData,
        // aunque los hechos estén completos.
        val layers = listOf(layer("layer_interior", "Interior", 10), layer("layer_cuerpo", "Cuerpo", 20))
        val activities = listOf(anchor("act_a", "layer_interior"), anchor("act_b", "layer_cuerpo"))
        val logs = activities.flatMap { a -> weekDates.map { log(a.id, it, actualValue = 20) } }
        val report = calculate(layers = layers, activities = activities, activityLogs = logs)

        assertEquals(ScoreState.NoData, report.state)
        assertNull(report.visibleScore)
    }

    @Test
    fun exactlyThreeActiveLayersWithAnchorOpensTheGateAndEmitsScoring() {
        // §7.4: con 3 capas activas con ancla el gate se abre y se emite scoring real.
        val layers = listOf(
            layer("layer_interior", "Interior", 10),
            layer("layer_cuerpo", "Cuerpo", 20),
            layer("layer_conducta", "Conducta", 30),
        )
        val activities = layers.map { anchor("act_${it.id}", it.id) }
        val logs = activities.flatMap { a -> weekDates.map { log(a.id, it, actualValue = 20) } }
        val report = calculate(layers = layers, activities = activities, activityLogs = logs)

        assertNotEquals(ScoreState.NoData, report.state)
        assertNotNull(report.visibleScore)
    }

    // Dos capas de relleno con ancla cumplida, para satisfacer el gate de 3 capas (§7.4)
    // en tests que aíslan UNA capa. No afectan los asserts por-capa (se filtra la capa bajo prueba).
    private fun fillerLayers(): List<Layer> =
        listOf(layer("layer_fill_a", "FillA", 91), layer("layer_fill_b", "FillB", 92))

    private fun fillerActivities(): List<ActivityDefinition> =
        listOf(anchor("act_fill_a", "layer_fill_a"), anchor("act_fill_b", "layer_fill_b"))

    private fun fillerLogs(): List<ActivityLog> =
        fillerActivities().flatMap { a -> weekDates.map { log(a.id, it, actualValue = 20) } }

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
        // 3 capas idénticas (§7.4 gate), cada una 3/4 días con actualValue=15 (target=20):
        // frequencyRatio = 3/4 = 0.75, valueRatio = 45/80 = 0.5625
        // anchorScore = 0.70*0.75 + 0.30*0.5625 = 0.69375 → weeklyBaseScore = 0.69375
        // worstLayerScore = 0.69375 (>= 0.55, < 0.75 → cap Motion)
        // Raw band = Attention (0.69375 < 0.70), margin = 0.70 - 0.69375 = 0.00625 <= 0.03.
        // With previousState=Motion from history → hysteresis holds at Motion.
        // Without previousState (no history) → falls to Attention.
        // (Sin sueño, §16.7 topea en Motion: no afecta a ninguno, ambos esperados son <= Motion.)
        val layers = listOf(
            layer("layer_interior", "Interior", 10),
            layer("layer_vinculos", "Vinculos", 40),
            layer("layer_proyecto", "Proyecto", 50),
        )
        val activities = layers.map { anchor("act_${it.id}", it.id) }
        val logs = activities.flatMap { a -> weekDates.take(3).map { log(a.id, it, actualValue = 15) } }

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
            layers = layers,
            activities = activities,
            activityLogs = logs,
            weeklyHistory = listOf(historyEntry),
        )

        val withoutHistory = calculate(
            layers = layers,
            activities = activities,
            activityLogs = logs,
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
