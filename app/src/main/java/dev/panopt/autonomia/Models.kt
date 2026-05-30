package dev.panopt.autonomia

import java.time.LocalDate

data class Layer(
    val id: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val active: Boolean = true,
)

data class ActivityLog(
    val activityId: String,
    val date: String,
    val completed: Boolean,
    val actualValue: Int? = null,
    val note: String = "",
    val updatedAt: Long = 0L,
    val status: DailyActivityStatus? = null,
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

/**
 * Sleep night header (v12+). PK conceptual = nightDate (date of waking up).
 * Replaces SleepLog in the data and scoring layers. SleepQuality removed (bug §10).
 * Sub-scores are cached (recalculable from segments) and null when confidence = NoData.
 */
data class SleepNight(
    val nightDate: String,               // ISO yyyy-MM-dd, day the user woke up
    val targetSleepAt: String,
    val targetWakeAt: String,
    val sleepOnsetAt: Long?,             // epoch millis; null if NoData
    val definitiveWakeAt: Long?,
    val confidenceLevel: String,         // SleepConfidence.name
    val durationScore: Float?,
    val continuityScore: Float?,
    val alignmentScore: Float?,
    val digitalInterruptionScore: Float?,
    val sleepScore: Float?,              // null when NoData — do NOT coerce to 0
    val note: String = "",
    val source: String,                  // "auto" | "manual"
    val updatedAt: Long = 0L,
)

// SleepLog kept for UI legacy references only. Do NOT use in scoring (bug §10).
// Will be removed once all UI references are updated to SleepNight.
@Deprecated("Use SleepNight (v12+). SleepLog maps to the dropped sleep_logs table.")
data class SleepLog(
    val date: String,
    val plannedSleepAt: String,
    val plannedWakeAt: String,
    val sleptAt: String,
    val wokeAt: String,
    val quality: SleepQuality,
    val note: String = "",
    val updatedAt: Long = 0L,
)

data class SleepConfig(
    val id: String = "default",
    val targetSleepAt: String,
    val targetWakeAt: String,
    val digitalWindDownMinutes: Int,
    val updatedAt: Long = 0L,
)

data class SleepSessionState(
    val id: String = "default",
    val date: String,
    val startedAt: String,
    val updatedAt: Long = 0L,
)

// -- Enums --

enum class ScoreState(val uiAlias: String) {
    NoData("Sin datos"),
    Restoration("Restauración"),
    Attention("Atención"),
    Motion("En marcha"),
    Plenitude("Plenitud"),
    Unbreakable("Inquebrantable")
}

enum class ActivityType { Check, Time, Count, Note, TimeOfDay, SelfCare, AbstinenceSupport, Weekly }
enum class ActivityRole { Practice, SelfCare, Boundary, DigitalHygiene, DomesticOrder, AdministrativeOrder, RelationalHabit, ProjectWork, Learning, Custom }
enum class DisplaySurface { PrimaryChecklist, SecondaryChecklist, Available, Compact, Contextual, Silent }
enum class ActivitySurface { Anchor, Support, Task }
enum class DailyActivityStatus { Done, NotDone, Omitted }
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
enum class SleepQuality { Low, Acceptable, Good }

fun todayKey(): String = LocalDate.now().toString()
