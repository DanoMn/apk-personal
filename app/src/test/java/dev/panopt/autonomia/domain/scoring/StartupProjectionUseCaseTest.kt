package dev.panopt.autonomia.domain.scoring

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
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.activity.ActivityTargetVersion
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lote 2 (tasks 2.4, 2.6, 2.7) — `StartupProjectionUseCase` + convergencia día 7→8 + invariante
 * de persistencia. Dominio puro JVM.
 *
 * La proyección corre el motor con `windowDays = d` y SIN filtrar las anclas en gracia, para
 * obtener el score real proyectado de la cuenta nueva. No persiste nada ni muta el camino maduro
 * (cuyo `ScoreReport` real sigue `NoData`).
 */
class StartupProjectionUseCaseTest {
    // Hoy fijo; las anclas se crean "hace d días" → en gracia.
    private val today = LocalDate.of(2026, 5, 24)

    @Test
    fun threeGraceAnchorsDayFourProjectPositiveEstadoWhileRealReportStaysNoData() {
        // 3 anclas creadas hace 4 días (en gracia), con hechos en los 4 días vividos.
        val daysLived = 4
        val source = sourceWithGraceAnchors(daysLived)

        // Proyección: corre el motor con windowDays=4 y sin filtrar gracia → estado > 0.
        val projection = StartupProjectionUseCase(source, windowDays = daysLived)
        assertTrue("la proyección debe existir (gate alcanzado con gracia)", projection != null)
        assertTrue(
            "las anclas en gracia SÍ aportan a la proyección (estado>0): ${projection!!.estado}",
            projection.estado > 0.0,
        )

        // El ScoreReport REAL (camino maduro, default filtra gracia) sigue NoData sobre los mismos hechos.
        val realInput = BuildScoreInputUseCase(source) // includeGraceAnchors=false por default
        val realReport = ScoreEngine.calculate(realInput)
        assertEquals(ScoreState.NoData, realReport.state)
        assertNull(realReport.visibleScore)
    }

    @Test
    fun projectionDoesNotMutateMatureReport() {
        // Correr la proyección no cambia el ScoreReport real (dos llamadas idénticas antes/después).
        val source = sourceWithGraceAnchors(daysLived = 4)

        val before = ScoreEngine.calculate(BuildScoreInputUseCase(source))
        StartupProjectionUseCase(source, windowDays = 4)
        val after = ScoreEngine.calculate(BuildScoreInputUseCase(source))

        assertEquals(before.state, after.state)
        assertEquals(before.estado, after.estado, 1e-9f)
    }

    @Test
    fun belowGateEvenWithGraceReturnsNull() {
        // Solo 2 capas con ancla: ni incluyendo la gracia se alcanza el gate (3) → null.
        val source = sourceWithGraceAnchors(daysLived = 4, layerCount = 2)

        val projection = StartupProjectionUseCase(source, windowDays = 4)
        assertNull("sin cobertura mínima la proyección no aplica", projection)
    }

    @Test
    fun day7CounterEqualsDay8MatureScoreSameFacts() {
        // NO-SALTO día 7→8 (convergencia). Mismos 7 días de hechos.
        // Día 7: cuenta en arranque, proyección windowDays=7, contador × 7/7.
        // Día 8: anclas fuera de gracia, motor maduro normal windowDays=7.
        // Con versiones cubriendo los 7 días con la misma meta, ambos cálculos usan la MISMA ventana
        // de hechos → el contador del día 7 == score maduro del día 8 (tolerancia 0 puntos).
        val facts = sevenDayFacts()

        // Día 7: anclas creadas hace 7 días (último día de gracia: isWithinGrace true para d<7→ d=7 sale,
        // pero la proyección NO filtra gracia, así que entran igual). windowDays=7, × 7/7.
        val day7 = facts.today
        val sourceDay7 = facts.sourceAt(day7, anchorAgeDays = 6) // creadas hace 6 días → en gracia el día 7
        val projection = StartupProjectionUseCase(sourceDay7, windowDays = 7)
        assertTrue("proyección día 7 existe", projection != null)
        val counter = StartupCounterPolicy.counter(projection!!.estado, daysLived = 7)

        // Día 8: las anclas cumplieron 7 días → salen de gracia → motor maduro las puntúa.
        val day8 = day7.plusDays(1)
        val sourceDay8 = facts.sourceAt(day8, anchorAgeDays = 7) // creadas hace 7 días → fuera de gracia
        val matureReport = ScoreEngine.calculate(BuildScoreInputUseCase(sourceDay8))

        // El maduro día 8 NO debe ser NoData (las anclas ya cuentan) y debe traer puntos.
        assertTrue("día 8 maduro NO es NoData", matureReport.state != ScoreState.NoData)
        assertTrue("día 8 maduro emite puntos", matureReport.visibleScore != null)

        // Convergencia exacta sobre los MISMOS hechos: contador(día7, ×7/7) == score maduro(día8).
        assertEquals(matureReport.visibleScore, counter.counterPoints)
    }

