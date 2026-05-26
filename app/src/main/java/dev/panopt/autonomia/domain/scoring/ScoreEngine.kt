package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.activity.goalProgress
import dev.panopt.autonomia.domain.activity.importanceWeight
import dev.panopt.autonomia.domain.activity.isGoal
import dev.panopt.autonomia.domain.activity.progressFor
import dev.panopt.autonomia.domain.sleep.SleepScoring
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

data class ScoreInput(
    val layers: List<Layer>,
    val activities: List<ActivityDefinition>,
    val todayActivityLogs: List<ActivityLog>,
    val periodActivityLogs: List<ActivityLog>,
    val abstinenceTracks: List<AbstinenceTrack>,
    val todayAbstinenceLogs: List<AbstinenceLog>,
    val allAbstinenceLogs: List<AbstinenceLog>,
    val tasks: List<Task>,
    val sleepLog: SleepLog?,
    val today: LocalDate = LocalDate.now(),
)

data class ScoreReport(
    val state: ScoreState,
    val visibleScore: Int?,
    val baseScore: Int?,
    val goalBonus: Int,
    val progress: Float,
    val layerScores: List<LayerScore>,
    val featureContributions: List<FeatureContribution>,
    val gates: List<ScoreGate>,
)

data class LayerScore(
    val layerId: String,
    val name: String,
    val score: Float,
    val configured: Boolean,
)

data class FeatureContribution(
    val feature: ScoreFeature,
    val layerId: String?,
    val label: String,
    val value: Float,
    val maxValue: Float,
)

data class ScoreGate(
    val kind: ScoreGateKind,
    val active: Boolean,
    val maxScore: Int,
    val message: String,
)

enum class ScoreFeature {
    Anchor,
    Support,
    Task,
    Sleep,
    Sobriety,
    Goal,
}

enum class ScoreGateKind {
    SleepMissing,
    SleepLow,
    RelapseToday,
    CleanStreak,
    FoundationLayerLow,
    GoalMissing,
    GoalPartial,
}

object ScoreEngine {
    private const val BODY_LAYER_ID = "layer_cuerpo"
    private const val CONDUCT_LAYER_ID = "layer_conducta"
    private const val RESTORATION_MAX = 749
    private const val ATTENTION_MAX = 799
    private const val MOTION_MAX = 899
    private const val PLENITUDE_MAX = 949

