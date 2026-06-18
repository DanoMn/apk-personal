package dev.panopt.autonomia.domain.scoring

import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * NIVEL 4 — opt-ins (sueño / sobriedad): señal `M ∈ [0, 1]` y término-sombra de la bolsa-global.
 *
 * Traducción directa de `BETA·Sigma·(1−M)` y de la señal de sobriedad
 * `M_sobr = Π_tracks (1 − A)^días_recaída` de `docs/scoring/verificacion_modelo_oficial.py`
 * (ver §NIVEL 4 de `docs/scoring/modelo-matematico-nucleo-v1.md`).
 *
 * Los axiomas que exigen el ESTADO agregado (`O2/C2` neutralidad, `I2/O11` capa solo-opt-in)
 * se validan en [StateAggregationPolicyTest] porque necesitan la bolsa-global completa. Aquí se
 * verifican las piezas atómicas: el término-sombra y la composición de la señal de sobriedad.
 *
 * Cálculo en [Double], tolerancia `1e-9`.
 */
class OptInPolicyTest {
    private val tol = 1e-9

    // ---------------- término-sombra ----------------

    @Test
    fun shadowTerm_escalaConSumaDePesosNoConCantidadDeCapas() {
        // w = BETA·Σpesos·(1−M). Doblar Σpesos dobla el peso del término.
        val m = 0.0
        val sigma = 3.5
        val expected = ScoringConstants.BETA * sigma * (1.0 - m)
        assertEquals(expected, OptInPolicy.shadowTerm(m, sigma), tol)
        // Escala lineal con Σpesos.
        assertEquals(2.0 * expected, OptInPolicy.shadowTerm(m, 2.0 * sigma), tol)
    }

    @Test
    fun shadowTerm_neutralCuandoMEsUno() {
        // O2/C2 a nivel atómico: M = 1 ⟹ w = 0 (el opt-in cumplido no arrastra).
        assertEquals(0.0, OptInPolicy.shadowTerm(1.0, 4.2), tol)
    }

    @Test
    fun shadowTerm_clampeaMFueraDeRango() {
        // M se clampea a [0, 1]: M > 1 cuenta como 1 (w = 0); M < 0 cuenta como 0.
        assertEquals(0.0, OptInPolicy.shadowTerm(1.5, 2.0), tol)
        assertEquals(ScoringConstants.BETA * 2.0, OptInPolicy.shadowTerm(-0.3, 2.0), tol)
    }

    // ---------------- señal de sobriedad M_sobr ----------------

    @Test
    fun sobriety_trackLimpioDaUno() {
        // Sin días de recaída en ningún track → M_sobr = 1 (neutral, no arrastra).
        assertEquals(1.0, OptInPolicy.sobrietySignal(listOf(0))!!, tol)
        assertEquals(1.0, OptInPolicy.sobrietySignal(listOf(0, 0, 0))!!, tol)
    }

    @Test
    fun sobriety_unDiaDeRecaidaAplicaUnGolpe() {
        // 1 día de recaída en 1 track → (1 − A)^1.
        val a = ScoringConstants.A
        assertEquals((1.0 - a).pow(1), OptInPolicy.sobrietySignal(listOf(1))!!, tol)
        // 2 días en el mismo track → (1 − A)^2.
        assertEquals((1.0 - a).pow(2), OptInPolicy.sobrietySignal(listOf(2))!!, tol)
    }

    @Test
    fun sobriety_multiTrackComponeSinTope() {
        // M_sobr = Π_tracks (1 − A)^días. Dos tracks con 1 día cada uno = (1 − A)^2.
        val a = ScoringConstants.A
        assertEquals((1.0 - a).pow(2), OptInPolicy.sobrietySignal(listOf(1, 1))!!, tol)
        // Track limpio no aporta (factor 1).
        assertEquals((1.0 - a).pow(1), OptInPolicy.sobrietySignal(listOf(0, 1, 0))!!, tol)
    }

    @Test
    fun sobriety_sinTracksEsNull() {
        // Opt-in inactivo: sin tracks no hay señal (M = null).
        assertNull(OptInPolicy.sobrietySignal(emptyList()))
    }
}
