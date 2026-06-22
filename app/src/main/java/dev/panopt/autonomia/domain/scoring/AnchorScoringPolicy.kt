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
 * `R ∈ [0, 1.5]`. Todas las constantes (γ, λ_v, κ, p, smax, s0) salen de [ScoringConstants];
 * en particular el exponente del gate es el parámetro `P`, NUNCA el literal 2.
 *
 * NIVEL 1 del motor núcleo v1 — único modelo de ancla tras PR-F (la `AnchorScoringPolicy` vieja
 * se eliminó). Lo invoca el orquestador [ScoreEngine] por ancla.
 */
internal object AnchorScoringPolicy {

    /**
     * @param f frecuencia meta (días/semana, 2–7).
     * @param t tiempo meta por sesión (minutos).
     * @param mins minutos hechos por día en la ventana semanal (solo cuentan los `> 0`).
     * @return `R ∈ [0, 1.5]`.
     */
    fun r(f: Int, t: Int, mins: List<Int>): Double =
        rFromRatios(f, mins.filter { it > 0 }.map { it.toDouble() / t.toDouble() })

    /**
     * Variante que recibe los RATIOS por día ya calculados (`m_i / T_i`), para que cada día pueda
     * usar su propia meta de tiempo (vara versionada por fecha, FASE 2). Núcleo IDÉNTICO a [r]:
     * solo cambia la fuente de los ratios. Solo cuentan los ratios `> 0`.
     *
     * @param f frecuencia meta (días/semana, 2–7).
     * @param dayRatios razón `m_i / T_i` de cada día con actividad en la ventana.
     */
    fun rFromRatios(f: Int, dayRatios: List<Double>): Double {
        val gamma = ScoringConstants.G_
        val lamV = ScoringConstants.LV
        val kappa = ScoringConstants.KP
        val p = ScoringConstants.P
        val smax = ScoringConstants.SMAX
        val s0 = ScoringConstants.S0

        // mk = ratios con actividad (>0) ordenados descendente; D = cantidad.
        val mk = dayRatios.filter { it > 0.0 }.sortedDescending()
        val d = mk.size
        if (d == 0) return 0.0

        // commit = mejores min(D, F); vol = resto.
        val cut = min(d, f)
        val commit = mk.subList(0, cut)
        val vol = mk.subList(cut, d)

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
