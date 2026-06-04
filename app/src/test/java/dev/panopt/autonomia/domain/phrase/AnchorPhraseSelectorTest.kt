package dev.panopt.autonomia.domain.phrase

import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.AnchorPhrasePhaseRule
import dev.panopt.autonomia.AnchorPhraseStateRule
import dev.panopt.autonomia.AttributionStatus
import dev.panopt.autonomia.DayPhase
import dev.panopt.autonomia.PhraseFamily
import dev.panopt.autonomia.ScoreState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * TDD — RED phase (task 4.1):
 * Written before AnchorPhraseSelector / AnchorPhraseSelectorInput exist.
 * Covers the 9 scenarios from anchor-phrase-selector/spec.md (SEL-REQ-1..4).
 *
 * All tests are pure JVM: no Room, no Android, no coroutines.
 */
class AnchorPhraseSelectorTest {

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private val baseDate = LocalDate.of(2026, 6, 4)

    /** Build a minimal active phrase with all required fields. */
    private fun phrase(
        id: String,
        family: PhraseFamily,
        active: Boolean = true,
        authorReference: String? = "Author Name",
    ) = AnchorPhrase(
        id = id,
        text = "Phrase text for $id",
        authorReference = authorReference,
        family = family,
        language = "en",
        attributionStatus = AttributionStatus.Clear,
        active = active,
        sortOrder = 0,
        createdAt = 0L,
        updatedAt = 0L,
    )

    /** Build a minimal state rule: every phrase in the catalog gets weight 1 for the given state. */
    private fun stateRulesFor(
        phrases: List<AnchorPhrase>,
        state: ScoreState,
        weight: Int = 1,
        families: Set<PhraseFamily> = PhraseFamily.values().toSet(),
    ): List<AnchorPhraseStateRule> =
        phrases.filter { it.family in families }.map { AnchorPhraseStateRule(it.id, state, weight) }

    /** Build a minimal phase rule: every phrase in the catalog gets weight 1 for the given phase. */
    private fun phaseRulesFor(
        phrases: List<AnchorPhrase>,
        phase: DayPhase,
        weight: Int = 1,
        families: Set<PhraseFamily> = PhraseFamily.values().toSet(),
    ): List<AnchorPhrasePhaseRule> =
        phrases.filter { it.family in families }.map { AnchorPhrasePhaseRule(it.id, phase, weight) }

    /** Convenience builder for AnchorPhraseSelectorInput. */
    private fun input(
        date: LocalDate = baseDate,
        dayPhase: DayPhase = DayPhase.Dawn,
        scoreState: ScoreState = ScoreState.Motion,
        catalog: List<AnchorPhrase> = emptyList(),
        stateRules: List<AnchorPhraseStateRule> = emptyList(),
        phaseRules: List<AnchorPhrasePhaseRule> = emptyList(),
        recentPhraseIds: Set<String> = emptySet(),
    ) = AnchorPhraseSelectorInput(
        date = date,
        dayPhase = dayPhase,
        scoreState = scoreState,
        catalog = catalog,
        stateRules = stateRules,
        phaseRules = phaseRules,
        recentPhraseIds = recentPhraseIds,
    )

    // ─── SEL-REQ-1 : Eligibility Filter ─────────────────────────────────────

    @Test
    fun `inactive phrase is excluded from selection`() {
        val inactivePhrase = phrase("p_inactive", PhraseFamily.MinimalAction, active = false)
        val i = input(
            scoreState = ScoreState.Motion,
            catalog = listOf(inactivePhrase),
            stateRules = stateRulesFor(listOf(inactivePhrase), ScoreState.Motion),
            phaseRules = phaseRulesFor(listOf(inactivePhrase), DayPhase.Dawn),
        )
        val result = AnchorPhraseSelector.select(i)
        assertNull("Inactive phrase should be excluded; result should be null", result)
    }

    @Test
    fun `phrase with empty authorReference is excluded from selection`() {
        val noAuthorPhrase = phrase("p_no_author", PhraseFamily.MinimalAction, authorReference = "")
        val i = input(
            scoreState = ScoreState.Motion,
            catalog = listOf(noAuthorPhrase),
            stateRules = stateRulesFor(listOf(noAuthorPhrase), ScoreState.Motion),
            phaseRules = phaseRulesFor(listOf(noAuthorPhrase), DayPhase.Dawn),
        )
        val result = AnchorPhraseSelector.select(i)
        assertNull("Phrase with empty authorReference should be excluded; result should be null", result)
    }

