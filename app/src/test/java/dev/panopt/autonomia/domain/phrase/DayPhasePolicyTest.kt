package dev.panopt.autonomia.domain.phrase

import dev.panopt.autonomia.DayPhase
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * TDD — RED phase (task 1.1):
 * Written before DayPhase and DayPhasePolicy exist.
 * Covers the 5 boundary scenarios from day-phase-policy/spec.md.
 */
class DayPhasePolicyTest {

    // ── Scenario: Time inside Dawn window ────────────────────────────────────

    @Test
    fun `midday is Dawn`() {
        val noon = LocalDateTime.of(2026, 6, 4, 12, 0)
        assertEquals(DayPhase.Dawn, DayPhasePolicy.phaseFor(noon))
    }

    // ── Scenario: Boundary at 14:59 is Dawn ──────────────────────────────────

    @Test
    fun `14h59 is still Dawn (upper boundary inclusive)`() {
        val time = LocalDateTime.of(2026, 6, 4, 14, 59)
        assertEquals(DayPhase.Dawn, DayPhasePolicy.phaseFor(time))
    }

    // ── Scenario: Boundary at 15:00 is Dusk ──────────────────────────────────

    @Test
    fun `15h00 is Dusk (Dusk window starts here)`() {
        val time = LocalDateTime.of(2026, 6, 4, 15, 0)
        assertEquals(DayPhase.Dusk, DayPhasePolicy.phaseFor(time))
    }

    // ── Scenario: Time in evening Dusk window ────────────────────────────────

    @Test
    fun `evening hour 20h00 is Dusk`() {
        val time = LocalDateTime.of(2026, 6, 4, 20, 0)
        assertEquals(DayPhase.Dusk, DayPhasePolicy.phaseFor(time))
    }

    // ── Scenario: Time in late-night Dusk window ─────────────────────────────

    @Test
    fun `04h59 late night is Dusk`() {
        val time = LocalDateTime.of(2026, 6, 5, 4, 59)
        assertEquals(DayPhase.Dusk, DayPhasePolicy.phaseFor(time))
    }

    // ── Scenario: 05:00 is the first Dawn minute ─────────────────────────────

    @Test
    fun `05h00 is Dawn (Dawn window starts here)`() {
        val time = LocalDateTime.of(2026, 6, 4, 5, 0)
        assertEquals(DayPhase.Dawn, DayPhasePolicy.phaseFor(time))
    }
}
