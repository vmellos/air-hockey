package dev.vmellos.airhockey.feature.stage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.game.ui.*

/**
 * Catálogo Canônico do Desbloqueio Progressivo de Arenas (GLOSSARY.md).
 *
 * Mapeia os 10 blocos de 10 fases do Modo Fases para cada arena esportiva,
 * estabelecendo os requisitos de Conquista de Arena da mais simples à mais épica.
 */
object ArenaConquestCatalog {

    data class ArenaTier(
        val tierIndex: Int,
        val stageRange: IntRange,
        val unlockStage: Int,
        val theme: TableTheme,
        val puckSkin: PuckSkin,
        val malletSkin: MalletSkin,
        val titleRes: Int,
        val descRes: Int,
        val icon: ImageVector,
        val primaryColor: Color,
        val secondaryColor: Color
    )

    val TIERS: List<ArenaTier> = listOf(
        ArenaTier(
            tierIndex = 1,
            stageRange = 1..10,
            unlockStage = 10,
            theme = TableTheme.CYBER_PONG,
            puckSkin = PuckSkin.PONG_SQUARE,
            malletSkin = MalletSkin.PONG_PADDLE,
            titleRes = R.string.theme_preset_pong,
            descRes = R.string.theme_preset_pong_desc,
            icon = Icons.Default.VideogameAsset,
            primaryColor = PongGreen,
            secondaryColor = PongAmber
        ),
        ArenaTier(
            tierIndex = 2,
            stageRange = 11..20,
            unlockStage = 20,
            theme = TableTheme.CYBER_SOCCER,
            puckSkin = PuckSkin.CYBER_SOCCER_BALL,
            malletSkin = MalletSkin.MAGNETIC_STRIKER,
            titleRes = R.string.theme_preset_cyber_soccer,
            descRes = R.string.theme_preset_cyber_soccer_desc,
            icon = Icons.Default.SportsSoccer,
            primaryColor = NeonEmerald,
            secondaryColor = NeonRuby
        ),
        ArenaTier(
            tierIndex = 3,
            stageRange = 21..30,
            unlockStage = 30,
            theme = TableTheme.NEO_BASKETBALL,
            puckSkin = PuckSkin.NEO_BASKETBALL,
            malletSkin = MalletSkin.DUNK_STRIKER,
            titleRes = R.string.theme_preset_neo_basketball,
            descRes = R.string.theme_preset_neo_basketball_desc,
            icon = Icons.Default.SportsBasketball,
            primaryColor = NeonAmber,
            secondaryColor = NeonCyan
        ),
        ArenaTier(
            tierIndex = 4,
            stageRange = 31..40,
            unlockStage = 40,
            theme = TableTheme.CYBER_BASEBALL,
            puckSkin = PuckSkin.CYBER_BASEBALL,
            malletSkin = MalletSkin.SLUGGER_STRIKER,
            titleRes = R.string.theme_preset_baseball,
            descRes = R.string.theme_preset_baseball_desc,
            icon = Icons.Default.SportsBaseball,
            primaryColor = BaseballRed,
            secondaryColor = BaseballCyan
        ),
        ArenaTier(
            tierIndex = 5,
            stageRange = 41..50,
            unlockStage = 50,
            theme = TableTheme.GRIDIRON_FOOTBALL,
            puckSkin = PuckSkin.GRIDIRON_FOOTBALL,
            malletSkin = MalletSkin.LINEBACKER_STRIKER,
            titleRes = R.string.theme_preset_gridiron,
            descRes = R.string.theme_preset_gridiron_desc,
            icon = Icons.Default.SportsFootball,
            primaryColor = GridironCyan,
            secondaryColor = GridironGold
        ),
        ArenaTier(
            tierIndex = 6,
            stageRange = 51..60,
            unlockStage = 60,
            theme = TableTheme.GRAVITY_VOLLEY,
            puckSkin = PuckSkin.GRAVITY_VOLLEY,
            malletSkin = MalletSkin.SPIKE_EMITTER,
            titleRes = R.string.theme_preset_gravity_volley,
            descRes = R.string.theme_preset_gravity_volley_desc,
            icon = Icons.Default.SportsVolleyball,
            primaryColor = NeonViolet,
            secondaryColor = NeonMagenta
        ),
        ArenaTier(
            tierIndex = 7,
            stageRange = 61..70,
            unlockStage = 70,
            theme = TableTheme.RETRO_SYNTHWAVE,
            puckSkin = PuckSkin.SYNTH_DISC,
            malletSkin = MalletSkin.OUTRUN_STRIKER,
            titleRes = R.string.theme_preset_synthwave,
            descRes = R.string.theme_preset_synthwave_desc,
            icon = Icons.Default.WbSunny,
            primaryColor = SynthPink,
            secondaryColor = SynthCyan
        ),
        ArenaTier(
            tierIndex = 8,
            stageRange = 71..80,
            unlockStage = 80,
            theme = TableTheme.WASTELAND_APOCALYPSE,
            puckSkin = PuckSkin.SAWBLADE,
            malletSkin = MalletSkin.RIPPER_STRIKER,
            titleRes = R.string.theme_preset_wasteland,
            descRes = R.string.theme_preset_wasteland_desc,
            icon = Icons.Default.Whatshot,
            primaryColor = WastelandFlame,
            secondaryColor = WastelandHazardYellow
        ),
        ArenaTier(
            tierIndex = 9,
            stageRange = 81..90,
            unlockStage = 90,
            theme = TableTheme.TOKAMAK_REACTOR,
            puckSkin = PuckSkin.ATOMIC_CORE,
            malletSkin = MalletSkin.MAGNETIC_COIL,
            titleRes = R.string.theme_preset_tokamak,
            descRes = R.string.theme_preset_tokamak_desc,
            icon = Icons.Default.Bolt,
            primaryColor = TokamakRadiation,
            secondaryColor = TokamakPlasma
        ),
        ArenaTier(
            tierIndex = 10,
            stageRange = 91..100,
            unlockStage = 100,
            theme = TableTheme.CYBER_COLOSSEUM,
            puckSkin = PuckSkin.GLADIATOR_SHIELD,
            malletSkin = MalletSkin.SPARTAN_AEGIS,
            titleRes = R.string.theme_preset_colosseum,
            descRes = R.string.theme_preset_colosseum_desc,
            icon = Icons.Default.Shield,
            primaryColor = ColosseumGold,
            secondaryColor = ColosseumCrimson
        )
    )