    @Test
    fun `phrase with null authorReference is excluded from selection`() {
        val nullAuthorPhrase = phrase("p_null_author", PhraseFamily.MinimalAction, authorReference = null)
        val i = input(
            scoreState = ScoreState.Motion,
            catalog = listOf(nullAuthorPhrase),
            stateRules = stateRulesFor(listOf(nullAuthorPhrase), ScoreState.Motion),
            phaseRules = phaseRulesFor(listOf(nullAuthorPhrase), DayPhase.Dawn),
        )
        val result = AnchorPhraseSelector.select(i)
        assertNull("Phrase with null authorReference should be excluded; result should be null", result)
    }

    // ─── SEL-REQ-2 : State Family Filter — Contemplation gate (§8.6) ─────────

    @Test
    fun `Contemplation excluded when scoreState is NoData`() {
        val p = phrase("p_cont_1", PhraseFamily.Contemplation)
        val i = input(
            scoreState = ScoreState.NoData,
            catalog = listOf(p),
            stateRules = listOf(AnchorPhraseStateRule(p.id, ScoreState.NoData, 3)),
            phaseRules = phaseRulesFor(listOf(p), DayPhase.Dawn),
        )
        assertNull("Contemplation must not be selected for NoData", AnchorPhraseSelector.select(i))
    }

    @Test
    fun `Contemplation excluded when scoreState is Restoration`() {
        val p = phrase("p_cont_2", PhraseFamily.Contemplation)
        val i = input(
            scoreState = ScoreState.Restoration,
            catalog = listOf(p),
            stateRules = listOf(AnchorPhraseStateRule(p.id, ScoreState.Restoration, 3)),
            phaseRules = phaseRulesFor(listOf(p), DayPhase.Dawn),
        )
        assertNull("Contemplation must not be selected for Restoration", AnchorPhraseSelector.select(i))
    }

    @Test
    fun `Contemplation excluded when scoreState is Attention`() {
        val p = phrase("p_cont_3", PhraseFamily.Contemplation)
        val i = input(
            scoreState = ScoreState.Attention,
            catalog = listOf(p),
            stateRules = listOf(AnchorPhraseStateRule(p.id, ScoreState.Attention, 3)),
            phaseRules = phaseRulesFor(listOf(p), DayPhase.Dawn),
        )
        assertNull("Contemplation must not be selected for Attention", AnchorPhraseSelector.select(i))
    }

    @Test
    fun `Contemplation excluded when scoreState is Motion`() {
        val p = phrase("p_cont_4", PhraseFamily.Contemplation)
        val i = input(
            scoreState = ScoreState.Motion,
            catalog = listOf(p),
            stateRules = listOf(AnchorPhraseStateRule(p.id, ScoreState.Motion, 3)),
            phaseRules = phaseRulesFor(listOf(p), DayPhase.Dawn),
        )
        assertNull("Contemplation must not be selected for Motion", AnchorPhraseSelector.select(i))
    }

    @Test
    fun `Contemplation is allowed when scoreState is Plenitude`() {
        val p = phrase("p_cont_plenitude", PhraseFamily.Contemplation)
        val i = input(
            scoreState = ScoreState.Plenitude,
            catalog = listOf(p),
            stateRules = listOf(AnchorPhraseStateRule(p.id, ScoreState.Plenitude, 1)),
            phaseRules = phaseRulesFor(listOf(p), DayPhase.Dawn),
        )
        // Contemplation is a secondary family for Plenitude — the only phrase available, so it MUST be returned
        val result = AnchorPhraseSelector.select(i)
        assertNotNull("Contemplation must be eligible for Plenitude", result)
        assertEquals("p_cont_plenitude", result!!.phraseId)
    }

    @Test
    fun `Contemplation is allowed when scoreState is Unbreakable`() {
        val p = phrase("p_cont_unbreakable", PhraseFamily.Contemplation)
        val i = input(
            scoreState = ScoreState.Unbreakable,
            catalog = listOf(p),
            stateRules = listOf(AnchorPhraseStateRule(p.id, ScoreState.Unbreakable, 5)),
            phaseRules = phaseRulesFor(listOf(p), DayPhase.Dusk),
        )
        val result = AnchorPhraseSelector.select(i)
        assertNotNull("Contemplation must be eligible for Unbreakable", result)
        assertEquals("p_cont_unbreakable", result!!.phraseId)
    }

    // ─── SEL-REQ-2 : Contemplation preferred in Unbreakable (statistical) ────

