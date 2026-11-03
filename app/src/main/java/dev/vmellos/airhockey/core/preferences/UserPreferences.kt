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

enum class PuckSpeedPreset(val maxSpeed: Float, val airFriction: Float = 0.40f) {
    SLOW(3600f, 0.55f),
    NORMAL(6800f, 0.40f),
    FAST(9500f, 0.35f)
}

enum class VisualEffectsProfile {
    FULL,
    OFF
}

data class UserPreferences(
    val appLanguage: String = LocaleHelper.SYSTEM_DEFAULT,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.8f,
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 0.15f,
    val hapticsEnabled: Boolean = false,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val bezelGapMm: Float = 8.0f,
    val tableAlignment: TableAlignment = TableAlignment.TOP_TO_TOP,
    val targetScore: Int = 7,
    val matchTimeSeconds: Int = 180,
    val touchOffsetEnabled: Boolean = true,
    val puckSpeed: PuckSpeedPreset = PuckSpeedPreset.NORMAL,
    val visualEffectsProfile: VisualEffectsProfile = VisualEffectsProfile.FULL,
    val playerName: String = ""
)
