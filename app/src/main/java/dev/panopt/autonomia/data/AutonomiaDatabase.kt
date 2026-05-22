package dev.panopt.autonomia.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        AnchorPhraseDailySlotEntity::class,
        SleepLogEntity::class
    ],
    version = 3,
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
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sleep_logs (
                        date TEXT NOT NULL PRIMARY KEY,
                        plannedSleepAt TEXT NOT NULL,
                        plannedWakeAt TEXT NOT NULL,
                        sleptAt TEXT NOT NULL,
                        wokeAt TEXT NOT NULL,
                        quality TEXT NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE abstinence_tracks
                    SET active = 0
                    WHERE id IN ('trk_alcohol', 'trk_sexual', 'trk_marihuana')
                    """.trimIndent(),
                )
            }
        }
    }
}
