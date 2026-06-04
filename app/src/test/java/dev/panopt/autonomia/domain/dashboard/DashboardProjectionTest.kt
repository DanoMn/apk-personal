package dev.panopt.autonomia.domain.dashboard

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.AttributionStatus
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.PhraseFamily
import dev.panopt.autonomia.SleepNight
import dev.panopt.autonomia.SleepSessionState
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardProjectionTest {
    private val today = LocalDate.of(2026, 5, 21)
    private val dateKey = today.toString()

    @Test
    fun `support activities populate support items`() {
        val anchor = supportActivity(
            id = "act_anchor",
            name = "Meditar",
            layerId = "layer_interior",
            activityType = ActivitySurface.Anchor,
        )
        val bath = supportActivity(
            id = "act_bath",
            name = "Bañarse",
            layerId = "layer_cuerpo",
            activityType = ActivitySurface.Support,
        )
        val eat = supportActivity(
            id = "act_eat",
            name = "Comer",
            layerId = "layer_cuerpo",
            activityType = ActivitySurface.Support,
        )
        val anchorLog = log("act_anchor")
        val bathLog = ActivityLog(
            activityId = "act_bath",
            date = dateKey,
            completed = true,
            actualValue = 1,
            updatedAt = 0L,
        )

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = listOf(anchor, bath, eat),
            todayActivityLogs = listOf(anchorLog, bathLog),
            weekActivityLogs = listOf(anchorLog, bathLog),
            periodActivityLogs = listOf(anchorLog, bathLog),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        // supportItems should contain configured supports.
        assertFalse(
            "supportItems should not be empty when supports are configured",
            state.supportItems.isEmpty(),
        )
        assertEquals(2, state.supportItems.size)

        // Each support item should have activityType populated
        val bathItem = state.supportItems.first { it.title == "Bañarse" }
        assertEquals("Support", bathItem.activityType)

        val eatItem = state.supportItems.first { it.title == "Comer" }
        assertEquals("Support", eatItem.activityType)
    }

    @Test
    fun `anchor items have activityType Anchor`() {
        val anchor = supportActivity(
            id = "act_anchor",
            name = "Meditar",
            layerId = "layer_interior",
            activityType = ActivitySurface.Anchor,
        )
        val log = log("act_anchor")

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = listOf(anchor),
            todayActivityLogs = listOf(log),
            weekActivityLogs = listOf(log),
            periodActivityLogs = listOf(log),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertEquals(1, state.anchorItems.size)
        assertEquals("Anchor", state.anchorItems[0].activityType)
    }

    @Test
    fun `no supports configured produces empty support items`() {
        val anchor = supportActivity(
            id = "act_anchor",
            name = "Meditar",
            layerId = "layer_interior",
            activityType = ActivitySurface.Anchor,
        )

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = listOf(anchor),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertTrue(
            "supportItems should be empty when no supports configured",
            state.supportItems.isEmpty(),
        )
    }

    @Test
    fun `DashboardCheckItemState has activityType default value`() {
        val item = DashboardCheckItemState(
            id = "test",
            title = "Test",
            layerId = "layer_interior",
            layerName = "Interior",
            value = "30 min",
            completed = false,
        )
        // Default should be empty string (backward compatible)
        assertEquals("", item.activityType)
    }

    @Test
    fun `DashboardCheckItemState accepts explicit activityType`() {
        val item = DashboardCheckItemState(
            id = "test",
            title = "Test",
            layerId = "layer_interior",
            layerName = "Interior",
            value = "30 min",
            completed = false,
            activityType = "Support",
        )
        assertEquals("Support", item.activityType)
    }

    @Test
    fun `completed support items show as completed in support items`() {
        val bath = supportActivity(
            id = "act_bath",
            name = "Bañarse",
            layerId = "layer_cuerpo",
            activityType = ActivitySurface.Support,
        )
        val eat = supportActivity(
            id = "act_eat",
            name = "Comer",
            layerId = "layer_cuerpo",
            activityType = ActivitySurface.Support,
        )
        val bathLog = log("act_bath")

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = listOf(bath, eat),
            todayActivityLogs = listOf(bathLog),
            weekActivityLogs = listOf(bathLog),
            periodActivityLogs = listOf(bathLog),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertEquals(2, state.supportItems.size)

        val bathItem = state.supportItems.first { it.title == "Bañarse" }
        assertTrue("Bañarse with log should be completed", bathItem.completed)

        val eatItem = state.supportItems.first { it.title == "Comer" }
        assertFalse("Comer without log should not be completed", eatItem.completed)
    }

    @Test
    fun `pending tasks populate pendingTasks and done tasks populate completedTasks`() {
        val pendingTask = task(
            id = "task_pending",
            title = "Comprar cafe",
            layerId = "layer_cuerpo",
            status = TaskStatus.Pending,
        )
        val doneTask = task(
            id = "task_done",
            title = "Enviar correo",
            layerId = "layer_proyecto",
            status = TaskStatus.Done,
            completedAt = 100L,
        )

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = listOf(pendingTask, doneTask),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertEquals(listOf("task_pending"), state.pendingTasks.map { it.id })
        assertEquals(listOf("task_done"), state.completedTasks.map { it.id })
    }

    @Test
    fun `completed tasks do not appear in dashboard pending tasks`() {
        val doneTask = task(
            id = "task_done",
            status = TaskStatus.Done,
            completedAt = 100L,
        )

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = listOf(doneTask),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertTrue(state.pendingTasks.isEmpty())
        assertEquals(listOf("task_done"), state.completedTasks.map { it.id })
    }

    @Test
    fun `task without layer is projected without layer for neutral dashboard rendering`() {
        val neutralTask = task(
            id = "task_neutral",
            title = "Cerrar una pestaña mental",
            layerId = null,
            contributionRole = ContributionRole.Neutral,
            status = TaskStatus.Pending,
        )

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = listOf(neutralTask),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertEquals(null, state.pendingTasks.single().layerId)
    }

    @Test
    fun `only active abstinence tracks appear in dashboard sobriety tracks`() {
        val active = abstinenceTrack(id = "trk_alcohol", active = true, sortOrder = 10)
        val inactive = abstinenceTrack(id = "trk_substances", active = false, sortOrder = 20)

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = listOf(active, inactive),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertEquals(listOf("trk_alcohol"), state.sobrietyTracks.map { it.id })
        assertEquals(listOf("trk_alcohol", "trk_substances"), state.sobrietyOptions.map { it.id })
    }

    @Test
    fun `unknown abstinence status is not projected as relapse`() {
        val track = abstinenceTrack(id = "trk_alcohol", active = true)

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = listOf(track),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        val projected = state.sobrietyTracks.single()
        assertFalse(projected.isRelapseToday)
        assertFalse(projected.isMarkedCleanToday)
        assertEquals(DashboardDimensionStatus.Unknown, projected.status)
    }

    @Test
    fun `clean abstinence streak counts consecutive clean days`() {
        val track = abstinenceTrack(id = "trk_alcohol", active = true)
        val logs = listOf(
            abstinenceLog(track.id, today, AbstinenceStatus.Clean),
            abstinenceLog(track.id, today.minusDays(1), AbstinenceStatus.Clean),
            abstinenceLog(track.id, today.minusDays(2), AbstinenceStatus.Relapse),
        )

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = listOf(track),
            todayAbstinenceLogs = logs.filter { it.date == dateKey },
            allAbstinenceLogs = logs,
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertEquals(2, state.sobrietyTracks.single().days)
    }

    @Test
    fun `relapse today on critical track projects restoration`() {
        val track = abstinenceTrack(id = "trk_alcohol", active = true, severity = AbstinenceSeverity.Critical)
        val logs = listOf(abstinenceLog(track.id, today, AbstinenceStatus.Relapse))

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = listOf(track),
            todayAbstinenceLogs = logs.filter { it.date == dateKey },
            allAbstinenceLogs = logs,
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        val projected = state.sobrietyTracks.single()
        assertTrue(projected.isRelapseToday)
        assertFalse(projected.isMarkedCleanToday)
        assertEquals(DashboardDimensionStatus.Restoration, projected.status)
        assertEquals("senal registrada", projected.meta)
        assertEquals(0, projected.days)
    }

    @Test
    fun `relapse today on non-critical track projects attention`() {
        val track = abstinenceTrack(id = "trk_alcohol", active = true, severity = AbstinenceSeverity.Moderate)
        val logs = listOf(abstinenceLog(track.id, today, AbstinenceStatus.Relapse))

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = listOf(track),
            todayAbstinenceLogs = logs.filter { it.date == dateKey },
            allAbstinenceLogs = logs,
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        val projected = state.sobrietyTracks.single()
        assertTrue(projected.isRelapseToday)
        assertEquals(DashboardDimensionStatus.Attention, projected.status)
    }

    @Test
    fun `relapse in the middle breaks the clean streak`() {
        val track = abstinenceTrack(id = "trk_alcohol", active = true)
        val logs = listOf(
            abstinenceLog(track.id, today, AbstinenceStatus.Clean),
            abstinenceLog(track.id, today.minusDays(1), AbstinenceStatus.Clean),
            abstinenceLog(track.id, today.minusDays(2), AbstinenceStatus.Relapse),
            abstinenceLog(track.id, today.minusDays(3), AbstinenceStatus.Clean),
        )

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = listOf(track),
            todayAbstinenceLogs = logs.filter { it.date == dateKey },
            allAbstinenceLogs = logs,
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        // La racha cuenta hacia atrás solo hasta la recaída: hoy + ayer = 2, el día limpio
        // anterior a la recaída no se acumula.
        assertEquals(2, state.sobrietyTracks.single().days)
    }

    @Test
    fun `sleep signal asks to register when there is no night`() {
        val state = emptyDashboardState(sleepNight = null)
        val signal = state.signals.first { it.kind == DashboardSignalKind.Sleep }

        assertEquals("--", signal.value)
        assertEquals("toca registrar", signal.meta)
        assertEquals(DashboardDimensionStatus.Unknown, signal.status)
    }

    @Test
    fun `sleep signal shows low sleep score as bajo`() {
        // SleepNight with scored sleepScore below 0.70 → Attention + "descanso bajo"
        val state = emptyDashboardState(
            sleepNight = sleepNightWithScore(sleepScore = 0.55f),
        )
        val signal = state.signals.first { it.kind == DashboardSignalKind.Sleep }

        assertEquals("descanso bajo", signal.meta)
        assertEquals(DashboardDimensionStatus.Attention, signal.status)
    }

    @Test
    fun `sleep signal marks high score as base cubierta`() {
        // SleepNight with sleepScore >= 0.90 → Stable + "base cubierta"
        val state = emptyDashboardState(
            sleepNight = sleepNightWithScore(sleepScore = 0.95f),
        )
        val signal = state.signals.first { it.kind == DashboardSignalKind.Sleep }

        assertEquals("base cubierta", signal.meta)
        assertEquals(DashboardDimensionStatus.Stable, signal.status)
    }

    @Test
    fun `sleep signal shows open sleep session`() {
        val state = emptyDashboardState(
            sleepNight = null,
            sleepSession = SleepSessionState(date = dateKey, startedAt = "23:10"),
        )
        val signal = state.signals.first { it.kind == DashboardSignalKind.Sleep }

        assertEquals("desde 23:10", signal.value)
        assertEquals("en descanso", signal.meta)
        assertEquals(DashboardDimensionStatus.Motion, signal.status)
        assertEquals(true, state.sleep.isSessionOpen)
    }

    // --- anchor phrase projection tests (Slice 6, DASH-REQ-1 / DASH-REQ-2) ---

    @Test
    fun `slot present with phraseId in catalog returns matching text and authorReference`() {
        val catalogPhrase = anchorPhrase(
            id = "phrase_001",
            text = "Cada dia es una oportunidad nueva.",
            authorReference = "Proverbio anonimo",
        )
        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = listOf(catalogPhrase),
            anchorPhrasePhraseId = "phrase_001",
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertEquals("Cada dia es una oportunidad nueva.", state.anchorPhrase.text)
        assertEquals("Proverbio anonimo", state.anchorPhrase.authorReference)
    }

    @Test
    fun `no slot produces empty neutral anchor phrase state without Kierkegaard`() {
        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            anchorPhrasePhraseId = null,
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        // Must NOT contain the old hardcoded Kierkegaard text
        assertFalse(
            "anchorPhrase.text must not contain 'Kierkegaard' or attributed text when no slot",
            state.anchorPhrase.text.contains("Kierkegaard") || state.anchorPhrase.authorReference.contains("Kierkegaard"),
        )
        // Default state should be empty/neutral
        assertEquals("", state.anchorPhrase.text)
        assertEquals("", state.anchorPhrase.authorReference)
    }

    @Test
    fun `slot phraseId not found in catalog returns graceful empty fallback`() {
        val catalogPhrase = anchorPhrase(
            id = "phrase_exists",
            text = "Una frase valida.",
            authorReference = "Autor conocido",
        )
        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = listOf(catalogPhrase),
            anchorPhrasePhraseId = "phrase_missing",
            sleepNight = null,
            focusSignalActivityId = null,
            today = today,
        )

        // phrase not found → graceful empty, no crash
        assertEquals("", state.anchorPhrase.text)
        assertEquals("", state.anchorPhrase.authorReference)
    }

    @Test
    fun `DashboardAnchorPhraseState default constructor has no Kierkegaard text`() {
        val defaultState = DashboardAnchorPhraseState()
        assertNotEquals(
            "Default anchorPhrase text must not be the old Kierkegaard quote",
            "Life can only be understood backwards; but it must be lived forwards.",
            defaultState.text,
        )
        assertFalse(
            "Default anchorPhrase authorReference must not contain 'Kierkegaard'",
            defaultState.authorReference.contains("Kierkegaard"),
        )
    }

    // --- helpers ---

    private fun emptyDashboardState(
        sleepNight: SleepNight? = null,
        sleepSession: SleepSessionState? = null,
    ): DashboardState =
        buildDashboardState(
            layers = defaultLayers(),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = sleepNight,
            sleepSession = sleepSession,
            focusSignalActivityId = null,
            today = today,
        )

    private fun defaultLayers(): List<Layer> = listOf(
        Layer("layer_interior", "Interior", "", 10),
        Layer("layer_cuerpo", "Cuerpo", "", 20),
        Layer("layer_conducta", "Conducta", "", 30),
        Layer("layer_vinculos", "Vinculos", "", 40),
        Layer("layer_proyecto", "Proyecto", "", 50),
    )

    private fun supportActivity(
        id: String,
        name: String,
        layerId: String,
        activityType: ActivitySurface,
    ): ActivityDefinition = ActivityDefinition(
        id = id,
        layerId = layerId,
        name = name,
        description = "",
        type = ActivityType.Check,
        role = ActivityRole.Practice,
        activityType = activityType,
        contributionRole = if (activityType == ActivitySurface.Anchor) ContributionRole.Core else ContributionRole.Support,
        importanceTier = ImportanceTier.Medium,
        cadence = ActivityCadence.Daily,
        targetValue = if (activityType == ActivitySurface.Anchor) 30 else null,
        minimumValue = if (activityType == ActivitySurface.Anchor) 1 else null,
        targetCount = null,
        targetPeriod = TargetPeriod.Day,
        unit = if (activityType == ActivitySurface.Anchor) ActivityUnit.Minutes else ActivityUnit.Boolean,
        sortOrder = 10,
    )

    private fun log(activityId: String): ActivityLog = ActivityLog(
        activityId = activityId,
        date = dateKey,
        completed = true,
        actualValue = 30,
        updatedAt = 0L,
    )

    private fun task(
        id: String,
        title: String = id,
        layerId: String? = "layer_interior",
        status: TaskStatus = TaskStatus.Pending,
        contributionRole: ContributionRole = if (layerId == null) ContributionRole.Neutral else ContributionRole.Support,
        completedAt: Long? = null,
        createdAt: Long = 0L,
    ): Task = Task(
        id = id,
        title = title,
        description = "",
        layerId = layerId,
        projectId = null,
        status = status,
        contributionRole = contributionRole,
        importanceTier = ImportanceTier.Medium,
        dueDate = null,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun abstinenceTrack(
        id: String,
        active: Boolean,
        sortOrder: Int = 10,
        severity: AbstinenceSeverity = AbstinenceSeverity.Critical,
    ): AbstinenceTrack = AbstinenceTrack(
        id = id,
        name = id,
        substanceLabel = id,
        severity = severity,
        contributionRole = ContributionRole.Protective,
        importanceTier = ImportanceTier.Critical,
        active = active,
        sortOrder = sortOrder,
    )

    private fun abstinenceLog(
        trackId: String,
        date: LocalDate,
        status: AbstinenceStatus,
    ): AbstinenceLog = AbstinenceLog(
        trackId = trackId,
        date = date.toString(),
        status = status,
        updatedAt = 0L,
    )

    private fun sleepNightWithScore(sleepScore: Float): SleepNight =
        SleepNight(
            nightDate = dateKey,
            targetSleepAt = "23:30",
            targetWakeAt = "07:30",
            sleepOnsetAt = null,
            definitiveWakeAt = null,
            confidenceLevel = "High",
            durationScore = sleepScore,
            continuityScore = sleepScore,
            alignmentScore = sleepScore,
            digitalInterruptionScore = sleepScore,
            sleepScore = sleepScore,
            note = "",
            source = "auto",
        )

    private fun anchorPhrase(
        id: String,
        text: String,
        authorReference: String,
    ): AnchorPhrase = AnchorPhrase(
        id = id,
        text = text,
        authorReference = authorReference,
        family = PhraseFamily.Containment,
        language = "es",
        attributionStatus = AttributionStatus.Clear,
        active = true,
        sortOrder = 10,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
