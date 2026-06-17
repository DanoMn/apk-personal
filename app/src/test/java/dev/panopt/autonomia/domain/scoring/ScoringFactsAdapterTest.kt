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
import dev.panopt.autonomia.DailyActivityStatus
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.sleep.SleepNightScore
import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PR-E — `ScoringFactsAdapter`: hechos Room (ventana 7d) → formas que el motor consume.
 *
 * Cada test reproduce un Scenario del delta-spec `scoring-facts-adapter`. Hechos sintéticos en
 * `Double`, tolerancia `1e-9`. El adapter NO recalcula scoring; solo adapta forma. La verificación
 * de que la forma reconstruida da el `R` esperado se hace pasando `mins` por
 * [AnchorScoringPolicyV2.r] (caso §1.4).
 */
class ScoringFactsAdapterTest {
    private val tol = 1e-9
    // Lunes de la semana de prueba; hoy = miércoles (3 días con actividad posibles lun..mié).
    private val monday = LocalDate.of(2026, 6, 1)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val today = wednesday

    // ---------------- Anclas: (F, T, mins) ----------------

    @Test
    fun tresDiasCumplidosReconstruyenMins_cumplirJusto() {
        val def = anchorDef("ancla", f = 3, t = 30)
        val logs = listOf(
            log("ancla", monday, 30),
            log("ancla", tuesday, 30),
            log("ancla", wednesday, 30),
        )
        val window = ScoringFactsAdapter.anchorWindow(def, logs)
        assertEquals(3, window.f)
        assertEquals(30, window.t)
        assertEquals(listOf(30, 30, 30), window.mins.sorted())
        // El motor da R = 1.000 (cumplir-justo §1.4).
        assertEquals(1.0, AnchorScoringPolicyV2.r(window.f, window.t, window.mins), 1e-3)
    }

    @Test
    fun superhabitPorDiasReconstruido() {
        val def = anchorDef("ancla", f = 4, t = 30)
        val logs = (0 until 6).map { log("ancla", monday.plusDays(it.toLong()), 30) }
        val window = ScoringFactsAdapter.anchorWindow(def, logs)
        assertEquals(List(6) { 30 }, window.mins)
        // El motor da R ≈ 1.266 (superhabit por días §1.4).
        assertEquals(1.266, AnchorScoringPolicyV2.r(window.f, window.t, window.mins), 1e-3)
    }

    @Test
    fun logDuplicadoNoInflaLaFrecuencia() {
        // El builder dedup por "activityId:date"; el adapter recibe ya deduplicado. Aquí
        // verificamos que dos logs de la MISMA fecha colapsan a un solo día con actividad.
        val def = anchorDef("ancla", f = 4, t = 30)
        val logs = listOf(
            log("ancla", monday, 30),
            log("ancla", monday, 30), // duplicado misma fecha
            log("ancla", tuesday, 30),
        )
        val window = ScoringFactsAdapter.anchorWindow(def, logs)
        assertEquals(2, window.mins.size) // solo 2 días con actividad, no 3
    }

    @Test
    fun notDoneCuentaComoDiaSinActividad() {
        val def = anchorDef("ancla", f = 4, t = 30)
        val logs = listOf(
            log("ancla", monday, 30, status = DailyActivityStatus.Done),
            log("ancla", tuesday, 0, status = DailyActivityStatus.NotDone),
            log("ancla", wednesday, 30, status = DailyActivityStatus.Done),
        )
        val window = ScoringFactsAdapter.anchorWindow(def, logs)
        assertEquals(listOf(30, 30), window.mins) // el día NotDone no aporta minuto positivo
    }

    @Test
    fun omittedSeExcluyeDeLaVentana() {
        val def = anchorDef("ancla", f = 4, t = 30)
        val logs = listOf(
            log("ancla", monday, 30, status = DailyActivityStatus.Done),
            log("ancla", tuesday, 30, status = DailyActivityStatus.Omitted),
        )
        val window = ScoringFactsAdapter.anchorWindow(def, logs)
        assertEquals(listOf(30), window.mins) // Omitted no cuenta como día con actividad
    }

    @Test
    fun actualValueNullConDoneEsCeroMinutos() {
        val def = anchorDef("ancla", f = 4, t = 30)
        val logs = listOf(
            log("ancla", monday, value = null, status = DailyActivityStatus.Done),
            log("ancla", tuesday, 30, status = DailyActivityStatus.Done),
        )
        val window = ScoringFactsAdapter.anchorWindow(def, logs)
        assertEquals(listOf(30), window.mins) // Done sin minutos = 0 min, no aporta
    }

    // ---------------- Soportes: días sostenidos (ventana 4d, UX inversa) ----------------

    @Test
    fun soporteSinRegistros_totalmenteSostenido() {
        val days = ScoringFactsAdapter.sustainedSupportDays(emptyList(), today)
        assertEquals(4, days) // s = 1.0 por defecto
        assertEquals(1.0, LayerValuePolicy.supportSignal(listOf(days)), tol)
    }

