package dev.panopt.autonomia.ui.anchors

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeWheelPickerTest {

    @Test
    fun `wheelHours contains 0 through 15`() {
        assertEquals((0..15).toList(), wheelHours)
        assertEquals(16, wheelHours.size)
    }

    @Test
    fun `wheelMinutes contains 0 through 59`() {
        assertEquals((0..59).toList(), wheelMinutes)
        assertEquals(60, wheelMinutes.size)
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
