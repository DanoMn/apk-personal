package dev.panopt.autonomia.platform.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryDrainWindowTest {

    @Test
    fun `with cursor starts just after last timestamp`() {
        val window = TelemetryDrainWindow.compute(lastTimestamp = 500L, now = 1000L, initialWindowMillis = 10L)
        assertEquals(501L, window.from)
        assertEquals(1000L, window.to)
    }

    @Test
    fun `without cursor starts at now minus initial window`() {
        val window = TelemetryDrainWindow.compute(lastTimestamp = null, now = 1000L, initialWindowMillis = 200L)
        assertEquals(800L, window.from)
        assertEquals(1000L, window.to)
    }

    @Test
    fun `from is never after to`() {
        val window = TelemetryDrainWindow.compute(lastTimestamp = null, now = 1000L, initialWindowMillis = 200L)
        assertTrue(window.from <= window.to)
    }
}
