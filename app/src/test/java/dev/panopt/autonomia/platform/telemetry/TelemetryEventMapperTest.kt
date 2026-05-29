package dev.panopt.autonomia.platform.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TelemetryEventMapperTest {

    @Test
    fun `activity resumed maps to APP_FOREGROUND keeping package`() {
        val event = TelemetryEventMapper.map(typeCode = 1, packageName = "com.app", timestamp = 100L)
        assertEquals(DeviceActivityEventType.APP_FOREGROUND, event?.eventType)
        assertEquals("com.app", event?.packageName)
        assertEquals(100L, event?.timestamp)
        assertEquals("usage_event:1", event?.source)
    }

    @Test
    fun `activity paused maps to APP_BACKGROUND`() {
        assertEquals(
            DeviceActivityEventType.APP_BACKGROUND,
            TelemetryEventMapper.map(2, "com.app", 1L)?.eventType,
        )
    }

    @Test
    fun `user interaction maps to USER_INTERACTION`() {
        assertEquals(
            DeviceActivityEventType.USER_INTERACTION,
            TelemetryEventMapper.map(7, null, 1L)?.eventType,
        )
    }

    @Test
    fun `screen interactive maps to SCREEN_ON without package`() {
        val event = TelemetryEventMapper.map(15, null, 1L)
        assertEquals(DeviceActivityEventType.SCREEN_ON, event?.eventType)
        assertNull(event?.packageName)
    }

    @Test
    fun `screen non interactive maps to SCREEN_OFF`() {
        assertEquals(DeviceActivityEventType.SCREEN_OFF, TelemetryEventMapper.map(16, null, 1L)?.eventType)
    }

    @Test
    fun `keyguard hidden maps to UNLOCK`() {
        assertEquals(DeviceActivityEventType.UNLOCK, TelemetryEventMapper.map(18, null, 1L)?.eventType)
    }

    @Test
    fun `keyguard shown maps to LOCK`() {
        assertEquals(DeviceActivityEventType.LOCK, TelemetryEventMapper.map(17, null, 1L)?.eventType)
    }

    @Test
    fun `unknown code maps to null`() {
        assertNull(TelemetryEventMapper.map(999, null, 1L))
    }
}
