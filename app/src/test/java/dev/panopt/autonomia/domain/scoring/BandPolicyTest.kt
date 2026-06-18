package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ScoreState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * NIVEL 6 — bandas `banda(ESTADO)`.
 *
 * Traducción verbatim de `banda(e)` de `docs/scoring/verificacion_modelo_oficial.py`
 * (ver §NIVEL 6 de `docs/scoring/modelo-matematico-nucleo-v1.md`): función PURA sobre los cortes
 * de [ScoringConstants] (`0.40 / 0.62 / 0.85 / 1.10`). Sin gates, sin worst-layer, sin
 * histéresis, sin memoria.
 *
 * Reproduce los asserts `BA1` (cortes R/A/EM/P/I) y `BA2` (Plenitud entra en 0.85; 0.84 = En
 * marcha) del script. [BandPolicy] coexiste con `BaseStatePolicy` (modelo viejo) hasta PR-F.
 */
class BandPolicyTest {

    // ---------------- BA1: cortes R/A/EM/P/I ----------------

    @Test
    fun ba1_cortesRestauracionAtencionMotionPlenitudInquebrantable() {
        assertEquals(ScoreState.Restoration, BandPolicy.band(0.30))
        assertEquals(ScoreState.Attention, BandPolicy.band(0.50))
        assertEquals(ScoreState.Motion, BandPolicy.band(0.70))
        assertEquals(ScoreState.Plenitude, BandPolicy.band(0.90))
        assertEquals(ScoreState.Unbreakable, BandPolicy.band(1.15))
    }

    // ---------------- BA2: Plenitud entra en 0.85 ----------------

    @Test
    fun ba2_plenitudEntraEn085Y084EsMotion() {
        assertEquals(ScoreState.Plenitude, BandPolicy.band(0.85))
        assertEquals(ScoreState.Motion, BandPolicy.band(0.84))
    }

    // ---------------- bordes exactos ----------------

    @Test
    fun bordesInferioresInclusivos() {
        // Cada corte es límite inferior inclusivo de la banda superior.
        assertEquals(ScoreState.Attention, BandPolicy.band(ScoringConstants.BAND_ATTENTION))
        assertEquals(ScoreState.Motion, BandPolicy.band(ScoringConstants.BAND_MOTION))
        assertEquals(ScoreState.Plenitude, BandPolicy.band(ScoringConstants.BAND_PLENITUDE))
        assertEquals(ScoreState.Unbreakable, BandPolicy.band(ScoringConstants.BAND_UNBREAKABLE))
    }

    @Test
    fun inquebrantableExacto() {
        // 1.10 → Inquebrantable; 1.099 (apenas por debajo) → Plenitud.
        assertEquals(ScoreState.Unbreakable, BandPolicy.band(1.10))
        assertEquals(ScoreState.Plenitude, BandPolicy.band(1.099))
    }

    @Test
    fun cumplirJustoEsPlenitud() {
        // BA-ref: ESTADO = 1.0 cae en Plenitud (entre 0.85 y 1.10).
        assertEquals(ScoreState.Plenitude, BandPolicy.band(1.0))
    }

    @Test
    fun pisoCeroEsRestauracion() {
        assertEquals(ScoreState.Restoration, BandPolicy.band(0.0))
    }
}
