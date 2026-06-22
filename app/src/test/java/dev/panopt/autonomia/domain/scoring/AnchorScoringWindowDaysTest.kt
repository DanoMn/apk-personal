package dev.panopt.autonomia.domain.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NIVEL 1 — generalización de `rFromRatios` a una ventana de `windowDays = N` días
 * (cambio `scoring-arranque-cuenta`, lote 1).
 *
 * Spec: `openspec/changes/scoring-arranque-cuenta/specs/anchor-scoring/spec.md`.
 *
 * Reconciliación cerrada: `f_eff = min(f, N)` gobierna SOLO los términos de superhábit
 * de ventana (`sd`, `wt`); `phi`/`cut`/`st` mantienen `f` crudo. Con `N = 7` el resultado
 * es byte-idéntico al modelo maduro (`f_eff = min(f,7) = f` ∀f∈[2,7]).
 *
 * Cálculo en [Double]; tolerancia `1e-9`.
 */
class AnchorScoringWindowDaysTest {
    private val tol = 1e-9

    private fun rRatios(f: Int, ratios: List<Double>, windowDays: Int = 7): Double =
        AnchorScoringPolicy.rFromRatios(f, ratios, windowDays)

    // --- Requirement: Default windowDays=7 es byte-idéntico al modelo actual ---

    @Test
    fun defaultWindowDays_esByteIdenticoAExplicito7() {
        // Batería representativa: f∈{2,3,4,7}, dayRatios variados.
        val casos: List<Pair<Int, List<Double>>> = listOf(
            2 to listOf(1.0, 1.0, 1.0, 1.0),
            2 to listOf(0.5, 0.8, 1.0),
            3 to listOf(1.0, 1.0, 1.0, 0.5, 0.5),
            3 to listOf(1.0, 1.0, 1.0),
            4 to listOf(2.0, 1.5, 1.0, 0.3, 0.9, 0.1),
            4 to List(7) { 1.0 },
            7 to List(7) { 1.0 },
            7 to listOf(1.0, 0.4, 0.9, 0.2, 1.0, 0.7, 0.3),
        )
        for ((f, ratios) in casos) {
            val implicito = rRatios(f, ratios) // default
            val explicito = rRatios(f, ratios, 7)
            // Byte-idéntico: igualdad exacta de Double, sin delta.
            assertEquals(
                "f=$f ratios=$ratios: default debe ser byte-idéntico a windowDays=7",
                explicito,
                implicito,
                0.0,
            )
        }
    }

    @Test
    fun f7_semanaCompleta_sinTerminoDeDias() {
        // f_eff = min(7,7) = 7 no es < 7 → sd = 0.0; resultado finito y acotado.
        val r = rRatios(7, List(7) { 1.0 }, 7)
        assertTrue("R=$r en [0,1.5]", r in 0.0..1.5)
    }

    // --- Requirement: windowDays<7 reparte el superhábit sobre la ventana parcial ---

    @Test
    fun f3_N4_sinPenalizarDiasFuturos_R10() {
        // commit=[1,1,1], vol=[], v=0, phi=1.0, sd=0, base=1.0 → R = 1.0
        val r = rRatios(3, listOf(1.0, 1.0, 1.0), windowDays = 4)
        assertEquals(1.0, r, 1e-9)
    }

    @Test
    fun f2_N4_superhabitRepartidoSobreVentana() {
        // commit=[1,1], vol=[1,1], v=2, phi=1.0, f_eff=2, sd=2/(4-2)=1.0, base=1.0.
        // base de compromiso pleno = 1.0; con superhábit R > 1.0 y finito.
        val rExtra = rRatios(2, listOf(1.0, 1.0, 1.0, 1.0), windowDays = 4)
        assertTrue("R=$rExtra debe superar la base 1.0 por superhábit", rExtra > 1.0)
        assertTrue("R=$rExtra en [0,1.5]", rExtra in 0.0..1.5)
        assertFalse(rExtra.isNaN())
        assertFalse(rExtra.isInfinite())

        // El reparto sobre N=4 difiere del reparto maduro sobre 7 (sd distinto).
        val rMaduro = rRatios(2, listOf(1.0, 1.0, 1.0, 1.0), windowDays = 7)
        assertTrue("sd se reparte sobre N=4, no sobre 7 → R distinto", rExtra != rMaduro)
    }

    // --- Requirement: f ≥ N no produce división por cero ni peso fuera de rango ---

    @Test
    fun f5_N2_guardaContraDivisionPorCero() {
        // f_eff = min(5,2) = 2; sd = 0.0 (2<2 falso); wt = (2/2)^κ = 1.0; v = 0.
        val r = rRatios(5, listOf(1.0, 0.8), windowDays = 2)
        assertFalse("R no debe ser NaN", r.isNaN())
        assertFalse("R no debe ser Infinity", r.isInfinite())
        assertTrue("R=$r en [0,1.5]", r in 0.0..1.5)
    }

    @Test
    fun f7_N1_unSoloDia_sinCrash() {
        val r = rRatios(7, listOf(1.0), windowDays = 1)
        assertFalse(r.isNaN())
        assertFalse(r.isInfinite())
        assertTrue("R=$r en [0,1.5]", r in 0.0..1.5)
    }

    @Test
    fun dayRatiosVacio_conWindowParcial_esCero() {
        assertEquals(0.0, rRatios(3, emptyList(), windowDays = 4), tol)
    }

    @Test
    fun transversal_todoFyN_finitoYAcotado() {
        for (f in 2..7) {
            for (n in 1..6) {
                val ratios = List(minOf(f, n).coerceAtLeast(1)) { 1.0 }
                val r = rRatios(f, ratios, windowDays = n)
                assertFalse("f=$f N=$n → NaN", r.isNaN())
                assertFalse("f=$f N=$n → Infinity", r.isInfinite())
                assertTrue("f=$f N=$n → R=$r fuera de [0,1.5]", r in 0.0..1.5)
            }
        }
    }

    // --- Requirement: windowDays se clampa a [1,7] ---

    @Test
    fun windowDaysMayorQue7_seTrataComo7() {
        val ratios = listOf(1.0, 1.0, 1.0)
        assertEquals(rRatios(3, ratios, 7), rRatios(3, ratios, 9), 0.0)
    }

    @Test
    fun windowDaysCeroONegativo_seTrataComo1() {
        val ratios = listOf(1.0)
        assertEquals(rRatios(3, ratios, 1), rRatios(3, ratios, 0), 0.0)
        assertEquals(rRatios(3, ratios, 1), rRatios(3, ratios, -5), 0.0)
    }
}
