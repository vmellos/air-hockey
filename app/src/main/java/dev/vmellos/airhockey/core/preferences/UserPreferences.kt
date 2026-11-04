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

enum class TableTheme {
    CLASSIC_NEON,
    CYBER_SOCCER,
    NEO_BASKETBALL,
    GRAVITY_VOLLEY,
    GRIDIRON_FOOTBALL,
    CYBER_BASEBALL,
    WASTELAND_APOCALYPSE,
    RETRO_SYNTHWAVE,
    TOKAMAK_REACTOR,
    CYBER_PONG,
    CYBER_COLOSSEUM
}

enum class PuckSkin {
    CLASSIC_PLASMA,
    CYBER_SOCCER_BALL,
    NEO_BASKETBALL,
    GRAVITY_VOLLEY,
    GRIDIRON_FOOTBALL,
    CYBER_BASEBALL,
    SAWBLADE,
    SYNTH_DISC,
    ATOMIC_CORE,
    PONG_SQUARE,
    GLADIATOR_SHIELD
}

enum class MalletSkin {
    CLASSIC_NEON,
    MAGNETIC_STRIKER,
    DUNK_STRIKER,
    SPIKE_EMITTER,
    LINEBACKER_STRIKER,
    SLUGGER_STRIKER,
    RIPPER_STRIKER,
    OUTRUN_STRIKER,
    MAGNETIC_COIL,
    PONG_PADDLE,
    SPARTAN_AEGIS
}

data class UserPreferences(
    val appLanguage: String = LocaleHelper.SYSTEM_DEFAULT,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.8f,
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 0.10f,
    val hapticsEnabled: Boolean = false,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val bezelGapMm: Float = 8.0f,
    val tableAlignment: TableAlignment = TableAlignment.TOP_TO_TOP,
    val targetScore: Int = 3,
    val matchTimeSeconds: Int = 180,
    val touchOffsetEnabled: Boolean = true,
    val puckSpeed: PuckSpeedPreset = PuckSpeedPreset.NORMAL,
    val visualEffectsProfile: VisualEffectsProfile = VisualEffectsProfile.FULL,
    val playerName: String = "",
    val tableTheme: TableTheme = TableTheme.CLASSIC_NEON,
    val puckSkin: PuckSkin = PuckSkin.CLASSIC_PLASMA,
    val player1MalletSkin: MalletSkin = MalletSkin.CLASSIC_NEON,
    val player2MalletSkin: MalletSkin = MalletSkin.CLASSIC_NEON
)
