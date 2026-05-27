package dev.panopt.autonomia.domain.abstinence

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AssumedRelapseRange(
    val trackId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    val dates: List<LocalDate> =
        startDate.datesUntil(endDate.plusDays(1)).toList()
}

object AbstinenceRelapseMaterializationPolicy {
    const val MISSING_TRACKING_WINDOW_DAYS = 5L
    private const val MAX_LOOKBACK_DAYS = 366L

    fun assumedRanges(
        tracks: List<AbstinenceTrack>,
        logs: List<AbstinenceLog>,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<AssumedRelapseRange> {
        val cutoff = today.minusDays(MISSING_TRACKING_WINDOW_DAYS + 1)
        if (cutoff.isBefore(today.minusDays(MAX_LOOKBACK_DAYS))) return emptyList()

        val logsByTrackAndDate = logs.associateBy { it.trackId to it.date }
        return tracks
            .filter { it.active }
            .flatMap { track ->
                val trackStart = track.createdLocalDate(zoneId)
                    .coerceAtLeast(today.minusDays(MAX_LOOKBACK_DAYS))
                if (trackStart.isAfter(cutoff)) {
                    emptyList()
                } else {
                    missingDates(track, trackStart, cutoff, logsByTrackAndDate)
                        .toAssumedRanges(track.id)
                }
            }
    }

    private fun missingDates(
        track: AbstinenceTrack,
        start: LocalDate,
        cutoff: LocalDate,
        logsByTrackAndDate: Map<Pair<String, String>, AbstinenceLog>,
    ): List<LocalDate> =
        start.datesUntil(cutoff.plusDays(1))
            .filter { date ->
                val status = logsByTrackAndDate[track.id to date.toString()]?.status
                status == null || status == AbstinenceStatus.Unknown
            }
            .toList()

    private fun List<LocalDate>.toAssumedRanges(trackId: String): List<AssumedRelapseRange> {
        if (isEmpty()) return emptyList()
        val ranges = mutableListOf<AssumedRelapseRange>()
        var start = first()
        var previous = first()

        drop(1).forEach { date ->
            if (date == previous.plusDays(1)) {
                previous = date
            } else {
                ranges += AssumedRelapseRange(trackId, start, previous)
                start = date
                previous = date
            }
        }
        ranges += AssumedRelapseRange(trackId, start, previous)
        return ranges
    }

    private fun AbstinenceTrack.createdLocalDate(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(createdAt)
            .atZone(zoneId)
            .toLocalDate()
}
