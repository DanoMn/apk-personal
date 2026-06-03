package dev.panopt.autonomia.ui.sleep

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Máscara de entrada del campo de hora. Debe auto-insertar el ':' a medida que el
 * usuario teclea dígitos, de modo que "0730" se convierta en "07:30" sin que el
 * usuario tenga que escribir el separador (deuda menor del slice 3).
 */
class SleepWindowFieldsTest {

    @Test
    fun empty_staysEmpty() {
        assertEquals("", "".filterTimeInput())
    }

    @Test
    fun oneDigit_unchanged() {
        assertEquals("0", "0".filterTimeInput())
    }

    @Test
    fun twoDigits_noColonYet() {
        assertEquals("07", "07".filterTimeInput())
    }

    @Test
    fun threeDigits_insertsColon() {
        assertEquals("07:3", "073".filterTimeInput())
    }

    @Test
    fun fourDigits_fullTime() {
        assertEquals("07:30", "0730".filterTimeInput())
    }

    @Test
    fun typedWithColon_isIdempotent() {
        // El usuario teclea el ':' a mano: el resultado no se duplica ni rompe.
        assertEquals("07:30", "07:30".filterTimeInput())
    }

    @Test
    fun partialWithColon_preserved() {
        assertEquals("07:3", "07:3".filterTimeInput())
    }

    @Test
    fun overflowDigits_truncatedToFour() {
        assertEquals("07:30", "073012".filterTimeInput())
    }

    @Test
    fun nonDigitsStripped() {
        assertEquals("07:30", "ab07:30".filterTimeInput())
    }

    @Test
    fun midnight_formats() {
        assertEquals("23:30", "2330".filterTimeInput())
    }
}
