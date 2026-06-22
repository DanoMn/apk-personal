package dev.panopt.autonomia.data.scoring

import dev.panopt.autonomia.data.AbstinenceLogEntity
import dev.panopt.autonomia.data.AbstinenceTrackEntity
import dev.panopt.autonomia.data.ActivityDefinitionEntity
import dev.panopt.autonomia.data.ActivityTargetVersionEntity
import dev.panopt.autonomia.data.DailyActivityLogEntity
import dev.panopt.autonomia.data.LayerEntity
import dev.panopt.autonomia.data.SleepNightEntity
import dev.panopt.autonomia.data.TaskEntity
import dev.panopt.autonomia.data.UserActivityConfigEntity
import dev.panopt.autonomia.data.WeeklyScoreSnapshotEntity
import dev.panopt.autonomia.domain.scoring.WeeklyScoreSnapshotConstants
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Cobertura del REGISTRO semanal: el [WeeklyScoreSnapshotWriter] junta los hechos de la
 * semana, calcula los límites (lunes → hoy), corre el motor y persiste el snapshot; y
 * rellena snapshots de semanas vencidas (back-fill). La matemática del score está cubierta
 * aparte (use case / engine); acá probamos la orquestación que antes no tenía tests.
 */
class WeeklyScoreSnapshotWriterTest {

    private val defaultLayers = listOf(
        LayerEntity("layer_interior", "Interior", "", 10),
        LayerEntity("layer_cuerpo", "Cuerpo", "", 20),
        LayerEntity("layer_conducta", "Conducta", "", 30),
        LayerEntity("layer_vinculos", "Vinculos", "", 40),
        LayerEntity("layer_proyecto", "Proyecto", "", 50),
    )

    // ─────────────────────────── refreshCurrentWeek ───────────────────────────

    @Test
    fun `refreshCurrentWeek snapshots the monday-to-today week and persists it`() = runTest {
        val fake = FakeWeeklySnapshotDataSource(layers = defaultLayers)
        val writer = WeeklyScoreSnapshotWriter(fake)
        val today = LocalDate.of(2026, 5, 27) // miércoles

        writer.refreshCurrentWeek(today)

        // Los hechos se piden para la ventana MÓVIL de 7 días terminada hoy: 05-21 → 05-27
        // (cruza el lunes anterior), no solo lunes-en-curso → hoy.
        assertEquals("2026-05-21" to "2026-05-27", fake.activityLogsRange)
        assertEquals("2026-05-21" to "2026-05-27", fake.sleepRange)

        // Pero el snapshot se persiste con la CLAVE de semana calendario (lunes en curso → hoy).
        val snapshot = fake.upserted
        assertNotNull("El writer debe persistir un snapshot", snapshot)
        assertEquals("2026-05-25", snapshot!!.weekStart)
        assertEquals("2026-05-27", snapshot.weekEnd)
        assertEquals(WeeklyScoreSnapshotConstants.SCORING_VERSION, snapshot.scoringVersion)
    }

    @Test
    fun `week-start anchors to monday even when today is sunday`() = runTest {
        val fake = FakeWeeklySnapshotDataSource(layers = defaultLayers)
        val writer = WeeklyScoreSnapshotWriter(fake)
        val sunday = LocalDate.of(2026, 5, 31) // domingo

        writer.refreshCurrentWeek(sunday)

        assertEquals("2026-05-25" to "2026-05-31", fake.activityLogsRange)
        assertEquals("2026-05-25", fake.upserted!!.weekStart)
        assertEquals("2026-05-31", fake.upserted!!.weekEnd)
    }

    // ─────────────────────────── closeElapsedWeeks ───────────────────────────

    @Test
    fun `closeElapsedWeeks back-fills a missing past week that has facts`() = runTest {
        val today = LocalDate.of(2026, 5, 27) // semana en curso arranca 2026-05-25
        // Hecho real en la semana de 2026-05-11..05-17 (dos semanas atrás).
        val fake = FakeWeeklySnapshotDataSource(
            layers = defaultLayers,
            abstinenceLogs = listOf(abstinenceLog("2026-05-13")),
        )
        val writer = WeeklyScoreSnapshotWriter(fake)

        writer.closeElapsedWeeks(today)

        assertTrue(
            "Debe rellenar la semana vencida con hechos",
            "2026-05-11" in fake.upsertedWeekStarts,
        )
        // No fabrica semanas vacías (la semana 2026-05-18 no tiene hechos).
        assertTrue("2026-05-18" !in fake.upsertedWeekStarts)
        // No toca la semana en curso (de eso se encarga refreshCurrentWeek).
        assertTrue("2026-05-25" !in fake.upsertedWeekStarts)
        assertEquals("2026-05-11", fake.upserted!!.weekStart)
        assertEquals("2026-05-17", fake.upserted!!.weekEnd)
    }

    @Test
    fun `closeElapsedWeeks skips weeks that already have a snapshot`() = runTest {
        val today = LocalDate.of(2026, 5, 27)
        val fake = FakeWeeklySnapshotDataSource(
            layers = defaultLayers,
            abstinenceLogs = listOf(abstinenceLog("2026-05-13")),
            existingSnapshots = listOf(existingSnapshot(weekStart = "2026-05-11", weekEnd = "2026-05-17")),
        )
        val writer = WeeklyScoreSnapshotWriter(fake)

        writer.closeElapsedWeeks(today)

        assertTrue("No re-materializa una semana ya snapshoteada", fake.upsertedWeekStarts.isEmpty())
    }

