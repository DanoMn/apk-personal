package dev.panopt.autonomia.domain.scoring

import kotlin.math.min

/**
 * NIVEL 5 del modelo de núcleo v1 — agregación bolsa-global → ESTADO.
 *
 * Traducción verbatim de `estado(capas)` de `docs/scoring/verificacion_modelo_oficial.py`
 * (ver §NIVEL 5 de `docs/scoring/modelo-matematico-nucleo-v1.md`). Cada capa aporta UN término
 * `(valor, peso)` a una sola "bolsa-global"; los opt-ins descuidados agregan términos-sombra
 * extra. La base es el promedio PONDERADO de la bolsa; el extra (superhabit + tasks) se suma
 * PLANO (promedio simple sobre las capas con anclas, sin pesos), de modo que un brillo pesa igual
 * en cualquier capa.
 *
 * ```
 * Términos por capa:
 *   con anclas      → (base_eff,  votos(n))
 *   solo-soportes   → (G,         ρ)
 *   solo-opt-in     → (M,         W0)
 * Términos-sombra (por cada capa con anclas y opt-in M):
 *   (M, BETA·Σpesos·(1 − M))     si w > 0
 *
 * Σpesos      = Σ peso_i  (solo términos de capa, NO sombras)
 * base_global = Σ(valor·peso) / Σ(peso)   (incluye términos-sombra)
 * extra_global= promedio simple de extra_final sobre capas-con-anclas  (PLANO)
 * ESTADO      = min(base_global, 1) + extra_global
 * ```
 *
 * Sin capas → `ESTADO = 0`. Dominio puro JVM; cálculo en [Double]; constantes (`W0`, `ρ`, `BETA`)
 * desde [ScoringConstants] vía [LayerWeightPolicy] / [OptInPolicy] — NUNCA se hardcodean.
 *
 * NIVEL 5 del motor núcleo v1 — única agregación tras PR-F (las policies viejas `WeeklyScorePolicy`
 * / `BaseStatePolicy` se eliminaron). Lo invoca el orquestador [ScoreEngine].
 */
internal object StateAggregationPolicy {

    /**
     * Forma de entrada de una capa para la bolsa-global (alineada al `LayerFacts` del design, pero
     * con las anclas ya resueltas a sus `R`-values por [AnchorScoringPolicy]).
     *
     * @param anchors valores `R_i` de las anclas de la capa (vacío = capa sin anclas).
     * @param supportDays días sostenidos de cada soporte (`null` = sin soportes).
     * @param nTasksToday tasks completadas hoy con esta capa (efímero).
     * @param optIn señal del opt-in de la capa (`M ∈ [0, 1]`), o `null` si no hay opt-in.
     */
    data class LayerInput(
        val anchors: List<Double> = emptyList(),
        val supportDays: List<Int>? = null,
        val nTasksToday: Int = 0,
        val optIn: Double? = null,
        /**
         * ID de la capa que origina este término. Opcional: el cálculo del ESTADO no lo usa, pero
         * [aggregate] lo propaga a [LayerResult] para que el orquestador pueble el detalle por-capa
         * del dashboard sin recalcular. `null` cuando el llamador solo necesita el ESTADO.
         */
        val layerId: String? = null,
    )

    /**
     * Resultado por-capa de la agregación, para que la presentación (dashboard) muestre el detalle
     * por capa SIN recalcular. Son exactamente los valores que la bolsa-global ya computó:
     *
     * @param layerId capa de origen (propagado desde [LayerInput.layerId]).
     * @param baseEff `base_eff ∈ [0, 1]` — "¿la capa está en pie?". Es el `value` del término de
     *   capa (anclas → `base_eff`; solo-soportes → `G`; solo-opt-in → `M`).
     * @param extra `extra_final ∈ [0, 0.5]` — "¿se destacó?" (superhabit + tasks). Solo capas con
     *   anclas aportan extra; el resto es `0`.
     * @param weight peso de la capa en la bolsa-global (votos / ρ / W0).
     */
    data class LayerResult(
        val layerId: String?,
        val baseEff: Double,
        val extra: Double,
        val weight: Double,
    )

