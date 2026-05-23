package dev.panopt.autonomia.domain.dashboard

import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.SleepQuality

internal data class DashboardState(
    val isLoading: Boolean = true,
    val status: DashboardStatusState = DashboardStatusState(),
    val dailyProgress: DashboardDailyProgressState = DashboardDailyProgressState(),
    val anchorPhrase: DashboardAnchorPhraseState = DashboardAnchorPhraseState(),
    val layers: List<DashboardLayerState> = emptyList(),
    val signals: List<DashboardSignalState> = emptyList(),
    val sobrietyTracks: List<DashboardSobrietyTrackState> = emptyList(),
    val checklistItems: List<DashboardChecklistItemState> = emptyList(),
    val supports: List<DashboardSupportState> = emptyList(),
    val weekRows: List<DashboardWeekRowState> = emptyList(),
    val dimensions: List<DashboardDimensionState> = emptyList(),
    val sleep: DashboardSleepState = DashboardSleepState(),
    val activityOptions: List<DashboardActivityOptionState> = emptyList(),
    val secondaryChecklistItems: List<DashboardChecklistItemState> = emptyList(),
    val pendingTasks: List<DashboardTaskState> = emptyList(),
)

internal data class DashboardStatusState(
    val scoreState: ScoreState = ScoreState.NoData,
    val title: String = "Sin datos",
    val headline: String = "Todavia no hay lectura suficiente.",
    val body: String = "Registra una accion minima para que el dia empiece a tener forma.",
    val score: Int = 0,
    val scoreLabel: String = "--",
    val progress: Float = 0f,
)

internal data class DashboardDailyProgressState(
    val percent: Int = 0,
    val progress: Float = 0f,
    val pendingLabel: String = "Sin registros",
    val activeLayersLabel: String = "0 de 0 capas activas",
)

internal data class DashboardAnchorPhraseState(
    val text: String = "Life can only be understood backwards; but it must be lived forwards.",
    val authorReference: String = "Soren Kierkegaard",
)

internal data class DashboardLayerState(
    val id: String,
    val name: String,
    val progress: Float,
)

internal data class DashboardSignalState(
    val kind: DashboardSignalKind,
    val label: String,
    val value: String,
    val meta: String,
    val status: DashboardDimensionStatus,
)

internal enum class DashboardSignalKind {
    Sleep,
    Project,
    Focus,
}

internal data class DashboardSobrietyTrackState(
    val id: String,
    val label: String,
    val days: Int,
    val meta: String,
    val status: DashboardDimensionStatus,
    val isRelapseToday: Boolean = false,
    val isMarkedCleanToday: Boolean,
)

internal data class DashboardChecklistItemState(
    val id: String,
    val title: String,
    val layerId: String,
    val layerName: String,
    val value: String,
    val completed: Boolean,
)

internal data class DashboardSupportState(
    val kind: DashboardSupportKind,
    val title: String,
    val value: String,
    val copy: String,
    val first: String,
    val firstChecked: Boolean,
    val second: String,
    val secondChecked: Boolean,
)

internal enum class DashboardSupportKind {
    SecondaryChecklist,
    Tasks,
}

internal data class DashboardWeekRowState(
    val layerId: String,
    val name: String,
    val score: String,
    val progress: Float,
)

internal data class DashboardDimensionState(
    val label: String,
    val value: String,
    val status: DashboardDimensionStatus,
)

internal enum class DashboardDimensionStatus {
    Stable,
    Motion,
    Attention,
    Restoration,
    Unknown,
}

internal data class DashboardSleepState(
    val plannedSleepAt: String = "23:30",
    val plannedWakeAt: String = "07:30",
    val sleptAt: String = "",
    val wokeAt: String = "",
    val quality: SleepQuality = SleepQuality.Acceptable,
    val note: String = "",
)

internal data class DashboardActivityOptionState(
    val id: String,
    val title: String,
    val layerId: String,
    val layerName: String,
    val targetValue: Int,
    val actualValue: Int,
    val isCompletedToday: Boolean,
    val isFocusSignal: Boolean,
    val displaySurface: String, // DEPRECATED — replaced by activityType
    val activityType: String = "", // NEW — replaces displaySurface
    val isGoal: Boolean = false,
)

internal data class DashboardTaskState(
    val id: String,
    val title: String,
    val layerId: String? = null,
)
