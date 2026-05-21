package dev.panopt.autonomia.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LayerEntity::class,
        ActivityEntity::class,
        ActivityLogEntity::class,
        AbstinenceTrackEntity::class,
        AbstinenceLogEntity::class,
        RiskEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AutonomiaDatabase : RoomDatabase() {
    abstract fun autonomiaDao(): AutonomiaDao

    companion object {
        @Volatile
        private var instance: AutonomiaDatabase? = null

        fun getInstance(context: Context): AutonomiaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AutonomiaDatabase::class.java,
                    "autonomia.db",
                )
                    .build()
                    .also { instance = it }
            }
    }
}
