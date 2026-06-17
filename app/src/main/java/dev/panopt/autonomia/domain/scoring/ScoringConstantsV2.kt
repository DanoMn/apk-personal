package dev.panopt.autonomia.domain.scoring

/**
 * Constantes calibradas del modelo de scoring de núcleo v1 (los 17 parámetros de §0.1 de
 * `docs/scoring/modelo-matematico-nucleo-v1.md`). Coexiste con [ScoringConstants] (modelo viejo)
 * hasta el recableado de PR-F; el motor nuevo (NIVELES 1–7) lee SIEMPRE de aquí, NUNCA hardcodea.
 *
 * Cálculo interno en [Double]. La fuente de verdad numérica es
 * `docs/scoring/verificacion_modelo_oficial.py` (función `R(...)` y asserts AN/PC/AG/...).
 */
internal object ScoringConstantsV2 {
    // --- NIVEL 1: ancla R(F, T, mins) ---
    /** γ — mata trivialidad del valor-día (un día cortísimo casi no cuenta). */
    const val G_ = 1.5
    /** λ_v — fuerza reparadora del voluntario sobre el déficit. */
    const val LV = 0.5
    /** κ — desplazamiento tiempo↔días con F en el peso del superhabit. */
    const val KP = 1.5
    /** p — dureza del gate "base completa"; exponente del gate base^p (NUNCA el literal 2). */
    const val P = 2.0
    /** smax — techo del superhabit por ancla (R ≤ 1 + smax = 1.5). */
    const val SMAX = 0.5
    /** s0 — escala de saturación del superhabit. */
    const val S0 = 0.5

    // --- NIVEL 2: soportes / tasks ---
    /** WS — peso del blend del soporte en la base efectiva. */
    const val WS = 0.07
    /** TAU — techo del aporte de tasks por capa. */
    const val TAU = 0.06
    /** N0 — saturación por conteo de tasks. */
    const val N0 = 1.0

    // --- NIVEL 3: peso de capa (votos) ---
    /** r — decrecimiento del voto por ancla en `votes(n) = (1 − r^n)/(1 − r)`. */
    const val RG = 0.5
    /** ρ — peso de una capa solo-soportes (n = 0). */
    const val RHO = 0.15
    /** W0 — peso base: 1 ancla, o una capa solo-opt-in. */
    const val W0 = 1.0

    // --- NIVEL 4: opt-ins (sueño / sobriedad) ---
    /** BETA — intensidad del término-sombra (despejado de TARGET = 0.55). */
    const val BETA = 0.818
    /** A — golpe por día de recaída (sobriedad). */
    const val A = 0.55
    /** B_SLEEP — señal de sueño sin dato. */
    const val B_SLEEP = 0.5

    // --- NIVEL 6: bandas ---
    /** δ — margen de Inquebrantable (1 + δ). */
    const val DELTA = 0.10

    /** Cortes de banda (límite inferior inclusivo / superior exclusivo). */
    const val BAND_ATTENTION = 0.40
    const val BAND_MOTION = 0.62
    const val BAND_PLENITUDE = 0.85
    /** Inquebrantable = 1 + δ = 1.10. */
    const val BAND_UNBREAKABLE = 1.0 + DELTA

    // --- NIVEL 7: mapeo E ESTADO → PUNTOS ---
    /** Piso/tope de puntos del dashboard. */
    const val POINTS_FLOOR = 650.0
    const val POINTS_CEILING = 1100.0

    /** Hitos `(c, w, A)` del mapeo sigmoide E (NIVEL 7). */
    val POINTS_MILESTONES: List<Triple<Double, Double, Double>> = listOf(
        Triple(0.18, 0.10, 60.0),
        Triple(0.55, 0.11, 110.0),
        Triple(0.83, 0.09, 100.0),
        Triple(1.07, 0.055, 130.0),
        Triple(1.35, 0.13, 50.0),
    )
}
