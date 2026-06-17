package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ScoreState
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NIVEL 2 — valor de capa (dos canales) + soportes (blend leve) + tasks (saturación conjunta).
 * Traducción directa de los asserts `VC3/VC4`, `SO2/SO4`, `TA5/TA-suma/TA3` de
 * `docs/scoring/verificacion_modelo_oficial.py` (ver §NIVEL 2 de
 * `docs/scoring/modelo-matematico-nucleo-v1.md`).
 *
 * Para los axiomas que exigen el ESTADO agregado (`SO2`, `SO4`, `TA5`, `TA-suma`, `TA3`) se usa
 * un agregador local [estado] que reproduce el bloque sin-opt-in de `estado(...)` del Python
 * combinando las piezas de NIVEL 2 ([LayerValuePolicy]) con el peso de NIVEL 3
 * ([LayerWeightPolicy]). La bolsa-global completa (con opt-ins / término-sombra) es PR-C
 * (`StateAggregationPolicy`); estos casos no usan opt-ins.
 *
 * Cálculo en [Double], tolerancia `1e-9`.
 */
class LayerValuePolicyTest {
    private val tol = 1e-9

    // Anclas de referencia del Python.
    private val J = AnchorScoringPolicyV2.r(4, 30, List(4) { 30 }) // cumplir-justo ≈ 1.0
    private val XL = AnchorScoringPolicyV2.r(4, 30, List(7) { 60 }) // superhabit grande
    private val HALF = AnchorScoringPolicyV2.r(4, 30, List(4) { 15 }) // base a medias

    /** Una capa con anclas (R-values), días de soporte opcionales y conteo de tasks de hoy. */
    private data class Layer(
        val anchors: List<Double> = emptyList(),
        val supportDays: List<Int>? = null,
        val nTasks: Int = 0,
    )

    /**
     * Agregador local (bloque sin-opt-in de `estado()` del Python): base ponderada por votos +
     * extra plano (promedio sobre las capas con anclas).
     */
    private fun estado(layers: List<Layer>): Double {
        data class Info(val value: Double, val weight: Double, val extra: Double, val hasAnchors: Boolean)
        val info = layers.mapNotNull { c ->
            if (c.anchors.isNotEmpty()) {
                val baseEff = LayerValuePolicy.baseEff(c.anchors, c.supportDays)
                val extra = LayerValuePolicy.extraFinal(c.anchors, c.supportDays, c.nTasks)
                Info(baseEff, LayerWeightPolicy.votes(c.anchors.size), extra, true)
            } else if (c.supportDays != null) {
                val g = LayerValuePolicy.supportSignal(c.supportDays)
                Info(g, LayerWeightPolicy.votes(0), 0.0, false)
            } else {
                null
            }
        }
        if (info.isEmpty()) return 0.0
        val base = info.sumOf { it.value * it.weight } / info.sumOf { it.weight }
        val extras = info.filter { it.hasAnchors }.map { it.extra }
        val extra = if (extras.isEmpty()) 0.0 else extras.average()
        return min(base, 1.0) + extra
    }

    /**
     * Banda local (criterio `banda()` del Python). La banda V2 sobre cortes de constantes es PR-C
     * (NIVEL 6); aquí solo se necesita distinguir Inquebrantable para `TA5`.
     */
    private fun banda(e: Double): ScoreState = when {
        e < ScoringConstantsV2.BAND_ATTENTION -> ScoreState.Restoration
        e < ScoringConstantsV2.BAND_MOTION -> ScoreState.Attention
        e < ScoringConstantsV2.BAND_PLENITUDE -> ScoreState.Motion
        e < ScoringConstantsV2.BAND_UNBREAKABLE -> ScoreState.Plenitude
        else -> ScoreState.Unbreakable
    }

    // ---------------- NIVEL 2: dos canales ----------------

