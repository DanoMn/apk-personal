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

/**
 * PR-F — tests END-TO-END del motor nuevo recableado (`ScoreEngine.calculate` →
 * `ScoringFactsAdapter` → niveles 1–6 → `ScoreReport.estado`). Reproducen los `chk(...)`
 * INTEGRADOS de `docs/scoring/verificacion_modelo_oficial.py` (AG-just, opt-in de sobriedad)
 * a través del orquestador completo, no de las policies aisladas (esas viven en sus propios
 * tests de nivel: AnchorScoringPolicyTest, StateAggregationPolicyTest, BandPolicyTest, …).
 *
 * El motor viejo (worst-layer, histéresis, 0.70/0.30, sueño-30%, §16.7 cap) quedó ELIMINADO
 * en PR-F; estos tests validan el contrato nuevo: ESTADO ∈ [0,1.5] + banda = banda(ESTADO).
 */
class ScoreEngineTest {
    // Lunes a domingo, semana completa, para que la ventana de 7 días contenga todos los logs.
    private val today = LocalDate.of(2026, 5, 24) // domingo
    private val weekDates = (0L..6L).map { LocalDate.of(2026, 5, 18).plusDays(it) }

    @Test
    fun noDataReturnsNoDataWithoutVisibleScore() {
        val report = calculate(
            layers = listOf(layer("layer_interior", "Interior")),
            activities = listOf(anchor("act_meditation", "layer_interior")),
        )

        assertEquals(ScoreState.NoData, report.state)
        assertNull(report.visibleScore)
        assertEquals(0f, report.estado, 1e-6f)
    }

    @Test
    fun fewerThanThreeActiveLayersWithAnchorReturnsNoData() {
        // §7.4: se exigen ≥3 capas activas con ancla. Con solo 2 válidas → NoData,
        // aunque los hechos estén completos.
        val layers = listOf(layer("layer_interior", "Interior", 10), layer("layer_cuerpo", "Cuerpo", 20))
        val activities = listOf(anchor("act_a", "layer_interior"), anchor("act_b", "layer_cuerpo"))
        val logs = activities.flatMap { a -> justDays.map { log(a.id, it, actualValue = 30) } }
        val report = calculate(layers = layers, activities = activities, activityLogs = logs)

        assertEquals(ScoreState.NoData, report.state)
        assertNull(report.visibleScore)
    }

    @Test
    fun exactlyThreeActiveLayersWithAnchorOpensTheGateAndEmitsScoring() {
        // §7.4: con 3 capas activas con ancla el gate se abre y se emite scoring real.
        val report = calculate(
            layers = coreThreeLayers(),
            activities = coreThreeAnchors(),
            activityLogs = justLogs(coreThreeAnchors()),
        )

        assertNotEquals(ScoreState.NoData, report.state)
        assertNotNull(report.visibleScore)
    }

    @Test
    fun complyingJustAcrossThreeLayersGivesEstadoOneAndPlenitude() {
        // AG-just (Python): 3 capas, cada una con UN ancla J = R(4,30,[30]*4) = 1.0 → ESTADO = 1.0.
        // Banda(1.0) = Plenitud (no Inquebrantable: 1.0 < 1.10).
        val report = calculate(
            layers = coreThreeLayers(),
            activities = coreThreeAnchors(),
            activityLogs = justLogs(coreThreeAnchors()),
        )

        assertEquals(1.0f, report.estado, 1e-4f)
        assertEquals(ScoreState.Plenitude, report.state)
    }

    @Test
    fun scoreReportExposesRawEstadoAndBandFromIt() {
        // El campo `estado` es el ESTADO crudo del NIVEL 5; `state` = banda(estado).
        // weeklyBaseScore/weeklyScore reflejan el ESTADO (no el modelo viejo 0–1).
        val report = calculate(
            layers = coreThreeLayers(),
            activities = coreThreeAnchors(),
            activityLogs = justLogs(coreThreeAnchors()),
        )

        assertEquals(1.0f, report.estado, 1e-4f)
        assertEquals(report.estado, report.weeklyBaseScore, 1e-6f)
        assertEquals(report.estado, report.weeklyScore, 1e-6f)
        assertEquals(ScoreState.Plenitude, report.state)
    }

