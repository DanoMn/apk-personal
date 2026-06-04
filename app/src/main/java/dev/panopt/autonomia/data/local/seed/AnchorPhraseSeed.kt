package dev.panopt.autonomia.data.local.seed

import dev.panopt.autonomia.AttributionStatus
import dev.panopt.autonomia.DayPhase
import dev.panopt.autonomia.PhraseFamily
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.data.AnchorPhraseEntity
import dev.panopt.autonomia.data.AnchorPhrasePhaseRuleEntity
import dev.panopt.autonomia.data.AnchorPhraseStateRuleEntity

/**
 * Canonical seed for the anchor-phrase rotation system.
 *
 * Sources:
 *   - docs/dominio/frases-ancla.md §13 (83 phrases, grouped by family)
 *   - docs/dominio/frases-ancla.md §9  (state weights + phase weights)
 *   - docs/dominio/frases-ancla.md §15 (counts per family: total = 83)
 *
 * Design (ADR-2): state/phase rule rows are DERIVED by iterating phrases × their
 * family's weight entry in [stateWeights] / [phaseWeights]. No rule rows are
 * hand-authored. This guarantees that counts and weights are always consistent.
 *
 * Invariants (verified by AnchorPhraseSeedTest):
 *   - 83 active phrases, 0 with null/blank [authorReference].
 *   - Per-family counts: Containment=12, MinimalAction=14, RegulationClarity=14,
 *     Persistence=10, IdentityValues=10, Recognition=10, Contemplation=13.
 *   - State rules derive from [stateWeights] (no Containment rows for Unbreakable, etc.)
 *   - Phase rules derive from [phaseWeights] (no Persistence rows for Dusk, etc.)
 */
internal object AnchorPhraseSeed {

    /** Stable seed timestamp (epoch ms). All phrases share this createdAt/updatedAt. */
    private const val SEED_TS = 1_748_995_200_000L // 2026-06-04 00:00:00 UTC

    // ─── §9 — State weights (by family) ──────────────────────────────────────
    //
    // Only families listed here generate state-rule rows.
    // Absence = not permitted (e.g., Containment absent from Unbreakable).

    val stateWeights: Map<ScoreState, Map<PhraseFamily, Int>> = mapOf(
        ScoreState.NoData to mapOf(
            PhraseFamily.Containment to 4,
            PhraseFamily.MinimalAction to 1,
        ),
        ScoreState.Restoration to mapOf(
            PhraseFamily.Containment to 4,
            PhraseFamily.MinimalAction to 3,
            PhraseFamily.RegulationClarity to 1,
        ),
        ScoreState.Attention to mapOf(
            PhraseFamily.MinimalAction to 4,
            PhraseFamily.RegulationClarity to 4,
            PhraseFamily.Containment to 1,
            PhraseFamily.Persistence to 1,
        ),
        ScoreState.Motion to mapOf(
            PhraseFamily.Persistence to 4,
            PhraseFamily.MinimalAction to 3,
            PhraseFamily.RegulationClarity to 2,
            PhraseFamily.IdentityValues to 1,
        ),
        ScoreState.Plenitude to mapOf(
            PhraseFamily.Recognition to 4,
            PhraseFamily.RegulationClarity to 2,
            PhraseFamily.IdentityValues to 2,
            PhraseFamily.Contemplation to 1,
        ),
        ScoreState.Unbreakable to mapOf(
            PhraseFamily.Contemplation to 5,
            PhraseFamily.IdentityValues to 3,
            PhraseFamily.Recognition to 2,
        ),
    )

    // ─── §9 — Phase weights (extra weight by family for each phase) ───────────
    //
    // Only families listed here generate phase-rule rows.
    // Absence = no phase rule for that family/phase combination.

    val phaseWeights: Map<DayPhase, Map<PhraseFamily, Int>> = mapOf(
        DayPhase.Dawn to mapOf(
            PhraseFamily.MinimalAction to 2,
            PhraseFamily.IdentityValues to 1,
            PhraseFamily.Persistence to 1,
        ),
        DayPhase.Dusk to mapOf(
            PhraseFamily.RegulationClarity to 2,
            PhraseFamily.Recognition to 2,
            PhraseFamily.Contemplation to 1,
            PhraseFamily.Containment to 1,
        ),
    )

    // ─── §13 — The 83 canonical phrases ──────────────────────────────────────