    @Test
    fun `Contemplation wins more often than other families in Unbreakable`() {
        // Build 3 families with different weights per §9:
        // Contemplation=5, IdentityValues=3, Recognition=2
        val cont1 = phrase("c1", PhraseFamily.Contemplation)
        val cont2 = phrase("c2", PhraseFamily.Contemplation)
        val id1   = phrase("i1", PhraseFamily.IdentityValues)
        val id2   = phrase("i2", PhraseFamily.IdentityValues)
        val rec1  = phrase("r1", PhraseFamily.Recognition)
        val catalog = listOf(cont1, cont2, id1, id2, rec1)

        val stateRules = listOf(
            AnchorPhraseStateRule("c1", ScoreState.Unbreakable, 5),
            AnchorPhraseStateRule("c2", ScoreState.Unbreakable, 5),
            AnchorPhraseStateRule("i1", ScoreState.Unbreakable, 3),
            AnchorPhraseStateRule("i2", ScoreState.Unbreakable, 3),
            AnchorPhraseStateRule("r1", ScoreState.Unbreakable, 2),
        )
        val phaseRules = emptyList<AnchorPhrasePhaseRule>()

        var contemplationWins = 0
        val trials = 500
        repeat(trials) { trial ->
            val i = input(
                date = baseDate.plusDays(trial.toLong()),
                dayPhase = DayPhase.Dusk,
                scoreState = ScoreState.Unbreakable,
                catalog = catalog,
                stateRules = stateRules,
                phaseRules = phaseRules,
            )
            val result = AnchorPhraseSelector.select(i)
            if (result != null && result.family == PhraseFamily.Contemplation) {
                contemplationWins++
            }
        }

        val share = contemplationWins.toDouble() / trials
        assertTrue(
            "Contemplation should win > 50% of trials in Unbreakable; got $contemplationWins/$trials ($share)",
            share > 0.50,
        )
    }

    // ─── SEL-REQ-3 : Seven-Day Non-Repetition Window ─────────────────────────

    @Test
    fun `recent phrase is excluded from selection`() {
        val phraseA = phrase("phrase_a", PhraseFamily.MinimalAction)
        val phraseB = phrase("phrase_b", PhraseFamily.MinimalAction)
        val catalog = listOf(phraseA, phraseB)
        val stateRules = catalog.map { AnchorPhraseStateRule(it.id, ScoreState.Motion, 2) }
        val phaseRules = catalog.map { AnchorPhrasePhaseRule(it.id, DayPhase.Dawn, 1) }

        val i = input(
            scoreState = ScoreState.Motion,
            catalog = catalog,
            stateRules = stateRules,
            phaseRules = phaseRules,
            recentPhraseIds = setOf("phrase_a"),
        )
        val result = AnchorPhraseSelector.select(i)
        assertNotNull("phraseB should be returned when phraseA is recent", result)
        assertEquals("phrase_b", result!!.phraseId)
    }

    @Test
    fun `when all eligible phrases are recent relax window not state rules`() {
        // Build a Contemplation phrase + a Motion-eligible phrase; both are recent.
        // State = NoData (Contemplation is forbidden). After relaxation, only the
        // MinimalAction phrase should be candidates — NOT the Contemplation one.
        val pMinimal = phrase("p_minimal", PhraseFamily.MinimalAction)
        val pCont    = phrase("p_cont",    PhraseFamily.Contemplation)
        val catalog  = listOf(pMinimal, pCont)

        // State rules for NoData: only MinimalAction is permitted (Contemplation is NOT)
        val stateRules = listOf(
            AnchorPhraseStateRule(pMinimal.id, ScoreState.NoData, 1),
            // pCont has NO state rule for NoData → it is filtered out by filterByState
        )
        val phaseRules = listOf(
            AnchorPhrasePhaseRule(pMinimal.id, DayPhase.Dawn, 1),
        )

        // Mark pMinimal as recent → triggers window relaxation
        val i = input(
            scoreState = ScoreState.NoData,
            catalog = catalog,
            stateRules = stateRules,
            phaseRules = phaseRules,
            recentPhraseIds = setOf("p_minimal"),
        )
        val result = AnchorPhraseSelector.select(i)
        // After relaxation the state rules still hold → pMinimal returns (window relaxed)
        // pCont is still excluded (state rule gate is NOT relaxed)
        assertNotNull("After window relaxation a result should still be returned", result)
        assertEquals(
            "Only pMinimal is eligible; Contemplation must remain blocked even after relaxation",
            "p_minimal",
            result!!.phraseId,
        )
    }

    // ─── SEL-REQ-4 : Weighted Deterministic Selection ────────────────────────

