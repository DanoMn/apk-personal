package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ScoreState

/**
 * Caso de uso de PROYECCIÓN de arranque (`scoring-arranque-cuenta`, Lote 2). Dominio JVM puro
 * (orquesta el motor, pero sin Room ni IO).
 *
 * Corre el motor de scoring con DOS diferencias frente al camino maduro:
 * 1. `windowDays = daysLived` → ventana parcial justa (no castiga días no vividos, NIVEL 1).
 * 2. `includeGraceAnchors = true` → las anclas en gracia NO se filtran (la cuenta nueva puntúa).
 *
 * Es read-only sobre los hechos: NO persiste nada ni muta el `ScoreReport` real (que el camino
 * maduro sigue produciendo `NoData`). Devuelve el ESTADO proyectado, o `null` si ni con la gracia
 * incluida se alcanza el gate mínimo (no hay proyección posible).
 */
object StartupProjectionUseCase {

    /**
     * @param source hechos crudos de la cuenta.
     * @param windowDays `d ∈ [1, 7]` días vividos (lo provee la proyección del dashboard).
     * @return [StartupProjection] con el ESTADO proyectado y el `d` usado, o `null` si no aplica.
     */
    operator fun invoke(source: ScoreInputSource, windowDays: Int): StartupProjection? {
        val input = BuildScoreInputUseCase(source, includeGraceAnchors = true)
        val report = ScoreEngine.calculateProjection(input, windowDays)
        if (report.state == ScoreState.NoData) return null
        return StartupProjection(estado = report.estado.toDouble(), windowDays = windowDays)
    }
}

/** ESTADO proyectado de una cuenta en arranque y la ventana (`d` días) con la que se calculó. */
data class StartupProjection(
    val estado: Double,
    val windowDays: Int,
)
