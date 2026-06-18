package dev.panopt.autonomia.domain.scoring

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * NIVEL 2 del modelo de núcleo v1 — valor de capa (dos canales) + soportes (blend leve) +
 * tasks (saturación conjunta, aporte efímero al extra).
 *
 * Traducción verbatim del bloque de capa de `estado(...)` en
 * `docs/scoring/verificacion_modelo_oficial.py` (ver §NIVEL 2 de
 * `docs/scoring/modelo-matematico-nucleo-v1.md`). Dominio puro JVM; cálculo en [Double]; todas
 * las constantes (WS, TAU, N0, SMAX, S0, gate `P`) salen de [ScoringConstants] — NUNCA se
 * hardcodean (en particular el exponente del gate es `P`, nunca el literal 2).
 *
 * Dos canales por capa con `n` anclas (valores `R_i`):
 * ```
 * base_anclas = (1/n)·Σ min(R_i, 1)     ∈ [0, 1]    "¿está en pie?"
 * extra_capa  = (1/n)·Σ max(R_i − 1, 0) ∈ [0, 0.5]  "¿se destacó?"  (SOLO anclas)
 * ```
 *
 * NIVEL 2 del motor núcleo v1 — único valor de capa tras PR-F (las policies viejas
 * `LayerScoringPolicy`/`LayerContributionPolicy`/`SupportScoringPolicy`/`TaskMomentumPolicy`
 * se eliminaron). Lo consume [StateAggregationPolicy].
 */
internal object LayerValuePolicy {

    /** Canal base: promedio simple de `min(R_i, 1)`. Las anclas pesan igual dentro de la capa. */
    fun baseAnchors(anchors: List<Double>): Double {
        if (anchors.isEmpty()) return 0.0
        return anchors.sumOf { min(it, 1.0) } / anchors.size.toDouble()
    }

    /** Canal extra (solo anclas): promedio simple de `max(R_i − 1, 0)`. El brillo se diluye en `1/n`. */
    fun extraLayer(anchors: List<Double>): Double {
        if (anchors.isEmpty()) return 0.0
        return anchors.sumOf { max(it - 1.0, 0.0) } / anchors.size.toDouble()
    }

    /**
     * Señal de bloque de soportes `G = (1/m)·Σ s_i` con `s_i = min(días_sostenidos_i / 4, 1)`.
     * PROMEDIO (no crece con la cantidad). Días negativos se clampean a 0 (defensivo).
     */
    fun supportSignal(supportDays: List<Int>): Double {
        if (supportDays.isEmpty()) return 0.0
        return supportDays.sumOf { min(max(it, 0).toDouble() / 4.0, 1.0) } / supportDays.size.toDouble()
    }

    /**
     * Base efectiva de la capa tras el blend leve de soportes:
     * ```
     * base_eff = (1 − WS)·base_anclas + WS·G   si hay soportes (blend bidireccional leve)
     *          = base_anclas                    si no hay soportes
     *          = G                              si la capa NO tiene anclas (la señal ES la base)
     * ```
     * Clampeada a `[0, 1]`.
     */
    fun baseEff(anchors: List<Double>, supportDays: List<Int>?): Double {
        val raw = if (anchors.isEmpty()) {
            supportSignal(supportDays.orEmpty())
        } else if (supportDays != null) {
            val ws = ScoringConstants.WS
            (1.0 - ws) * baseAnchors(anchors) + ws * supportSignal(supportDays)
        } else {
            baseAnchors(anchors)
        }
        return min(max(raw, 0.0), 1.0)
    }

    /**
     * Extra final de la capa = `extra_capa + task_lift`. Las tasks aportan SOLO al extra por
     * saturación conjunta sobre la misma curva del superhabit (techo `SMAX`), con gate `base_eff^P`:
     * ```
     * su_anc      = −s0·ln(1 − extra_capa/smax)               surplus crudo (inversa de la exp)
     * g_task      = 1 − exp(−n_hoy / N0)                       saturación por conteo
     * THETA       = −s0·ln(1 − TAU/smax)                       presupuesto pre-saturación
     * extra_joint = smax·(1 − exp(−(su_anc + THETA·g_task)/s0))  re-satura por la MISMA curva
     * task_lift   = (extra_joint − extra_capa) · base_eff^P    gate base^P: sin cimiento no aporta
     * ```
     * Sin tasks (`nTasksToday == 0`), `extra_final = extra_capa` (efímero: mañana sin tasks vuelve
     * al baseline). Nunca resta.
     *
     * @param nTasksToday tasks completadas HOY con esta capa (conteo efímero, se resetea cada día).
     */
    fun extraFinal(anchors: List<Double>, supportDays: List<Int>?, nTasksToday: Int): Double {
        val extraCapa = extraLayer(anchors)
        if (nTasksToday <= 0) return extraCapa

        val smax = ScoringConstants.SMAX
        val s0 = ScoringConstants.S0
        val tau = ScoringConstants.TAU
        val n0 = ScoringConstants.N0
        val p = ScoringConstants.P

        val suAnc = if (extraCapa < smax) -s0 * ln(1.0 - extraCapa / smax) else 1e9
        val theta = -s0 * ln(1.0 - tau / smax)
        val gTask = 1.0 - exp(-nTasksToday.toDouble() / n0)
        val extraJoint = smax * (1.0 - exp(-(suAnc + theta * gTask) / s0))

        val baseEff = baseEff(anchors, supportDays)
        return extraCapa + (extraJoint - extraCapa) * baseEff.pow(p)
    }
}
