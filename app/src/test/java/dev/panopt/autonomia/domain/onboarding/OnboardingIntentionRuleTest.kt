package dev.panopt.autonomia.domain.onboarding

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingIntentionRuleTest {

    @Test
    fun `canAdvance_null_returnsFalse`() {
        assertFalse(OnboardingIntentionRule.canAdvance(null))
    }

    @Test
    fun `canAdvance_STANDARD_returnsTrue`() {
        assertTrue(OnboardingIntentionRule.canAdvance(OnboardingIntention.STANDARD))
    }

    @Test
    fun `canAdvance_PROTECTION_returnsTrue`() {
        assertTrue(OnboardingIntentionRule.canAdvance(OnboardingIntention.PROTECTION))
    }
}