    fun getTierForStage(stageNumber: Int): ArenaTier {
        val validStage = stageNumber.coerceIn(1, 100)
        return TIERS.firstOrNull { validStage in it.stageRange } ?: TIERS.first()
    }

    fun getTierForTheme(theme: TableTheme): ArenaTier? = TIERS.firstOrNull { it.theme == theme }

    fun getThemeForStage(stageNumber: Int): TableTheme = getTierForStage(stageNumber).theme

    fun getPuckSkinForStage(stageNumber: Int): PuckSkin = getTierForStage(stageNumber).puckSkin

    fun getMalletSkinForStage(stageNumber: Int): MalletSkin = getTierForStage(stageNumber).malletSkin

    fun getUnlockStageForTheme(theme: TableTheme): Int {
        if (theme == TableTheme.CLASSIC_NEON || theme == TableTheme.ARCADE_3D) return 0
        return TIERS.firstOrNull { it.theme == theme }?.unlockStage ?: 0
    }

    fun getUnlockStageForPuck(skin: PuckSkin): Int {
        if (skin == PuckSkin.CLASSIC_PLASMA) return 0
        return TIERS.firstOrNull { it.puckSkin == skin }?.unlockStage ?: 0
    }

    fun getUnlockStageForMallet(skin: MalletSkin): Int {
        if (skin == MalletSkin.CLASSIC_NEON) return 0
        return TIERS.firstOrNull { it.malletSkin == skin }?.unlockStage ?: 0
    }

    fun isDefaultUnlocked(theme: TableTheme): Boolean =
        theme == TableTheme.CLASSIC_NEON || theme == TableTheme.ARCADE_3D

    fun isDefaultUnlocked(skin: PuckSkin): Boolean =
        skin == PuckSkin.CLASSIC_PLASMA

    fun isDefaultUnlocked(skin: MalletSkin): Boolean =
        skin == MalletSkin.CLASSIC_NEON
}
