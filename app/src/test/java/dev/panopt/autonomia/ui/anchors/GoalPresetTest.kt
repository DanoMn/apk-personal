package dev.panopt.autonomia.ui.anchors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalPresetTest {

    @Test
    fun `weekly frequency presets contain exactly two through seven`() {
        assertEquals(
            listOf(2, 3, 4, 5, 6, 7),
            weeklyFrequencyPresets.map { weeklyFrequencyTargetFromPreset(it) },
        )
        assertFalse(weeklyFrequencyPresets.any { it.count == 1 })
    }

    @Test
    fun `weekly frequency normalizes to anchor range`() {
        assertEquals(3, normalizeWeeklyFrequencyTarget(null))
        assertEquals(2, normalizeWeeklyFrequencyTarget(1))
        assertEquals(7, normalizeWeeklyFrequencyTarget(20))
        assertEquals(5, normalizeWeeklyFrequencyTarget(5))
    }

    @Test
    fun `anchor target contract requires weekly frequency and session minutes`() {
        assertTrue(isValidAnchorTargetContract(20, 3))
        assertFalse(isValidAnchorTargetContract(null, 3))
        assertFalse(isValidAnchorTargetContract(20, null))
        assertFalse(isValidAnchorTargetContract(0, 3))
        assertFalse(isValidAnchorTargetContract(901, 3))
        assertFalse(isValidAnchorTargetContract(20, 1))
        assertFalse(isValidAnchorTargetContract(20, 8))
    }

    @Test
    fun `commitment duration presets default to indefinite and include requested months`() {
        assertNull(CommitmentDurationPreset.Indefinite.months)
        assertEquals(
            listOf(null, 3, 5, 7, 9, 11, 13, null),
            commitmentDurationPresets.map { it.months },
        )
    }

    @Test
    fun `commitment duration label treats null as indefinite`() {
        assertEquals("Indefinido", commitmentDurationLabel(null))
        assertEquals("7 meses", commitmentDurationLabel(7))
    }

    @Test
    fun `custom commitment months are clamped to safe range`() {
        assertNull(normalizeCustomCommitmentMonths(""))
        assertEquals(1, normalizeCustomCommitmentMonths("0"))
        assertEquals(13, normalizeCustomCommitmentMonths("13"))
        assertEquals(120, normalizeCustomCommitmentMonths("999"))
    }
}
