package dev.vmellos.airhockey.core.preferences

import dev.vmellos.airhockey.core.localization.LocaleHelper

enum class AiDifficulty {
    EASY,
    MEDIUM,
    HARD
}

enum class TableAlignment {
    TOP_TO_TOP,
    SIDE_BY_SIDE
}

data class UserPreferences(
    val appLanguage: String = LocaleHelper.SYSTEM_DEFAULT,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.8f,
    val hapticsEnabled: Boolean = true,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val bezelGapMm: Float = 8.0f,
    val tableAlignment: TableAlignment = TableAlignment.TOP_TO_TOP,
    val targetScore: Int = 7,
    val matchTimeSeconds: Int = 180
)
