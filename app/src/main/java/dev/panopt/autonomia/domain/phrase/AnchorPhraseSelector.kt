package dev.panopt.autonomia.domain.phrase

import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.AnchorPhrasePhaseRule
import dev.panopt.autonomia.AnchorPhraseSelection
import dev.panopt.autonomia.AnchorPhraseStateRule
import dev.panopt.autonomia.DayPhase
import dev.panopt.autonomia.PhraseFamily
import dev.panopt.autonomia.ScoreState
import java.time.LocalDate
import kotlin.random.Random

/**
 * Input snapshot for [AnchorPhraseSelector.select].
 *
 * All fields are pure value types — no Room, no I/O, no suspend.
 * Place this in the same file as the selector to keep the API surface small
 * (tasks.md task 4.2: may live in Models.kt or here; size is small so it lives here).
 *
 * @param date          The calendar date for which a phrase is being resolved.
 * @param dayPhase      The phase (Dawn/Dusk) for which a phrase is being resolved.
 * @param scoreState    The current weekly score state driving family selection.
 * @param catalog       All known phrases (active and inactive).
 * @param stateRules    Per-phrase per-state weight rules (§9, derived from AnchorPhraseSeed).
 * @param phaseRules    Per-phrase per-phase weight rules (§9, derived from AnchorPhraseSeed).
 * @param recentPhraseIds IDs of phrases shown in the last 7 days (7-day non-repetition window, §8.4).
 */
data class AnchorPhraseSelectorInput(
    val date: LocalDate,
    val dayPhase: DayPhase,
    val scoreState: ScoreState,
    val catalog: List<AnchorPhrase>,
    val stateRules: List<AnchorPhraseStateRule>,
    val phaseRules: List<AnchorPhrasePhaseRule>,
    val recentPhraseIds: Set<String>,
)

/**
 * Pure, deterministic anchor-phrase selector.
 *
 * Mirrors the scoring "gears" pattern (BaseStatePolicy, LayerScoringPolicy, etc.):
 * [select] ONLY orchestrates small composable private functions — it does not contain
 * any rule logic itself. Each private function has a single responsibility.
 *
 * Rules implemented (frases-ancla.md §3, §6, §8, §9):
 *   1. [filterEligible]  — active && authorReference non-blank.
 *   2. [filterByState]   — only families allowed for scoreState; Contemplation ONLY in Plenitude/Unbreakable (§8.6).
 *   3. [excludeRecent]   — drop phrases in the 7-day window; if all excluded → relax window, NOT state rules (§8.7).
 *   4. [weightOf]        — stateWeight(family) + phaseWeight(family) (§9).
 *   5. [weightedPick]    — deterministic weighted choice using Random(seed) where seed = hash(date, dayPhase).
 *
 * Returns null when no eligible phrase exists (caller handles graceful fallback).
 * No I/O. No Room. No suspend. Always inject inputs — never call now() here.
 */
internal object AnchorPhraseSelector {

    // ─── Families allowed per scoreState (§6) ────────────────────────────────
    //
    // Contemplation is explicitly controlled: allowed ONLY in Plenitude and Unbreakable (§8.6).
    // For all other states it must be absent even if a state rule with non-zero weight exists
    // (the spec overrides the rules table for this family specifically).

    private val allowedFamiliesByState: Map<ScoreState, Set<PhraseFamily>> = mapOf(
        ScoreState.NoData to setOf(
            PhraseFamily.Containment,
            PhraseFamily.MinimalAction,
        ),
        ScoreState.Restoration to setOf(
            PhraseFamily.Containment,
            PhraseFamily.MinimalAction,
            PhraseFamily.RegulationClarity,
        ),
        ScoreState.Attention to setOf(
            PhraseFamily.MinimalAction,
            PhraseFamily.RegulationClarity,
            PhraseFamily.Containment,
            PhraseFamily.Persistence,
        ),
        ScoreState.Motion to setOf(
            PhraseFamily.Persistence,
            PhraseFamily.MinimalAction,
            PhraseFamily.RegulationClarity,
            PhraseFamily.IdentityValues,
        ),
        ScoreState.Plenitude to setOf(
            PhraseFamily.Recognition,
            PhraseFamily.RegulationClarity,
            PhraseFamily.IdentityValues,
            PhraseFamily.Contemplation,   // secondary — allowed as antesala (§8.6)
        ),
        ScoreState.Unbreakable to setOf(
            PhraseFamily.Contemplation,   // primary (§8.6)
            PhraseFamily.IdentityValues,
            PhraseFamily.Recognition,
        ),
    )

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Select one anchor phrase for the given [input].
     *
     * Pipeline:
     *   eligibles  = filterEligible(catalog)
     *   byState    = filterByState(eligibles, scoreState)
     *   candidates = excludeRecent(byState, recentPhraseIds)   [relaxes window if empty]
     *   weighted   = candidates.map { it to weightOf(it, ...) }
     *   return       weightedPick(weighted, seed(date, dayPhase))
     *
     * Returns null if the eligible set is empty after all filters.
     */
    fun select(input: AnchorPhraseSelectorInput): AnchorPhraseSelection? {
        val eligibles  = filterEligible(input.catalog)
        val byState    = filterByState(eligibles, input.scoreState)
        val candidates = excludeRecent(byState, input.recentPhraseIds)

        if (candidates.isEmpty()) return null

        val stateWeightIndex = input.stateRules.groupBy { it.phraseId }
        val phaseWeightIndex = input.phaseRules.groupBy  { it.phraseId }

        val weighted = candidates.map { phrase ->
            phrase to weightOf(phrase, input.scoreState, input.dayPhase, stateWeightIndex, phaseWeightIndex)
        }

        val seed   = stableHash(input.date, input.dayPhase)
        val chosen = weightedPick(weighted, seed) ?: return null

        return AnchorPhraseSelection(
            phraseId        = chosen.id,
            text            = chosen.text,
            authorReference = chosen.authorReference.orEmpty(),
            family          = chosen.family,
            scoreState      = input.scoreState,
            dayPhase        = input.dayPhase,
        )
    }

