package dev.panopt.autonomia.data.phrase

import dev.panopt.autonomia.DayPhase
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.data.AnchorPhraseDailySlotEntity
import dev.panopt.autonomia.data.AnchorPhraseImpressionEntity
import dev.panopt.autonomia.domain.phrase.AnchorPhraseSelector
import dev.panopt.autonomia.domain.phrase.AnchorPhraseSelectorInput
import dev.panopt.autonomia.domain.phrase.DayPhasePolicy
import dev.panopt.autonomia.domain.scoring.WeeklyScoreSnapshotConstants
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID

/**
 * Data-layer coordinator that enforces phase stability, reads the current score state
 * from the weekly snapshot, delegates selection to [AnchorPhraseSelector], and persists
 * the daily slot and impression atomically.
 *
 * This is the ONLY component that may write to `anchor_phrase_daily_slots` and
 * `anchor_phrase_impressions`.
 *
 * Patrón espejo de [dev.panopt.autonomia.data.scoring.WeeklyScoreSnapshotWriter]:
 * - Constructor recibe un [AnchorPhraseDataSource] (seam testeable).
 * - Expone un único método `suspend` (sin estado interno).
 * - Dominio puro ([DayPhasePolicy], [AnchorPhraseSelector]) nunca toca Room.
 *
 * ADR-3: el estado de la semana actual se deriva del snapshot ya refrescado por
 * [dev.panopt.autonomia.AutonomiaRepository.refreshCurrentWeeklyScoreSnapshot],
 * que corre inmediatamente ANTES de esta llamada en `runDailyMaintenance`.
 */
class AnchorPhraseResolver(
    private val dataSource: AnchorPhraseDataSource,
) {

    /**
     * Determina y persiste (si corresponde) la frase ancla para [today] y la fase
     * del día derivada de [now].
     *
     * Flujo (spec RSLV-REQ-1..5):
     * 1. Calcula la fase del día actual.
     * 2. Lee el slot existente para (today, phase).
     * 3. Deriva el estado de la semana actual del snapshot (ADR-3). Fallback = NoData.
     * 4. Si el slot existe Y su scoreState == currentState → no hace nada (estabilidad).
     * 5. Si no: carga catálogo, reglas, IDs recientes; invoca [AnchorPhraseSelector.select].
     * 6. Si el selector devuelve null → no escribe (graceful no-op).
     * 7. Si el selector devuelve una selección → escribe slot + impresión atómicamente.
     */
    suspend fun resolveForToday(today: LocalDate, now: LocalDateTime) {
        val phase = DayPhasePolicy.phaseFor(now)
        val dateKey = today.toString()
        val phaseKey = phase.name

        val existingSlot = dataSource.getSlot(dateKey, phaseKey)
        val currentState = deriveCurrentState(today)

        // ADR-1: estabilidad dentro de la fase
        if (existingSlot != null && existingSlot.scoreState == currentState.name) return

        // Necesitamos nueva selección
        val catalog = dataSource.getCatalog()
        val stateRules = dataSource.getStateRules()
        val phaseRules = dataSource.getPhaseRules()

        // Ventana de 7 días (today inclusive): today.minusDays(6) .. today
        val windowStart = today.minusDays(6).toString()
        val windowEnd = dateKey
        val recentIds = dataSource.getRecentImpressionPhraseIds(windowStart, windowEnd)

        val input = AnchorPhraseSelectorInput(
            date = today,
            dayPhase = phase,
            scoreState = currentState,
            catalog = catalog,
            stateRules = stateRules,
            phaseRules = phaseRules,
            recentPhraseIds = recentIds,
        )

        val selection = AnchorPhraseSelector.select(input) ?: return   // graceful no-op

        val now_ = System.currentTimeMillis()
        val newSlot = AnchorPhraseDailySlotEntity(
            date = dateKey,
            dayPhase = phaseKey,
            scoreState = currentState.name,
            phraseId = selection.phraseId,
            resolvedAt = now_,
        )
        val impression = AnchorPhraseImpressionEntity(
            id = UUID.randomUUID().toString(),
            phraseId = selection.phraseId,
            date = dateKey,
            dayPhase = phaseKey,
            scoreState = currentState.name,
            shownAt = now_,
        )

        // Escritura atómica: slot + impresión juntos o ninguno (RSLV-REQ-3)
        dataSource.writeSlotAndImpression(newSlot, impression)
    }

    /**
     * Deriva el [ScoreState] de la semana en curso a partir del snapshot ya almacenado.
     *
     * Filtra por [WeeklyScoreSnapshotConstants.SCORING_VERSION] y [weekStart == mondayOf(today)].
     * Fallback gracioso: [ScoreState.NoData] si la fila aún no existe (primera apertura del lunes).
     */
    private suspend fun deriveCurrentState(today: LocalDate): ScoreState {
        val weekStart = mondayOf(today).toString()
        val snapshot = dataSource.getWeeklySnapshots()
            .firstOrNull {
                it.weekStart == weekStart &&
                    it.scoringVersion == WeeklyScoreSnapshotConstants.SCORING_VERSION
            }
        val stateName = snapshot?.state ?: return ScoreState.NoData
        return runCatching { ScoreState.valueOf(stateName) }.getOrDefault(ScoreState.NoData)
    }

    private fun mondayOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}