    fun calculate(input: ScoreInput): ScoreReport {
        val activeLayers = input.layers.filter { it.active }.sortedBy { it.sortOrder }
        if (activeLayers.isEmpty() || !input.hasAnyFact()) {
            return ScoreReport(
                state = ScoreState.NoData,
                visibleScore = null,
                baseScore = null,
                goalBonus = 0,
                progress = 0f,
                layerScores = activeLayers.map {
                    LayerScore(layerId = it.id, name = it.name, score = 0f, configured = false)
                },
                featureContributions = emptyList(),
                gates = emptyList(),
            )
        }

        val visibleActivities = input.activities
            .filter { it.active && !it.archived }
            .sortedBy { it.sortOrder }
        val todayLogsByActivity = input.todayActivityLogs.associateBy { it.activityId }
        val periodLogsByActivity = input.periodActivityLogs.groupBy { it.activityId }
        val activeTracks = input.abstinenceTracks.filter { it.active }.sortedBy { it.sortOrder }
        val todayLogsByTrack = input.todayAbstinenceLogs.associateBy { it.trackId }
        val sleepScore = input.sleepLog?.let(SleepScoring::score)
        val sobrietyScore = scoreSobriety(activeTracks, todayLogsByTrack)
        val completedTasks = input.tasks.filter { it.isScoringTaskCompletedOn(input.today) }

        val contributions = mutableListOf<FeatureContribution>()
        val layerScores = activeLayers.map { layer ->
            val layerActivities = visibleActivities.filter { it.layerId == layer.id && !it.isGoal() }
            val primaryRatio = layerActivities
                .filter { it.activityType == ActivitySurface.Anchor }
                .weightedActivityRatio(todayLogsByActivity)
            val secondaryRatio = layerActivities
                .filter { it.activityType == ActivitySurface.Support }
                .weightedActivityRatio(todayLogsByActivity)
            val taskRatio = completedTasks
                .filter { it.layerId == layer.id }
                .weightedTaskRatio()
            val hasPrimary = layerActivities.any { it.activityType == ActivitySurface.Anchor }
            val hasSecondary = layerActivities.any { it.activityType == ActivitySurface.Support }
            val hasTasks = input.tasks.any { it.layerId == layer.id && it.contributionRole != ContributionRole.Neutral }
            val hasSleep = layer.id == BODY_LAYER_ID && sleepScore != null
            val hasSobriety = layer.id == CONDUCT_LAYER_ID && activeTracks.isNotEmpty()

            val score = when (layer.id) {
                BODY_LAYER_ID -> {
                    val primary = primaryRatio * 0.40f
                    val secondary = secondaryRatio * 0.10f
                    val tasks = taskRatio * 0.05f
                    val sleep = (sleepScore ?: 0f) * 0.45f
                    addContribution(contributions, ScoreFeature.Anchor, layer, "Anclas", primary, 0.40f, hasPrimary)
                    addContribution(contributions, ScoreFeature.Support, layer, "Soportes", secondary, 0.10f, hasSecondary)
                    addContribution(contributions, ScoreFeature.Task, layer, "Pendientes", tasks, 0.05f, hasTasks)
                    addContribution(contributions, ScoreFeature.Sleep, layer, "Sueno", sleep, 0.45f, hasSleep)
                    primary + secondary + tasks + sleep
                }
                CONDUCT_LAYER_ID -> {
                    val primary = primaryRatio * 0.40f
                    val secondary = secondaryRatio * 0.10f
                    val tasks = taskRatio * 0.08f
                    val sobriety = if (activeTracks.isEmpty()) {
                        0.42f
                    } else {
                        (sobrietyScore ?: 0f) * 0.42f
                    }
                    addContribution(contributions, ScoreFeature.Anchor, layer, "Anclas", primary, 0.40f, hasPrimary)
                    addContribution(contributions, ScoreFeature.Support, layer, "Soportes", secondary, 0.10f, hasSecondary)
                    addContribution(contributions, ScoreFeature.Task, layer, "Pendientes", tasks, 0.08f, hasTasks)
                    addContribution(contributions, ScoreFeature.Sobriety, layer, "Sobriedad", sobriety, 0.42f, hasSobriety)
                    primary + secondary + tasks + sobriety
                }
                else -> {
                    val primary = primaryRatio * 0.78f
                    val secondary = secondaryRatio * 0.14f
                    val tasks = taskRatio * 0.08f
                    addContribution(contributions, ScoreFeature.Anchor, layer, "Anclas", primary, 0.78f, hasPrimary)
                    addContribution(contributions, ScoreFeature.Support, layer, "Soportes", secondary, 0.14f, hasSecondary)
                    addContribution(contributions, ScoreFeature.Task, layer, "Pendientes", tasks, 0.08f, hasTasks)
                    primary + secondary + tasks
                }
            }

            val configured = hasPrimary || hasSecondary || hasTasks || hasSleep || hasSobriety
            LayerScore(
                layerId = layer.id,
                name = layer.name,
                score = if (configured) score.coerceIn(0f, 1f) else 0.50f,
                configured = configured,
            )
        }

        val layerAverage = layerScores.map { it.score }.averageOrZero()
        val baseScore = (700 + layerAverage * 200).roundToInt().coerceIn(700, 900)
        val goalBonus = calculateGoalBonus(
            activities = visibleActivities.filter { it.isGoal() },
            logsByActivity = periodLogsByActivity,
            today = input.today,
            contributions = contributions,
        )
        val rawScore = (baseScore + goalBonus).coerceIn(700, 1000)
        val gates = buildGates(
            sleepScore = sleepScore,
            layerScores = layerScores,
            activeTracks = activeTracks,
            todayLogsByTrack = todayLogsByTrack,
            allAbstinenceLogs = input.allAbstinenceLogs,
            today = input.today,
            goalBonus = goalBonus,
        )
        val maxAllowed = gates.filter { it.active }.minOfOrNull { it.maxScore } ?: 1000
        val visibleScore = rawScore.coerceAtMost(maxAllowed).coerceAtLeast(700)
        val state = visibleScore.toScoreState()

        return ScoreReport(
            state = state,
            visibleScore = visibleScore,
            baseScore = baseScore,
            goalBonus = goalBonus,
            progress = visibleScore / 1000f,
            layerScores = layerScores,
            featureContributions = contributions,
            gates = gates,
        )
    }

