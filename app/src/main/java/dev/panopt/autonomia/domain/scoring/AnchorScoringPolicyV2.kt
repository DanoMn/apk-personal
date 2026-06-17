package dev.panopt.autonomia.domain.scoring

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * NIVEL 1 del modelo de núcleo v1 — el ancla `R(F, T, mins)`.
 *
 * Traducción verbatim de la función `R(...)` de `docs/scoring/verificacion_modelo_oficial.py`
 * (ver también `docs/scoring/modelo-matematico-nucleo-v1.md` §NIVEL 1). Dominio puro JVM: no
 * conoce Room, Compose ni la forma de los hechos crudos. Cálculo en [Double]; produce
 * `R ∈ [0, 1.5]`. Todas las constantes (γ, λ_v, κ, p, smax, s0) salen de [ScoringConstantsV2];
 * en particular el exponente del gate es el parámetro `P`, NUNCA el literal 2.
 *
 * Coexiste con [AnchorScoringPolicy] (modelo viejo, aún consumido por `LayerScoringPolicy`)
 * hasta el recableado de PR-F.
 */
internal object AnchorScoringPolicyV2 {

    /**
     * @param f frecuencia meta (días/semana, 2–7).
     * @param t tiempo meta por sesión (minutos).
     * @param mins minutos hechos por día en la ventana semanal (solo cuentan los `> 0`).
     * @return `R ∈ [0, 1.5]`.
     */
    fun r(f: Int, t: Int, mins: List<Int>): Double {
        val gamma = ScoringConstantsV2.G_
        val lamV = ScoringConstantsV2.LV
        val kappa = ScoringConstantsV2.KP
        val p = ScoringConstantsV2.P
        val smax = ScoringConstantsV2.SMAX
        val s0 = ScoringConstantsV2.S0

        // mk = días con actividad (>0) ordenados descendente; D = cantidad.
        val mk = mins.filter { it > 0 }.sortedDescending()
        val d = mk.size
        if (d == 0) return 0.0

        // Razones de tiempo r_i = t_i / T. commit = mejores min(D, F); vol = resto.
        val ratios = mk.map { it.toDouble() / t.toDouble() }
        val cut = min(d, f)
        val commit = ratios.subList(0, cut)
        val vol = ratios.subList(cut, d)

        // u(r) = min(r, 1)^γ
        fun u(x: Double): Double = min(x, 1.0).pow(gamma)

        val phi = commit.sumOf { u(it) } / f.toDouble()
        val v = vol.sumOf { u(it) }

        val base = 1.0 - (1.0 - phi) * exp(-lamV * v)

        // Superhabit: tiempo (St) y días (Sd), fundidos por wt = (F/7)^κ.
        val st = commit.sumOf { max(it - 1.0, 0.0) } / f.toDouble()
        val sd = if (f < 7) v / (7 - f).toDouble() else 0.0
        val wt = (f.toDouble() / 7.0).pow(kappa)
        val s = smax * (1.0 - exp(-(wt * st + (1.0 - wt) * sd) / s0))

        // Gate base^p: sin cimiento no hay gloria.
        return base + base.pow(p) * s
    }
}