    @Test
    fun soporteConUnaOmision_senalDegradada() {
        // Una omisión registrada (countsAsDone) dentro de la ventana de 4 días.
        val logs = listOf(log("sop", today, 1, status = DailyActivityStatus.Done))
        val days = ScoringFactsAdapter.sustainedSupportDays(logs, today)
        assertTrue("dias_sostenidos debe ser < 4", days < 4)
        assertTrue("la senal s debe degradarse", LayerValuePolicy.supportSignal(listOf(days)) < 1.0)
    }

    @Test
    fun soporteOmisionFueraDeVentana4d_noResta() {
        // Una omisión hace 5 días (fuera de la ventana indulgente de 4) no resta.
        val logs = listOf(log("sop", today.minusDays(5), 1, status = DailyActivityStatus.Done))
        val days = ScoringFactsAdapter.sustainedSupportDays(logs, today)
        assertEquals(4, days)
    }

    // ---------------- Tasks: n_tasks_hoy por capa (efímero) ----------------

    @Test
    fun tasksDeHoyCuentan_lasDeAyerNo() {
        val tasks = listOf(
            task("t_hoy", layerId = "cuerpo", completedAt = epochOf(today)),
            task("t_ayer", layerId = "cuerpo", completedAt = epochOf(today.minusDays(1))),
        )
        val byLayer = ScoringFactsAdapter.tasksTodayByLayer(tasks, today)
        assertEquals(1, byLayer["cuerpo"] ?: 0) // solo la de hoy
    }

    @Test
    fun taskSinCapaNoCuenta() {
        val tasks = listOf(task("t", layerId = null, completedAt = epochOf(today)))
        val byLayer = ScoringFactsAdapter.tasksTodayByLayer(tasks, today)
        assertTrue("una task sin capa no incrementa ninguna capa", byLayer.values.all { it == 0 } || byLayer.isEmpty())
    }

    @Test
    fun taskNeutralNoCuenta() {
        val tasks = listOf(
            task("t", layerId = "cuerpo", completedAt = epochOf(today), role = ContributionRole.Neutral),
        )
        val byLayer = ScoringFactsAdapter.tasksTodayByLayer(tasks, today)
        assertEquals(0, byLayer["cuerpo"] ?: 0)
    }

    // ---------------- Tracks: días de recaída (ventana 7d) ----------------

    @Test
    fun trackLimpioNoDiluye() {
        val track = track("trk")
        val days = ScoringFactsAdapter.relapseDaysByTrack(track, emptyList(), weekDates())
        assertEquals(0, days)
        assertEquals(1.0, OptInPolicy.sobrietySignal(listOf(days))!!, tol) // factor 1.0
    }

    @Test
    fun multiTrackCompone() {
        val a = track("a")
        val b = track("b")
        val logsA = listOf(relapse("a", monday))
        val logsB = listOf(relapse("b", tuesday))
        val daysA = ScoringFactsAdapter.relapseDaysByTrack(a, logsA, weekDates())
        val daysB = ScoringFactsAdapter.relapseDaysByTrack(b, logsB, weekDates())
        assertEquals(1, daysA)
        assertEquals(1, daysB)
        // M_sobr = (1−A)^1 · (1−A)^1, composición sin tope.
        val expected = (1.0 - ScoringConstantsV2.A) * (1.0 - ScoringConstantsV2.A)
        assertEquals(expected, OptInPolicy.sobrietySignal(listOf(daysA, daysB))!!, tol)
    }

    // ---------------- Sueño: señal M ----------------

    @Test
    fun sinNochesConDato_mEsNull() {
        val nights = listOf(night(null), night(null))
        assertNull(ScoringFactsAdapter.sleepSignal(nights))
    }

    @Test
    fun nochesConDato_promedio() {
        val nights = listOf(night(0.8f), night(0.6f), night(1.0f), night(null))
        val m = ScoringFactsAdapter.sleepSignal(nights)!!
        assertEquals(0.8, m, 1e-7) // (0.8+0.6+1.0)/3, NoData excluida
    }

    // ---------------- Caso límite: semana vacía ----------------

    @Test
    fun semanaVacia_estructurasVaciasYmNull() {
        val ctx = WeeklyScoringContext(
            weekStart = monday,
            weekDates = weekDates(),
            activeLayers = emptyList(),
            visibleActivities = emptyList(),
            weeklyLogsByActivity = emptyMap(),
            activeSobrietyTracks = emptyList(),
            sleepScore = null,
            sobrietyScore = null,
            completedTasksByLayer = emptyMap(),
        )
        val facts = ScoringFactsAdapter.buildLayerFacts(ctx, emptyList(), today)
        assertTrue("sin capas activas → sin LayerFacts", facts.isEmpty())
        assertNull(ScoringFactsAdapter.sleepSignal(emptyList()))
    }