    private fun ScoreInput.hasAnyFact(): Boolean {
        val hasActivityFact = todayActivityLogs.isNotEmpty() || periodActivityLogs.isNotEmpty()
        val activeTrackIds = abstinenceTracks
            .filter { it.active }
            .map { it.id }
            .toSet()
        val hasAbstinenceFact = (todayAbstinenceLogs + allAbstinenceLogs).any { log ->
            log.trackId in activeTrackIds && log.status != AbstinenceStatus.Unknown
        }
        val hasTaskFact = tasks.any { it.status == TaskStatus.Done }
        return hasActivityFact || hasAbstinenceFact || hasTaskFact || sleepLog != null
    }

    private fun addContribution(
        contributions: MutableList<FeatureContribution>,
        feature: ScoreFeature,
        layer: Layer,
        label: String,
        value: Float,
        maxValue: Float,
        enabled: Boolean,
    ) {
        if (!enabled || maxValue <= 0f) return
        contributions += FeatureContribution(
            feature = feature,
            layerId = layer.id,
            label = label,
            value = value.coerceIn(0f, maxValue),
            maxValue = maxValue,
        )
    }

    private fun calculateGoalBonus(
        activities: List<ActivityDefinition>,
        logsByActivity: Map<String, List<ActivityLog>>,
        today: LocalDate,
        contributions: MutableList<FeatureContribution>,
    ): Int {
        if (activities.isEmpty()) return 0

        val progressValues = activities.map { activity ->
            val logs = logsByActivity[activity.id].orEmpty().filter { log ->
                val date = runCatching { LocalDate.parse(log.date) }.getOrNull() ?: return@filter false
                when (activity.targetPeriod) {
                    TargetPeriod.Month -> date.month == today.month && date.year == today.year
                    else -> date >= today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)) && date <= today
                }
            }
            activity.goalProgress(logs)
        }
        val ratio = progressValues.averageOrZero().coerceIn(0f, 1f)
        contributions += FeatureContribution(
            feature = ScoreFeature.Goal,
            layerId = null,
            label = "Metas de anclas",
            value = ratio,
            maxValue = 1f,
        )
        return (ratio * 100).roundToInt().coerceIn(0, 100)
    }

    private fun buildGates(
        sleepScore: Float?,
        layerScores: List<LayerScore>,
        activeTracks: List<AbstinenceTrack>,
        todayLogsByTrack: Map<String, AbstinenceLog>,
        allAbstinenceLogs: List<AbstinenceLog>,
        today: LocalDate,
        goalBonus: Int,
    ): List<ScoreGate> {
        val criticalRelapseToday = activeTracks.any { track ->
            track.severity == AbstinenceSeverity.Critical &&
                todayLogsByTrack[track.id]?.status == AbstinenceStatus.Relapse
        }
        val anyRelapseToday = activeTracks.any { todayLogsByTrack[it.id]?.status == AbstinenceStatus.Relapse }
        val minCleanStreak = activeTracks
            .map { track -> streakDays(track.id, allAbstinenceLogs, today, todayLogsByTrack[track.id]) }
            .minOrNull()
        val lowFoundation = layerScores.any {
            (it.layerId == BODY_LAYER_ID || it.layerId == CONDUCT_LAYER_ID) && it.score < 0.45f
        }

        return listOf(
            ScoreGate(
                kind = ScoreGateKind.RelapseToday,
                active = criticalRelapseToday,
                maxScore = RESTORATION_MAX,
                message = "Recaida critica registrada hoy.",
            ),
            ScoreGate(
                kind = ScoreGateKind.RelapseToday,
                active = anyRelapseToday && !criticalRelapseToday,
                maxScore = ATTENTION_MAX,
                message = "Recaida registrada hoy.",
            ),
            ScoreGate(
                kind = ScoreGateKind.SleepMissing,
                active = sleepScore == null,
                maxScore = MOTION_MAX,
                message = "Falta registrar sueno.",
            ),
            ScoreGate(
                kind = ScoreGateKind.SleepLow,
                active = sleepScore != null && sleepScore < 0.55f,
                maxScore = MOTION_MAX,
                message = "El descanso esta bajo.",
            ),
            ScoreGate(
                kind = ScoreGateKind.FoundationLayerLow,
                active = lowFoundation,
                maxScore = MOTION_MAX,
                message = "Cuerpo o Conducta todavia estan bajos.",
            ),
            ScoreGate(
                kind = ScoreGateKind.CleanStreak,
                active = minCleanStreak != null && minCleanStreak < 7,
                maxScore = MOTION_MAX,
                message = "La racha limpia aun esta reconstruyendose.",
            ),
            ScoreGate(
                kind = ScoreGateKind.CleanStreak,
                active = minCleanStreak != null && minCleanStreak in 7..13,
                maxScore = PLENITUDE_MAX,
                message = "La racha sostiene Plenitud, pero aun no Inquebrantable.",
            ),
            ScoreGate(
                kind = ScoreGateKind.GoalMissing,
                active = goalBonus <= 0,
                maxScore = MOTION_MAX,
                message = "Faltan metas de anclas para estados altos.",
            ),
            ScoreGate(
                kind = ScoreGateKind.GoalPartial,
                active = goalBonus in 1..74,
                maxScore = PLENITUDE_MAX,
                message = "Las metas de anclas aun no alcanzan para Inquebrantable.",
            ),
        )
    }
}

