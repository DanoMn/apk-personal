package dev.panopt.autonomia.domain.scoring

import kotlin.math.pow

/**
 * NIVEL 3 del modelo de núcleo v1 — peso de capa (votos por anclas).
 *
 * Traducción verbatim de `votos(n)` de `docs/scoring/verificacion_modelo_oficial.py`
 * (ver §NIVEL 3 de `docs/scoring/modelo-matematico-nucleo-v1.md`):
 * ```
 * votos(n) = ρ                  si n == 0   (capa SOLO-soportes, peso reducido)
 *          = (1 − r^n)/(1 − r)  si n ≥ 1    (cada ancla nueva suma la mitad; techo 1/(1−r) = 2.0)
 * ```
 * Una capa solo-opt-in (sin anclas pero con señal) pesa `W0`, decisión que vive en la agregación
 * (NIVEL 5, PR-C), no aquí. Dominio puro JVM; constantes desde [ScoringConstantsV2].
 */
internal object LayerWeightPolicy {

    /**
     * @param n número de anclas de la capa (`0` = capa solo-soportes).
     * @return peso de la capa para la bolsa-global.
     */
    fun votes(n: Int): Double {
        if (n <= 0) return ScoringConstantsV2.RHO
        val r = ScoringConstantsV2.RG
        return (1.0 - r.pow(n)) / (1.0 - r)
    }
}