    @Test
    fun vc3_extraCapaSeDiluyeEntreLasAnclas() {
        // 3 anclas: una en 1.5 (extra 0.5), dos en 1.0 (extra 0). extra_capa = (1/3)·0.5 = 0.1667.
        val anchors = listOf(1.5, 1.0, 1.0)
        assertEquals(0.5 / 3.0, LayerValuePolicy.extraLayer(anchors), tol)
    }

    @Test
    fun vc4_anclasPesanIgualEnLaBase() {
        // base_anclas es promedio simple de min(R,1): tres anclas justas → 1.0.
        assertEquals(1.0, LayerValuePolicy.baseAnchors(listOf(1.0, 1.0, 1.0)), tol)
        // Una en déficit baja la base proporcionalmente a 1/n.
        assertEquals((0.5 + 1.0 + 1.0) / 3.0, LayerValuePolicy.baseAnchors(listOf(0.5, 1.0, 1.0)), tol)
    }

    // ---------------- NIVEL 2.1: soportes ----------------

    @Test
    fun so2_blendBidireccionalLeve() {
        // descuidado (0 días) < sin soporte < sostenido (4 días).
        val vNo = estado(listOf(Layer(anchors = listOf(HALF))))
        val vDescuidado = estado(listOf(Layer(anchors = listOf(HALF), supportDays = listOf(0))))
        val vSostenido = estado(listOf(Layer(anchors = listOf(HALF), supportDays = listOf(4))))
        assertTrue(
            "descuidado $vDescuidado < sin $vNo < sostenido $vSostenido",
            vDescuidado < vNo && vNo < vSostenido,
        )
    }

    @Test
    fun so4_noCreceConLaCantidadDeSoportes() {
        // 1 soporte sostenido = 5 soportes sostenidos (G es promedio, no suma).
        val v1 = estado(listOf(Layer(anchors = listOf(HALF), supportDays = listOf(4))))
        val v5 = estado(listOf(Layer(anchors = listOf(HALF), supportDays = listOf(4, 4, 4, 4, 4))))
        assertEquals(v1, v5, tol)
    }

    // ---------------- NIVEL 2.2: tasks ----------------

    @Test
    fun ta5_antiAbusoJustoMas100TasksNoEsInquebrantable() {
        // 3 capas justas, cada una con 100 tasks de hoy: el extra satura por TAU, NO sube a Inquebrantable.
        val tope = estado(List(3) { Layer(anchors = listOf(J), nTasks = 100) })
        assertTrue("estado=$tope no debe ser Inquebrantable", banda(tope) != ScoreState.Unbreakable)
    }

    @Test
    fun taSuma_taskNuncaResta() {
        val t0 = estado(List(3) { Layer(anchors = listOf(J)) })
        val t1 = estado(
            listOf(
                Layer(anchors = listOf(J)),
                Layer(anchors = listOf(J)),
                Layer(anchors = listOf(J), nTasks = 1),
            ),
        )
        assertTrue("task no puede bajar el estado ($t0 → $t1)", t1 >= t0 - tol)
    }

    @Test
    fun ta3_taskEsEfimera() {
        // Mañana sin tasks vuelve al baseline (n_hoy=0 → sin lift).
        val withTasks = estado(List(3) { Layer(anchors = listOf(J)) })
        val tomorrow = estado(List(3) { Layer(anchors = listOf(J), nTasks = 0) })
        assertEquals(withTasks, tomorrow, tol)
    }

    @Test
    fun taskLift_topePorCapaEmergeDeLaSaturacion() {
        // Una capa justa: el lift de tasks no supera ~TAU=0.06 por capa aun con muchas tasks.
        val extraSinTasks = LayerValuePolicy.extraFinal(listOf(J), null, 0)
        val extraConTasks = LayerValuePolicy.extraFinal(listOf(J), null, 1000)
        val lift = extraConTasks - extraSinTasks
        assertTrue("lift=$lift nunca resta", lift >= -tol)
        assertTrue("lift=$lift no supera TAU≈0.06 por capa", lift <= ScoringConstantsV2.TAU + 1e-6)
    }
}
