package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.domain.scoring.StateAggregationPolicy.LayerInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NIVEL 5 — agregación bolsa-global → ESTADO.
 *
 * Traducción verbatim de `estado(capas)` de `docs/scoring/verificacion_modelo_oficial.py`
 * (ver §NIVEL 5 de `docs/scoring/modelo-matematico-nucleo-v1.md`): términos por capa
 * (anclas `(base_eff, votos(n))`, solo-soportes `(G, ρ)`, solo-opt-in `(M, W0)`) + términos-sombra
 * opt-in `(M, BETA·Σpesos·(1−M))`; `base_global = Σ(v·w)/Σ(w)`; `extra_global` PLANO (promedio
 * sobre capas con anclas); `ESTADO = min(base_global, 1) + extra_global`.
 *
 * Reproduce EXACTAMENTE los asserts `AG-just`, `AG2/O3`, `I1`, `O2/C2`, `O5/Sol=Tin`, `I2/O11`
 * del script. Cálculo en [Double], tolerancia `1e-9` (`±0.01` para el arrastre plano de `AG2/O3`).
 */
class StateAggregationPolicyTest {
    private val tol = 1e-9

    // Anclas de referencia del Python.
    private val J = AnchorScoringPolicyV2.r(4, 30, List(4) { 30 }) // cumplir-justo ≈ 1.0
    private val XL = AnchorScoringPolicyV2.r(4, 30, List(7) { 60 }) // superhabit grande
    private val DEF = AnchorScoringPolicyV2.r(4, 30, List(3) { 30 }) // déficit (3 de 4 días)

    private fun anc(vararg r: Double, optin: Double? = null, nTasks: Int = 0) =
        LayerInput(anchors = r.toList(), optIn = optin, nTasksToday = nTasks)

    // ---------------- AG-just ----------------

    @Test
    fun agJust_cumplirJustoTresCapasEsUno() {
        val e = StateAggregationPolicy.estado(List(3) { anc(J) })
        assertEquals(1.0, e, tol)
    }

    // ---------------- AG2 / O3: arrastre plano ----------------

    @Test
    fun ag2o3_arrastrePlanoRecaidaConAnclasPerfectas() {
        // Config A: 1 ancla justa con opt-in en 0 (recaída total) + 2 capas justas.
        val c1 = StateAggregationPolicy.estado(
            listOf(anc(J, optin = 0.0), anc(J), anc(J)),
        )
        // Config B: 3 anclas justas con opt-in en 0 + 2 capas justas.
        val c2 = StateAggregationPolicy.estado(
            listOf(anc(J, J, J, optin = 0.0), anc(J), anc(J)),
        )
        assertEquals("config A arrastre plano ≈ 0.55", 0.55, c1, 0.01)
        assertEquals("config B arrastre plano ≈ 0.55", 0.55, c2, 0.01)
    }

    // ---------------- I1: opt-in global ----------------

    @Test
    fun i1_optInGlobalCapaPesadaIgualLiviana() {
        // El opt-in en la capa pesada (3 anclas) = en la liviana (1 ancla): el término-sombra
        // escala con Σpesos, no con la capa.
        val pesada = StateAggregationPolicy.estado(
            listOf(anc(J, J, J, optin = 0.15), anc(J), anc(J)),
        )
        val liviana = StateAggregationPolicy.estado(
            listOf(anc(J, J, J), anc(J, optin = 0.15), anc(J)),
        )
        assertEquals(pesada, liviana, tol)
    }

    // ---------------- O2 / C2: neutralidad con déficit ----------------

    @Test
    fun o2c2_neutralidadOptInBienConDeficit() {
        // M = 1 ⟹ término-sombra nulo: el déficit de la capa NO se ve alterado por el opt-in.
        val sin = StateAggregationPolicy.estado(listOf(anc(DEF), anc(J), anc(J)))
        val con = StateAggregationPolicy.estado(listOf(anc(DEF, optin = 1.0), anc(J), anc(J)))
        assertEquals(sin, con, tol)
    }

    // ---------------- O5 / Sol=Tin: superhabit plano ----------------

    @Test
    fun o5_superhabitIgualEnCualquierCapa() {
        // XL en la capa 1 = XL en la capa 2: el extra es PLANO (promedio entre capas con anclas).
        val sol = StateAggregationPolicy.estado(listOf(anc(XL), anc(J), anc(J)))
        val tin = StateAggregationPolicy.estado(listOf(anc(J), anc(XL), anc(J)))
        assertEquals(sol, tin, tol)
    }

    // ---------------- I2 / O11: capa solo-opt-in ----------------

    @Test
    fun i2o11_capaSoloOptInPesaW0() {
        // Una capa sin anclas ni soportes, solo opt-in cumplido (M=1), pesa W0=1.0 → ESTADO = 1.0.
        val e = StateAggregationPolicy.estado(
            listOf(anc(J), anc(J), LayerInput(optIn = 1.0)),
        )
        assertEquals(1.0, e, tol)
    }

    // ---------------- degradación ----------------

    @Test
    fun sinCapasEsCero() {
        assertEquals(0.0, StateAggregationPolicy.estado(emptyList()), tol)
    }

    @Test
    fun extraPlanoNoColapsaConCapaDebil() {
        // Sanity: una config con superhabit produce ESTADO > 1.0 (entra extra plano).
        val e = StateAggregationPolicy.estado(listOf(anc(XL), anc(J), anc(J)))
        assertTrue("estado=$e debe superar 1.0 por el superhabit", e > 1.0)
    }
}
