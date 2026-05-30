package dev.panopt.autonomia.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.panopt.autonomia.app.AppGraph
import java.time.LocalDate
import java.time.ZoneId

class DailyClosureWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result =
        runCatching {
            val zoneId = ZoneId.systemDefault()
            val today = LocalDate.now(zoneId)
            val repository = AppGraph.autonomiaRepository(applicationContext)

            repository.ensureSeeded()
            repository.materializeAssumedAbstinenceRelapses(today = today, zoneId = zoneId)
            repository.closeElapsedActivityDays(
                today = today,
                zoneId = zoneId,
                source = DailyClosureWorkScheduler.WORK_SOURCE,
            )
            // WU-6: materialize sleep night BEFORE the weekly score snapshot
            // so that the scored segments are available before telemetry purge.
            repository.materializeSleepNight(nightDate = today, zoneId = zoneId)
            repository.refreshCurrentWeeklyScoreSnapshot(today = today)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
}
