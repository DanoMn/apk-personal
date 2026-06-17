package dev.panopt.autonomia.domain.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NIVEL 7 — mapeo E `ESTADO ∈ [0, 1.5] → PUNTOS ∈ [650, 1100]` (enfoque E: suma de rampas
 * logísticas). Fuente: `docs/scoring/modelo-matematico-nucleo-v1.md` § NIVEL 7 y
 * `docs/scoring/verificacion_modelo_oficial.py` (`sig`, `HITOS`, `rawpt`, `P(e)`).
 *
 * Escenarios PU1/PU3/PU4/PU5 de `specs/scoring-points-mapping/spec.md`.
 */
class PointsMappingPolicyTest {

    // --- PU1: rango [650, 1100] ---
    @Test
    fun `PU1 - estado 0 maps to 650 floor`() {
        assertEquals(650, PointsMappingPolicy.points(0.0))
    }

    @Test
    fun `PU1 - estado 1_5 maps to 1100 ceiling`() {
        assertEquals(1100, PointsMappingPolicy.points(1.5))
    }

    // --- PU3: cumplir-justo = 941; Inquebrantable entra ≈ 1011 ---
    @Test
    fun `PU3 - estado 1_0 cumplir-justo maps to 941`() {
        assertEquals(941.0, PointsMappingPolicy.points(1.0).toDouble(), 2.0)
    }

    @Test
    fun `PU3 - estado 1_10 unbreakable entry maps to 1011`() {
        assertEquals(1011.0, PointsMappingPolicy.points(1.10).toDouble(), 3.0)
    }

    // --- PU4: monótono no decreciente (de a ~1 punto) barriendo 0..1.5 en pasos de 0.001 ---
    @Test
    fun `PU4 - monotonic non-decreasing across 0 to 1_5`() {
        var previous = PointsMappingPolicy.points(0.0)
        var e = 0.0
        while (e <= 1.5 + 1e-9) {
            val current = PointsMappingPolicy.points(e)
            assertTrue(
                "PUNTOS bajó en e=$e: $previous -> $current",
                current >= previous,
            )
            previous = current
            e += 0.001
        }
    }

    // --- PU5: hitos en los cortes de banda ---
    @Test
    fun `PU5 - banda cut milestones`() {
        assertEquals(721.0, PointsMappingPolicy.points(0.40).toDouble(), 2.0)
        assertEquals(788.0, PointsMappingPolicy.points(0.62).toDouble(), 2.0)
        assertEquals(873.0, PointsMappingPolicy.points(0.85).toDouble(), 2.0)
    }

    // --- Clamp fuera de rango ---
    @Test
    fun `clamps estado below 0 to floor`() {
        assertEquals(650, PointsMappingPolicy.points(-0.5))
    }

    @Test
    fun `clamps estado above 1_5 to ceiling`() {
        assertEquals(1100, PointsMappingPolicy.points(2.3))
    }
}