    @Test
    fun buildLayerFacts_componeAnclaSoporteTaskPorCapa() {
        val cuerpo = Layer("cuerpo", "Cuerpo", "", sortOrder = 1)
        val anchorDef = anchorDef("ancla", f = 4, t = 30, layerId = "cuerpo")
        val supportDef = supportDef("sop", layerId = "cuerpo")
        val logs = mapOf(
            "ancla" to listOf(
                log("ancla", monday, 30),
                log("ancla", tuesday, 30),
                log("ancla", wednesday, 30),
            ),
            "sop" to emptyList(), // soporte sostenido por defecto
        )
        val ctx = WeeklyScoringContext(
            weekStart = monday,
            weekDates = weekDates(),
            activeLayers = listOf(cuerpo),
            visibleActivities = listOf(anchorDef, supportDef),
            weeklyLogsByActivity = logs,
            activeSobrietyTracks = emptyList(),
            sleepScore = null,
            sobrietyScore = null,
            completedTasksByLayer = emptyMap(),
        )
        val tasks = listOf(task("t", layerId = "cuerpo", completedAt = epochOf(today)))
        val facts = ScoringFactsAdapter.buildLayerFacts(ctx, tasks, today)
        assertEquals(1, facts.size)
        val cuerpoFacts = facts.single()
        assertEquals(1, cuerpoFacts.anchors.size)
        assertEquals(listOf(30, 30, 30), cuerpoFacts.anchors.single().mins.sorted())
        assertEquals(listOf(4), cuerpoFacts.supportDays) // sostenido por defecto
        assertEquals(1, cuerpoFacts.nTasksToday) // task de hoy
    }

    // ---------------- helpers ----------------

    private fun weekDates(): List<LocalDate> = (0..6).map { monday.plusDays(it.toLong()) }

    private fun anchorDef(
        id: String,
        f: Int,
        t: Int,
        layerId: String = "cuerpo",
    ): ActivityDefinition = baseDef(id, layerId, ActivitySurface.Anchor).copy(
        weeklyFrequencyTarget = f,
        sessionTargetMinutes = t,
        targetValue = t,
        cadence = ActivityCadence.Custom,
    )

    private fun supportDef(id: String, layerId: String = "cuerpo"): ActivityDefinition =
        baseDef(id, layerId, ActivitySurface.Support)

    private fun baseDef(id: String, layerId: String, surface: ActivitySurface): ActivityDefinition =
        ActivityDefinition(
            id = id,
            layerId = layerId,
            name = id,
            description = "",
            type = ActivityType.Time,
            role = ActivityRole.Practice,
            activityType = surface,
            contributionRole = ContributionRole.Core,
            importanceTier = ImportanceTier.Medium,
            cadence = ActivityCadence.Daily,
            targetValue = 30,
            minimumValue = 1,
            targetCount = null,
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = 4,
            sessionTargetMinutes = 30,
            commitmentDurationMonths = null,
            unit = ActivityUnit.Minutes,
            active = true,
            archived = false,
            sortOrder = 0,
        )

    private fun log(
        activityId: String,
        date: LocalDate,
        value: Int?,
        status: DailyActivityStatus = DailyActivityStatus.Done,
    ): ActivityLog = ActivityLog(
        activityId = activityId,
        date = date.toString(),
        completed = status == DailyActivityStatus.Done && (value ?: 0) > 0,
        actualValue = value,
        status = status,
    )

    private fun task(
        id: String,
        layerId: String?,
        completedAt: Long?,
        role: ContributionRole = ContributionRole.Core,
    ): Task = Task(
        id = id,
        title = id,
        description = "",
        layerId = layerId,
        projectId = null,
        status = TaskStatus.Done,
        contributionRole = role,
        importanceTier = ImportanceTier.Medium,
        dueDate = null,
        completedAt = completedAt,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun track(id: String): AbstinenceTrack = AbstinenceTrack(
        id = id,
        name = id,
        substanceLabel = id,
        severity = AbstinenceSeverity.Moderate,
        contributionRole = ContributionRole.Protective,
        importanceTier = ImportanceTier.High,
        active = true,
        sortOrder = 0,
    )

    private fun relapse(trackId: String, date: LocalDate): AbstinenceLog = AbstinenceLog(
        trackId = trackId,
        date = date.toString(),
        status = AbstinenceStatus.Relapse,
    )

    private fun night(score: Float?): SleepNightScore = SleepNightScore(
        duration = 0f,
        continuity = 0f,
        alignment = 0f,
        digitalInterruption = 0f,
        sleepScore = score,
        confidence = if (score == null) SleepConfidence.NoData else SleepConfidence.High,
    )

    private fun epochOf(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Suppress("unused")
    private fun instant(epoch: Long): Instant = Instant.ofEpochMilli(epoch)
}
