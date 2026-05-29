package dev.panopt.autonomia.app

import android.content.Context
import dev.panopt.autonomia.AutonomiaRepository
import dev.panopt.autonomia.data.repository.TelemetryRepository

object AppGraph {
    fun autonomiaRepository(context: Context): AutonomiaRepository =
        AutonomiaRepository(context.applicationContext)

    fun telemetryRepository(context: Context): TelemetryRepository =
        TelemetryRepository(context.applicationContext)
}
