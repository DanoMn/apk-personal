package dev.panopt.autonomia.data

import androidx.room.Entity
import androidx.room.ForeignKey
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
    val role: String,
    val displaySurface: String,
    val contributionRole: String,
    val importanceTier: String,
    val cadence: String?,
    val targetValue: Int?,
    val minimumValue: Int?,
    val targetCount: Int?,
    val targetPeriod: String?,
    val unit: String,
    val active: Boolean = true,
    val archived: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
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
    val actualValue: Int?,
    val note: String = "",
    val updatedAt: Long,
)

@Entity(tableName = "abstinence_tracks")
data class AbstinenceTrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val substanceLabel: String,
    val severity: String,
    val contributionRole: String,
    val importanceTier: String,
    val active: Boolean = true,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
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

@Entity(
    tableName = "tasks",
    indices = [Index("layerId")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val layerId: String?,
    val projectId: String?,
    val status: String,
    val contributionRole: String,
    val importanceTier: String,
    val dueDate: String?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "anchor_phrases")
data class AnchorPhraseEntity(
    @PrimaryKey val id: String,
    val text: String,
    val authorReference: String?,
    val family: String,
    val language: String,
    val attributionStatus: String,
    val active: Boolean = true,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "anchor_phrase_state_rules",
    primaryKeys = ["phraseId", "scoreState"]
)
data class AnchorPhraseStateRuleEntity(
    val phraseId: String,
    val scoreState: String,
    val weight: Int,
)

@Entity(
    tableName = "anchor_phrase_phase_rules",
    primaryKeys = ["phraseId", "dayPhase"]
)
data class AnchorPhrasePhaseRuleEntity(
    val phraseId: String,
    val dayPhase: String,
    val weight: Int,
)

@Entity(
    tableName = "anchor_phrase_impressions",
    indices = [Index("date", "dayPhase"), Index("phraseId", "shownAt")]
)
data class AnchorPhraseImpressionEntity(
    @PrimaryKey val id: String,
    val phraseId: String,
    val date: String,
    val dayPhase: String,
    val scoreState: String,
    val shownAt: Long,
)

@Entity(
    tableName = "anchor_phrase_daily_slots",
    primaryKeys = ["date", "dayPhase"]
)
data class AnchorPhraseDailySlotEntity(
    val date: String,
    val dayPhase: String,
    val scoreState: String,
    val phraseId: String,
    val resolvedAt: Long,
)

@Entity(
    tableName = "activity_definitions",
    indices = [Index("layerId")],
)
data class ActivityDefinitionEntity(
    @PrimaryKey val id: String,
    val layerId: String,
    val name: String,
    val description: String,
    val type: String,           // Check | Time | Count | Note | TimeOfDay
    val role: String,           // Practice | SelfCare | Boundary | DigitalHygiene | DomesticOrder | RelationalHabit | ProjectWork | Learning | Custom
    val unit: String,           // Minutes | Boolean | Count | Time | Text
    val contributionRole: String, // Core | Support | Protective | Recovery | Neutral
    val importanceTier: String, // Low | Medium | High | Critical
    val presetCategory: String?, // "anchor" | "support" | null (user-created)
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "user_activity_configs",
    foreignKeys = [ForeignKey(
        entity = ActivityDefinitionEntity::class,
        parentColumns = ["id"],
        childColumns = ["activityId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("activityId", unique = true)],
)
data class UserActivityConfigEntity(
    @PrimaryKey val activityId: String,  // FK → activity_definitions.id
    val activityType: String,            // "Anchor" | "Support" | "Task"
    val active: Boolean = true,
    val archived: Boolean = false,
    val customName: String? = null,
    val customDescription: String? = null,
    val cadence: String?,        // Weekly | Monthly (REQUIRED for Anchor, null otherwise)
    val targetValue: Int?,       // REQUIRED for Anchor
    val minimumValue: Int?,
    val targetCount: Int?,       // REQUIRED for Anchor
    val targetPeriod: String?,   // Week | Month (REQUIRED for Anchor)
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "sleep_logs")
data class SleepLogEntity(
    @PrimaryKey val date: String,
    val plannedSleepAt: String,
    val plannedWakeAt: String,
    val sleptAt: String,
    val wokeAt: String,
    val quality: String,
    val note: String = "",
    val updatedAt: Long,
)
