package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.TaskStatus
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

internal object WeeklyScoringContextBuilder {
    fun build(input: ScoreInput): WeeklyScoringContext {
        val activeLayers = input.layers.filter { it.active }.sortedBy { it.sortOrder }
        val weekStart = input.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = weekStart.datesUntilInclusive(input.today)
        val visibleActivities = input.activities
            .filter { it.active && !it.archived }
            .sortedBy { it.sortOrder }
        val weeklyLogsByActivity = (input.periodActivityLogs + input.todayActivityLogs)
            .filter { it.dateAsLocalDate()?.let { date -> date in weekDates } == true }
            .distinctBy { "${it.activityId}:${it.date}" }
            .groupBy { it.activityId }
        val activeSobrietyTracks = input.abstinenceTracks
            .filter { it.active }
            .sortedBy { it.sortOrder }

        // PR-E: logs de abstinencia de la ventana por track activo (forma cruda para el adapter
        // nuevo). El log de HOY pisa al histórico de la misma fecha (igual que SobrietyScoringPolicy).
        val activeTrackIds = activeSobrietyTracks.mapTo(HashSet()) { it.id }
        val weekDateStrings = weekDates.mapTo(HashSet()) { it.toString() }
        val weeklyAbstinenceLogsByTrack = (input.allAbstinenceLogs + input.todayAbstinenceLogs)
            .filter { it.trackId in activeTrackIds && it.date in weekDateStrings }
            // hoy pisa al histórico de la misma (track, fecha): associateBy deja el ÚLTIMO.
            .associateBy { "${it.trackId}:${it.date}" }
            .values
            .groupBy { it.trackId }

        // Weekly sleep score: average of nights WITH data only (NoData nights excluded).
        // Design §5: cobertura suave — una noche sin dato NO entra como 0.
        val nightsWithData = input.sleepNights.mapNotNull { it.sleepScore }
        val weeklySleepScore: Float? = if (nightsWithData.isEmpty()) null
        else nightsWithData.average().toFloat()

        return WeeklyScoringContext(
            weekStart = weekStart,
            weekDates = weekDates,
            activeLayers = activeLayers,
            visibleActivities = visibleActivities,
            weeklyLogsByActivity = weeklyLogsByActivity,
            activeSobrietyTracks = activeSobrietyTracks,
            sleepScore = weeklySleepScore,
            sobrietyScore = SobrietyScoringPolicy.score(
                tracks = activeSobrietyTracks,
                allLogs = input.allAbstinenceLogs,
                todayLogs = input.todayAbstinenceLogs,
                weekDates = weekDates,
                today = input.today,
            ),
            completedTasksByLayer = input.tasks
                .filter { it.isScoringTaskCompletedIn(weekStart, input.today) }
                .groupBy { it.layerId.orEmpty() },
        )
    }

    fun hasAnyFact(input: ScoreInput): Boolean {
        val activeTrackIds = input.abstinenceTracks.filter { it.active }.map { it.id }.toSet()
        val hasAbstinenceFact = (input.todayAbstinenceLogs + input.allAbstinenceLogs).any { log ->
            log.trackId in activeTrackIds && log.status != AbstinenceStatus.Unknown
        }
        val hasTaskFact = input.tasks.any { it.status == TaskStatus.Done && it.layerId != null }
        // 5.6: any night with sleep data (not NoData) counts as a fact
        val hasSleepFact = input.sleepNights.any { it.sleepScore != null }
        return input.todayActivityLogs.isNotEmpty() ||
            input.periodActivityLogs.isNotEmpty() ||
            hasAbstinenceFact ||
            hasTaskFact ||
            hasSleepFact
    }
}
