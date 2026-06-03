package dev.panopt.autonomia.data.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.panopt.autonomia.domain.closure.WindDownSchedulePolicy
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Programs or cancels the daily wind-down reminder (Notification A).
 *
 * Follows the pattern of [DailyClosureWorkScheduler]: a stateless scheduler
 * that creates a unique periodic work request and delegates delivery to
 * [WindDownNotificationWorker].
 *
 * Work is uniquely keyed so calling [schedule] again replaces the previous
 * request (e.g., when the user changes their [targetSleepAt] time).
 */
object WindDownNotificationScheduler {

    private const val UNIQUE_WORK_NAME = "wind_down_reminder"

    fun schedule(context: Context, targetSleepAt: String, zoneId: ZoneId = ZoneId.systemDefault()) {
        val now = ZonedDateTime.now(zoneId)
        val delayMillis = WindDownSchedulePolicy.initialDelay(now, targetSleepAt).toMillis()

        val request = PeriodicWorkRequestBuilder<WindDownNotificationWorker>(
            1L,
            TimeUnit.DAYS,
        )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request,
            )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}
