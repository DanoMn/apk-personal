package dev.panopt.autonomia

import java.time.LocalDate

data class Layer(
    val id: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val active: Boolean = true,
)

data class TrackedActivity(
    val id: String,
    val layerId: String,
    val name: String,
    val description: String,
    val type: ActivityType,
    val role: ActivityRole,
    val displaySurface: DisplaySurface,
    val contributionRole: ContributionRole,
    val importanceTier: ImportanceTier,
    val cadence: ActivityCadence? = null,
    val targetValue: Int? = null,
    val minimumValue: Int? = null,
    val targetCount: Int? = null,
    val targetPeriod: TargetPeriod? = null,
    val unit: ActivityUnit,
    val active: Boolean = true,
    val archived: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

data class ActivityLog(
    val activityId: String,
    val date: String,
    val completed: Boolean,
    val actualValue: Int? = null,
    val note: String = "",
    val updatedAt: Long = 0L,
)

data class AbstinenceTrack(
    val id: String,
    val name: String,
    val substanceLabel: String,
    val severity: AbstinenceSeverity,
    val contributionRole: ContributionRole,
    val importanceTier: ImportanceTier,
    val active: Boolean = true,
    val sortOrder: Int,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

data class AbstinenceLog(
    val trackId: String,
    val date: String,
    val status: AbstinenceStatus,
    val urge: Boolean = false,
    val urgeIntensity: Int = 0,
    val note: String = "",
    val updatedAt: Long = 0L,
)

data class RiskEvent(
    val id: String,
    val date: String,
    val createdAt: Long,
    val intensity: Int,
    val trigger: String,
    val actionTaken: String,
    val actedOnImpulse: Boolean,
    val note: String,
)

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val layerId: String?,
    val projectId: String?,
    val status: TaskStatus,
    val contributionRole: ContributionRole,
    val importanceTier: ImportanceTier,
    val dueDate: String?,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class AnchorPhrase(
    val id: String,
    val text: String,
    val authorReference: String?,
    val family: PhraseFamily,
    val language: String,
    val attributionStatus: AttributionStatus,
    val active: Boolean,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

// -- Enums --

enum class ScoreState(val uiAlias: String) {
    NoData("Sin datos"),
    Restoration("Recuperación"),
    Attention("Bajo movimiento"),
    Motion("En marcha"),
    Plenitude("Estable"),
    Unbreakable("Inquebrantable")
}

enum class ActivityType { Check, Time, Count, Note, TimeOfDay, SelfCare, AbstinenceSupport, Weekly }
enum class ActivityRole { Practice, SelfCare, Boundary, DigitalHygiene, DomesticOrder, RelationalHabit, ProjectWork, Learning, Custom }
enum class DisplaySurface { PrimaryChecklist, SecondaryChecklist, Compact, Contextual, Silent }
enum class ContributionRole { Core, Support, Protective, Recovery, Neutral }
enum class ImportanceTier { Low, Medium, High, Critical }
enum class ActivityCadence { Daily, Weekly, Monthly, Custom, EventBased }
enum class TargetPeriod { Day, Week, Month }
enum class ActivityUnit { Minutes, Count, Boolean, Time, Text }
enum class AbstinenceSeverity { Critical, Moderate }
enum class AbstinenceStatus { Unknown, Clean, Relapse }
enum class TaskStatus { Pending, Done, Archived }
enum class PhraseFamily { Containment, MinimalAction, RegulationClarity, Persistence, IdentityValues, Recognition, Contemplation }
enum class AttributionStatus { Clear, Traditional, Disputed, NeedsReview }

fun todayKey(): String = LocalDate.now().toString()
