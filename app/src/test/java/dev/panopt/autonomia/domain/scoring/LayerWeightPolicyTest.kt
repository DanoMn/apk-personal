package dev.panopt.autonomia.domain.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NIVEL 3 — peso de capa (votos por anclas). Traducción directa de los asserts `PC2`/`PC3`/`PC5`
 * de `docs/scoring/verificacion_modelo_oficial.py` (`votos(n) = RHO if n==0 else (1−r^n)/(1−r)`).
 *
 * Cálculo en [Double], tolerancia `1e-9`.
 */
class LayerWeightPolicyTest {
    private val tol = 1e-9

    private fun votes(n: Int): Double = LayerWeightPolicy.votes(n)

    @Test
    fun pc2_votosDecrecientesYTechoBajoDos() {
        // peso(1)=1.0, peso(2)=1.5, peso(3)=1.75; con techo asintótico 1/(1−r)=2.0 nunca alcanzado.
        assertEquals(1.0, votes(1), tol)
        assertEquals(1.5, votes(2), tol)
        assertEquals(1.75, votes(3), tol)
        assertTrue("votes(50) debe ser < 2.0", votes(50) < 2.0)
    }

    @Test
    fun pc3_ningunaCapaDecideMasDe50PorCiento() {
        // Peor caso: 3 capas, una saturada (n=50) y dos con 1 ancla. Fracción de la saturada ≤ 0.50.
        val worst = votes(50) / (votes(50) + votes(1) + votes(1))
        assertTrue("peor caso = ${worst * 100}% debe ser ≤ 50%", worst <= 0.50 + tol)
    }

    @Test
    fun pc5_capaSoloSoportesPesaRho() {
        assertEquals(ScoringConstantsV2.RHO, votes(0), tol)
        assertEquals(0.15, votes(0), tol)
    }
}
