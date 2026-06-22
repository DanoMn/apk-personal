package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.domain.activity.AnchorGraceRule

object BuildScoreInputUseCase {
    operator fun invoke(source: ScoreInputSource): ScoreInput =
        ScoreInput(
            layers = source.layers
                .filter { it.active }
                .sortedBy { it.sortOrder },
            activities = source.activities
                .filter { it.active && !it.archived && it.activityType != ActivitySurface.Task }
                // GRACIA (FASE 2 §3.3): un ancla creada hace < 7 días no entra al puntaje (ni al
                // gate del mínimo ni al cálculo) hasta tener una ventana de historial suficiente.
                .filterNot {
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
