package dev.panopt.autonomia.domain.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ActiveTargetVersionRuleTest {

    private fun v(validFrom: String, minutes: Int, days: Int, createdAt: Long = 0L) =
        ActivityTargetVersion(
            activityId = "act",
            validFrom = LocalDate.parse(validFrom),
            targetMinutes = minutes,
            targetDays = days,
            createdAt = createdAt,
        )

    @Test
    fun `returns the latest version not after the date`() {
        val versions = listOf(
            v("2026-06-01", minutes = 20, days = 5),
            v("2026-06-10", minutes = 60, days = 5),
        )
        // El 2026-06-05 todavía rige la vara del 06-01 (la del 06-10 aún no entró).
        assertEquals(20, ActiveTargetVersionRule.resolve(versions, LocalDate.parse("2026-06-05"))!!.targetMinutes)
        // El 2026-06-10 ya rige la nueva.
        assertEquals(60, ActiveTargetVersionRule.resolve(versions, LocalDate.parse("2026-06-10"))!!.targetMinutes)
    }

    @Test
    fun `validFrom equal to date counts as in effect`() {
        val versions = listOf(v("2026-06-10", minutes = 60, days = 5))
        assertEquals(60, ActiveTargetVersionRule.resolve(versions, LocalDate.parse("2026-06-10"))!!.targetMinutes)
    }

    @Test
    fun `same validFrom resolves to the last inserted (highest createdAt)`() {
        // Editar dos veces el mismo día: vale la última.
        val versions = listOf(
            v("2026-06-01", minutes = 20, days = 5, createdAt = 100L),
            v("2026-06-01", minutes = 30, days = 5, createdAt = 200L),
        )
        assertEquals(30, ActiveTargetVersionRule.resolve(versions, LocalDate.parse("2026-06-01"))!!.targetMinutes)
    }

    @Test
    fun `returns null when all versions are in the future`() {
        val versions = listOf(v("2026-06-10", minutes = 60, days = 5))
        assertNull(ActiveTargetVersionRule.resolve(versions, LocalDate.parse("2026-06-05")))
    }

    @Test
    fun `returns null for empty version list`() {
        assertNull(ActiveTargetVersionRule.resolve(emptyList(), LocalDate.parse("2026-06-05")))
    }
}
