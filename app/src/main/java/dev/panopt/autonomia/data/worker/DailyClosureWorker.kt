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
            repository.closeElapsedActivityDays(
                today = today,
                zoneId = zoneId,
                source = DailyClosureWorkScheduler.WORK_SOURCE,
            )
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
}
