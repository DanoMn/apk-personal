package dev.panopt.autonomia.domain.scoring

import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NIVEL 1 — ancla `R(F, T, mins)`. Traducción directa de los asserts de ancla de
 * `docs/scoring/verificacion_modelo_oficial.py` (AN1–AN11) + casos de referencia §1.4 del
 * blueprint `docs/scoring/modelo-matematico-nucleo-v1.md`.
 *
 * Cálculo en [Double], tolerancia `1e-9` (`±0.001` para los casos §1.4).
 */
class AnchorScoringPolicyTest {
    private val tol = 1e-9
    private val tol14 = 1e-3

    private fun r(f: Int, t: Int, mins: List<Int>): Double =
        AnchorScoringPolicy.r(f, t, mins)

    private fun extra(f: Int, t: Int, mins: List<Int>): Double =
        max(r(f, t, mins) - 1.0, 0.0)

    @Test
    fun an1_rangoAcotado() {
        val value = r(4, 30, List(7) { 600 })
        assertTrue("R=$value debe estar en [0, 1.5]", value in 0.0..1.5)
    }

    @Test
    fun an2_pisoCero() {
        assertEquals(0.0, r(4, 30, emptyList()), tol)
    }

    @Test
    fun an3_cumplirJustoEsUnoParaCualquierF() {
        assertEquals(1.0, r(4, 30, List(4) { 30 }), tol)
        assertEquals(1.0, r(7, 30, List(7) { 30 }), tol)
    }

    @Test
    fun an6_gateBaseCuadradoSinFrecuenciaNoRinde() {
        // 2 de 4 días con doble tiempo → extra = 0; 4 de 4 días con doble tiempo → extra > 0.
        assertEquals(0.0, extra(4, 30, listOf(60, 60)), tol)
        assertTrue(extra(4, 30, List(4) { 60 }) > 0.0)
    }

    @Test
    fun an7_superhabitDeTiempoYDeDias() {
        assertTrue(extra(4, 30, List(4) { 60 }) > 0.0) // superhabit por tiempo
        assertTrue(extra(4, 30, List(6) { 30 }) > 0.0) // superhabit por días
    }

    @Test
    fun an8_monotoniaDiaExtraNoBaja() {
        val baseline = r(4, 30, List(4) { 30 })
        for (x in listOf(1, 15, 30, 60)) {
            val withExtra = r(4, 30, List(4) { 30 } + x)
            assertTrue("x=$x bajó R", withExtra >= baseline - tol)
        }
    }

    @Test
    fun an10_invarianzaDeEscala() {
        assertEquals(r(4, 30, listOf(40, 30, 30)), r(4, 120, listOf(160, 120, 120)), tol)
    }

    @Test
    fun an11_continuidadSinSaltos() {
        // x barriendo 0..200 en pasos de 0.1 (minutos fraccionales). Como la firma usa Int,
        // reproducimos el barrido del Python con escala T grande para mantener la finura.
        var prev: Double? = null
        var continuous = true
        for (i in 0..2000) {
            // x*0.1 minutos sobre T=30 ≡ x minutos sobre T=300 (invarianza de escala).
            val value = r(4, 300, listOf(i, 300, 300, 300))
            val p = prev
            if (p != null && kotlin.math.abs(value - p) > 0.02) continuous = false
            prev = value
        }
        assertTrue("hubo un salto > 0.02", continuous)
    }

    @Test
    fun ref14_cumplirJustoF3() {
        assertEquals(1.000, r(3, 30, listOf(30, 30, 30)), tol14)
    }

    @Test
    fun ref14_superhabitTiempo4x60() {
        assertEquals(1.289, r(4, 30, List(4) { 60 }), tol14)
    }

    @Test
    fun ref14_superhabitDias6x30() {
        assertEquals(1.266, r(4, 30, List(6) { 30 }), tol14)
    }

    @Test
    fun ref14_gateSinFrecuencia2x60() {
        assertEquals(0.544, r(4, 30, listOf(60, 60)), tol14)
    }

    @Test
    fun ref14_cumplirJustoF7() {
        assertEquals(1.0, r(7, 30, List(7) { 30 }), tol14)
    }

    @Test
    fun ref14_f7_45x7() {
        // §1.4 lista 1.32 (redondeo a centésimas); el valor oficial del Python es 1.31606.
        // Afirmamos contra el valor oficial con ±0.001, y contra el redondeo del doc con ±0.005.
        val value = r(7, 30, List(7) { 45 })
        assertEquals(1.31606, value, tol14)
        assertEquals(1.32, value, 5e-3)
    }

    @Test
    fun ref14_f7_120x7_acotadoEnTope() {
        // §1.4: 1.499 (acotado bajo el tope 1.5). Valor oficial: 1.49876.
        assertEquals(1.499, r(7, 30, List(7) { 120 }), tol14)
    }
}
