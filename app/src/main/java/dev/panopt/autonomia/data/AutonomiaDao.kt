package dev.panopt.autonomia.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AutonomiaDao {
    @Query("SELECT COUNT(*) FROM layers")
    suspend fun layerCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLayers(layers: List<LayerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivities(activities: List<ActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAbstinenceTracks(tracks: List<AbstinenceTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivityLog(log: ActivityLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAbstinenceLog(log: AbstinenceLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRiskEvent(event: RiskEventEntity)

    @Query("DELETE FROM activity_logs WHERE activityId = :activityId AND date = :date")
    suspend fun deleteActivityLog(activityId: String, date: String)

    @Query("SELECT * FROM layers WHERE active = 1 ORDER BY sortOrder ASC")
    fun observeLayers(): Flow<List<LayerEntity>>

    @Query("SELECT * FROM activities WHERE active = 1 ORDER BY sortOrder ASC")
    fun observeActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activity_logs WHERE date = :date")
    fun observeActivityLogsForDate(date: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM abstinence_tracks WHERE active = 1 ORDER BY sortOrder ASC")
    fun observeAbstinenceTracks(): Flow<List<AbstinenceTrackEntity>>

    @Query("SELECT * FROM abstinence_logs WHERE date = :date")
    fun observeAbstinenceLogsForDate(date: String): Flow<List<AbstinenceLogEntity>>

    @Query("SELECT * FROM activity_logs")
    fun observeAllActivityLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM abstinence_logs")
    fun observeAllAbstinenceLogs(): Flow<List<AbstinenceLogEntity>>

    @Query("SELECT * FROM risk_events WHERE date = :date ORDER BY createdAt DESC")
    fun observeRiskEventsForDate(date: String): Flow<List<RiskEventEntity>>

    @Query("SELECT * FROM activity_logs WHERE date BETWEEN :startDate AND :endDate")
    fun observeActivityLogsBetween(startDate: String, endDate: String): Flow<List<ActivityLogEntity>>
}
