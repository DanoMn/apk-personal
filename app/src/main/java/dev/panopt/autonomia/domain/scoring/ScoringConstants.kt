package dev.panopt.autonomia.domain.scoring

/**
 * Constantes residuales del modelo viejo de scoring tras el recableado de PR-F. La mayoría de
 * los parámetros del modelo de pesos puros (NIVELES 1–7) vive en [ScoringConstantsV2]; aquí solo
 * quedan las constantes con consumidor vivo:
 * - los IDs de capa que el orquestador usa para cablear los opt-ins (sueño → Cuerpo, sobriedad →
 *   Conducta) y el gate de configuración mínima ([MIN_ACTIVE_LAYERS_WITH_ANCHOR]);
 * - los pesos de la mezcla promedio/peor que [StabilityScoringPolicy] (APARCADA/inerte) todavía
 *   referencia (la estabilidad multi-semana quedó fuera de alcance, pero el objeto compila).
 *
 * Las constantes del modelo viejo (worst-layer, histéresis, Inquebrantable gate, pesos
 * frecuencia/valor 0.70/0.30, sueño/sobriedad 30%, umbrales de banda 0.40/0.70/0.85) se ELIMINARON
 * en PR-F junto con sus policies.
 */
internal object ScoringConstants {
    const val BODY_LAYER_ID = "layer_cuerpo"
    const val CONDUCT_LAYER_ID = "layer_conducta"

    // Configuración mínima para emitir scoring (árbol §7.4): de las 5 capas,
    // mínimo 3 activas con al menos 1 ancla cada una. Si no se cumple → NoData.
    const val MIN_ACTIVE_LAYERS_WITH_ANCHOR = 3

    // Mezcla promedio/peor de la estabilidad multi-semana (StabilityScoringPolicy, inerte/aparcada).
    const val WEEKLY_AVERAGE_WEIGHT = 0.75f
    const val WEEKLY_WORST_WEIGHT = 0.25f
}
