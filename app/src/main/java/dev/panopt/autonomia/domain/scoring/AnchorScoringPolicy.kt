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
     * Generalizada a una ventana de `windowDays = N` días (cambio `scoring-arranque-cuenta`):
     * en una ventana parcial (`N < 7`, cuenta nueva) los términos de superhábit de ventana
     * (`sd`, `wt`) se reparten sobre `N` en vez de sobre `7`, usando la frecuencia efectiva de
     * ventana `f_eff = min(f, N)` (no se puede comprometer más días de los vividos). `phi`, `cut`
     * y `st` mantienen el `f` crudo (la meta es semanal y no se prorratea). Con `N = 7`,
     * `f_eff = min(f, 7) = f` ∀f∈[2,7] → resultado byte-idéntico al modelo maduro.
     *
     * @param f frecuencia meta (días/semana, 2–7).
     * @param dayRatios razón `m_i / T_i` de cada día con actividad en la ventana.
     * @param windowDays horizonte de la ventana en días vividos; default `7` = semana madura.
     *   Se clampa a `[1, 7]` antes de calcular.
     */
    fun rFromRatios(f: Int, dayRatios: List<Double>, windowDays: Int = 7): Double {
        val gamma = ScoringConstants.G_
        val lamV = ScoringConstants.LV
        val kappa = ScoringConstants.KP
        val p = ScoringConstants.P
        val smax = ScoringConstants.SMAX
        val s0 = ScoringConstants.S0

        // Ventana de N días, clampada a [1,7]. f_eff = frecuencia efectiva de ventana:
        // gobierna SOLO los términos de superhábit de ventana (sd, wt).
        val n = windowDays.coerceIn(1, 7)
        val fEff = min(f, n)

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

        // Superhabit: tiempo (St) y días (Sd), fundidos por wt = (f_eff/N)^κ.
        // st usa f crudo (meta semanal); sd/wt usan f_eff sobre la ventana N (guard contra div/0).
        val st = commit.sumOf { max(it - 1.0, 0.0) } / f.toDouble()
        val sd = if (fEff < n) v / (n - fEff).toDouble() else 0.0
        val wt = (fEff.toDouble() / n.toDouble()).pow(kappa)
        val s = smax * (1.0 - exp(-(wt * st + (1.0 - wt) * sd) / s0))

        // Gate base^p: sin cimiento no hay gloria.
        return base + base.pow(p) * s
    }
}
