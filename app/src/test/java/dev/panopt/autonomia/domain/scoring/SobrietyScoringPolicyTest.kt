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

    // --- §13.3 RelapseProtectionScore = exp(-relapseDays / 1.5) ---------------
    // Caracterizada vía el trackScore compuesto de UN solo track sin días pendientes:
    //   trackScore = cleanCoverage * relapseProtection * trackingConfidence
    // Con un único track, SobrietyWeekly = 0.70*track + 0.30*track = track.
    // Sin pendientes → confidence = 1.0, así el peso de la recaída se ve limpio.

    @Test
    fun zeroRelapsesGivesFullProtection() {
        // 7/7 Clean → coverage 1.0 · relapseProtection exp(0)=1.0 · conf 1.0 = 1.0
        val score = scoreSingleTrack(relapseDays = 0)
        assertEquals(1.0f, score, 0.001f)
    }

    @Test
    fun oneRelapseDecaysByTheExpCurve() {
        // 6 Clean / 1 Relapse →
        //   coverage = 6/7            = 0.857143
        //   relapseProtection = exp(-1/1.5) = 0.513417
        //   confidence = 1.0
        //   trackScore = 0.857143 * 0.513417 = 0.440072
        val score = scoreSingleTrack(relapseDays = 1)
        assertEquals(0.440072f, score, 0.001f)
    }

    @Test
    fun twoRelapsesDecayFasterThanLinear() {
        // 5 Clean / 2 Relapse →
        //   coverage = 5/7            = 0.714286
        //   relapseProtection = exp(-2/1.5) = 0.263597
        //   trackScore = 0.714286 * 0.263597 = 0.188284
        val score = scoreSingleTrack(relapseDays = 2)
        assertEquals(0.188284f, score, 0.001f)
    }

    @Test
    fun threeRelapsesCollapseTheTrack() {
        // 4 Clean / 3 Relapse →
        //   coverage = 4/7            = 0.571429
        //   relapseProtection = exp(-3/1.5) = exp(-2) = 0.135335
        //   trackScore = 0.571429 * 0.135335 = 0.077334
        val score = scoreSingleTrack(relapseDays = 3)
        assertEquals(0.077334f, score, 0.001f)
    }

    /** Un solo track: [relapseDays] días Relapse explícitos, el resto Clean (sin pendientes). */
    private fun scoreSingleTrack(relapseDays: Int): Float {
        val trk = track("trk_alcohol")
        val logs = weekDates.mapIndexed { index, date ->
            val status = if (index < relapseDays) AbstinenceStatus.Relapse else AbstinenceStatus.Clean
            logFor(trk.id, date, status)
        }
        return SobrietyScoringPolicy.score(
            tracks = listOf(trk),
            allLogs = logs,
            todayLogs = emptyList(),
            weekDates = weekDates,
            today = today,
        )!!
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
