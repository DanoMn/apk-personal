package dev.panopt.autonomia.domain.abstinence

import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier

data class AbstinenceTrackDraft(
    val name: String,
    val substanceLabel: String,
    val severity: AbstinenceSeverity = AbstinenceSeverity.Moderate,
    val contributionRole: ContributionRole = ContributionRole.Protective,
    val importanceTier: ImportanceTier = ImportanceTier.High,
    val active: Boolean = true,
)

object AbstinencePolicy {
    val presetTrackIds: Set<String> = setOf(
        "trk_alcohol",
        "trk_substances",
        "trk_sexual",
    )

    fun createCustomDraft(name: String): AbstinenceTrackDraft? {
        val normalizedName = normalizeName(name)
        if (normalizedName.isBlank()) return null

        return AbstinenceTrackDraft(
            name = normalizedName,
            substanceLabel = normalizedName,
        )
    }

    fun isPresetTrackId(trackId: String): Boolean =
        trackId in presetTrackIds

    fun isCustomTrack(track: AbstinenceTrack): Boolean =
        !isPresetTrackId(track.id)

    fun canDelete(track: AbstinenceTrack): Boolean =
        isCustomTrack(track)

    fun canRecordDailyLog(track: AbstinenceTrack): Boolean =
        track.active

    fun normalizeName(name: String): String =
        name.trim().replace(Regex("\\s+"), " ")
}