    @Test
    fun newAccountWithoutVersionsRespectsWindowDaysAndIsNotPunished() {
        // FIX B: cuenta nueva SIN target-versions (ramo legacy `r(f,t,mins)`, dayRatios == null),
        // día 2 con sus 2 días cumplidos. La proyección con windowDays=2 NO debe castigar los días
        // todavía no vividos (3..7). Comparada con la ventana injusta de 7 (el bug), la ventana justa
        // (2) debe dar un estado >= : el superhábit por días no reparte sobre días que no llegaron.
        val daysLived = 2
        val source = sourceWithGraceAnchorsNoVersions(daysLived)

        val fair = StartupProjectionUseCase(source, windowDays = daysLived)
        assertTrue("la proyección justa existe", fair != null)

        // Reproducción del bug: si windowDays NO se propaga por el ramo legacy, la proyección usaría
        // ventana 7 igual. Forzamos esa comparación corriendo el motor con windowDays=7 sobre los
        // mismos hechos: la ventana justa (2) no debe ser PEOR que la de 7.
        val input = BuildScoreInputUseCase(source, includeGraceAnchors = true)
        val window7 = ScoreEngine.calculateProjection(input, windowDays = 7)

        assertTrue(
            "ventana justa (d=2) no debe castigar más que la de 7: justa=${fair!!.estado} v7=${window7.estado}",
            fair.estado >= window7.estado.toDouble() - 1e-9,
        )
        assertTrue("la cuenta nueva proyecta estado positivo", fair.estado > 0.0)
    }

    @Test
    fun matureWindowDaysSevenIsByteIdenticalForLegacyBranch() {
        // Cero regresión: el cálculo real (windowDays=7 default) sobre el ramo legacy NO cambia con
        // el FIX B. Mismos hechos, motor maduro: el estado debe ser idéntico antes/después.
        // (Validado indirectamente por la suite de ScoreEngineTest, explícito aquí por el FIX B.)
        val source = sourceWithGraceAnchorsNoVersions(daysLived = 7)
        val inputDefault = BuildScoreInputUseCase(source, includeGraceAnchors = true)
        val explicit7 = ScoreEngine.calculateProjection(inputDefault, windowDays = 7)
        val matureClosed = ScoreEngine.calculate(inputDefault)
        // calculate usa windowDays=GRACE_DAYS=7 → debe coincidir con calculateProjection(.,7).
        assertEquals(matureClosed.estado, explicit7.estado, 1e-9f)
    }