    @Test
    fun `closeElapsedWeeks processes weeks oldest-first`() = runTest {
        val today = LocalDate.of(2026, 5, 27)
        val fake = FakeWeeklySnapshotDataSource(
            layers = defaultLayers,
            abstinenceLogs = listOf(
                abstinenceLog("2026-05-13"), // semana 2026-05-11
                abstinenceLog("2026-05-20"), // semana 2026-05-18
            ),
        )
        val writer = WeeklyScoreSnapshotWriter(fake)

        writer.closeElapsedWeeks(today)

        assertEquals(listOf("2026-05-11", "2026-05-18"), fake.upsertedWeekStarts)
    }

    @Test
    fun `closeElapsedWeeks respects the lookback bound`() = runTest {
        val today = LocalDate.of(2026, 5, 27) // currentWeekStart 2026-05-25
        // 7 semanas atrás (2026-04-06), fuera del tope de 6 → no se rellena.
        val fake = FakeWeeklySnapshotDataSource(
            layers = defaultLayers,
            abstinenceLogs = listOf(abstinenceLog("2026-04-08")),
        )
        val writer = WeeklyScoreSnapshotWriter(fake)

        writer.closeElapsedWeeks(today)

        assertTrue("Semana fuera de la ventana no se rellena", fake.upsertedWeekStarts.isEmpty())
    }

    private fun abstinenceLog(date: String): AbstinenceLogEntity =
        AbstinenceLogEntity(trackId = "trk_alcohol", date = date, status = "Clean", updatedAt = 0L)

    private fun existingSnapshot(weekStart: String, weekEnd: String): WeeklyScoreSnapshotEntity =
        WeeklyScoreSnapshotEntity(
            weekStart = weekStart,
            weekEnd = weekEnd,
            scoringVersion = WeeklyScoreSnapshotConstants.SCORING_VERSION,
            calculatedAt = 0L,
            configHash = "",
            factsHash = "",
            weeklyBaseScore = 0.5f,
            weeklyScore = 0.5f,
            stabilityScore = null,
            state = "Motion",
            visibleScore = 500,
            worstLayerId = null,
            layerSummariesJson = "[]",
            reasonsJson = "[]",
        )
}

/**
 * Fake en memoria del [WeeklySnapshotDataSource]. Filtra logs por rango, acumula los
 * snapshots upserteados (los siguientes back-fills los ven como memoria previa) y registra
 * todos los upserts para las aserciones.
 */
internal class FakeWeeklySnapshotDataSource(
    private val layers: List<LayerEntity> = emptyList(),
    private val activityLogs: List<DailyActivityLogEntity> = emptyList(),
    private val abstinenceLogs: List<AbstinenceLogEntity> = emptyList(),
    existingSnapshots: List<WeeklyScoreSnapshotEntity> = emptyList(),
) : WeeklySnapshotDataSource {
    private val snapshots = existingSnapshots.toMutableList()
    private val upserts = mutableListOf<WeeklyScoreSnapshotEntity>()

    var activityLogsRange: Pair<String, String>? = null
        private set
    var sleepRange: Pair<String, String>? = null
        private set

    val upserted: WeeklyScoreSnapshotEntity? get() = upserts.lastOrNull()
    val upsertedWeekStarts: List<String> get() = upserts.map { it.weekStart }

    override suspend fun getLayersSnapshot(): List<LayerEntity> = layers

    override suspend fun getActivityLogsBetween(startDate: String, endDate: String): List<DailyActivityLogEntity> {
        activityLogsRange = startDate to endDate
        return activityLogs.filter { it.date in startDate..endDate }
    }

    override suspend fun getSleepNightsInRange(from: String, to: String): List<SleepNightEntity> {
        sleepRange = from to to
        return emptyList()
    }

    override suspend fun getAllAbstinenceLogsSnapshot(): List<AbstinenceLogEntity> = abstinenceLogs

    override suspend fun getWeeklyScoreSnapshotsSnapshot(): List<WeeklyScoreSnapshotEntity> = snapshots.toList()

    override suspend fun upsertWeeklyScoreSnapshot(snapshot: WeeklyScoreSnapshotEntity) {
        snapshots.removeAll { it.weekStart == snapshot.weekStart && it.scoringVersion == snapshot.scoringVersion }
        snapshots.add(snapshot)
        upserts.add(snapshot)
    }

    override suspend fun getAbstinenceTracksSnapshot(): List<AbstinenceTrackEntity> = emptyList()
    override suspend fun getTasksSnapshot(): List<TaskEntity> = emptyList()
    override suspend fun getActiveUserActivityConfigs(): List<UserActivityConfigEntity> = emptyList()
    override suspend fun getActivityDefinitionsSnapshot(): List<ActivityDefinitionEntity> = emptyList()
    override suspend fun getActivityTargetVersionsSnapshot(): List<ActivityTargetVersionEntity> = emptyList()
}
