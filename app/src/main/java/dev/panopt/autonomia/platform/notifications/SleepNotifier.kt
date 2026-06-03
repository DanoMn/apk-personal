package dev.panopt.autonomia.platform.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.panopt.autonomia.MainActivity
import dev.panopt.autonomia.R

/**
 * Builds and posts the two sleep notifications.
 *
 * Posting is a no-op if the caller has not been granted [android.Manifest.permission.POST_NOTIFICATIONS]
 * (system-level silencing — no crash, no side effects).
 */
object SleepNotifier {

    /** Stable notification ID for the wind-down reminder (Notif A). */
    const val NOTIF_ID_WIND_DOWN = 1001

    /** Stable notification ID for the sleep data alert (Notif B). */
    const val NOTIF_ID_DATA_ALERT = 1002

    /**
     * Posts Notification A — wind-down reminder.
     *
     * The caller is responsible for verifying the permission and the scheduling condition
     * before invoking this. This function only builds and posts; it never requests permission.
     */
    fun postWindDown(context: Context) {
        if (!PostNotificationsPermission.isGranted(context)) return
        val pendingIntent = openAppPendingIntent(context)
        val notification = NotificationCompat.Builder(context, SleepNotificationChannels.CHANNEL_WIND_DOWN)
            .setSmallIcon(R.drawable.ic_spiral_foreground)
            .setContentTitle(context.getString(R.string.notif_wind_down_title))
            .setContentText(context.getString(R.string.notif_wind_down_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        @Suppress("MissingPermission") // guarded by PostNotificationsPermission.isGranted above
        NotificationManagerCompat.from(context).notify(NOTIF_ID_WIND_DOWN, notification)
    }

    /**
     * Posts Notification B — sleep data alert.
     *
     * The caller is responsible for verifying the policy condition
     * (N consecutive NoData nights) before invoking this. Permission is
     * checked internally and the call is a no-op if not granted.
     */
    fun postDataAlert(context: Context) {
        if (!PostNotificationsPermission.isGranted(context)) return
        val pendingIntent = openAppPendingIntent(context)
        val notification = NotificationCompat.Builder(context, SleepNotificationChannels.CHANNEL_DATA_ALERT)
            .setSmallIcon(R.drawable.ic_spiral_foreground)
            .setContentTitle(context.getString(R.string.notif_data_alert_title))
            .setContentText(context.getString(R.string.notif_data_alert_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        @Suppress("MissingPermission") // guarded by PostNotificationsPermission.isGranted above
        NotificationManagerCompat.from(context).notify(NOTIF_ID_DATA_ALERT, notification)
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
