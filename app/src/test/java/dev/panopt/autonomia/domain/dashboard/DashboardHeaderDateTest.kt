package dev.panopt.autonomia.domain.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DashboardHeaderDateTest {

    @Test
    fun `formats a weekday in spanish without leading zero`() {
        assertEquals("Jueves 4 de junio", DashboardHeaderDate.format(LocalDate.of(2026, 6, 4)))
    }

    @Test
    fun `uses accented day and month names`() {
        // Sábado (acento) + mayo.
        assertEquals("Sábado 30 de mayo", DashboardHeaderDate.format(LocalDate.of(2026, 5, 30)))
        // Miércoles (acento) + enero.
        assertEquals("Miércoles 7 de enero", DashboardHeaderDate.format(LocalDate.of(2026, 1, 7)))
    }
}
