package dev.panopt.autonomia.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingSleepRuleTest {

    // ── canAdvance ─────────────────────────────────────────────────────────

    @Test
    fun canAdvance_validDefault_8h_returnsTrue() {
        // 23:30 → 07:30 = 8 h, bien por encima del mínimo de 5 h
        assertTrue(OnboardingSleepRule.canAdvance("23:30", "07:30"))
    }

    @Test
    fun canAdvance_below5h_returnsFalse() {
        // 23:30 → 02:00 = 2,5 h, por debajo del mínimo
        assertFalse(OnboardingSleepRule.canAdvance("23:30", "02:00"))
    }

    @Test
    fun canAdvance_exactly300min_returnsTrue() {
        // 23:30 → 04:30 = exactamente 300 min (el límite es ≥ 300, NO estricto)
        assertTrue(OnboardingSleepRule.canAdvance("23:30", "04:30"))
    }

    @Test
    fun canAdvance_adjustedValid_6h_returnsTrue() {
        // Escenario "ajustar pickers": 23:30 → 05:30 = 6 h, válido
        assertTrue(OnboardingSleepRule.canAdvance("23:30", "05:30"))
    }

    // ── derivedWindowMinutes ───────────────────────────────────────────────

    @Test
    fun derivedWindowMinutes_crossMidnight_480min() {
        // 23:30 → 07:30 cruza la medianoche = 480 min
        assertEquals(480, OnboardingSleepRule.derivedWindowMinutes("23:30", "07:30"))
    }

    @Test
    fun derivedWindowMinutes_unparseable_returnsNull() {
        // Cadena vacía no es parseable → null (alimenta el "—" de la UI)
        assertNull(OnboardingSleepRule.derivedWindowMinutes("", "07:30"))
    }
}
