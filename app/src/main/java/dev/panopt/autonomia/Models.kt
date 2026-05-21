package dev.panopt.autonomia

import java.time.LocalDate

data class Layer(
    val id: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
)

data class TrackedActivity(
    val id: String,
    val layerId: String,
    val name: String,
    val description: String,
    val type: ActivityType,
    val targetValue: Int,
    val minimumValue: Int,
    val unit: ActivityUnit,
    val weeklyTarget: Int,
    val importance: Int,
    val active: Boolean,
    val sortOrder: Int,
)

data class ActivityLog(
    val activityId: String,
    val date: String,
    val completed: Boolean,
    val actualValue: Int,
    val note: String = "",
)

data class AbstinenceTrack(
    val id: String,
    val name: String,
    val substanceLabel: String,
    val severity: AbstinenceSeverity,
    val active: Boolean,
    val sortOrder: Int,
)

data class AbstinenceLog(
    val trackId: String,
    val date: String,
    val status: AbstinenceStatus,
    val urge: Boolean = false,
    val urgeIntensity: Int = 0,
    val note: String = "",
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

data class DashboardState(
    val today: String,
    val globalState: GlobalState,
    val globalMessage: String,
    val dimensions: List<DashboardDimension>,
    val layers: List<Layer>,
    val activities: List<TrackedActivity>,
    val activityLogsToday: Map<String, ActivityLog>,
    val abstinenceTracks: List<AbstinenceTrack>,
    val abstinenceLogsToday: Map<String, AbstinenceLog>,
    val weeklyGymDone: Int,
    val riskEventsToday: Int,
)

data class DashboardDimension(
    val name: String,
    val status: DimensionStatus,
    val message: String,
)

enum class ActivityType {
    Time,
    Check,
    SelfCare,
    AbstinenceSupport,
    Weekly,
    TimeOfDay,
    Note,
}

enum class ActivityUnit {
    Minutes,
    Count,
    Boolean,
    Time,
    Text,
}

enum class AbstinenceSeverity {
    Critical,
    Moderate,
}

enum class AbstinenceStatus {
    Unknown,
    Clean,
    Relapse,
}

enum class GlobalState(
    val label: String,
) {
    NoData("sin datos"),
    InMotion("en marcha"),
    Stable("base estable"),
    LowMotion("base baja"),
    Risk("riesgo"),
    Crisis("crisis"),
    Recovery("recuperacion"),
}

enum class DimensionStatus(
    val label: String,
) {
    Stable("estable"),
    InMotion("en marcha"),
    Low("bajo"),
    Alert("alerta"),
    Unknown("sin datos"),
}

fun todayKey(): String = LocalDate.now().toString()
