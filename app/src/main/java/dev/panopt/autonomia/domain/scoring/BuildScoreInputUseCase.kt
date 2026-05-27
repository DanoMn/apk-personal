package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivitySurface

object BuildScoreInputUseCase {
    operator fun invoke(source: ScoreInputSource): ScoreInput =
        ScoreInput(
            layers = source.layers
                .filter { it.active }
                .sortedBy { it.sortOrder },
            activities = source.activities
                .filter { it.active && !it.archived && it.activityType != ActivitySurface.Task }
                .sortedBy { it.sortOrder },
            todayActivityLogs = source.todayActivityLogs,
            periodActivityLogs = source.periodActivityLogs,
            abstinenceTracks = source.abstinenceTracks
                .filter { it.active }
                .sortedBy { it.sortOrder },
            todayAbstinenceLogs = source.todayAbstinenceLogs,
            allAbstinenceLogs = source.allAbstinenceLogs,
            tasks = source.tasks,
            sleepLog = source.sleepLog,
            today = source.today,
        )
}