    val phrases: List<AnchorPhraseEntity> = buildList {
        // ── Contencion (12) ──────────────────────────────────────────────────
        add(phrase("phrase_containment_001", PhraseFamily.Containment,
            "This is a moment of suffering. Suffering is part of life. May I be kind to myself.",
            "Kristin Neff", "en", AttributionStatus.Clear, 10))
        add(phrase("phrase_containment_002", PhraseFamily.Containment,
            "If your compassion does not include yourself, it is incomplete.",
            "Jack Kornfield", "en", AttributionStatus.Clear, 20))
        add(phrase("phrase_containment_003", PhraseFamily.Containment,
            "Compassion is not a relationship between the healer and the wounded. It's a relationship between equals.",
            "Pema Chodron", "en", AttributionStatus.Clear, 30))
        add(phrase("phrase_containment_004", PhraseFamily.Containment,
            "In some ways suffering ceases to be suffering at the moment it finds a meaning.",
            "Viktor Frankl", "en", AttributionStatus.Clear, 40))
        add(phrase("phrase_containment_005", PhraseFamily.Containment,
            "There can be no lotus flower without the mud.",
            "Thich Nhat Hanh", "en", AttributionStatus.Clear, 50))
        add(phrase("phrase_containment_006", PhraseFamily.Containment,
            "Hoy es siempre todavia.",
            "Antonio Machado", "es", AttributionStatus.Clear, 60))
        add(phrase("phrase_containment_007", PhraseFamily.Containment,
            "Attention is the rarest and purest form of generosity.",
            "Simone Weil", "en", AttributionStatus.Clear, 70))
        add(phrase("phrase_containment_008", PhraseFamily.Containment,
            "In the depths of winter, I finally learned that within me there lay an invincible summer.",
            "Albert Camus", "en", AttributionStatus.Clear, 80))
        add(phrase("phrase_containment_009", PhraseFamily.Containment,
            "Quien ha visto la Esperanza, no la olvida.",
            "Octavio Paz", "es", AttributionStatus.NeedsReview, 90))
        add(phrase("phrase_containment_010", PhraseFamily.Containment,
            "Only to the extent that we expose ourselves can that which is indestructible be found in us.",
            "Pema Chodron", "en", AttributionStatus.Clear, 100))
        add(phrase("phrase_containment_011", PhraseFamily.Containment,
            "None of us is okay and all of us are fine. We are walking, talking paradoxes.",
            "Pema Chodron", "en", AttributionStatus.Clear, 110))
        add(phrase("phrase_containment_012", PhraseFamily.Containment,
            "Un viaje de mil millas comienza con un solo paso.",
            "Lao Tse", "es", AttributionStatus.Traditional, 120))

        // ── Accion minima (14) ───────────────────────────────────────────────
        add(phrase("phrase_minimal_action_001", PhraseFamily.MinimalAction,
            "For the things we have to learn before we can do them, we learn by doing them.",
            "Aristoteles", "en", AttributionStatus.Clear, 10))
        add(phrase("phrase_minimal_action_002", PhraseFamily.MinimalAction,
            "Great things are not done by impulse, but by a series of small things brought together.",
            "Vincent van Gogh", "en", AttributionStatus.Clear, 20))
        add(phrase("phrase_minimal_action_003", PhraseFamily.MinimalAction,
            "Action is the antidote to despair.",
            "Joan Baez", "en", AttributionStatus.Clear, 30))
        add(phrase("phrase_minimal_action_004", PhraseFamily.MinimalAction,
            "Caminante, no hay camino, se hace camino al andar.",
            "Antonio Machado", "es", AttributionStatus.Clear, 40))
        add(phrase("phrase_minimal_action_005", PhraseFamily.MinimalAction,
            "The impediment to action advances action. What stands in the way becomes the way.",
            "Marco Aurelio", "en", AttributionStatus.Clear, 50))
        add(phrase("phrase_minimal_action_006", PhraseFamily.MinimalAction,
            "You never see further than your headlights, but you can make the whole trip that way.",
            "E. L. Doctorow", "en", AttributionStatus.Clear, 60))
        add(phrase("phrase_minimal_action_007", PhraseFamily.MinimalAction,
            "Knowing is not enough; we must apply. Willing is not enough; we must do.",
            "Johann Wolfgang von Goethe", "en", AttributionStatus.Traditional, 70))
        add(phrase("phrase_minimal_action_008", PhraseFamily.MinimalAction,
            "What saves a man is to take a step. Then another step.",
            "Antoine de Saint-Exupery", "en", AttributionStatus.Clear, 80))
        add(phrase("phrase_minimal_action_009", PhraseFamily.MinimalAction,
            "In order to do something well we must first be willing to do it badly.",
            "Julia Cameron", "en", AttributionStatus.Clear, 90))
        add(phrase("phrase_minimal_action_010", PhraseFamily.MinimalAction,
            "A small daily task, if it be really daily, will beat the labours of a spasmodic Hercules.",
            "Anthony Trollope", "en", AttributionStatus.Clear, 100))
        add(phrase("phrase_minimal_action_011", PhraseFamily.MinimalAction,
            "Well done is better than well said.",
            "Benjamin Franklin", "en", AttributionStatus.Traditional, 110))
        add(phrase("phrase_minimal_action_012", PhraseFamily.MinimalAction,
            "Life can only be understood backwards; but it must be lived forwards.",
            "Soren Kierkegaard", "en", AttributionStatus.Clear, 120))
        add(phrase("phrase_minimal_action_013", PhraseFamily.MinimalAction,
            "Inspiration exists, but it has to find you working.",
            "Pablo Picasso", "en", AttributionStatus.Traditional, 130))
        add(phrase("phrase_minimal_action_014", PhraseFamily.MinimalAction,
            "Nothing will work unless you do.",
            "Maya Angelou", "en", AttributionStatus.Clear, 140))

        // ── Claridad / regulacion (14) ───────────────────────────────────────
        add(phrase("phrase_regulation_clarity_001", PhraseFamily.RegulationClarity,
            "Sufrimos mas a menudo en la imaginacion que en la realidad.",
            "Seneca", "es", AttributionStatus.Traditional, 10))
        add(phrase("phrase_regulation_clarity_002", PhraseFamily.RegulationClarity,
            "There is nothing either good or bad but thinking makes it so.",
            "William Shakespeare, Hamlet", "en", AttributionStatus.Clear, 20))
        add(phrase("phrase_regulation_clarity_003", PhraseFamily.RegulationClarity,
            "Feelings come and go like clouds in a windy sky. Conscious breathing is my anchor.",
            "Thich Nhat Hanh", "en", AttributionStatus.Clear, 30))
        add(phrase("phrase_regulation_clarity_004", PhraseFamily.RegulationClarity,
            "Name it to tame it.",
            "Daniel J. Siegel", "en", AttributionStatus.Clear, 40))
        add(phrase("phrase_regulation_clarity_005", PhraseFamily.RegulationClarity,
            "The curious paradox is that when I accept myself just as I am, then I can change.",
            "Carl Rogers", "en", AttributionStatus.Clear, 50))
        add(phrase("phrase_regulation_clarity_006", PhraseFamily.RegulationClarity,
            "The first principle is that you must not fool yourself, and you are the easiest person to fool.",
            "Richard Feynman", "en", AttributionStatus.Clear, 60))
        add(phrase("phrase_regulation_clarity_007", PhraseFamily.RegulationClarity,
            "This is called the sacred pause, a moment where we stop and release our identification with problems and reactions.",
            "Jack Kornfield", "en", AttributionStatus.Clear, 70))
        add(phrase("phrase_regulation_clarity_008", PhraseFamily.RegulationClarity,
            "Tienes paciencia para esperar hasta que el lodo se asiente y el agua este clara?",
            "Lao Tse", "es", AttributionStatus.Traditional, 80))
        add(phrase("phrase_regulation_clarity_009", PhraseFamily.RegulationClarity,
            "No era solo banarme lo que queria, sino mantener mi mente en buen orden.",
            "Epicteto", "es", AttributionStatus.NeedsReview, 90))
        add(phrase("phrase_regulation_clarity_010", PhraseFamily.RegulationClarity,
            "Retirate en ti mismo.",
            "Marco Aurelio", "es", AttributionStatus.Traditional, 100))
        add(phrase("phrase_regulation_clarity_011", PhraseFamily.RegulationClarity,
            "Breathing in, I am aware of my feeling. Breathing out, I calm my feeling.",
            "Thich Nhat Hanh / Plum Village", "en", AttributionStatus.Clear, 110))
        add(phrase("phrase_regulation_clarity_012", PhraseFamily.RegulationClarity,
            "Please calm down, my friend. Lay down your sharp sword of conceptual thinking.",
            "Thich Nhat Hanh", "en", AttributionStatus.Clear, 120))
        add(phrase("phrase_regulation_clarity_013", PhraseFamily.RegulationClarity,
            "As soon as the sun of awareness shines, at that very moment a great change takes place.",
            "Thich Nhat Hanh", "en", AttributionStatus.Clear, 130))
        add(phrase("phrase_regulation_clarity_014", PhraseFamily.RegulationClarity,
            "Not everything that is faced can be changed, but nothing can be changed until it is faced.",
            "James Baldwin, No Name in the Street", "en", AttributionStatus.Clear, 140))

        // ── Persistencia (10) ────────────────────────────────────────────────
        add(phrase("phrase_persistence_001", PhraseFamily.Persistence,
            "Todo lo que sucede es soportable o no. Si es soportable, soportalo.",
            "Marco Aurelio, Meditaciones", "es", AttributionStatus.Clear, 10))
        add(phrase("phrase_persistence_002", PhraseFamily.Persistence,
            "Ever tried. Ever failed. No matter. Try again. Fail again. Fail better.",
            "Samuel Beckett, Worstward Ho", "en", AttributionStatus.Clear, 20))
        add(phrase("phrase_persistence_003", PhraseFamily.Persistence,
            "You may encounter many defeats, but you must not be defeated.",
            "Maya Angelou", "en", AttributionStatus.Clear, 30))
        add(phrase("phrase_persistence_004", PhraseFamily.Persistence,
            "The world breaks everyone and afterward many are strong at the broken places.",
            "Ernest Hemingway, A Farewell to Arms", "en", AttributionStatus.Clear, 40))
        add(phrase("phrase_persistence_005", PhraseFamily.Persistence,
            "Quien tiene un porque para vivir puede soportar casi cualquier como.",
            "Friedrich Nietzsche, El crepusculo de los idolos", "es", AttributionStatus.Clear, 50))
        add(phrase("phrase_persistence_006", PhraseFamily.Persistence,
            "Everything is gestation and then bringing forth.",
            "Rainer Maria Rilke, Letters to a Young Poet", "en", AttributionStatus.Clear, 60))
        add(phrase("phrase_persistence_007", PhraseFamily.Persistence,
            "La lucha misma hacia las cumbres basta para llenar un corazon humano.",
            "Albert Camus, El mito de Sisifo", "es", AttributionStatus.Clear, 70))
        add(phrase("phrase_persistence_008", PhraseFamily.Persistence,
            "Things take the time they take. Don't worry.",
            "Mary Oliver, \"Don't Worry\"", "en", AttributionStatus.Clear, 80))
        add(phrase("phrase_persistence_009", PhraseFamily.Persistence,
            "It is good to have an end to journey toward; but it is the journey that matters, in the end.",
            "Ursula K. Le Guin, The Left Hand of Darkness", "en", AttributionStatus.Clear, 90))
        add(phrase("phrase_persistence_010", PhraseFamily.Persistence,
            "I learned this, at least, by my experiment: that if one advances confidently in the direction of his dreams...",
            "Henry David Thoreau, Walden", "en", AttributionStatus.Clear, 100))

        // ── Identidad / valores (10) ─────────────────────────────────────────
        add(phrase("phrase_identity_values_001", PhraseFamily.IdentityValues,
            "This above all: to thine own self be true.",
            "William Shakespeare, Hamlet", "en", AttributionStatus.Clear, 10))
        add(phrase("phrase_identity_values_002", PhraseFamily.IdentityValues,
            "No legacy is so rich as honesty.",
            "William Shakespeare, All's Well That Ends Well", "en", AttributionStatus.Clear, 20))
        add(phrase("phrase_identity_values_003", PhraseFamily.IdentityValues,
            "I wished to live deliberately, to front only the essential facts of life.",
            "Henry David Thoreau, Walden", "en", AttributionStatus.Clear, 30))
        add(phrase("phrase_identity_values_004", PhraseFamily.IdentityValues,
            "The unexamined life is not worth living.",
            "Socrates / Platon, Apology", "en", AttributionStatus.Traditional, 40))
        add(phrase("phrase_identity_values_005", PhraseFamily.IdentityValues,
            "Act only according to that maxim whereby you can at the same time will that it should become a universal law.",
            "Immanuel Kant", "en", AttributionStatus.Clear, 50))
        add(phrase("phrase_identity_values_006", PhraseFamily.IdentityValues,
            "If I didn't define myself for myself, I would be crunched into other people's fantasies for me and eaten alive.",
            "Audre Lorde", "en", AttributionStatus.Clear, 60))
        add(phrase("phrase_identity_values_007", PhraseFamily.IdentityValues,
            "Freeing yourself was one thing, claiming ownership of that freed self was another.",
            "Toni Morrison, Beloved", "en", AttributionStatus.Clear, 70))
        add(phrase("phrase_identity_values_008", PhraseFamily.IdentityValues,
            "Definitions belong to the definers, not the defined.",
            "Toni Morrison, Beloved", "en", AttributionStatus.Clear, 80))
        add(phrase("phrase_identity_values_009", PhraseFamily.IdentityValues,
            "Do I contradict myself? Very well then I contradict myself, I am large, I contain multitudes.",
            "Walt Whitman, Song of Myself", "en", AttributionStatus.Clear, 90))
        add(phrase("phrase_identity_values_010", PhraseFamily.IdentityValues,
            "Your silence will not protect you.",
            "Audre Lorde", "en", AttributionStatus.Clear, 100))

        // ── Reconocimiento (10) ──────────────────────────────────────────────
        add(phrase("phrase_recognition_001", PhraseFamily.Recognition,
            "I am larger, better than I thought, I did not know I held so much goodness.",
            "Walt Whitman, Song of the Open Road", "en", AttributionStatus.Clear, 10))
        add(phrase("phrase_recognition_002", PhraseFamily.Recognition,
            "I celebrate myself, and sing myself.",
            "Walt Whitman, Song of Myself", "en", AttributionStatus.Clear, 20))
        add(phrase("phrase_recognition_003", PhraseFamily.Recognition,
            "Give back your heart to itself.",
            "Derek Walcott, Love After Love", "en", AttributionStatus.Clear, 30))
        add(phrase("phrase_recognition_004", PhraseFamily.Recognition,
            "And now that you don't have to be perfect, you can be good.",
            "John Steinbeck, East of Eden", "en", AttributionStatus.Clear, 40))
        add(phrase("phrase_recognition_005", PhraseFamily.Recognition,
            "Each of us is more than the worst thing we've ever done.",
            "Bryan Stevenson, Just Mercy / EJI", "en", AttributionStatus.Clear, 50))
        add(phrase("phrase_recognition_006", PhraseFamily.Recognition,
            "Instructions for living a life: Pay attention. Be astonished. Tell about it.",
            "Mary Oliver, \"Sometimes\"", "en", AttributionStatus.Clear, 60))
        add(phrase("phrase_recognition_007", PhraseFamily.Recognition,
            "You do not have to be good.",
            "Mary Oliver, \"Wild Geese\"", "en", AttributionStatus.Clear, 70))
        add(phrase("phrase_recognition_008", PhraseFamily.Recognition,
            "Before you know kindness as the deepest thing inside, you must know sorrow as the other deepest thing.",
            "Naomi Shihab Nye, \"Kindness\"", "en", AttributionStatus.Clear, 80))
        add(phrase("phrase_recognition_009", PhraseFamily.Recognition,
            "It is a serious thing just to be alive on this fresh morning in this broken world.",
            "Mary Oliver, \"Invitation\"", "en", AttributionStatus.Clear, 90))
        add(phrase("phrase_recognition_010", PhraseFamily.Recognition,
            "We can only be said to be alive in those moments when our hearts are conscious of our treasure.",
            "Thornton Wilder, The Woman of Andros", "en", AttributionStatus.Clear, 100))

        // ── Contemplacion (13) ───────────────────────────────────────────────
        add(phrase("phrase_contemplation_001", PhraseFamily.Contemplation,
            "Out beyond ideas of wrongdoing and rightdoing, there is a field. I'll meet you there.",
            "Rumi / version de Coleman Barks", "en", AttributionStatus.Disputed, 10))
        add(phrase("phrase_contemplation_002", PhraseFamily.Contemplation,
            "The quieter you become, the more you are able to hear.",
            "Lema asociado a Kali Linux / atribucion popular discutida", "en", AttributionStatus.Disputed, 20))
        add(phrase("phrase_contemplation_003", PhraseFamily.Contemplation,
            "Vende tu astucia y compra asombro.",
            "Rumi, Masnavi / traduccion libre", "es", AttributionStatus.Traditional, 30))
        add(phrase("phrase_contemplation_004", PhraseFamily.Contemplation,
            "El ojo con que veo a Dios es el mismo ojo con que Dios me ve.",
            "Meister Eckhart", "es", AttributionStatus.Traditional, 40))
        add(phrase("phrase_contemplation_005", PhraseFamily.Contemplation,
            "Para venir a lo que no sabes, has de ir por donde no sabes.",
            "San Juan de la Cruz, Monte de Perfeccion", "es", AttributionStatus.Clear, 50))
        add(phrase("phrase_contemplation_006", PhraseFamily.Contemplation,
            "El Tao que puede ser nombrado no es el Tao eterno.",
            "Lao Tse, Tao Te Ching", "es", AttributionStatus.Traditional, 60))
        add(phrase("phrase_contemplation_007", PhraseFamily.Contemplation,
            "Estudiar el camino de Buda es estudiarse a uno mismo; estudiarse a uno mismo es olvidarse de uno mismo.",
            "Dogen, Genjokoan", "es", AttributionStatus.Clear, 70))
        add(phrase("phrase_contemplation_008", PhraseFamily.Contemplation,
            "Por amor puede ser alcanzado y sostenido; por pensamiento, nunca.",
            "Anonimo, The Cloud of Unknowing", "es", AttributionStatus.Traditional, 80))
        add(phrase("phrase_contemplation_009", PhraseFamily.Contemplation,
            "La rosa no tiene porque; florece porque florece.",
            "Angelus Silesius, Cherubinischer Wandersmann", "es", AttributionStatus.Clear, 90))
        add(phrase("phrase_contemplation_010", PhraseFamily.Contemplation,
            "El Reino esta dentro de ti y fuera de ti.",
            "Evangelio de Tomas", "es", AttributionStatus.Traditional, 100))
        add(phrase("phrase_contemplation_011", PhraseFamily.Contemplation,
            "La vida es el vuelo del solo hacia el Solo.",
            "Plotino", "es", AttributionStatus.Traditional, 110))
        add(phrase("phrase_contemplation_012", PhraseFamily.Contemplation,
            "He vivido al borde de la locura, queriendo saber razones, tocando una puerta. Se abre. He estado tocando desde dentro.",
            "Rumi", "es", AttributionStatus.Traditional, 120))
        add(phrase("phrase_contemplation_013", PhraseFamily.Contemplation,
            "Que es la vida? Un frenesi. Que es la vida? Una ilusion, una sombra, una ficcion; y el mayor bien es pequeno, que toda la vida es sueno, y los suenos, suenos son.",
            "Pedro Calderon de la Barca, La vida es sueno", "es", AttributionStatus.Clear, 130))
    }

