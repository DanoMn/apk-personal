package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Caracteriza la fórmula multi-track de sobriedad (árbol §13.4):
 *   SobrietyWeekly = 0.70 * promedio(trackScores) + 0.30 * peor(trackScore)
 * Hojas de respuesta calculadas a mano DESDE EL CONTRATO, no desde el código.
 */
class SobrietyScoringPolicyTest {
    private val weekDates = (0L..6L).map { LocalDate.of(2026, 5, 18).plusDays(it) } // 7 días
    private val today = LocalDate.of(2026, 5, 24)

    @Test
    fun multiTrackBlendsSeventyAverageAndThirtyWorstSoOneRelapseDragsTheSet() {
        // Track A: 7/7 Clean → cleanCoverage 1.0 · relapseProtection 1.0 · confidence 1.0 = 1.0
        // Track B: 7/7 Relapse → cleanCoverage 0.0 → trackScore 0.0
        // avg = (1.0 + 0.0)/2 = 0.5 ; worst = 0.0
        // SobrietyWeekly = 0.70*0.5 + 0.30*0.0 = 0.35
        // (El promedio puro daría 0.5; el 0.35 PRUEBA que el peor track pesa el 30%:
        //  una racha limpia NO oculta otra en recaída — §13.4.)
        val clean = track("trk_alcohol")
        val relapsing = track("trk_nicotine")
        val logs = weekDates.map { logFor(clean.id, it, AbstinenceStatus.Clean) } +
            weekDates.map { logFor(relapsing.id, it, AbstinenceStatus.Relapse) }

        val score = SobrietyScoringPolicy.score(
            tracks = listOf(clean, relapsing),
            allLogs = logs,
            todayLogs = emptyList(),
            weekDates = weekDates,
            today = today,
        )

        assertEquals(0.35f, score!!, 0.001f)
    }

    @Test
    fun multiTrackAllCleanScoresPerfect() {
        // Ambos tracks limpios toda la semana → trackScore 1.0 cada uno.
        // avg = 1.0 ; worst = 1.0 → 0.70*1.0 + 0.30*1.0 = 1.0
        val a = track("trk_alcohol")
        val b = track("trk_nicotine")
        val logs = weekDates.flatMap { date ->
            listOf(
                logFor(a.id, date, AbstinenceStatus.Clean),
                logFor(b.id, date, AbstinenceStatus.Clean),
            )
        }

        val score = SobrietyScoringPolicy.score(
            tracks = listOf(a, b),
            allLogs = logs,
            todayLogs = emptyList(),
            weekDates = weekDates,
            today = today,
        )

        assertEquals(1.0f, score!!, 0.001f)
    }

    private fun track(id: String): AbstinenceTrack =
        AbstinenceTrack(
            id = id,
            name = id,
            substanceLabel = id,
            severity = AbstinenceSeverity.Critical,
            contributionRole = ContributionRole.Protective,
            importanceTier = ImportanceTier.Critical,
            active = true,
            sortOrder = 10,
        )

    private fun logFor(trackId: String, date: LocalDate, status: AbstinenceStatus): AbstinenceLog =
        AbstinenceLog(
            trackId = trackId,
            date = date.toString(),
            status = status,
            updatedAt = 0L,
        )
}
