package dev.panopt.autonomia.domain.activity

import dev.panopt.autonomia.ActivitySurface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R9 — Forma canónica de la config de un soporte custom: `activityType = Support`, sin targets.
 * Dominio puro: sin Room ni Compose.
 */
class SupportConfigFactoryTest {

    // R9-E2 — la config de soporte tiene activityType=Support y todos los targets en null
    @Test
    fun `buildSupportConfig produces a valid support config with no targets`() {
        val now = 7_000L
        val config = SupportConfigFactory.buildSupportConfig(activityId = "act_custom_x", now = now)

        assertEquals("act_custom_x", config.activityId)
        assertEquals(ActivitySurface.Support.name, config.activityType)
        assertTrue(config.active)
        assertFalse(config.archived)

        // Sin targets por diseño de dominio (paridad con addSupport)
        assertNull(config.cadence)
        assertNull(config.targetValue)
        assertNull(config.minimumValue)
        assertNull(config.targetCount)
        assertNull(config.targetPeriod)
        assertNull(config.weeklyFrequencyTarget)
        assertNull(config.sessionTargetMinutes)
        assertNull(config.commitmentDurationMonths)

        assertEquals(now, config.createdAt)
        assertEquals(now, config.updatedAt)
        assertEquals(now.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), config.sortOrder)
    }
}
