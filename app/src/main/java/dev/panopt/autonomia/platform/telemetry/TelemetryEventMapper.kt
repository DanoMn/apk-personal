package dev.panopt.autonomia.platform.telemetry

/**
 * Pure mapper from Android `UsageEvents.Event` type codes to the generic
 * [DeviceActivityEventType]. Kept free of `android.*` imports so it is unit-testable
 * on the JVM. The integer codes mirror `android.app.usage.UsageEvents.Event` constants.
 *
 * minSdk 26 note: the SCREEN_* / KEYGUARD_* codes are only emitted on API 28+. On API
 * 26/27 the OS simply never feeds those codes, so no special handling is needed — the
 * mapper maps whatever code it receives; the fallback is emergent (app/interaction
 * events still map on all supported API levels).
 */
object TelemetryEventMapper {

    // Mirrors android.app.usage.UsageEvents.Event.* (stable public constants).
    private const val ACTIVITY_RESUMED = 1
    private const val ACTIVITY_PAUSED = 2
    private const val USER_INTERACTION = 7
    private const val SCREEN_INTERACTIVE = 15 // API 28+
    private const val SCREEN_NON_INTERACTIVE = 16 // API 28+
    private const val KEYGUARD_SHOWN = 17 // API 28+
    private const val KEYGUARD_HIDDEN = 18 // API 28+

    fun map(typeCode: Int, packageName: String?, timestamp: Long): DeviceActivityEvent? {
        val type = when (typeCode) {
            ACTIVITY_RESUMED -> DeviceActivityEventType.APP_FOREGROUND
            ACTIVITY_PAUSED -> DeviceActivityEventType.APP_BACKGROUND
            USER_INTERACTION -> DeviceActivityEventType.USER_INTERACTION
            SCREEN_INTERACTIVE -> DeviceActivityEventType.SCREEN_ON
            SCREEN_NON_INTERACTIVE -> DeviceActivityEventType.SCREEN_OFF
            KEYGUARD_HIDDEN -> DeviceActivityEventType.UNLOCK
            KEYGUARD_SHOWN -> DeviceActivityEventType.LOCK
            else -> return null
        }
        val keepsPackage = type == DeviceActivityEventType.APP_FOREGROUND ||
            type == DeviceActivityEventType.APP_BACKGROUND
        return DeviceActivityEvent(
            eventType = type,
            packageName = if (keepsPackage) packageName else null,
            timestamp = timestamp,
            source = "usage_event:$typeCode",
        )
    }
}
