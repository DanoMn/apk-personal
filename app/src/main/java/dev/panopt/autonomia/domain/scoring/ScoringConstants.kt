package dev.panopt.autonomia.domain.scoring

internal object ScoringConstants {
    const val BODY_LAYER_ID = "layer_cuerpo"
    const val CONDUCT_LAYER_ID = "layer_conducta"
    const val ANCHOR_FREQUENCY_WEIGHT = 0.70f
    const val ANCHOR_VALUE_WEIGHT = 0.30f
    const val ANCHOR_WITH_SUPPORT_WEIGHT = 0.80f
    const val SUPPORT_WEIGHT = 0.20f
    const val SLEEP_WEIGHT_IN_BODY = 0.30f
    const val SOBRIETY_WEIGHT_IN_CONDUCT = 0.30f
    const val WEEKLY_AVERAGE_WEIGHT = 0.75f
    const val WEEKLY_WORST_WEIGHT = 0.25f
    const val TASK_MOMENTUM_MAX = 0.050f
    const val ANCHOR_SURPLUS_MAX = 0.100f
    const val SOBRIETY_PENDING_CLEAN_VALUE = 0.50f
    const val SOBRIETY_PENDING_CONFIDENCE_PENALTY = 0.15f
    const val SOBRIETY_RELAPSE_DECAY = 1.5f
    const val SOBRIETY_FORGIVENESS_WINDOW_DAYS = 5L
}
