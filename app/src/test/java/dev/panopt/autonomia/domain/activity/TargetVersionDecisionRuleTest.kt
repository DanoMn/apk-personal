package dev.panopt.autonomia.domain.activity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetVersionDecisionRuleTest {

    @Test
    fun `creation records the initial version`() {
        assertTrue(
            TargetVersionDecisionRule.shouldRecordVersion(
                previousMinutes = null, previousDays = null,
                newMinutes = 30, newDays = 4,
            ),
        )
    }

    @Test
    fun `minutes change records a new version`() {
        assertTrue(
            TargetVersionDecisionRule.shouldRecordVersion(
                previousMinutes = 30, previousDays = 4,
                newMinutes = 60, newDays = 4,
            ),
        )
    }

    @Test
    fun `frequency change records a new version`() {
        assertTrue(
            TargetVersionDecisionRule.shouldRecordVersion(
                previousMinutes = 30, previousDays = 4,
                newMinutes = 30, newDays = 6,
            ),
        )
    }

    @Test
    fun `no target change does not record a version`() {
        assertFalse(
            TargetVersionDecisionRule.shouldRecordVersion(
                previousMinutes = 30, previousDays = 4,
                newMinutes = 30, newDays = 4,
            ),
        )
    }
}
