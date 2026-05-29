package dev.panopt.autonomia.platform.telemetry

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryRetentionPolicyTest {

    @Test
    fun `purge threshold is now minus retention`() {
        assertEquals(8500L, TelemetryRetentionPolicy.purgeThreshold(now = 10000L, retentionMillis = 1500L))
    }

    @Test
    fun `zero retention purges everything up to now`() {
        assertEquals(10000L, TelemetryRetentionPolicy.purgeThreshold(now = 10000L, retentionMillis = 0L))
    }
}