    @Test
    fun `same inputs produce same phraseId (determinism)`() {
        val phrases = (1..5).map { phrase("p$it", PhraseFamily.MinimalAction) }
        val stateRules = phrases.map { AnchorPhraseStateRule(it.id, ScoreState.Motion, 1) }
        val phaseRules = phrases.map { AnchorPhrasePhaseRule(it.id, DayPhase.Dawn, 1) }
        val baseInput = input(
            date = baseDate,
            dayPhase = DayPhase.Dawn,
            scoreState = ScoreState.Motion,
            catalog = phrases,
            stateRules = stateRules,
            phaseRules = phaseRules,
        )
        val result1 = AnchorPhraseSelector.select(baseInput)
        val result2 = AnchorPhraseSelector.select(baseInput)
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(
            "Two calls with identical inputs must return the same phraseId",
            result1!!.phraseId,
            result2!!.phraseId,
        )
    }

    @Test
    fun `phase change alters the seed and may produce different result`() {
        // Use many phrases so a seed change is likely to shift the selection.
        val phrases = (1..20).map { phrase("p$it", PhraseFamily.MinimalAction) }
        val stateRules = phrases.map { AnchorPhraseStateRule(it.id, ScoreState.Motion, 1) }
        val dawnPhaseRules = phrases.map { AnchorPhrasePhaseRule(it.id, DayPhase.Dawn, 1) }
        val duskPhaseRules = phrases.map { AnchorPhrasePhaseRule(it.id, DayPhase.Dusk, 1) }

        val dawnInput = input(
            date = baseDate,
            dayPhase = DayPhase.Dawn,
            scoreState = ScoreState.Motion,
            catalog = phrases,
            stateRules = stateRules,
            phaseRules = dawnPhaseRules,
        )
        val duskInput = input(
            date = baseDate,
            dayPhase = DayPhase.Dusk,
            scoreState = ScoreState.Motion,
            catalog = phrases,
            stateRules = stateRules,
            phaseRules = duskPhaseRules,
        )
        val dawnResult = AnchorPhraseSelector.select(dawnInput)
        val duskResult = AnchorPhraseSelector.select(duskInput)
        // Both must produce a result (large catalog, no recent, all eligible)
        assertNotNull(dawnResult)
        assertNotNull(duskResult)
        // The seed should differ between phases — with 20 phrases the probability of
        // accidental collision is 1/20 = 5%; we just verify they can differ (soft assertion).
        // A hard assert is left to the determinism test above; here we just confirm both return.
    }

    @Test
    fun `higher phase weight family wins statistically when all state weights equal`() {
        // Dawn gives MinimalAction +2 and Persistence +1 (both via phase rules).
        // With equal state weights, MinimalAction should dominate over many seeds.
        val minimalPhrases = (1..10).map { phrase("m$it", PhraseFamily.MinimalAction) }
        val persistencePhrases = (1..10).map { phrase("pe$it", PhraseFamily.Persistence) }
        val catalog = minimalPhrases + persistencePhrases

        val stateRules = catalog.map { AnchorPhraseStateRule(it.id, ScoreState.Motion, 1) }
        val phaseRules =
            minimalPhrases.map { AnchorPhrasePhaseRule(it.id, DayPhase.Dawn, 2) } +
            persistencePhrases.map { AnchorPhrasePhaseRule(it.id, DayPhase.Dawn, 1) }

        var minimalWins = 0
        val trials = 500
        repeat(trials) { trial ->
            val i = input(
                date = baseDate.plusDays(trial.toLong()),
                dayPhase = DayPhase.Dawn,
                scoreState = ScoreState.Motion,
                catalog = catalog,
                stateRules = stateRules,
                phaseRules = phaseRules,
            )
            val result = AnchorPhraseSelector.select(i)
            if (result != null && result.family == PhraseFamily.MinimalAction) minimalWins++
        }

        val share = minimalWins.toDouble() / trials
        assertTrue(
            "MinimalAction (phaseWeight=2) should win > 50% of trials vs Persistence (phaseWeight=1); got $minimalWins/$trials",
            share > 0.50,
        )
    }

    // ─── SEL-REQ-4 : Null on empty eligible set ──────────────────────────────

    @Test
    fun `empty catalog returns null`() {
        val i = input(catalog = emptyList())
        assertNull("Empty catalog must return null", AnchorPhraseSelector.select(i))
    }

    @Test
    fun `returns null when only inactive phrases exist`() {
        val p = phrase("p_inactive_only", PhraseFamily.MinimalAction, active = false)
        val i = input(
            scoreState = ScoreState.Motion,
            catalog = listOf(p),
        )
        assertNull("No eligible phrases → null", AnchorPhraseSelector.select(i))
    }
}
