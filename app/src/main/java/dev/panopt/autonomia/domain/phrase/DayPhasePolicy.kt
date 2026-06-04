package dev.panopt.autonomia.domain.phrase

import dev.panopt.autonomia.DayPhase
import java.time.LocalDateTime

/**
 * Pure domain policy: maps any local [LocalDateTime] to a [DayPhase].
 *
 * Rules (frases-ancla.md §7, design §4.2):
 *   - Dawn  → 05:00–14:59 local (inclusive on both ends)
 *   - Dusk  → 15:00–04:59 local (wraps around midnight)
 *
 * No I/O, no Room, no suspend. Always inject [now]; never call [LocalDateTime.now] here.
 */
internal object DayPhasePolicy {

    /**
     * Returns the [DayPhase] for the given local [now].
     *
     * Hour 5..14 → [DayPhase.Dawn]; everything else → [DayPhase.Dusk].
     */
    fun phaseFor(now: LocalDateTime): DayPhase {
        val hour = now.hour
        return if (hour in 5..14) DayPhase.Dawn else DayPhase.Dusk
    }
}
