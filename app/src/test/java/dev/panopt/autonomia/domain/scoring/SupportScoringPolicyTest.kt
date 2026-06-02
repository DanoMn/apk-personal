package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit directo de SupportScoringPolicy (árbol §9, UX INVERSA):
 *   SupportScore = 1 - omittedSupportDays / expectedSupportDays   (clamp 0..1)
 *   expectedSupportDays = nº soportes * nº días de la semana
 *
 * En soportes el usuario marca lo que NO hizo: un log "hecho" cuenta como OMISIÓN.
 * Sin omisiones el soporte está en 1.0. Hojas de respuesta desde el contrato.
 */
class SupportScoringPolicyTest {
    private val weekDates = (0L..6L).map { LocalDate.of(2026, 5, 18).plusDays(it) } // 7 días

    @Test
    fun noSupportsConfiguredReturnsNull() {
        // Soportes son opt-in: sin soportes, no aportan ni limitan → null.
        val score = SupportScoringPolicy.evaluate(emptyList(), emptyMap(), weekDates)
        assertNull(score)
    }

    @Test
    fun supportWithoutOmissionsScoresPerfect() {
        // 1 soporte, sin logs → 0 omisiones sobre 7 esperados → 1 - 0/7 = 1.0
        val support = support("sup_a")
        val score = SupportScoringPolicy.evaluate(listOf(support), emptyMap(), weekDates)
        assertEquals(1.0f, score!!, 0.001f)
    }

    @Test
    fun supportOmittedEveryDayScoresZero() {
        // 1 soporte omitido los 7 días → 1 - 7/7 = 0.0
        val support = support("sup_a")
        val logs = weekDates.map { doneLog(support.id, it) }
        val score = SupportScoringPolicy.evaluate(
            supports = listOf(support),
            weeklyLogsByActivity = mapOf(support.id to logs),
            weekDates = weekDates,
        )
        assertEquals(0.0f, score!!, 0.001f)
    }

    @Test
    fun multiSupportAveragesOmissionsAcrossExpectedDays() {
        // 2 soportes → esperados = 2*7 = 14. Uno omitido los 7 días, el otro 0 omisiones.
        // omitidos = 7 → 1 - 7/14 = 0.5. (Una racha limpia NO compensa el otro al 100%.)
        val omitted = support("sup_omitted")
        val clean = support("sup_clean")
        val logs = weekDates.map { doneLog(omitted.id, it) }
        val score = SupportScoringPolicy.evaluate(
            supports = listOf(omitted, clean),
            weeklyLogsByActivity = mapOf(omitted.id to logs),
            weekDates = weekDates,
        )
        assertEquals(0.5f, score!!, 0.001f)
    }

    @Test
    fun emptyWeekDatesHitGuardAndReturnPerfect() {
        // Guard anti-división-por-cero: expectedSupportDays = 1*0 = 0 → return 1f
        // (sin semana evaluable, no hay omisiones que penalizar).
        val support = support("sup_a")
        val score = SupportScoringPolicy.evaluate(listOf(support), emptyMap(), emptyList())
        assertEquals(1.0f, score!!, 0.0001f)
    }

    private fun support(id: String): ActivityDefinition =
        ActivityDefinition(
            id = id,
            layerId = "layer_cuerpo",
            name = id,
            description = "",
            type = ActivityType.Check,
            role = ActivityRole.SelfCare,
            activityType = ActivitySurface.Support,
            contributionRole = ContributionRole.Support,
            importanceTier = ImportanceTier.Medium,
            cadence = ActivityCadence.Daily,
            targetValue = 1,
            minimumValue = 1,
            targetCount = null,
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = null,
            sessionTargetMinutes = null,
            unit = ActivityUnit.Boolean,
            sortOrder = 10,
        )

    private fun doneLog(activityId: String, date: LocalDate): ActivityLog =
        ActivityLog(
            activityId = activityId,
            date = date.toString(),
            completed = true,
            actualValue = null,
            updatedAt = 0L,
        )
}