    @Test
    fun bandDoesNotDependOnHistory() {
        // La banda es función PURA del ESTADO: misma config, distinta historia → misma banda.
        val withHistory = calculate(
            layers = coreThreeLayers(),
            activities = coreThreeAnchors(),
            activityLogs = justLogs(coreThreeAnchors()),
            weeklyHistory = highHistory(),
        )
        val withoutHistory = calculate(
            layers = coreThreeLayers(),
            activities = coreThreeAnchors(),
            activityLogs = justLogs(coreThreeAnchors()),
        )

        assertEquals(withoutHistory.state, withHistory.state)
        assertEquals(withoutHistory.estado, withHistory.estado, 1e-9f)
        assertEquals(ScoreState.Plenitude, withHistory.state)
    }

    @Test
    fun sobrietyRelapseDragsEstadoDownAsShadowTerm() {
        // El opt-in de sobriedad cablea a la capa Conducta (sobriedad M → layer_conducta).
        // Con anclas perfectas en 3 capas (ESTADO base 1.0) y una recaída en el track activo,
        // el término-sombra arrastra la base hacia abajo: ESTADO con recaída < ESTADO limpio.
        val track = abstinenceTrack()
        val cleanReport = calculate(
            layers = coreThreeLayers(),
            activities = coreThreeAnchors(),
            activityLogs = justLogs(coreThreeAnchors()),
            abstinenceTracks = listOf(track),
            abstinenceLogs = weekDates.map { abstinenceLog(track.id, it, AbstinenceStatus.Clean) },
        )
        val relapseReport = calculate(
            layers = coreThreeLayers(),
            activities = coreThreeAnchors(),
            activityLogs = justLogs(coreThreeAnchors()),
            abstinenceTracks = listOf(track),
            abstinenceLogs = weekDates.dropLast(1).map { abstinenceLog(track.id, it, AbstinenceStatus.Clean) } +
                abstinenceLog(track.id, today, AbstinenceStatus.Relapse),
        )

        // Track limpio: M_sobr = 1 → término-sombra nulo → ESTADO no cae por sobriedad.
        assertEquals(1.0f, cleanReport.estado, 1e-4f)
        // Con recaída: M_sobr < 1 → término-sombra > 0 → ESTADO baja.
        assertTrue(
            "Relapse must drag estado below clean (${relapseReport.estado} < ${cleanReport.estado})",
            relapseReport.estado < cleanReport.estado,
        )
    }

    @Test
    fun cleanSleepOptInDoesNotChangeEstado() {
        // Sueño M = 1.0 (perfecto) cablea a Cuerpo pero su término-sombra es nulo (neutralidad):
        // ESTADO con sueño perfecto = ESTADO sin dato de sueño (ausencia no penaliza tampoco).
        val noSleep = calculate(
            layers = coreThreeLayers(),
            activities = coreThreeAnchors(),
            activityLogs = justLogs(coreThreeAnchors()),
        )
        val perfectSleep = calculate(
            layers = coreThreeLayers(),
            activities = coreThreeAnchors(),
            activityLogs = justLogs(coreThreeAnchors()),
            sleepNights = weekDates.map { sleepNight(score = 1.0f) },
        )

        assertEquals(noSleep.estado, perfectSleep.estado, 1e-9f)
    }

    @Test
    fun visibleScoreStaysPopulatedForTheDashboardAndSeam() {
        // PR-F mantiene `visibleScore` poblado (mapeo interino lineal) para que el dashboard no
        // muestre "--" y el writer persista un número. PR-G lo reemplaza por el mapeo sigmoide.
        val report = calculate(
            layers = coreThreeLayers(),
            activities = coreThreeAnchors(),
            activityLogs = justLogs(coreThreeAnchors()),
        )

        assertNotNull(report.visibleScore)
        assertTrue(report.visibleScore!! in 650..1100)
    }

