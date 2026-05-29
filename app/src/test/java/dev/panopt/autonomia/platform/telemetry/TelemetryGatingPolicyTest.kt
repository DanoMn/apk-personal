package dev.panopt.autonomia.platform.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryGatingPolicyTest {

    @Test
    fun `first consumer schedules collection`() {
        assertEquals(GatingAction.SCHEDULE, TelemetryGatingPolicy.onRegister(previousActiveCount = 0))
    }

    @Test
    fun `additional consumer does not reschedule`() {
        assertEquals(GatingAction.NO_OP, TelemetryGatingPolicy.onRegister(previousActiveCount = 1))
    }

    @Test
    fun `removing one of many keeps collecting`() {
        assertEquals(GatingAction.NO_OP, TelemetryGatingPolicy.onUnregister(newActiveCount = 1))
    }

    @Test
    fun `removing the last consumer cancels collection`() {
        assertEquals(GatingAction.CANCEL, TelemetryGatingPolicy.onUnregister(newActiveCount = 0))
    }
}
