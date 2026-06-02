package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ScoreState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Caracteriza StabilityScore (árbol §15, fórmula validada el 2026-06-02):
 *   - requiere 5 semanas previas versionadas (REQUIRED_PREVIOUS_WEEKS = 5);
 *   - window = (5 previas más recientes).weeklyBaseScore + WeeklyBaseScore actual;
 *   - StabilityScore = 0.750 * average(window) + 0.250 * worst(window).
 *
 * Hojas de respuesta calculadas a mano DESDE EL CONTRATO §15.2, no desde el código.
 * El verde certifica que código y contrato coinciden tras validar la fórmula.
 */
class StabilityScoringPolicyTest {
    private val currentWeek = "2026-05-18"
    // 5 lunes previos consecutivos (orden ascendente; el policy los ordena desc).
    private val fivePreviousWeeks = listOf(
        "2026-04-13", "2026-04-20", "2026-04-27", "2026-05-04", "2026-05-11",
    )

    @Test
    fun fewerThanFivePreviousWeeksHasNoTemporalMemory() {
        // Solo 4 semanas previas válidas → sin memoria temporal: NO se puede
        // evaluar estabilidad (bloquea Inquebrantable §16.4).
        val history = fivePreviousWeeks.take(4).map { entry(it, 0.90f) }

        val result = StabilityScoringPolicy.evaluate(currentWeek, 0.90f, history)

        assertNull(result.stabilityScore)
        assertFalse(result.hasTemporalMemory)
        assertEquals(5, result.evaluatedWeeks) // 4 previas + la actual
    }

    @Test
    fun fiveSteadyWeeksScoreFullStability() {
        // 5 previas a 0.90 + actual 0.90 → window de 6 valores todos 0.90.
        // average = 0.90 ; worst = 0.90
        // StabilityScore = 0.750*0.90 + 0.250*0.90 = 0.90
        val history = fivePreviousWeeks.map { entry(it, 0.90f) }

        val result = StabilityScoringPolicy.evaluate(currentWeek, 0.90f, history)

        assertEquals(0.90f, result.stabilityScore!!, 0.001f)
        assertTrue(result.hasTemporalMemory)
        assertEquals(6, result.evaluatedWeeks)
    }

    @Test
    fun oneWeakWeekDragsStabilityViaWorstWeight() {
        // previas = [0.80, 0.85, 0.90, 0.95, 1.00] ; actual = 0.70
        // window = esos 5 + 0.70 → suma 5.20, average = 5.20/6 = 0.866667 ; worst = 0.70
        // StabilityScore = 0.750*0.866667 + 0.250*0.70 = 0.650 + 0.175 = 0.825
        // (El promedio puro daría 0.8667; el 0.825 PRUEBA que la peor semana pesa 25%.)
        val scores = listOf(0.80f, 0.85f, 0.90f, 0.95f, 1.00f)
        val history = fivePreviousWeeks.mapIndexed { i, week -> entry(week, scores[i]) }

        val result = StabilityScoringPolicy.evaluate(currentWeek, 0.70f, history)

        assertEquals(0.825f, result.stabilityScore!!, 0.001f)
        assertTrue(result.hasTemporalMemory)
    }

    @Test
    fun onlyMostRecentFivePreviousWeeksCount() {
        // 6 semanas previas: la más vieja es 0.0; las 5 recientes y la actual son 1.0.
        // El policy toma solo las 5 MÁS RECIENTES → la de 0.0 se descarta.
        // window = 5×1.0 + actual 1.0 → average 1.0, worst 1.0 → StabilityScore 1.0.
        // (Si no descartara la vieja, worst sería 0.0 y bajaría: el 1.0 prueba el take(5).)
        val history = listOf(entry("2026-04-06", 0.0f)) +
            fivePreviousWeeks.map { entry(it, 1.0f) }

        val result = StabilityScoringPolicy.evaluate(currentWeek, 1.0f, history)

        assertEquals(1.0f, result.stabilityScore!!, 0.001f)
    }

    @Test
    fun entriesFromAnotherScoringVersionAreIgnored() {
        // 4 previas válidas + 1 de una versión vieja de scoring → la vieja NO cuenta.
        // Quedan 4 válidas (< 5) → sin memoria temporal.
        val history = fivePreviousWeeks.take(4).map { entry(it, 0.90f) } +
            entry("2026-05-11", 0.90f, scoringVersion = "weekly-base-v0")

        val result = StabilityScoringPolicy.evaluate(currentWeek, 0.90f, history)

        assertNull(result.stabilityScore)
        assertFalse(result.hasTemporalMemory)
        assertEquals(5, result.evaluatedWeeks) // 4 válidas + la actual
    }

    @Test
    fun currentWeekSnapshotInHistoryIsExcluded() {
        // 5 previas válidas + un snapshot de la semana EN CURSO (weekStart == actual).
        // El de la semana actual se filtra (weekStart != currentWeekStart); el score
        // actual entra solo vía el parámetro. window = 5×0.90 + actual 0.90 → 0.90.
        val history = fivePreviousWeeks.map { entry(it, 0.90f) } +
            entry(currentWeek, 0.0f) // snapshot espurio de la semana en curso

        val result = StabilityScoringPolicy.evaluate(currentWeek, 0.90f, history)

        assertEquals(0.90f, result.stabilityScore!!, 0.001f)
        assertTrue(result.hasTemporalMemory)
        assertEquals(6, result.evaluatedWeeks)
    }

    private fun entry(
        weekStart: String,
        weeklyBaseScore: Float,
        scoringVersion: String = WeeklyScoreSnapshotConstants.SCORING_VERSION,
    ): WeeklyScoreHistoryEntry =
        WeeklyScoreHistoryEntry(
            weekStart = weekStart,
            weekEnd = weekStart,
            scoringVersion = scoringVersion,
            weeklyBaseScore = weeklyBaseScore,
            weeklyScore = weeklyBaseScore,
            state = ScoreState.Plenitude,
        )
}
