package dev.panopt.autonomia.app

import android.content.Context
import dev.panopt.autonomia.AutonomiaRepository

object AppGraph {
    fun autonomiaRepository(context: Context): AutonomiaRepository =
        AutonomiaRepository(context.applicationContext)
}
