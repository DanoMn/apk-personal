package dev.panopt.autonomia.ui.anchors

import dev.panopt.autonomia.TargetPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalPresetTest {

    @Test
    fun `None toCountAndPeriod returns null null`() {
        val (count, period) = GoalPreset.None.toCountAndPeriod()
        assertNull("count should be null for None", count)
        assertNull("period should be null for None", period)
    }

    @Test
    fun `TwoPerWeek toCountAndPeriod returns 2 Week`() {
        val (count, period) = GoalPreset.TwoPerWeek.toCountAndPeriod()
        assertEquals(2, count)
        assertEquals(TargetPeriod.Week, period)
    }

    @Test
    fun `ThreePerWeek toCountAndPeriod returns 3 Week`() {
        val (count, period) = GoalPreset.ThreePerWeek.toCountAndPeriod()
        assertEquals(3, count)
        assertEquals(TargetPeriod.Week, period)
    }

    @Test
    fun `FourPerWeek toCountAndPeriod returns 4 Week`() {
        val (count, period) = GoalPreset.FourPerWeek.toCountAndPeriod()
        assertEquals(4, count)
        assertEquals(TargetPeriod.Week, period)
    }

    @Test
    fun `FivePerWeek toCountAndPeriod returns 5 Week`() {
        val (count, period) = GoalPreset.FivePerWeek.toCountAndPeriod()
        assertEquals(5, count)
        assertEquals(TargetPeriod.Week, period)
    }

    @Test
    fun `SixPerWeek toCountAndPeriod returns 6 Week`() {
        val (count, period) = GoalPreset.SixPerWeek.toCountAndPeriod()
        assertEquals(6, count)
        assertEquals(TargetPeriod.Week, period)
    }

    @Test
    fun `SevenPerWeek toCountAndPeriod returns 7 Week`() {
        val (count, period) = GoalPreset.SevenPerWeek.toCountAndPeriod()
        assertEquals(7, count)
        assertEquals(TargetPeriod.Week, period)
    }

    @Test
    fun `TwoPerMonth toCountAndPeriod returns 2 Month`() {
        val (count, period) = GoalPreset.TwoPerMonth.toCountAndPeriod()
        assertEquals(2, count)
        assertEquals(TargetPeriod.Month, period)
    }

    @Test
    fun `ThreePerMonth toCountAndPeriod returns 3 Month`() {
        val (count, period) = GoalPreset.ThreePerMonth.toCountAndPeriod()
        assertEquals(3, count)
        assertEquals(TargetPeriod.Month, period)
    }

    @Test
    fun `FourPerMonth toCountAndPeriod returns 4 Month`() {
        val (count, period) = GoalPreset.FourPerMonth.toCountAndPeriod()
        assertEquals(4, count)
        assertEquals(TargetPeriod.Month, period)
    }

    @Test
    fun `SixPerMonth toCountAndPeriod returns 6 Month`() {
        val (count, period) = GoalPreset.SixPerMonth.toCountAndPeriod()
        assertEquals(6, count)
        assertEquals(TargetPeriod.Month, period)
    }

    @Test
    fun `EightPerMonth toCountAndPeriod returns 8 Month`() {
        val (count, period) = GoalPreset.EightPerMonth.toCountAndPeriod()
        assertEquals(8, count)
        assertEquals(TargetPeriod.Month, period)
    }

    @Test
    fun `TenPerMonth toCountAndPeriod returns 10 Month`() {
        val (count, period) = GoalPreset.TenPerMonth.toCountAndPeriod()
        assertEquals(10, count)
        assertEquals(TargetPeriod.Month, period)
    }

    @Test
    fun `Custom toCountAndPeriod returns null null`() {
        val (count, period) = GoalPreset.Custom.toCountAndPeriod()
        assertNull("count should be null for Custom", count)
        assertNull("period should be null for Custom", period)
    }

    @Test
    fun `toCountAndPeriod is deterministic across calls`() {
        val first = GoalPreset.FivePerWeek.toCountAndPeriod()
        val second = GoalPreset.FivePerWeek.toCountAndPeriod()
        assertEquals("count should match", first.first, second.first)
        assertEquals("period should match", first.second, second.second)
    }

    @Test
    fun `all numeric presets have unique count-period pairs`() {
        val numericPresets = GoalPreset.entries.filter {
            it != GoalPreset.None && it != GoalPreset.Custom
        }
        val pairs = numericPresets.map { it.toCountAndPeriod() }
        val uniquePairs = pairs.toSet()
        assertEquals("Each numeric preset should have a unique (count, period) pair", pairs.size, uniquePairs.size)
    }

    @Test
    fun `countFromPreset returns numeric string for all numeric presets`() {
        val numericPresets = GoalPreset.entries.filter {
            it != GoalPreset.None && it != GoalPreset.Custom
        }
        for (preset in numericPresets) {
            val countStr = countFromPreset(preset)
            val parsed = countStr.toIntOrNull()
            assertEquals(
                "countFromPreset(${preset.name}) should return a numeric string",
                true,
                parsed != null,
            )
            assertEquals(
                "countFromPreset should match toCountAndPeriod().first",
                preset.toCountAndPeriod().first,
                parsed,
            )
        }
    }

    @Test
    fun `countFromPreset returns dash for None and Custom`() {
        assertEquals("—", countFromPreset(GoalPreset.None))
        assertEquals("—", countFromPreset(GoalPreset.Custom))
    }

    @Test
    fun `frequencyLabel returns sem for weekly presets`() {
        for (preset in weeklyPresets) {
            assertEquals("/sem", frequencyLabel(preset))
        }
    }

    @Test
    fun `frequencyLabel returns mes for monthly presets`() {
        for (preset in monthlyPresets) {
            assertEquals("/mes", frequencyLabel(preset))
        }
    }

    @Test
    fun `frequencyLabel returns empty for None and Custom`() {
        assertEquals("", frequencyLabel(GoalPreset.None))
        assertEquals("", frequencyLabel(GoalPreset.Custom))
    }

    @Test
    fun `weeklyPresets contains exactly 6 entries from 2 to 7`() {
        assertEquals(6, weeklyPresets.size)
        assertEquals(GoalPreset.TwoPerWeek, weeklyPresets[0])
        assertEquals(GoalPreset.ThreePerWeek, weeklyPresets[1])
        assertEquals(GoalPreset.FourPerWeek, weeklyPresets[2])
        assertEquals(GoalPreset.FivePerWeek, weeklyPresets[3])
        assertEquals(GoalPreset.SixPerWeek, weeklyPresets[4])
        assertEquals(GoalPreset.SevenPerWeek, weeklyPresets[5])
    }

    @Test
    fun `monthlyPresets contains exactly 6 entries from 2 to 10`() {
        assertEquals(6, monthlyPresets.size)
        assertEquals(GoalPreset.TwoPerMonth, monthlyPresets[0])
        assertEquals(GoalPreset.ThreePerMonth, monthlyPresets[1])
        assertEquals(GoalPreset.FourPerMonth, monthlyPresets[2])
        assertEquals(GoalPreset.SixPerMonth, monthlyPresets[3])
        assertEquals(GoalPreset.EightPerMonth, monthlyPresets[4])
        assertEquals(GoalPreset.TenPerMonth, monthlyPresets[5])
    }
}