    @Test
    fun matureReportRemainsNoDataInStartupSoNoSnapshotLeak() {
        // Invariante de persistencia (2.7): en arranque el ScoreReport real es NoData → visibleScore
        // null/0 (lo que el writer persistiría). Ningún componente de arranque expone escritura.
        val source = sourceWithGraceAnchors(daysLived = 3)

        val realReport = ScoreEngine.calculate(BuildScoreInputUseCase(source))
        assertEquals(ScoreState.NoData, realReport.state)
        assertNull(realReport.visibleScore)
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    /** Millis epoch del inicio del día `daysAgo` antes de [today]. */
    private fun createdAtDaysAgo(daysAgo: Int, ref: LocalDate = today): Long =
        ref.minusDays(daysAgo.toLong())
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /**
     * Source con [layerCount] anclas (1 por capa), creadas hace [daysLived] días (en gracia), cada
     * una con un hecho de 30 min por cada día vivido. Versiones para que `dayRatios != null` y la
     * ventana parcial (`windowDays`) se propague.
     */
    private fun sourceWithGraceAnchors(daysLived: Int, layerCount: Int = 3): ScoreInputSource {
        val layers = (0 until layerCount).map { layer("layer_$it", "L$it", it * 10) }
        val anchors = layers.map { anchor("act_${it.id}", it.id, createdAtDaysAgo(daysLived)) }
        val livedDays = (0 until daysLived).map { today.minusDays(it.toLong()) }
        val logs = anchors.flatMap { a -> livedDays.map { log(a.id, it, 30) } }
        val firstLived = livedDays.minOrNull()!!
        val versions = anchors.associate { a ->
            a.id to listOf(
                ActivityTargetVersion(a.id, firstLived, targetMinutes = 30, targetDays = 4, createdAt = 1L),
            )
        }
        return source(layers, anchors, logs, today, versions)
    }

    /**
     * Igual que [sourceWithGraceAnchors] pero SIN target-versions → `dayRatios == null` → el motor
     * resuelve cada ancla por el ramo legacy `r(f, t, mins)`. Modela la cuenta nueva típica que aún
     * no generó versiones de meta (FIX B: ese ramo debe respetar `windowDays`).
     */
    private fun sourceWithGraceAnchorsNoVersions(daysLived: Int, layerCount: Int = 3): ScoreInputSource {
        val layers = (0 until layerCount).map { layer("layer_$it", "L$it", it * 10) }
        val anchors = layers.map { anchor("act_${it.id}", it.id, createdAtDaysAgo(daysLived)) }
        val livedDays = (0 until daysLived).map { today.minusDays(it.toLong()) }
        val logs = anchors.flatMap { a -> livedDays.map { log(a.id, it, 30) } }
        return source(layers, anchors, logs, today, emptyMap())
    }

    private inner class SevenDayFacts(val today: LocalDate) {
        // 7 días de hechos de 30 min, 3 anclas en 3 capas, meta 30 min / 4 días.
        private val layers = (0 until 3).map { layer("layer_$it", "L$it", it * 10) }

        fun sourceAt(ref: LocalDate, anchorAgeDays: Int): ScoreInputSource {
            val anchors = layers.map { anchor("act_${it.id}", it.id, createdAtDaysAgo(anchorAgeDays, ref)) }
            val sevenDays = (0 until 7).map { ref.minusDays(it.toLong()) }
            val logs = anchors.flatMap { a -> sevenDays.map { log(a.id, it, 30) } }
            val firstDay = sevenDays.minOrNull()!!
            val versions = anchors.associate { a ->
                a.id to listOf(
                    ActivityTargetVersion(a.id, firstDay, targetMinutes = 30, targetDays = 4, createdAt = 1L),
                )
            }
            return source(layers, anchors, logs, ref, versions)
        }
    }

    private fun sevenDayFacts() = SevenDayFacts(today)

    private fun source(
        layers: List<Layer>,
        activities: List<ActivityDefinition>,
        logs: List<ActivityLog>,
        ref: LocalDate,
        versions: Map<String, List<ActivityTargetVersion>>,
    ): ScoreInputSource =
        ScoreInputSource(
            layers = layers,
            activities = activities,
            todayActivityLogs = logs.filter { it.date == ref.toString() },
            periodActivityLogs = logs,
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            tasks = emptyList(),
            today = ref,
            weeklyHistory = emptyList(),
            targetVersions = versions,
        )

    private fun layer(id: String, name: String, sortOrder: Int): Layer =
        Layer(id = id, name = name, description = "", sortOrder = sortOrder)

    private fun anchor(id: String, layerId: String, createdAt: Long): ActivityDefinition =
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
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = 4,
            sessionTargetMinutes = 30,
            unit = ActivityUnit.Minutes,
            sortOrder = 10,
            createdAt = createdAt,
        )

    private fun log(activityId: String, date: LocalDate, actualValue: Int): ActivityLog =
        ActivityLog(
            activityId = activityId,
            date = date.toString(),
            completed = true,
            actualValue = actualValue,
            updatedAt = 0L,
        )
}