private fun List<ActivityDefinition>.weightedActivityRatio(logsByActivity: Map<String, ActivityLog>): Float {
    if (isEmpty()) return 0f
    val max = sumOf { it.importanceWeight().toDouble() }.toFloat()
    if (max <= 0f) return 0f
    val actual = sumOf { activity ->
        (activity.progressFor(logsByActivity[activity.id]) * activity.importanceWeight()).toDouble()
    }.toFloat()
    return (actual / max).coerceIn(0f, 1f)
}

private fun List<Task>.weightedTaskRatio(): Float {
    if (isEmpty()) return 0f
    val max = sumOf { it.importanceWeight().toDouble() }.toFloat()
    val actual = sumOf { it.importanceWeight().toDouble() }.toFloat()
    return if (max <= 0f) 0f else (actual / max).coerceIn(0f, 1f)
}

private fun Task.importanceWeight(): Float =
    importanceTier.importanceWeight()

private fun Task.isScoringTaskCompletedOn(today: LocalDate): Boolean {
    if (status != TaskStatus.Done || layerId == null || contributionRole == ContributionRole.Neutral) return false
    val completed = completedAt ?: return false
    val completedDate = Instant.ofEpochMilli(completed)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return completedDate == today
}

private fun scoreSobriety(
    activeTracks: List<AbstinenceTrack>,
    todayLogsByTrack: Map<String, AbstinenceLog>,
): Float? {
    if (activeTracks.isEmpty()) return null
    return activeTracks.map { track ->
        when (todayLogsByTrack[track.id]?.status) {
            AbstinenceStatus.Clean -> 1f
            AbstinenceStatus.Relapse -> if (track.severity == AbstinenceSeverity.Critical) 0f else 0.20f
            AbstinenceStatus.Unknown,
            null -> 0.45f
        }
    }.averageOrZero()
}

private fun streakDays(
    trackId: String,
    allLogs: List<AbstinenceLog>,
    today: LocalDate,
    todayLog: AbstinenceLog?,
): Int {
    val logsByDate = allLogs
        .filter { it.trackId == trackId }
        .mapNotNull { log ->
            runCatching { LocalDate.parse(log.date) }.getOrNull()?.let { date -> date to log }
        }
        .toMap()
        .let { logs ->
            if (todayLog == null) logs else logs + (today to todayLog)
        }

    var cursor = today
    var days = 0
    while (logsByDate[cursor]?.status == AbstinenceStatus.Clean) {
        days += 1
        cursor = cursor.minusDays(1)
    }
    return days
}

private fun Int.toScoreState(): ScoreState =
    when {
        this < 750 -> ScoreState.Restoration
        this < 800 -> ScoreState.Attention
        this < 900 -> ScoreState.Motion
        this < 950 -> ScoreState.Plenitude
        else -> ScoreState.Unbreakable
    }

private fun Iterable<Float>.averageOrZero(): Float {
    val values = toList()
    return if (values.isEmpty()) 0f else values.sum() / values.size
}
