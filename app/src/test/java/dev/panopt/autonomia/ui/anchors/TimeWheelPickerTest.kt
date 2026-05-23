package dev.panopt.autonomia.ui.anchors

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeWheelPickerTest {

    @Test
    fun `wheelHours contains 0 through 8`() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8), wheelHours)
        assertEquals(9, wheelHours.size)
    }

    @Test
    fun `wheelMinutes contains 0 5 10 up to 55`() {
        val expected = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55)
        assertEquals(expected, wheelMinutes)
        assertEquals(12, wheelMinutes.size)
    }

    @Test
    fun `formatWheelValue pads single digit to two`() {
        assertEquals("00", formatWheelValue(0))
        assertEquals("03", formatWheelValue(3))
        assertEquals("09", formatWheelValue(9))
    }

    @Test
    fun `formatWheelValue keeps two-digit values unchanged`() {
        assertEquals("10", formatWheelValue(10))
        assertEquals("25", formatWheelValue(25))
        assertEquals("55", formatWheelValue(55))
        assertEquals("99", formatWheelValue(99))
    }

    @Test
    fun `formatWheelValue handles edge case 100`() {
        assertEquals("100", formatWheelValue(100))
    }
}
