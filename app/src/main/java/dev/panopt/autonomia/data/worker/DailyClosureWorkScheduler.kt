package dev.panopt.autonomia.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.panopt.autonomia.domain.closure.DailyClosureSchedulePolicy
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object DailyClosureWorkScheduler {
    const val WORK_SOURCE = "work_manager"

    private const val UNIQUE_WORK_NAME = "daily_activity_closure"
    private const val REPEAT_INTERVAL_DAYS = 1L

    fun schedule(context: Context, zoneId: ZoneId = ZoneId.systemDefault()) {
        val now = ZonedDateTime.now(zoneId)
        val delayMillis = DailyClosureSchedulePolicy.initialDelay(now).toMillis()
        val request = PeriodicWorkRequestBuilder<DailyClosureWorker>(
            REPEAT_INTERVAL_DAYS,
            TimeUnit.DAYS,
        )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }
}
