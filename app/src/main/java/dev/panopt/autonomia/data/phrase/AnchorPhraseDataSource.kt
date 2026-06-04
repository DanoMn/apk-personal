package dev.panopt.autonomia.data.phrase

import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.AnchorPhrasePhaseRule
import dev.panopt.autonomia.AnchorPhraseStateRule
import dev.panopt.autonomia.data.AnchorPhraseDailySlotEntity
import dev.panopt.autonomia.data.AnchorPhraseImpressionEntity
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import dev.panopt.autonomia.data.AutonomiaDao
import dev.panopt.autonomia.data.WeeklyScoreSnapshotEntity
import dev.panopt.autonomia.data.local.mapper.toDomain
import kotlinx.coroutines.flow.first

/**
 * Seam testeable entre [AnchorPhraseResolver] y Room.
 *
 * Declara SOLO las consultas y escrituras que el resolver necesita, de modo que
 * pueda probarse en JVM puro con un fake — sin instanciar Room ni un Context.
 *
 * Patrón espejo de [dev.panopt.autonomia.data.scoring.WeeklySnapshotDataSource].
 */
interface AnchorPhraseDataSource {

    // ── Lecturas ───────────────────────────────────────────────────────────────

    suspend fun getSlot(date: String, dayPhase: String): AnchorPhraseDailySlotEntity?

    suspend fun getCatalog(): List<AnchorPhrase>

    suspend fun getStateRules(): List<AnchorPhraseStateRule>

    suspend fun getPhaseRules(): List<AnchorPhrasePhaseRule>

    /** IDs de frases mostradas entre [start] y [end] (ventana de 7 días). */
    suspend fun getRecentImpressionPhraseIds(start: String, end: String): Set<String>

    suspend fun getWeeklySnapshots(): List<WeeklyScoreSnapshotEntity>

    // ── Escritura atómica (slot + impresión en una sola transacción) ───────────

    suspend fun writeSlotAndImpression(
        slot: AnchorPhraseDailySlotEntity,
        impression: AnchorPhraseImpressionEntity,
    )
}

/**
 * Adaptador de producción: reenvía cada consulta al [AutonomiaDao] real.
 * La escritura atómica usa [RoomDatabase.withTransaction] (room-ktx) para
 * garantizar que slot + impresión se persisten juntos o ninguno.
 */
internal class DaoAnchorPhraseDataSource(
    private val dao: AutonomiaDao,
    private val db: RoomDatabase,
) : AnchorPhraseDataSource {

    override suspend fun getSlot(date: String, dayPhase: String): AnchorPhraseDailySlotEntity? =
        dao.getAnchorPhraseDailySlot(date, dayPhase)

    override suspend fun getCatalog(): List<AnchorPhrase> =
        dao.observeAnchorPhrases().first().map { it.toDomain() }

    override suspend fun getStateRules(): List<AnchorPhraseStateRule> =
        dao.getAnchorPhraseStateRules().mapNotNull { runCatching { it.toDomain() }.getOrNull() }

    override suspend fun getPhaseRules(): List<AnchorPhrasePhaseRule> =
        dao.getAnchorPhrasePhaseRules().mapNotNull { runCatching { it.toDomain() }.getOrNull() }

    override suspend fun getRecentImpressionPhraseIds(start: String, end: String): Set<String> =
        dao.getAnchorPhraseImpressionsBetween(start, end).map { it.phraseId }.toSet()

    override suspend fun getWeeklySnapshots(): List<WeeklyScoreSnapshotEntity> =
        dao.getWeeklyScoreSnapshotsSnapshot()

    override suspend fun writeSlotAndImpression(
        slot: AnchorPhraseDailySlotEntity,
        impression: AnchorPhraseImpressionEntity,
    ) {
        db.withTransaction {
            dao.upsertAnchorPhraseDailySlot(slot)
            dao.upsertAnchorPhraseImpression(impression)
        }
    }
}
