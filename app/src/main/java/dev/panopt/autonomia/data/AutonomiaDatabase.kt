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
        TaskEntity::class,
        AnchorPhraseEntity::class,
        AnchorPhraseStateRuleEntity::class,
        AnchorPhrasePhaseRuleEntity::class,
        AnchorPhraseImpressionEntity::class,
        AnchorPhraseDailySlotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AutonomiaDatabase : RoomDatabase() {
    abstract fun autonomiaDao(): AutonomiaDao

    companion object {
        @Volatile
        private var INSTANCE: AutonomiaDatabase? = null

        fun getInstance(context: Context): AutonomiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AutonomiaDatabase::class.java,
                    "autonomia_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
