package dev.panopt.autonomia.data.scoring

import dev.panopt.autonomia.DayPhase
import dev.panopt.autonomia.PhraseFamily
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.data.local.seed.AnchorPhraseSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM tests verifying the canonical anchor-phrase seed invariants
 * from docs/dominio/frases-ancla.md §15 and the spec SEED-REQ-1..4.
 *
 * No Room. No Android. Inspects in-memory objects from AnchorPhraseSeed directly.
 */
class AnchorPhraseSeedTest {

    // ─── SEED-REQ-1 : exactly 83 active phrases, distributed per family ───────

    @Test
    fun `total active phrases equals 83`() {
        val activeCount = AnchorPhraseSeed.phrases.count { it.active }
        assertEquals("Expected exactly 83 active phrases", 83, activeCount)
    }

    @Test
    fun `Containment family has 12 active phrases`() {
        val count = AnchorPhraseSeed.phrases.count { it.active && it.family == PhraseFamily.Containment.name }
        assertEquals(12, count)
    }

    @Test
    fun `MinimalAction family has 14 active phrases`() {
        val count = AnchorPhraseSeed.phrases.count { it.active && it.family == PhraseFamily.MinimalAction.name }
        assertEquals(14, count)
    }

    @Test
    fun `RegulationClarity family has 14 active phrases`() {
        val count = AnchorPhraseSeed.phrases.count { it.active && it.family == PhraseFamily.RegulationClarity.name }
        assertEquals(14, count)
    }

    @Test
    fun `Persistence family has 10 active phrases`() {
        val count = AnchorPhraseSeed.phrases.count { it.active && it.family == PhraseFamily.Persistence.name }
        assertEquals(10, count)
    }

    @Test
    fun `IdentityValues family has 10 active phrases`() {
        val count = AnchorPhraseSeed.phrases.count { it.active && it.family == PhraseFamily.IdentityValues.name }
        assertEquals(10, count)
    }

    @Test
    fun `Recognition family has 10 active phrases`() {
        val count = AnchorPhraseSeed.phrases.count { it.active && it.family == PhraseFamily.Recognition.name }
        assertEquals(10, count)
    }

    @Test
    fun `Contemplation family has 13 active phrases`() {
        val count = AnchorPhraseSeed.phrases.count { it.active && it.family == PhraseFamily.Contemplation.name }
        assertEquals(13, count)
    }

    // ─── SEED-REQ-2 : 0 active phrases with null/blank authorReference ────────

    @Test
    fun `no active phrase has null or blank authorReference`() {
        val violations = AnchorPhraseSeed.phrases.filter { phrase ->
            phrase.active && phrase.authorReference.isNullOrBlank()
        }
        assertTrue(
            "Active phrases with missing authorReference: ${violations.map { it.id }}",
            violations.isEmpty()
        )
    }

    // ─── SEED-REQ-3 : state rules derived from family-weight maps ─────────────

    @Test
    fun `state rule Restoration-MinimalAction has weight 3`() {
        val minimalActionPhraseIds = AnchorPhraseSeed.phrases
            .filter { it.family == PhraseFamily.MinimalAction.name }
            .map { it.id }
            .toSet()

        val rule = AnchorPhraseSeed.stateRules.firstOrNull { rule ->
            rule.phraseId in minimalActionPhraseIds && rule.scoreState == ScoreState.Restoration.name
        }
        assertEquals(
            "Expected Restoration→MinimalAction weight = 3 (frases-ancla.md §9)",
            3,
            rule?.weight
        )
    }

    @Test
    fun `no state rule exists for Containment phrases under Unbreakable`() {
        val containmentPhraseIds = AnchorPhraseSeed.phrases
            .filter { it.family == PhraseFamily.Containment.name }
            .map { it.id }
            .toSet()

        val found = AnchorPhraseSeed.stateRules.any { rule ->
            rule.phraseId in containmentPhraseIds && rule.scoreState == ScoreState.Unbreakable.name
        }
        assertFalse(
            "Containment phrases must have no rule under Unbreakable (excluded in §6)",
            found
        )
    }

    @Test
    fun `every state rule references an existing phraseId`() {
        val phraseIds = AnchorPhraseSeed.phrases.map { it.id }.toSet()
        val orphaned = AnchorPhraseSeed.stateRules.filter { it.phraseId !in phraseIds }
        assertTrue(
            "Orphaned state rule phraseIds: ${orphaned.map { it.phraseId }}",
            orphaned.isEmpty()
        )
    }

    // ─── SEED-REQ-4 : phase rules derived from family-weight maps ────────────

    @Test
    fun `phase rule Dawn-MinimalAction has weight 2`() {
        val minimalActionPhraseIds = AnchorPhraseSeed.phrases
            .filter { it.family == PhraseFamily.MinimalAction.name }
            .map { it.id }
            .toSet()

        val rule = AnchorPhraseSeed.phaseRules.firstOrNull { rule ->
            rule.phraseId in minimalActionPhraseIds && rule.dayPhase == DayPhase.Dawn.name
        }
        assertEquals(
            "Expected Dawn→MinimalAction weight = 2 (frases-ancla.md §9 +2)",
            2,
            rule?.weight
        )
    }

    @Test
    fun `no phase rule exists for Persistence phrases under Dusk`() {
        val persistencePhraseIds = AnchorPhraseSeed.phrases
            .filter { it.family == PhraseFamily.Persistence.name }
            .map { it.id }
            .toSet()

        val found = AnchorPhraseSeed.phaseRules.any { rule ->
            rule.phraseId in persistencePhraseIds && rule.dayPhase == DayPhase.Dusk.name
        }
        assertFalse(
            "Persistence phrases must have no Dusk phase rule (Persistence absent from Dusk map in §9)",
            found
        )
    }

    @Test
    fun `every phase rule references an existing phraseId`() {
        val phraseIds = AnchorPhraseSeed.phrases.map { it.id }.toSet()
        val orphaned = AnchorPhraseSeed.phaseRules.filter { it.phraseId !in phraseIds }
        assertTrue(
            "Orphaned phase rule phraseIds: ${orphaned.map { it.phraseId }}",
            orphaned.isEmpty()
        )
    }

    // ─── Additional: stateWeights and phaseWeights maps consistency ───────────

    @Test
    fun `stateWeights map matches frases-ancla §9 for NoData-Containment weight 4`() {
        val weight = AnchorPhraseSeed.stateWeights[ScoreState.NoData]?.get(PhraseFamily.Containment)
        assertEquals(4, weight)
    }

    @Test
    fun `phaseWeights map matches frases-ancla §9 for Dusk-RegulationClarity weight 2`() {
        val weight = AnchorPhraseSeed.phaseWeights[DayPhase.Dusk]?.get(PhraseFamily.RegulationClarity)
        assertEquals(2, weight)
    }
}
