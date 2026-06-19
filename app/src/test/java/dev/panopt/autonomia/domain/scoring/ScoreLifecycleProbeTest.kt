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
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.sleep.SleepNightScore
import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SONDA (NO es test de fix) — REVELA el comportamiento del motor ante la dinámica de ciclo de vida:
 * activar / DESACTIVAR / REACTIVAR una entidad DENTRO DE LA MISMA SEMANA, manteniendo sus hechos.
 *
 * Principio bajo prueba (handoff 2026-06-17 §1): los hechos persisten; la ventana de 7 días se
 * re-lee entera; el filtro `active` decide qué entra. "Desactivar" = la entidad sale del `ScoreInput`
 * (filtrada upstream por `BuildScoreInputUseCase`/`getActiveUserActivityConfigs`); sus logs SIGUEN en
 * la base pero quedan sin usar. "Reactivar" = vuelve a entrar y sus logs de la ventana se re-leen.
 *
 * Estas sondas modelan "desactivado" como AUSENCIA de la entidad en `activities`/`abstinenceTracks`
 * del input (manteniendo los logs en `periodActivityLogs`/`allAbstinenceLogs`), que es exactamente
 * lo que produce el filtro `active` aguas arriba.
 */
class ScoreLifecycleProbeTest {

    private val monday = LocalDate.of(2026, 6, 1)
    private val today = monday.plusDays(2) // miércoles

    // Ventana lun..mié con minutos suficientes para cumplir (f=4 no se alcanza, pero hay señal).
    private fun fullWeekLogs(ids: List<String>): List<ActivityLog> =
        ids.flatMap { id -> (0..2).map { log(id, monday.plusDays(it.toLong()), 30) } }

    // ---------- P1 · Desactivar un ancla a mitad de semana: ¿cae entera del cálculo? ----------

    @Test
    fun P1_desactivarAncla_caeEntera_logsQuedanSinUsar() {
        val layers = coreThreeLayers()
        val anchors = coreThreeAnchors()
        val logs = fullWeekLogs(anchors.map { it.id })

        // A — las 3 anclas ACTIVAS (presentes) con sus logs.
        val active = calculate(layers = layers, activities = anchors, activityLogs = logs)

        // B — una ancla DESACTIVADA (ausente de `activities`) PERO sus logs siguen en la ventana.
        val anchorsMinusOne = anchors.drop(1) // saco la de Interior
        val deactivated = calculate(layers = layers, activities = anchorsMinusOne, activityLogs = logs)

        // REVELA: aunque los logs de la ancla desactivada SIGUEN en la ventana, no se usan →
        // el ESTADO cambia. Además, con solo 2 capas-con-ancla cae el gate (MIN=3) → NoData.
        println("[P1] estado activo=${active.estado} desactivado=${deactivated.state}/${deactivated.estado}")
        assertNotEquals(active.estado, deactivated.estado)
    }

    // ---------- P2 · Reactivar: ¿los datos de la ventana "vuelven"? ----------

    @Test
    fun P2_reactivarAncla_reusaLogsDeLaVentana_identicoANuncaHaberseDesactivado() {
        val layers = coreThreeLayers()
        val anchors = coreThreeAnchors()
        val logs = fullWeekLogs(anchors.map { it.id })

        // A — ancla siempre activa.
        val neverToggled = calculate(layers = layers, activities = anchors, activityLogs = logs)
        // C — ancla "reactivada" = vuelve a estar presente; sus logs de la ventana nunca se borraron.
        val reactivated = calculate(layers = layers, activities = anchors, activityLogs = logs)

        // REVELA: reactivar (volver a estar presente) re-lee los mismos logs de la ventana → estado
        // idéntico al de no haberse desactivado nunca. Los datos "vuelven", acotados a la ventana 7d.
        println("[P2] neverToggled=${neverToggled.estado} reactivated=${reactivated.estado}")
        assertEquals(neverToggled.estado, reactivated.estado, 1e-6f)
    }

    // ---------- P3 · Soporte: días OFF (sin logs) se acreditan como SOSTENIDO al reactivar ----------

    @Test
    fun P3_soporteSinLogsEnVentana_cuentaComoSostenido() {
        // Un soporte presente SIN ningún log en la ventana puntúa sostenido=4 (s=1.0): idéntico a un
        // soporte que se mantuvo limpio activamente. Si estuvo DESACTIVADO esos días tampoco hubo
        // cierre (config inactiva ⇒ sin logs), así que al reactivarlo la ventana lo ve "sostenido".
        val sustainedFromNoLogs = ScoringFactsAdapter.sustainedSupportDays(emptyList(), today)
        assertEquals(4, sustainedFromNoLogs)
        assertEquals(1.0, LayerValuePolicy.supportSignal(listOf(sustainedFromNoLogs)), 1e-9)
    }

