package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.sleep.SleepScoring
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.exp
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
    val weeklyBaseScore: Float = 0f,
    val weeklyScore: Float = 0f,
    val averageLayerScore: Float = 0f,
    val worstLayerScore: Float = 0f,
    val worstLayerId: String? = null,
    val reasons: List<String> = emptyList(),
)

data class LayerScore(
    val layerId: String,
    val name: String,
    val score: Float,
    val configured: Boolean,
    val baseScore: Float = 0f,
    val rawScore: Float = 0f,
    val anchorScore: Float? = null,
    val supportScore: Float? = null,
    val anchorSurplusBonus: Float = 0f,
    val taskMomentumBonus: Float = 0f,
    val sleepScore: Float? = null,
    val sobrietyScore: Float? = null,
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
    private const val ANCHOR_FREQUENCY_WEIGHT = 0.70f
    private const val ANCHOR_VALUE_WEIGHT = 0.30f
    private const val ANCHOR_WITH_SUPPORT_WEIGHT = 0.80f
    private const val SUPPORT_WEIGHT = 0.20f
    private const val SLEEP_WEIGHT_IN_BODY = 0.30f
    private const val SOBRIETY_WEIGHT_IN_CONDUCT = 0.30f
    private const val WEEKLY_AVERAGE_WEIGHT = 0.75f
    private const val WEEKLY_WORST_WEIGHT = 0.25f
    private const val TASK_MOMENTUM_MAX = 0.050f
    private const val ANCHOR_SURPLUS_MAX = 0.100f
    private const val SOBRIETY_PENDING_CLEAN_VALUE = 0.50f
    private const val SOBRIETY_PENDING_CONFIDENCE_PENALTY = 0.15f
    private const val SOBRIETY_RELAPSE_DECAY = 1.5f
    private const val SOBRIETY_FORGIVENESS_WINDOW_DAYS = 5L

    fun calculate(input: ScoreInput): ScoreReport {
        val activeLayers = input.layers.filter { it.active }.sortedBy { it.sortOrder }
        if (activeLayers.isEmpty() || !input.hasAnyFact()) {
            return noDataReport(activeLayers)
        }

        val weekStart = input.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = weekStart.datesUntilInclusive(input.today)
        val visibleActivities = input.activities
            .filter { it.active && !it.archived }
            .sortedBy { it.sortOrder }
        val weeklyLogsByActivity = (input.periodActivityLogs + input.todayActivityLogs)
            .filter { it.dateAsLocalDate()?.let { date -> date in weekDates } == true }
            .distinctBy { "${it.activityId}:${it.date}" }
            .groupBy { it.activityId }
        val activeSobrietyTracks = input.abstinenceTracks.filter { it.active }.sortedBy { it.sortOrder }
        val sobrietyScore = scoreSobriety(
            tracks = activeSobrietyTracks,
            allLogs = input.allAbstinenceLogs,
            todayLogs = input.todayAbstinenceLogs,
            weekDates = weekDates,
            today = input.today,
        )
        val sleepScore = input.sleepLog?.let(SleepScoring::score)
        val completedTasksByLayer = input.tasks
            .filter { it.isScoringTaskCompletedIn(weekStart, input.today) }
            .groupBy { it.layerId.orEmpty() }

        val contributions = mutableListOf<FeatureContribution>()
        val layerEvaluations = activeLayers.map { layer ->
            evaluateLayer(
                layer = layer,
                activities = visibleActivities.filter { it.layerId == layer.id },
                weeklyLogsByActivity = weeklyLogsByActivity,
                weekDates = weekDates,
                completedTasks = completedTasksByLayer[layer.id].orEmpty(),
                sleepScore = if (layer.id == BODY_LAYER_ID) sleepScore else null,
                sobrietyScore = if (layer.id == CONDUCT_LAYER_ID) sobrietyScore else null,
                hasActiveSobriety = layer.id == CONDUCT_LAYER_ID && activeSobrietyTracks.isNotEmpty(),
                contributions = contributions,
            )
        }

        val averageLayerScore = layerEvaluations.map { it.rawScore }.averageOrZero()
        val worstLayer = layerEvaluations.minByOrNull { it.baseScore }
        val worstLayerScore = worstLayer?.baseScore ?: 0f
        val weeklyBaseScore = (
            WEEKLY_AVERAGE_WEIGHT * averageLayerScore +
                WEEKLY_WORST_WEIGHT * worstLayerScore
            ).coerceAtLeast(0f)
        val visibleScore = weeklyBaseScore.toVisibleScore()
        val state = visibleScore.toScoreState()

        return ScoreReport(
            state = state,
            visibleScore = visibleScore,
            baseScore = visibleScore,
            goalBonus = 0,
            progress = visibleScore / 1000f,
            layerScores = layerEvaluations.map { it.toLayerScore() },
            featureContributions = contributions,
            gates = emptyList(),
            weeklyBaseScore = weeklyBaseScore,
            weeklyScore = weeklyBaseScore,
            averageLayerScore = averageLayerScore,
            worstLayerScore = worstLayerScore,
            worstLayerId = worstLayer?.layerId,
            reasons = buildReasons(layerEvaluations, activeSobrietyTracks.isNotEmpty(), sleepScore),
        )
    }

    private fun noDataReport(activeLayers: List<Layer>): ScoreReport =
        ScoreReport(
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

    private fun evaluateLayer(
        layer: Layer,
        activities: List<ActivityDefinition>,
        weeklyLogsByActivity: Map<String, List<ActivityLog>>,
        weekDates: List<LocalDate>,
        completedTasks: List<Task>,
        sleepScore: Float?,
        sobrietyScore: Float?,
        hasActiveSobriety: Boolean,
        contributions: MutableList<FeatureContribution>,
    ): LayerEvaluation {
        val anchors = activities.filter { it.activityType == ActivitySurface.Anchor }
        val supports = activities.filter { it.activityType == ActivitySurface.Support }
        val anchorEvaluations = anchors.map { activity ->
            evaluateAnchor(activity, weeklyLogsByActivity[activity.id].orEmpty())
        }
        val anchorLayerScore = anchorEvaluations.map { it.baseScore }.averageOrNull()
        val anchorSurplusBonus = anchorEvaluations.map { it.surplusBonus }.averageOrZero()
        val supportLayerScore = evaluateSupports(supports, weeklyLogsByActivity, weekDates)
        val baseWithoutSpecial = when {
            anchorLayerScore == null && supportLayerScore == null -> 0f
            supportLayerScore == null -> anchorLayerScore ?: 0f
            else -> ANCHOR_WITH_SUPPORT_WEIGHT * (anchorLayerScore ?: 0f) + SUPPORT_WEIGHT * supportLayerScore
        }.coerceIn(0f, 1f)
        val taskMomentumBonus = taskMomentumBonus(completedTasks.size)
        val baseWithPositiveMargin = (baseWithoutSpecial + anchorSurplusBonus + taskMomentumBonus)
            .coerceIn(0f, 1.20f)

        val layerBase = when {
            layer.id == BODY_LAYER_ID -> {
                val sleep = sleepScore ?: 0f
                (1f - SLEEP_WEIGHT_IN_BODY) * baseWithoutSpecial + SLEEP_WEIGHT_IN_BODY * sleep
            }
            layer.id == CONDUCT_LAYER_ID && hasActiveSobriety -> {
                val sobriety = sobrietyScore ?: 0f
                (1f - SOBRIETY_WEIGHT_IN_CONDUCT) * baseWithoutSpecial +
                    SOBRIETY_WEIGHT_IN_CONDUCT * sobriety
            }
            else -> baseWithoutSpecial
        }.coerceIn(0f, 1f)

        val rawLayerScore = when {
            layer.id == BODY_LAYER_ID -> {
                val sleep = sleepScore ?: 0f
                ((1f - SLEEP_WEIGHT_IN_BODY) * baseWithPositiveMargin + SLEEP_WEIGHT_IN_BODY * sleep)
                    .coerceIn(0f, 1.20f)
            }
            layer.id == CONDUCT_LAYER_ID && hasActiveSobriety -> {
                val sobriety = sobrietyScore ?: 0f
                ((1f - SOBRIETY_WEIGHT_IN_CONDUCT) * baseWithPositiveMargin +
                    SOBRIETY_WEIGHT_IN_CONDUCT * sobriety)
                    .coerceIn(0f, 1.20f)
            }
            else -> baseWithPositiveMargin
        }

        val configured = anchors.isNotEmpty() ||
            supports.isNotEmpty() ||
            completedTasks.isNotEmpty() ||
            sleepScore != null ||
            hasActiveSobriety

        addContribution(contributions, ScoreFeature.Anchor, layer, "Anclas", anchorLayerScore, anchors.isNotEmpty())
        addContribution(contributions, ScoreFeature.Support, layer, "Soportes", supportLayerScore, supports.isNotEmpty())
        addContribution(
            contributions = contributions,
            feature = ScoreFeature.Task,
            layer = layer,
            label = "Pendientes",
            value = taskMomentumBonus,
            enabled = completedTasks.isNotEmpty(),
            maxValue = TASK_MOMENTUM_MAX,
        )
        addContribution(contributions, ScoreFeature.Sleep, layer, "Sueno", sleepScore, layer.id == BODY_LAYER_ID && sleepScore != null)
        addContribution(
            contributions = contributions,
            feature = ScoreFeature.Sobriety,
            layer = layer,
            label = "Sobriedad",
            value = sobrietyScore,
            enabled = layer.id == CONDUCT_LAYER_ID && hasActiveSobriety,
        )

        return LayerEvaluation(
            layerId = layer.id,
            name = layer.name,
            configured = configured,
            baseScore = layerBase,
            rawScore = rawLayerScore,
            anchorScore = anchorLayerScore,
            supportScore = supportLayerScore,
            anchorSurplusBonus = anchorSurplusBonus,
            taskMomentumBonus = taskMomentumBonus,
            sleepScore = sleepScore,
            sobrietyScore = sobrietyScore,
        )
    }

    private fun evaluateAnchor(activity: ActivityDefinition, logs: List<ActivityLog>): AnchorEvaluation {
        val targetDays = activity.targetDays()
        val targetDailyValue = activity.targetDailyValue()
        val targetWeeklyValue = (targetDays * targetDailyValue).coerceAtLeast(1)
        val doneDates = logs
            .filter { it.countsAsDone() }
            .mapNotNull { it.dateAsLocalDate() }
            .toSet()
        val actualValue = logs.sumOf { log ->
            val fallback = if (log.countsAsDone()) targetDailyValue else 0
            (log.actualValue ?: fallback).coerceAtLeast(0)
        }
        val frequencyRatio = doneDates.size.toFloat() / targetDays.toFloat()
        val valueRatio = actualValue.toFloat() / targetWeeklyValue.toFloat()
        val frequencyScore = frequencyRatio.coerceIn(0f, 1f)
        val valueScore = valueRatio.coerceIn(0f, 1f)
        val baseScore = ANCHOR_FREQUENCY_WEIGHT * frequencyScore + ANCHOR_VALUE_WEIGHT * valueScore
        val frequencySurplusBonus = surplusBonus(frequencyRatio - 1f)
        val valueSurplusBonus = surplusBonus(valueRatio - 1f)
        val surplusBonus = ANCHOR_FREQUENCY_WEIGHT * frequencySurplusBonus + ANCHOR_VALUE_WEIGHT * valueSurplusBonus
        return AnchorEvaluation(
            baseScore = baseScore.coerceIn(0f, 1f),
            surplusBonus = surplusBonus.coerceIn(0f, ANCHOR_SURPLUS_MAX),
        )
    }

    private fun evaluateSupports(
        supports: List<ActivityDefinition>,
        weeklyLogsByActivity: Map<String, List<ActivityLog>>,
        weekDates: List<LocalDate>,
    ): Float? {
        if (supports.isEmpty()) return null
        val expectedSupportDays = supports.size * weekDates.size
        if (expectedSupportDays <= 0) return 1f
        val omittedSupportDays = supports.sumOf { support ->
            weeklyLogsByActivity[support.id].orEmpty()
                .filter { it.countsAsDone() }
                .mapNotNull { it.dateAsLocalDate() }
                .distinct()
                .count()
        }
        return (1f - omittedSupportDays.toFloat() / expectedSupportDays.toFloat()).coerceIn(0f, 1f)
    }

    private fun scoreSobriety(
        tracks: List<AbstinenceTrack>,
        allLogs: List<AbstinenceLog>,
        todayLogs: List<AbstinenceLog>,
        weekDates: List<LocalDate>,
        today: LocalDate,
    ): Float? {
        if (tracks.isEmpty() || weekDates.isEmpty()) return null
        val todayOverrides = todayLogs.associateBy { it.trackId to it.date }
        val allLogsByTrackAndDate = allLogs
            .associateBy { it.trackId to it.date }
            .let { logs -> logs + todayOverrides }

        val trackScores = tracks.map { track ->
            val evaluableDays = weekDates.size.toFloat()
            var confirmedCleanDays = 0f
            var pendingDays = 0f
            var relapseDays = 0f

            weekDates.forEach { date ->
                when (allLogsByTrackAndDate[track.id to date.toString()]?.status) {
                    AbstinenceStatus.Clean -> confirmedCleanDays += 1f
                    AbstinenceStatus.Relapse -> relapseDays += 1f
                    AbstinenceStatus.Unknown,
                    null -> {
                        val age = ChronoUnit.DAYS.between(date, today)
                        if (age <= SOBRIETY_FORGIVENESS_WINDOW_DAYS) {
                            pendingDays += 1f
                        } else {
                            relapseDays += 1f
                        }
                    }
                }
            }

            val cleanCoverage = ((confirmedCleanDays + SOBRIETY_PENDING_CLEAN_VALUE * pendingDays) / evaluableDays)
                .coerceIn(0f, 1f)
            val relapseProtection = exp(-(relapseDays / SOBRIETY_RELAPSE_DECAY)).coerceIn(0f, 1f)
            val trackingConfidence = (1f - SOBRIETY_PENDING_CONFIDENCE_PENALTY * (pendingDays / evaluableDays))
                .coerceIn(0f, 1f)
            (cleanCoverage * relapseProtection * trackingConfidence).coerceIn(0f, 1f)
        }

        return (0.70f * trackScores.averageOrZero() + 0.30f * (trackScores.minOrNull() ?: 0f))
            .coerceIn(0f, 1f)
    }

    private fun ScoreInput.hasAnyFact(): Boolean {
        val activeTrackIds = abstinenceTracks.filter { it.active }.map { it.id }.toSet()
        val hasAbstinenceFact = (todayAbstinenceLogs + allAbstinenceLogs).any { log ->
            log.trackId in activeTrackIds && log.status != AbstinenceStatus.Unknown
        }
        val hasTaskFact = tasks.any { it.status == TaskStatus.Done && it.layerId != null }
        return todayActivityLogs.isNotEmpty() ||
            periodActivityLogs.isNotEmpty() ||
            hasAbstinenceFact ||
            hasTaskFact ||
            sleepLog != null
    }

    private fun buildReasons(
        layerEvaluations: List<LayerEvaluation>,
        hasActiveSobriety: Boolean,
        sleepScore: Float?,
    ): List<String> {
        val reasons = mutableListOf<String>()
        val weakestLayer = layerEvaluations.minByOrNull { it.baseScore }
        if (weakestLayer != null && weakestLayer.baseScore < 0.60f) {
            reasons += "La capa mas baja es ${weakestLayer.name}."
        }
        if (sleepScore != null && sleepScore < 0.70f) {
            reasons += "El descanso bajo esta afectando Cuerpo."
        }
        val conduct = layerEvaluations.firstOrNull { it.layerId == CONDUCT_LAYER_ID }
        if (hasActiveSobriety && conduct?.sobrietyScore != null && conduct.sobrietyScore < 0.70f) {
            reasons += "Sobriedad esta reduciendo Conducta esta semana."
        }
        return reasons
    }

    private fun addContribution(
        contributions: MutableList<FeatureContribution>,
        feature: ScoreFeature,
        layer: Layer,
        label: String,
        value: Float?,
        enabled: Boolean,
        maxValue: Float = 1f,
    ) {
        if (!enabled || value == null || maxValue <= 0f) return
        contributions += FeatureContribution(
            feature = feature,
            layerId = layer.id,
            label = label,
            value = value.coerceIn(0f, maxValue),
            maxValue = maxValue,
        )
    }
}

private data class LayerEvaluation(
    val layerId: String,
    val name: String,
    val configured: Boolean,
    val baseScore: Float,
    val rawScore: Float,
    val anchorScore: Float?,
    val supportScore: Float?,
    val anchorSurplusBonus: Float,
    val taskMomentumBonus: Float,
    val sleepScore: Float?,
    val sobrietyScore: Float?,
) {
    fun toLayerScore(): LayerScore =
        LayerScore(
            layerId = layerId,
            name = name,
            score = rawScore.coerceIn(0f, 1f),
            configured = configured,
            baseScore = baseScore,
            rawScore = rawScore,
            anchorScore = anchorScore,
            supportScore = supportScore,
            anchorSurplusBonus = anchorSurplusBonus,
            taskMomentumBonus = taskMomentumBonus,
            sleepScore = sleepScore,
            sobrietyScore = sobrietyScore,
        )
}

private data class AnchorEvaluation(
    val baseScore: Float,
    val surplusBonus: Float,
)

private fun ActivityDefinition.targetDays(): Int {
    val fallback = when (cadence) {
        ActivityCadence.Daily -> 7
        ActivityCadence.Weekly -> 1
        ActivityCadence.Monthly -> 1
        ActivityCadence.Custom,
        ActivityCadence.EventBased,
        null -> 1
    }
    return (weeklyFrequencyTarget ?: targetCount ?: fallback).coerceIn(1, 7)
}

private fun ActivityDefinition.targetDailyValue(): Int =
    when (unit) {
        ActivityUnit.Boolean,
        ActivityUnit.Text -> 1
        ActivityUnit.Minutes,
        ActivityUnit.Count,
        ActivityUnit.Time -> sessionTargetMinutes ?: targetValue ?: minimumValue ?: 1
    }.coerceAtLeast(1)

private fun ActivityLog.countsAsDone(): Boolean {
    if (completed) return true
    return (actualValue ?: 0) > 0
}

private fun ActivityLog.dateAsLocalDate(): LocalDate? =
    runCatching { LocalDate.parse(date) }.getOrNull()

private fun Task.isScoringTaskCompletedIn(weekStart: LocalDate, today: LocalDate): Boolean {
    if (status != TaskStatus.Done || layerId == null || contributionRole == ContributionRole.Neutral) return false
    val completed = completedAt ?: return false
    val completedDate = Instant.ofEpochMilli(completed)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return completedDate in weekStart..today
}

private fun taskMomentumBonus(completedLayerTasks: Int): Float {
    if (completedLayerTasks <= 0) return 0f
    return (TASK_MOMENTUM_MAX_FOR_HELPERS * (1f - exp(-completedLayerTasks.toFloat() / 2f)))
        .coerceIn(0f, TASK_MOMENTUM_MAX_FOR_HELPERS)
}

private const val TASK_MOMENTUM_MAX_FOR_HELPERS = 0.050f

private fun surplusBonus(surplusMagnitude: Float): Float {
    if (surplusMagnitude <= 0f) return 0f
    return (0.100f * (1f - exp(-surplusMagnitude / 2f))).coerceIn(0f, 0.100f)
}

private fun LocalDate.datesUntilInclusive(end: LocalDate): List<LocalDate> {
    val days = ChronoUnit.DAYS.between(this, end).coerceAtLeast(0)
    return (0..days).map { plusDays(it) }
}

private fun Iterable<Float>.averageOrZero(): Float {
    val values = toList()
    return if (values.isEmpty()) 0f else values.sum() / values.size
}

private fun Iterable<Float>.averageOrNull(): Float? {
    val values = toList()
    return if (values.isEmpty()) null else values.sum() / values.size
}

private fun Float.toVisibleScore(): Int =
    (700 + coerceIn(0f, 1f) * 300f).roundToInt().coerceIn(700, 1000)

private fun Int.toScoreState(): ScoreState =
    when {
        this < 750 -> ScoreState.Restoration
        this < 800 -> ScoreState.Attention
        this < 900 -> ScoreState.Motion
        else -> ScoreState.Plenitude
    }
