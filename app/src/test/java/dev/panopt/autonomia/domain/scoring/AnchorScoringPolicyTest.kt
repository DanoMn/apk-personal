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
import org.junit.Test

/**
 * Unit directo de AnchorScoringPolicy (árbol §7.2 base + §8.2 superhábit):
 *   baseScore   = 0.70 * frequencyScore + 0.30 * valueScore   (cada uno clamp 0..1)
 *   surplusBonus = 0.70 * freqSurplusBonus + 0.30 * valueSurplusBonus
 *                  con surplusBonus_i = 0.100 * (1 - exp(-magnitud_i / 2))
 *
 * Anchor de prueba: targetDays = 2 (weeklyFrequencyTarget), targetDailyValue = 10
 * (sessionTargetMinutes) → targetWeeklyValue = 20. Hojas de respuesta desde el contrato.
 */
class AnchorScoringPolicyTest {
    private val monday = LocalDate.of(2026, 5, 18)

    @Test
    fun frequencyWeighsSeventyAndValueThirty() {
        // 2 días hechos (freqRatio = 2/2 = 1.0) pero valor flojo: 2*1 = 2 sobre 20
        // (valueRatio = 0.1). baseScore = 0.70*1.0 + 0.30*0.1 = 0.73.
        // El 0.73 PRUEBA el reparto 70/30: si pesaran igual daría 0.55.
        val logs = listOf(logOn(0, value = 1), logOn(1, value = 1))

        val result = AnchorScoringPolicy.evaluate(anchor(), logs)

        assertEquals(0.73f, result.baseScore, 0.001f)
        assertEquals(0f, result.surplusBonus, 0.0001f)
    }

    @Test
    fun onTargetGivesFullBaseAndNoSurplus() {
        // 2 días hechos, valor 10 c/u = 20 (exacto al target semanal).
        // freqRatio = valueRatio = 1.0 → baseScore = 1.0 ; sin excedente → surplus 0.
        val logs = listOf(logOn(0, value = 10), logOn(1, value = 10))

        val result = AnchorScoringPolicy.evaluate(anchor(), logs)

        assertEquals(1.0f, result.baseScore, 0.001f)
        assertEquals(0f, result.surplusBonus, 0.0001f)
    }

    @Test
    fun surplusFollowsCappedExpCurve() {
        // 4 días hechos (freqRatio = 4/2 = 2.0 → magnitud 1.0) y valor 40 (valueRatio
        // = 40/20 = 2.0 → magnitud 1.0). baseScore satura en 1.0 (clamp).
        // surplusBonus_i = 0.100*(1-exp(-0.5)) = 0.039347 para freq y valor.
        // AnchorSurplusBonus = 0.70*0.039347 + 0.30*0.039347 = 0.039347.
        val logs = (0..3).map { logOn(it, value = 10) }

        val result = AnchorScoringPolicy.evaluate(anchor(), logs)

        assertEquals(1.0f, result.baseScore, 0.001f)
        assertEquals(0.039347f, result.surplusBonus, 0.0005f)
    }

    @Test
    fun noLogsGiveZeroBaseAndZeroSurplus() {
        // Sin registros: freqRatio = valueRatio = 0 → base 0 ; magnitud negativa → surplus 0.
        val result = AnchorScoringPolicy.evaluate(anchor(), emptyList())

        assertEquals(0f, result.baseScore, 0.0001f)
        assertEquals(0f, result.surplusBonus, 0.0001f)
    }

    private fun anchor(): ActivityDefinition =
        ActivityDefinition(
            id = "anc_test",
            layerId = "layer_interior",
            name = "anc_test",
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
            weeklyFrequencyTarget = 2,
            sessionTargetMinutes = 10,
            unit = ActivityUnit.Minutes,
            sortOrder = 10,
        )

    private fun logOn(dayOffset: Int, value: Int): ActivityLog =
        ActivityLog(
            activityId = "anc_test",
            date = monday.plusDays(dayOffset.toLong()).toString(),
            completed = true,
            actualValue = value,
            updatedAt = 0L,
        )
}