    /**
     * Salida completa de la agregación: el [estado] global y el detalle [layerResults] por capa.
     * El estado es idéntico a [estado]; los resultados por capa son los términos de capa ya
     * resueltos (no recalculados), en el mismo orden que las [LayerInput] de entrada que aportaron
     * un término (las capas vacías —sin anclas, soportes ni opt-in— no producen [LayerResult]).
     */
    data class Aggregation(
        val estado: Double,
        val layerResults: List<LayerResult>,
    )

    /** Término ya resuelto de la bolsa-global. */
    private data class Term(
        val kind: Kind,
        val value: Double,
        val weight: Double,
        val extraFinal: Double,
        val optIn: Double?,
        val layerId: String?,
    ) {
        enum class Kind { ANCHOR, SUPPORT, OPT_IN }
    }

    /** ESTADO ∈ [0, 1.5] de la bolsa-global. Sin capas → 0. */
    fun estado(layers: List<LayerInput>): Double = aggregate(layers).estado

    /**
     * Agrega las capas en UNA pasada y devuelve el ESTADO ∈ [0, 1.5] junto con el detalle por capa
     * ([LayerResult]). El ESTADO es idéntico al de [estado] (que delega aquí): no hay doble cálculo
     * ni divergencia posible entre el número global y el detalle del dashboard. Sin capas → ESTADO 0
     * y lista vacía.
     */
    fun aggregate(layers: List<LayerInput>): Aggregation {
        val terms = layers.mapNotNull { layer ->
            when {
                layer.anchors.isNotEmpty() -> {
                    val baseEff = LayerValuePolicy.baseEff(layer.anchors, layer.supportDays)
                    val extraFinal = LayerValuePolicy.extraFinal(
                        layer.anchors,
                        layer.supportDays,
                        layer.nTasksToday,
                    )
                    Term(
                        kind = Term.Kind.ANCHOR,
                        value = baseEff,
                        weight = LayerWeightPolicy.votes(layer.anchors.size),
                        extraFinal = extraFinal,
                        optIn = layer.optIn,
                        layerId = layer.layerId,
                    )
                }

                layer.supportDays != null -> {
                    val g = LayerValuePolicy.supportSignal(layer.supportDays)
                    Term(
                        kind = Term.Kind.SUPPORT,
                        value = g,
                        weight = LayerWeightPolicy.votes(0),
                        extraFinal = 0.0,
                        optIn = layer.optIn,
                        layerId = layer.layerId,
                    )
                }

                layer.optIn != null -> Term(
                    kind = Term.Kind.OPT_IN,
                    value = layer.optIn,
                    weight = ScoringConstants.W0,
                    extraFinal = 0.0,
                    optIn = null, // capa solo-opt-in entra como término propio, sin sombra extra
                    layerId = layer.layerId,
                )

                else -> null
            }
        }
        if (terms.isEmpty()) return Aggregation(estado = 0.0, layerResults = emptyList())

        val layerResults = terms.map {
            LayerResult(layerId = it.layerId, baseEff = it.value, extra = it.extraFinal, weight = it.weight)
        }

        // Σpesos = suma de pesos de los términos de capa (NO de las sombras).
        val sigma = terms.sumOf { it.weight }

        // Bolsa-global = términos de capa + términos-sombra de opt-ins descuidados (solo anclas).
        var weightedSum = terms.sumOf { it.value * it.weight }
        var weightSum = sigma
        for (term in terms) {
            if (term.kind == Term.Kind.ANCHOR && term.optIn != null) {
                val w = OptInPolicy.shadowTerm(term.optIn, sigma)
                if (w > 1e-12) {
                    weightedSum += term.optIn * w
                    weightSum += w
                }
            }
        }
        val baseGlobal = weightedSum / weightSum

        // Extra PLANO: promedio simple de extra_final sobre las capas con anclas.
        val extrasAnchors = terms.filter { it.kind == Term.Kind.ANCHOR }.map { it.extraFinal }
        val extraGlobal = if (extrasAnchors.isEmpty()) 0.0 else extrasAnchors.average()

        return Aggregation(
            estado = min(baseGlobal, 1.0) + extraGlobal,
            layerResults = layerResults,
        )
    }
}
