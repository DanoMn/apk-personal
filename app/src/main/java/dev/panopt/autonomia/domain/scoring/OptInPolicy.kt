package dev.panopt.autonomia.domain.scoring

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * NIVEL 4 del modelo de núcleo v1 — opt-ins (sueño / sobriedad).
 *
 * Un opt-in es una señal `M ∈ [0, 1]` que vive ADENTRO de una capa con anclas y solo puede
 * arrastrar (nunca premia): si `M < 1`, agrega a la bolsa-global un *término-sombra*
 * ```
 * w = BETA · Σpesos · (1 − M)
 * ```
 * que escala con la suma TOTAL de pesos (no con la capa ni con `N`), de modo que un opt-in
 * descuidado pesa igual venga de la capa más pesada o de la más liviana (axioma I1). Con `M = 1`
 * el término es nulo (neutralidad O2/C2). Una capa SOLO-opt-in (sin anclas ni soportes) entra como
 * término propio `(M, W0)`, decisión que vive en la agregación (NIVEL 5).
 *
 * Traducción verbatim de `BETA·Sigma·(1−M)` y `M_sobr = Π_tracks (1 − A)^días` de
 * `docs/scoring/verificacion_modelo_oficial.py` (ver §NIVEL 4 de
 * `docs/scoring/modelo-matematico-nucleo-v1.md`). Dominio puro JVM; cálculo en [Double]; todas las
 * constantes (BETA, A) salen de [ScoringConstants] — NUNCA se hardcodean.
 *
 * NIVEL 4 del motor núcleo v1 — única señal de opt-ins tras PR-F (la `SobrietyScoringPolicy` vieja
 * se eliminó). El orquestador [ScoreEngine] cablea `sobrietySignal` a Conducta y el sueño a Cuerpo.
 */
internal object OptInPolicy {

    /**
     * Peso del término-sombra de un opt-in en la bolsa-global:
     * ```
     * w = BETA · Σpesos · (1 − clamp(M, 0, 1))
     * ```
     * Escala con `Σpesos` (la suma total de pesos de las capas), NO con `N`. `M = 1` ⟹ `w = 0`.
     *
     * @param m señal del opt-in (se clampea a `[0, 1]`).
     * @param sigmaWeights suma total de pesos de las capas (`Σ peso_i`).
     */
    fun shadowTerm(m: Double, sigmaWeights: Double): Double {
        val mClamped = min(max(m, 0.0), 1.0)
        return ScoringConstants.BETA * sigmaWeights * (1.0 - mClamped)
    }

    /**
     * Señal de sobriedad `M_sobr = Π_tracks (1 − A)^días_recaída`. Un track limpio
     * (`días_recaída = 0`) aporta factor `1` (neutral); cada día de recaída multiplica por
     * `(1 − A)`. Multi-track compone sin tope (el producto puede acercarse a 0).
     *
     * @param relapseDaysPerTrack días de recaída de cada track en la ventana semanal.
     * @return `M_sobr ∈ (0, 1]`, o `null` si no hay tracks (opt-in inactivo).
     */
    fun sobrietySignal(relapseDaysPerTrack: List<Int>): Double? {
        if (relapseDaysPerTrack.isEmpty()) return null
        val a = ScoringConstants.A
        return relapseDaysPerTrack.fold(1.0) { acc, days ->
            acc * (1.0 - a).pow(max(days, 0))
        }
    }
}
