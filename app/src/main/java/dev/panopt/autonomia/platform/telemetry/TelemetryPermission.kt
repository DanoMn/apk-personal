package dev.panopt.autonomia.platform.telemetry

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

/** Whether the user has granted usage-access to the app. */
enum class TelemetryPermissionState { GRANTED, MISSING }

/**
 * Helper for the `PACKAGE_USAGE_STATS` special permission. It is NOT a runtime
 * permission: the user grants it in Settings (Usage access). Without it, telemetry
 * must NOT crash — it stays a no-op and exposes [TelemetryPermissionState.MISSING]
 * so the consuming feature can show the right UX.
 */
object TelemetryPermission {

    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun state(context: Context): TelemetryPermissionState =
        if (isGranted(context)) TelemetryPermissionState.GRANTED else TelemetryPermissionState.MISSING

    /** Intent to the system "Usage access" settings screen where the user grants it. */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
