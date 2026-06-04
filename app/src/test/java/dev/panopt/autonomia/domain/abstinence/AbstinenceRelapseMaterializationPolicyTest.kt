package dev.panopt.autonomia.domain.abstinence

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AbstinenceRelapseMaterializationPolicyTest {
    private val zoneId = ZoneId.of("America/Lima")
    private val today = LocalDate.of(2026, 5, 27)

    @Test
    fun keepsMissingDaysInsideFiveDayWindowPending() {
        val ranges = AbstinenceRelapseMaterializationPolicy.assumedRanges(
            tracks = listOf(track(createdAt = today.minusDays(4))),
            logs = emptyList(),
            today = today,
            zoneId = zoneId,
        )

        assertTrue(ranges.isEmpty())
    }

    @Test
    fun materializesMissingDaysOlderThanFiveDays() {
        val ranges = AbstinenceRelapseMaterializationPolicy.assumedRanges(
            tracks = listOf(track(createdAt = today.minusDays(8))),
            logs = emptyList(),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(1, ranges.size)
        assertEquals(today.minusDays(8), ranges.single().startDate)
        assertEquals(today.minusDays(6), ranges.single().endDate)
        assertEquals(3, ranges.single().dates.size)
    }

    @Test
    fun confirmedLogsSplitAssumedRanges() {
        val cleanDate = today.minusDays(7)
        val ranges = AbstinenceRelapseMaterializationPolicy.assumedRanges(
            tracks = listOf(track(createdAt = today.minusDays(9))),
            logs = listOf(log(date = cleanDate, status = AbstinenceStatus.Clean)),
            today = today,
            zoneId = zoneId,
        )

        assertEquals(2, ranges.size)
        assertEquals(today.minusDays(9), ranges[0].startDate)
        assertEquals(today.minusDays(8), ranges[0].endDate)
        assertEquals(today.minusDays(6), ranges[1].startDate)
        assertEquals(today.minusDays(6), ranges[1].endDate)
    }

    @Test
    fun inactiveTracksAreNeverMaterialized() {
        val ranges = AbstinenceRelapseMaterializationPolicy.assumedRanges(
            tracks = listOf(track(createdAt = today.minusDays(10), active = false)),
            logs = emptyList(),
            today = today,
            zoneId = zoneId,
        )

        assertTrue(ranges.isEmpty())
    }

    @Test
    fun confirmedRelapseLogsAreNotReMaterialized() {
        val relapseDate = today.minusDays(7)
        val ranges = AbstinenceRelapseMaterializationPolicy.assumedRanges(
            tracks = listOf(track(createdAt = today.minusDays(9))),
            logs = listOf(log(date = relapseDate, status = AbstinenceStatus.Relapse)),
            today = today,
            zoneId = zoneId,
        )

        // Un día ya registrado (aunque sea recaída) no se vuelve a materializar:
        // parte el rango asumido igual que un día limpio confirmado.
        assertEquals(2, ranges.size)
        assertEquals(today.minusDays(9), ranges[0].startDate)
        assertEquals(today.minusDays(8), ranges[0].endDate)
        assertEquals(today.minusDays(6), ranges[1].startDate)
        assertEquals(today.minusDays(6), ranges[1].endDate)
        assertTrue(ranges.none { range -> relapseDate in range.dates })
    }

    private fun track(createdAt: LocalDate, active: Boolean = true): AbstinenceTrack =
        AbstinenceTrack(
            id = "trk_custom",
            name = "Custom",
            substanceLabel = "Custom",
            severity = AbstinenceSeverity.Critical,
            contributionRole = ContributionRole.Protective,
            importanceTier = ImportanceTier.Critical,
            active = active,
            sortOrder = 10,
            createdAt = createdAt.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            updatedAt = createdAt.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        )

    private fun log(
        date: LocalDate,
        status: AbstinenceStatus,
    ): AbstinenceLog =
        AbstinenceLog(
            trackId = "trk_custom",
            date = date.toString(),
            status = status,
            updatedAt = 1L,
        )
}
