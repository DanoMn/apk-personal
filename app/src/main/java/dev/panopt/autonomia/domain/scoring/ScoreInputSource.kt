package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.sleep.SleepNightScore
import java.time.LocalDate

data class ScoreInputSource(
    val layers: List<Layer>,
    val activities: List<ActivityDefinition>,
    val todayActivityLogs: List<ActivityLog>,
    val periodActivityLogs: List<ActivityLog>,
    val abstinenceTracks: List<AbstinenceTrack>,
    val todayAbstinenceLogs: List<AbstinenceLog>,
    val allAbstinenceLogs: List<AbstinenceLog>,
    val tasks: List<Task>,
    /**
     * Sleep nights for the current week (already scored).
     * Weekly aggregation: average of nights with data. NoData nights are excluded.
     * Empty → null propagated downstream (no sleep data this week).
     *
     * D3: digitalWindDownMinutes is INERT in scoring (not passed here).
     * The sleep term in Cuerpo uses this aggregated weekly average (design §5).
     *
     * Replaces the deprecated single `sleepLog: SleepLog?` (WU-5 / design §5).
     */
    val sleepNights: List<SleepNightScore> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val weeklyHistory: List<WeeklyScoreHistoryEntry> = emptyList(),
)