    // ─── Gear 1 : Eligibility filter (§3, §8.5) ──────────────────────────────

    /**
     * Retains only phrases that are active AND have a non-blank authorReference.
     * Applied first, before any other rule.
     */
    private fun filterEligible(catalog: List<AnchorPhrase>): List<AnchorPhrase> =
        catalog.filter { it.active && !it.authorReference.isNullOrBlank() }

    // ─── Gear 2 : State family filter (§6, §8.6) ─────────────────────────────

    /**
     * Retains only phrases whose family is allowed for the given [scoreState].
     * Contemplation is allowed ONLY for [ScoreState.Plenitude] and [ScoreState.Unbreakable].
     * If [scoreState] is not in [allowedFamiliesByState], all families are permitted
     * (safe fallback — should not happen in production given the complete mapping above).
     */
    private fun filterByState(
        phrases: List<AnchorPhrase>,
        scoreState: ScoreState,
    ): List<AnchorPhrase> {
        val allowed = allowedFamiliesByState[scoreState] ?: return phrases
        return phrases.filter { it.family in allowed }
    }

    // ─── Gear 3 : Recent-exclusion + window relaxation (§8.4, §8.7) ──────────

    /**
     * Excludes phrases whose ID appears in [recentPhraseIds] (7-day window).
     *
     * Relaxation rule (§8.7): if excluding recent phrases leaves ZERO candidates,
     * retry with the window relaxed (ignore [recentPhraseIds]).
     * The state family rules are NEVER relaxed — Contemplation gating always holds.
     */
    private fun excludeRecent(
        phrases: List<AnchorPhrase>,
        recentPhraseIds: Set<String>,
    ): List<AnchorPhrase> {
        if (recentPhraseIds.isEmpty()) return phrases
        val withoutRecent = phrases.filter { it.id !in recentPhraseIds }
        return if (withoutRecent.isNotEmpty()) withoutRecent else phrases   // relax window
    }

    // ─── Gear 4 : Weight computation (§9) ────────────────────────────────────

    /**
     * Combined weight for a single phrase:
     *   stateWeight(phrase, scoreState) + phaseWeight(phrase, dayPhase)
     *
     * Weights are looked up from pre-indexed maps (phraseId → rules) to avoid O(n²) scanning.
     * Minimum weight is 1 to keep every candidate eligible even if no rules exist.
     */
    private fun weightOf(
        phrase: AnchorPhrase,
        scoreState: ScoreState,
        dayPhase: DayPhase,
        stateWeightIndex: Map<String, List<AnchorPhraseStateRule>>,
        phaseWeightIndex: Map<String, List<AnchorPhrasePhaseRule>>,
    ): Int {
        val sw = stateWeightIndex[phrase.id]
            ?.firstOrNull { it.scoreState == scoreState }
            ?.weight ?: 0
        val pw = phaseWeightIndex[phrase.id]
            ?.firstOrNull { it.dayPhase == dayPhase }
            ?.weight ?: 0
        return maxOf(1, sw + pw)
    }

    // ─── Gear 5 : Deterministic weighted pick (§8, design §4.3) ─────────────

    /**
     * Picks one phrase from [weightedPhrases] using a deterministic [Random] seeded
     * with [seed]. The probability of each phrase being chosen is proportional to its weight.
     *
     * Returns null only if [weightedPhrases] is empty (guard; should be caught above).
     */
    private fun weightedPick(
        weightedPhrases: List<Pair<AnchorPhrase, Int>>,
        seed: Long,
    ): AnchorPhrase? {
        if (weightedPhrases.isEmpty()) return null

        val totalWeight = weightedPhrases.sumOf { it.second }
        val rng    = Random(seed)
        var target = rng.nextInt(totalWeight) + 1   // 1..totalWeight inclusive

        for ((phrase, weight) in weightedPhrases) {
            target -= weight
            if (target <= 0) return phrase
        }
        // Fallback (floating-point edge case should not occur with Int weights)
        return weightedPhrases.last().first
    }

    // ─── Seed derivation ─────────────────────────────────────────────────────

    /**
     * Produces a stable Long seed from [date] and [dayPhase].
     *
     * Properties:
     *   - Same (date, dayPhase) → same seed → same phrase selection (stable within a phase).
     *   - Different dayPhase → different seed (Dawn ≠ Dusk even on the same date).
     *   - Uses epochDay (stable across locales/timezones) shifted by a phase ordinal multiplier.
     */
    private fun stableHash(date: LocalDate, dayPhase: DayPhase): Long {
        val epochDay = date.toEpochDay()
        val phaseOffset = (dayPhase.ordinal + 1).toLong() * 1_000_000L
        return epochDay + phaseOffset
    }
}
