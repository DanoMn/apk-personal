package dev.panopt.autonomia.data.repository

import android.content.Context
import dev.panopt.autonomia.data.AutonomiaDatabase
import dev.panopt.autonomia.data.DeviceActivityEventEntity
import dev.panopt.autonomia.platform.telemetry.DeviceActivityEvent
import dev.panopt.autonomia.platform.telemetry.DeviceActivityEventType
import dev.panopt.autonomia.platform.telemetry.TelemetryPermission
import dev.panopt.autonomia.platform.telemetry.TelemetryPermissionState

/**
 * PULL read access to raw device telemetry facts. Consumers (e.g. Sleep) read facts
 * by time range through this repository and NEVER touch the platform API. Telemetry
 * stays blind to who reads it.
 */
class TelemetryRepository(context: Context) {

    private val dao = AutonomiaDatabase.getInstance(context.applicationContext).autonomiaDao()
    private val appContext = context.applicationContext

    suspend fun eventsInRange(from: Long, to: Long): List<DeviceActivityEvent> =
        dao.getDeviceActivityEventsInRange(from, to).map { it.toDomain() }

    fun permissionState(): TelemetryPermissionState = TelemetryPermission.state(appContext)

    private fun DeviceActivityEventEntity.toDomain(): DeviceActivityEvent =
        DeviceActivityEvent(
            eventType = DeviceActivityEventType.valueOf(eventType),
            packageName = packageName,
            timestamp = timestamp,
            source = source,
        )
}
