package dev.panopt.autonomia.domain.activity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AnchorGraceRuleTest {

    private val utc = ZoneId.of("UTC")

    private fun millis(date: String): Long =
        LocalDate.parse(date).atStartOfDay(utc).toInstant().toEpochMilli()

    @Test
    fun `within grace before 7 days`() {
        // Creada el 06-20, hoy 06-25 → 5 días < 7 → en gracia.
        assertTrue(AnchorGraceRule.isWithinGrace(millis("2026-06-20"), LocalDate.parse("2026-06-25"), utc))
    }

    @Test
    fun `within grace on creation day`() {
        assertTrue(AnchorGraceRule.isWithinGrace(millis("2026-06-25"), LocalDate.parse("2026-06-25"), utc))
    }

    @Test
    fun `evaluable at exactly 7 days`() {
        // Creada el 06-18, hoy 06-25 → 7 días → ya NO en gracia (entra al 8vo día).
        assertFalse(AnchorGraceRule.isWithinGrace(millis("2026-06-18"), LocalDate.parse("2026-06-25"), utc))
    }

    @Test
    fun `evaluable when much older`() {
        assertFalse(AnchorGraceRule.isWithinGrace(0L, LocalDate.parse("2026-06-25"), utc))
    }
}
