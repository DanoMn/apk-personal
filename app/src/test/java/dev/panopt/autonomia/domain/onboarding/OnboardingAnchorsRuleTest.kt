package dev.panopt.autonomia.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingAnchorsRuleTest {

    @Test
    fun `sin anclas no permite avanzar`() {
        assertEquals(0, OnboardingAnchorsRule.distinctLayersWithAnchor(emptyList()))
        assertFalse(OnboardingAnchorsRule.canAdvance(emptyList()))
    }

    @Test
    fun `tres anclas en la misma capa cuentan como una capa`() {
        val anchors = listOf("interior", "interior", "interior")
        assertEquals(1, OnboardingAnchorsRule.distinctLayersWithAnchor(anchors))
        assertFalse(OnboardingAnchorsRule.canAdvance(anchors))
    }

    @Test
    fun `dos capas distintas no alcanzan`() {
        val anchors = listOf("interior", "cuerpo")
        assertFalse(OnboardingAnchorsRule.canAdvance(anchors))
    }

    @Test
    fun `tres capas distintas permiten avanzar`() {
        val anchors = listOf("interior", "cuerpo", "conducta")
        assertEquals(3, OnboardingAnchorsRule.distinctLayersWithAnchor(anchors))
        assertTrue(OnboardingAnchorsRule.canAdvance(anchors))
    }

    @Test
    fun `anclas repetidas en tres capas distintas permiten avanzar`() {
        val anchors = listOf("interior", "interior", "cuerpo", "conducta")
        assertEquals(3, OnboardingAnchorsRule.distinctLayersWithAnchor(anchors))
        assertTrue(OnboardingAnchorsRule.canAdvance(anchors))
    }
}