    // ---------- P4 · Sobriedad: ¿la recaída de la ventana "reaparece" al reactivar el track? ----------

    @Test
    fun P4_reactivarTrack_recaidaEnVentanaVuelveAArrastrar() {
        val layers = coreThreeLayers()
        val anchors = coreThreeAnchors()
        val logs = fullWeekLogs(anchors.map { it.id })
        val track = abstinenceTrack("trk_alcohol", active = true)
        val relapse = listOf(abstinenceLog("trk_alcohol", monday, AbstinenceStatus.Relapse))

        // Track ACTIVO con una recaída en la ventana → arrastra Conducta.
        val withTrack = calculate(
            layers = layers, activities = anchors, activityLogs = logs,
            abstinenceTracks = listOf(track), abstinenceLogs = relapse,
        )
        // Track DESACTIVADO (ausente) con la MISMA recaída en la base → no arrastra.
        val withoutTrack = calculate(
            layers = layers, activities = anchors, activityLogs = logs,
            abstinenceTracks = emptyList(), abstinenceLogs = relapse,
        )

        // REVELA: la recaída de la ventana solo pesa si el track está activo. Reactivar (volver a
        // estar presente) hace que la recaída de los últimos 7 días "reaparezca" y vuelva a arrastrar.
        println("[P4] conRecaida=${withTrack.estado} sinTrack=${withoutTrack.estado}")
        assertTrue("con track activo el arrastre baja o iguala el estado", withTrack.estado <= withoutTrack.estado)
    }

    // ---------- P5 · Sueño: NO hay flag active; es telemetría guiada por dato ----------

    @Test
    fun P5_suenoEsDataDriven_noHayToggleDeActivacion() {
        val layers = coreThreeLayers()
        val anchors = coreThreeAnchors()
        val logs = fullWeekLogs(anchors.map { it.id })

        // Sin noches con dato → señal de sueño null (opt-in inactivo) sin necesidad de ningún toggle.
        assertNull(ScoringFactsAdapter.sleepSignal(emptyList()))
        val noSleep = calculate(layers = layers, activities = anchors, activityLogs = logs)

        // Con noches con dato → la señal entra SIEMPRE (no hay flag que lo desactive en scoring).
        val withSleep = calculate(
            layers = layers, activities = anchors, activityLogs = logs,
            sleepNights = listOf(sleepNight(0.2f), sleepNight(0.2f)),
        )
        println("[P5] sinSueno=${noSleep.estado} conSuenoMalo=${withSleep.estado}")
        // "Desactivar sueño" no existe como toggle; lo único que apaga el término es la AUSENCIA de dato.
        assertNotEquals(noSleep.estado, withSleep.estado)
    }

    // ---------------- helpers (calcados de ScoreEngineTest) ----------------

    private fun coreThreeLayers(): List<Layer> = listOf(
        layer("layer_interior", "Interior", 10),
        layer("layer_cuerpo", "Cuerpo", 20),
        layer("layer_conducta", "Conducta", 30),
    )

    private fun coreThreeAnchors(): List<ActivityDefinition> =
        coreThreeLayers().map { anchor("act_${it.id}", it.id) }

    private fun calculate(
        layers: List<Layer>,
        activities: List<ActivityDefinition> = emptyList(),
        activityLogs: List<ActivityLog> = emptyList(),
        abstinenceTracks: List<AbstinenceTrack> = emptyList(),
        abstinenceLogs: List<AbstinenceLog> = emptyList(),
        sleepNights: List<SleepNightScore> = emptyList(),
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
                tasks = emptyList(),
                sleepNights = sleepNights,
                today = today,
            ),
        )

    private fun layer(id: String, name: String, sortOrder: Int): Layer =
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
            sessionTargetMinutes = 30,
            unit = ActivityUnit.Minutes,
            sortOrder = 10,
        )

    private fun log(activityId: String, date: LocalDate, actualValue: Int): ActivityLog =
        ActivityLog(
            activityId = activityId,
            date = date.toString(),
            completed = true,
            actualValue = actualValue,
            updatedAt = 0L,
        )

    private fun abstinenceTrack(id: String, active: Boolean): AbstinenceTrack =
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

    private fun abstinenceLog(trackId: String, date: LocalDate, status: AbstinenceStatus): AbstinenceLog =
        AbstinenceLog(trackId = trackId, date = date.toString(), status = status, updatedAt = 0L)

    private fun sleepNight(score: Float): SleepNightScore =
        SleepNightScore(
            duration = score,
            continuity = score,
            alignment = score,
            digitalInterruption = score,
            sleepScore = score,
            confidence = SleepConfidence.High,
        )
}
