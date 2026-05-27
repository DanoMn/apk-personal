package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.exp

internal object SobrietyScoringPolicy {
    fun score(
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
            scoreTrack(
                track = track,
                logsByTrackAndDate = allLogsByTrackAndDate,
                weekDates = weekDates,
                today = today,
            )
        }

        return (0.70f * trackScores.averageOrZero() + 0.30f * (trackScores.minOrNull() ?: 0f))
            .coerceIn(0f, 1f)
    }

    private fun scoreTrack(
        track: AbstinenceTrack,
        logsByTrackAndDate: Map<Pair<String, String>, AbstinenceLog>,
        weekDates: List<LocalDate>,
        today: LocalDate,
    ): Float {
        val evaluableDays = weekDates.size.toFloat()
        var confirmedCleanDays = 0f
        var pendingDays = 0f
        var relapseDays = 0f

        weekDates.forEach { date ->
            when (logsByTrackAndDate[track.id to date.toString()]?.status) {
                AbstinenceStatus.Clean -> confirmedCleanDays += 1f
                AbstinenceStatus.Relapse -> relapseDays += 1f
                AbstinenceStatus.Unknown,
                null -> {
                    val age = ChronoUnit.DAYS.between(date, today)
                    if (age <= ScoringConstants.SOBRIETY_FORGIVENESS_WINDOW_DAYS) {
                        pendingDays += 1f
                    } else {
                        relapseDays += 1f
                    }
                }
            }
        }

        val cleanCoverage = (
            (confirmedCleanDays + ScoringConstants.SOBRIETY_PENDING_CLEAN_VALUE * pendingDays) /
                evaluableDays
            ).coerceIn(0f, 1f)
        val relapseProtection = exp(-(relapseDays / ScoringConstants.SOBRIETY_RELAPSE_DECAY))
            .coerceIn(0f, 1f)
        val trackingConfidence = (
            1f - ScoringConstants.SOBRIETY_PENDING_CONFIDENCE_PENALTY * (pendingDays / evaluableDays)
            ).coerceIn(0f, 1f)
        return (cleanCoverage * relapseProtection * trackingConfidence).coerceIn(0f, 1f)
    }
}
