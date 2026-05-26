package dev.panopt.autonomia.domain.abstinence

import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AbstinencePolicyTest {
    @Test
    fun `blank custom track name is rejected`() {
        assertNull(AbstinencePolicy.createCustomDraft("   "))
    }

    @Test
    fun `custom track name is normalized`() {
        val draft = checkNotNull(AbstinencePolicy.createCustomDraft("  No   apuestas  "))

        assertEquals("No apuestas", draft.name)
        assertEquals("No apuestas", draft.substanceLabel)
    }

    @Test
    fun `custom track draft is active moderate and protective`() {
        val draft = checkNotNull(AbstinencePolicy.createCustomDraft("Cafeina"))

        assertTrue(draft.active)
        assertEquals(AbstinenceSeverity.Moderate, draft.severity)
        assertEquals(ContributionRole.Protective, draft.contributionRole)
        assertEquals(ImportanceTier.High, draft.importanceTier)
    }

    @Test
    fun `preset tracks cannot be deleted`() {
        assertFalse(AbstinencePolicy.canDelete(track(id = "trk_alcohol")))
        assertFalse(AbstinencePolicy.canDelete(track(id = "trk_substances")))
        assertFalse(AbstinencePolicy.canDelete(track(id = "trk_sexual")))
    }

    @Test
    fun `custom tracks can be deleted`() {
        assertTrue(AbstinencePolicy.canDelete(track(id = "trk_custom_test")))
    }

    @Test
    fun `daily logs can only be recorded for active tracks`() {
        assertTrue(AbstinencePolicy.canRecordDailyLog(track(active = true)))
        assertFalse(AbstinencePolicy.canRecordDailyLog(track(active = false)))
    }

    private fun track(
        id: String = "trk_custom_test",
        active: Boolean = true,
    ): AbstinenceTrack =
        AbstinenceTrack(
            id = id,
            name = "Test",
            substanceLabel = "Test",
            severity = AbstinenceSeverity.Moderate,
            contributionRole = ContributionRole.Protective,
            importanceTier = ImportanceTier.High,
            active = active,
            sortOrder = 10,
        )
}
