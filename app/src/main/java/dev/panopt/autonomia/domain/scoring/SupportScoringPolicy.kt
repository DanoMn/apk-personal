package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.LocalDate

internal object SupportScoringPolicy {
    fun evaluate(
        supports: List<ActivityDefinition>,
        weeklyLogsByActivity: Map<String, List<ActivityLog>>,
        weekDates: List<LocalDate>,
    ): Float? {
        if (supports.isEmpty()) return null
        val expectedSupportDays = supports.size * weekDates.size
        if (expectedSupportDays <= 0) return 1f
        val omittedSupportDays = supports.sumOf { support ->
            weeklyLogsByActivity[support.id].orEmpty()
                .filter { it.countsAsDone() }
                .mapNotNull { it.dateAsLocalDate() }
                .distinct()
                .count()
        }
        return (1f - omittedSupportDays.toFloat() / expectedSupportDays.toFloat()).coerceIn(0f, 1f)
    }
}
