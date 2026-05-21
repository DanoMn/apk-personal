package dev.panopt.autonomia.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "layers")
data class LayerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val active: Boolean = true,
)

@Entity(
    tableName = "activities",
    indices = [Index("layerId")],
)
data class ActivityEntity(
    @PrimaryKey val id: String,
    val layerId: String,
    val name: String,
    val description: String,
    val type: String,
    val targetValue: Int,
    val minimumValue: Int,
    val unit: String,
    val weeklyTarget: Int = 0,
    val importance: Int = 1,
    val active: Boolean = true,
    val sortOrder: Int,
)

@Entity(
    tableName = "activity_logs",
    primaryKeys = ["activityId", "date"],
    indices = [Index("date")],
)
data class ActivityLogEntity(
    val activityId: String,
    val date: String,
    val completed: Boolean,
    val actualValue: Int,
    val note: String = "",
    val updatedAt: Long,
)

@Entity(tableName = "abstinence_tracks")
data class AbstinenceTrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val substanceLabel: String,
    val severity: String,
    val active: Boolean = true,
    val sortOrder: Int,
)

@Entity(
    tableName = "abstinence_logs",
    primaryKeys = ["trackId", "date"],
    indices = [Index("date")],
)
data class AbstinenceLogEntity(
    val trackId: String,
    val date: String,
    val status: String,
    val urge: Boolean = false,
    val urgeIntensity: Int = 0,
    val note: String = "",
    val updatedAt: Long,
)

@Entity(
    tableName = "risk_events",
    indices = [Index("date")],
)
data class RiskEventEntity(
    @PrimaryKey val id: String,
    val date: String,
    val createdAt: Long,
    val intensity: Int,
    val trigger: String,
    val actionTaken: String,
    val actedOnImpulse: Boolean,
    val note: String,
)