    @Test
    fun layerScoresExposePerLayerBaseEffNotZero() {
        // Tres capas con cumplimiento DISTINTO: justa (R=1.0), déficit (3/4 días), justa.
        // El detalle por-capa debe traer el base_eff REAL de cada capa (no 0f placeholder),
        // y el ESTADO global NO debe cambiar respecto del mismo cálculo agregado puro.
        val layers = coreThreeLayers()
        val anchors = coreThreeAnchors()
        val deficitLayerId = "layer_cuerpo"
        // La capa Cuerpo cumple solo 3 de 4 días (déficit); las otras dos cumplen justo (4 días).
        val activityLogs = anchors.flatMap { a ->
            val days = if (a.layerId == deficitLayerId) justDays.take(3) else justDays
            days.map { log(a.id, it, actualValue = 30) }
        }

        val report = calculate(layers = layers, activities = anchors, activityLogs = activityLogs)

        // Cada capa activa tiene su LayerScore con score = base_eff real.
        assertEquals(layers.size, report.layerScores.size)
        val byId = report.layerScores.associateBy { it.layerId }

        // Capas justas: base_eff = 1.0; capa con déficit: base_eff < 1.0 pero > 0 (no placeholder).
        val justBaseEff = AnchorScoringPolicy.r(4, 30, List(4) { 30 })
        val deficitBaseEff = AnchorScoringPolicy.r(4, 30, List(3) { 30 })
        assertEquals(justBaseEff.toFloat(), byId["layer_interior"]!!.score, 1e-6f)
        assertEquals(justBaseEff.toFloat(), byId["layer_conducta"]!!.score, 1e-6f)
        assertEquals(deficitBaseEff.toFloat(), byId["layer_cuerpo"]!!.score, 1e-6f)
        assertTrue(
            "déficit base_eff debe ser >0 y <1 (${byId["layer_cuerpo"]!!.score})",
            byId["layer_cuerpo"]!!.score > 0f && byId["layer_cuerpo"]!!.score < 1f,
        )
        // baseScore/rawScore reflejan el mismo base_eff que score.
        assertEquals(byId["layer_cuerpo"]!!.score, byId["layer_cuerpo"]!!.baseScore, 1e-9f)
        assertEquals(byId["layer_cuerpo"]!!.score, byId["layer_cuerpo"]!!.rawScore, 1e-9f)

        // El ESTADO global coincide EXACTAMENTE con la agregación pura (presentación no lo movió).
        val expectedEstado = StateAggregationPolicy.estado(
            listOf(
                StateAggregationPolicy.LayerInput(anchors = listOf(justBaseEff)),
                StateAggregationPolicy.LayerInput(anchors = listOf(deficitBaseEff)),
                StateAggregationPolicy.LayerInput(anchors = listOf(justBaseEff)),
            ),
        )
        assertEquals(expectedEstado.toFloat(), report.estado, 1e-9f)
    }

    @Test
    fun layerScoresCarrySurplusInAnchorSurplusBonus() {
        // Una capa con superhabit grande debe exponer extra (>0) en anchorSurplusBonus; las justas, 0.
        val layers = coreThreeLayers()
        val anchors = coreThreeAnchors()
        val surplusLayerId = "layer_interior"
        // Interior con superhabit (7 días, 60 min ⇒ R>1); las otras dos cumplen justo.
        val activityLogs = anchors.flatMap { a ->
            if (a.layerId == surplusLayerId) {
                weekDates.map { log(a.id, it, actualValue = 60) }
            } else {
                justDays.map { log(a.id, it, actualValue = 30) }
            }
        }

        val report = calculate(layers = layers, activities = anchors, activityLogs = activityLogs)
        val byId = report.layerScores.associateBy { it.layerId }

        assertTrue(
            "capa con superhabit debe tener anchorSurplusBonus>0 (${byId[surplusLayerId]!!.anchorSurplusBonus})",
            byId[surplusLayerId]!!.anchorSurplusBonus > 0f,
        )
        assertEquals(0f, byId["layer_cuerpo"]!!.anchorSurplusBonus, 1e-9f)
        assertEquals(0f, byId["layer_conducta"]!!.anchorSurplusBonus, 1e-9f)
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    // Días de "cumplir-justo": 4 días con 30 min (F=4, T=30 → R = 1.0).
    private val justDays = weekDates.take(4)

    private fun justLogs(activities: List<ActivityDefinition>): List<ActivityLog> =
        activities.flatMap { a -> justDays.map { log(a.id, it, actualValue = 30) } }

    private fun coreThreeLayers(): List<Layer> =
        listOf(
            layer("layer_interior", "Interior", 10),
            layer("layer_cuerpo", "Cuerpo", 20),
            layer("layer_conducta", "Conducta", 30),
        )

    private fun coreThreeAnchors(): List<ActivityDefinition> =
        coreThreeLayers().map { anchor("act_${it.id}", it.id) }

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

    private fun sleepNight(score: Float = 1.0f): SleepNightScore =
        SleepNightScore(
            duration = score,
            continuity = score,
            alignment = score,
            digitalInterruption = score,
            sleepScore = score,
            confidence = SleepConfidence.High,
        )

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