    // ─── Derived: state rules (phrases × stateWeights) ───────────────────────
    //
    // For each phrase, for each state, if the phrase's family has a weight in that
    // state's map, emit one AnchorPhraseStateRuleEntity row.
    // No hand-authored rows. No family absent from the map generates a row.

    val stateRules: List<AnchorPhraseStateRuleEntity> = buildList {
        phrases.forEach { phrase ->
            val family = runCatching { PhraseFamily.valueOf(phrase.family) }.getOrNull()
                ?: return@forEach
            stateWeights.forEach { (state, familyWeights) ->
                val weight = familyWeights[family] ?: return@forEach
                add(AnchorPhraseStateRuleEntity(
                    phraseId = phrase.id,
                    scoreState = state.name,
                    weight = weight,
                ))
            }
        }
    }

    // ─── Derived: phase rules (phrases × phaseWeights) ───────────────────────
    //
    // Same derivation pattern as stateRules.

    val phaseRules: List<AnchorPhrasePhaseRuleEntity> = buildList {
        phrases.forEach { phrase ->
            val family = runCatching { PhraseFamily.valueOf(phrase.family) }.getOrNull()
                ?: return@forEach
            phaseWeights.forEach { (phase, familyWeights) ->
                val weight = familyWeights[family] ?: return@forEach
                add(AnchorPhrasePhaseRuleEntity(
                    phraseId = phrase.id,
                    dayPhase = phase.name,
                    weight = weight,
                ))
            }
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private fun phrase(
        id: String,
        family: PhraseFamily,
        text: String,
        author: String,
        language: String,
        attribution: AttributionStatus,
        sortOrder: Int,
    ): AnchorPhraseEntity = AnchorPhraseEntity(
        id = id,
        text = text,
        authorReference = author,
        family = family.name,
        language = language,
        attributionStatus = attribution.name,
        active = true,
        sortOrder = sortOrder,
        createdAt = SEED_TS,
        updatedAt = SEED_TS,
    )
}
