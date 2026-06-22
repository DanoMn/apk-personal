package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.domain.activity.AnchorGraceRule

object BuildScoreInputUseCase {
    /**
     * @param includeGraceAnchors si es `true`, las anclas dentro de su período de gracia NO se
     *   filtran (entran al gate y al cálculo). Lo usa SOLO la PROYECCIÓN de arranque
     *   ([StartupProjectionUseCase]) para puntuar una cuenta nueva con una ventana parcial. El
     *   camino maduro usa el default `false` → comportamiento byte-idéntico (filtra la gracia).
     */
    operator fun invoke(source: ScoreInputSource, includeGraceAnchors: Boolean = false): ScoreInput =
        ScoreInput(
            layers = source.layers
                .filter { it.active }
                .sortedBy { it.sortOrder },
            activities = source.activities
                .filter { it.active && !it.archived && it.activityType != ActivitySurface.Task }
                // GRACIA (FASE 2 §3.3): un ancla creada hace < 7 días no entra al puntaje (ni al
                // gate del mínimo ni al cálculo) hasta tener una ventana de historial suficiente.
                // EXCEPCIÓN (arranque): includeGraceAnchors=true deja entrar las anclas en gracia
                // para que la proyección de arranque puntúe la cuenta nueva (ventana parcial).
                .filterNot {
                    !includeGraceAnchors &&
                        it.activityType == ActivitySurface.Anchor &&
                        AnchorGraceRule.isWithinGrace(it.createdAt, source.today)
                }
                .sortedBy { it.sortOrder },
            todayActivityLogs = source.todayActivityLogs,
            periodActivityLogs = source.periodActivityLogs,
            abstinenceTracks = source.abstinenceTracks
                .filter { it.active }
                .sortedBy { it.sortOrder },
            todayAbstinenceLogs = source.todayAbstinenceLogs,
            allAbstinenceLogs = source.allAbstinenceLogs,
            tasks = source.tasks,
            sleepNights = source.sleepNights,
            today = source.today,
            weeklyHistory = source.weeklyHistory,
            targetVersions = source.targetVersions,
        )
}
