package dev.vmellos.airhockey.core.catalog

import androidx.annotation.StringRes
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.TableTheme

/**
 * Perfil rítmico e estrutural usado na síntese procedural da trilha de fundo.
 */
data class ThemeAudioProfile(
    val bpm: Double,
    val totalBars: Int = 8
)

/**
 * Descritor canônico puro de um Conjunto Temático de Arena, desacoplado de recursos visuais de UI.
 */
data class ArenaPreset(
    val tierIndex: Int,
    val stageRange: IntRange,
    val unlockStage: Int,
    val theme: TableTheme,
    val puckSkin: PuckSkin,
    val malletSkin: MalletSkin,
    @param:StringRes val titleRes: Int,
    @param:StringRes val descRes: Int,
    @param:StringRes val puckTitleRes: Int,
    @param:StringRes val malletTitleRes: Int,
    val audioProfile: ThemeAudioProfile
)

/**
 * Converte valores legados de [TableTheme] para sua forma canônica atual.
 */
fun TableTheme.canonical(): TableTheme =
    if (this == TableTheme.ARCADE_3D) TableTheme.CLASSIC_NEON else this

/**
 * Retorna o [ArenaPreset] canônico associado a este [TableTheme].
 */
fun TableTheme.preset(): ArenaPreset = ArenaPresetCatalog.getPresetForTheme(this)

/**
 * Catálogo unificado de progressão, desbloqueio e áudio das 12 arenas temáticas do jogo.
 */
object ArenaPresetCatalog {

    val PRESET_CLASSIC = ArenaPreset(
        tierIndex = 0,
        stageRange = 0..0,
        unlockStage = 0,
        theme = TableTheme.CLASSIC_NEON,
        puckSkin = PuckSkin.CLASSIC_PLASMA,
        malletSkin = MalletSkin.CLASSIC_NEON,
        titleRes = R.string.theme_preset_classic,
        descRes = R.string.theme_preset_classic_desc,
        puckTitleRes = R.string.skin_puck_classic,
        malletTitleRes = R.string.skin_mallet_classic,
        audioProfile = ThemeAudioProfile(bpm = 125.0, totalBars = 8)
    )

    val PRESET_KIARA_CAT = ArenaPreset(
        tierIndex = 11,
        stageRange = 0..0,
        unlockStage = 0,
        theme = TableTheme.KIARA_CAT,
        puckSkin = PuckSkin.KIARA_PAW,
        malletSkin = MalletSkin.NEKO_STRIKER,
        titleRes = R.string.theme_preset_kiara_cat,
        descRes = R.string.theme_preset_kiara_cat_desc,
        puckTitleRes = R.string.skin_puck_kiara_paw,
        malletTitleRes = R.string.skin_mallet_neko_striker,
        audioProfile = ThemeAudioProfile(bpm = 124.0, totalBars = 8)
    )

    val TIERS: List<ArenaPreset> = listOf(
        ArenaPreset(
            tierIndex = 1,
            stageRange = 1..10,
            unlockStage = 10,
            theme = TableTheme.CYBER_PONG,
            puckSkin = PuckSkin.PONG_SQUARE,
            malletSkin = MalletSkin.PONG_PADDLE,
            titleRes = R.string.theme_preset_pong,
            descRes = R.string.theme_preset_pong_desc,
            puckTitleRes = R.string.skin_puck_pong,
            malletTitleRes = R.string.skin_mallet_pong,
            audioProfile = ThemeAudioProfile(bpm = 120.0, totalBars = 8)
        ),
        ArenaPreset(
            tierIndex = 2,
            stageRange = 11..20,
            unlockStage = 20,
            theme = TableTheme.CYBER_SOCCER,
            puckSkin = PuckSkin.CYBER_SOCCER_BALL,
            malletSkin = MalletSkin.MAGNETIC_STRIKER,
            titleRes = R.string.theme_preset_cyber_soccer,
            descRes = R.string.theme_preset_cyber_soccer_desc,
            puckTitleRes = R.string.skin_puck_soccer,
            malletTitleRes = R.string.skin_mallet_soccer,
            audioProfile = ThemeAudioProfile(bpm = 128.0, totalBars = 8)
        ),
        ArenaPreset(
            tierIndex = 3,
            stageRange = 21..30,
            unlockStage = 30,
            theme = TableTheme.NEO_BASKETBALL,
            puckSkin = PuckSkin.NEO_BASKETBALL,
            malletSkin = MalletSkin.DUNK_STRIKER,
            titleRes = R.string.theme_preset_neo_basketball,
            descRes = R.string.theme_preset_neo_basketball_desc,
            puckTitleRes = R.string.skin_puck_basketball,
            malletTitleRes = R.string.skin_mallet_basketball,
            audioProfile = ThemeAudioProfile(bpm = 96.0, totalBars = 8)
        ),
        ArenaPreset(
            tierIndex = 4,
            stageRange = 31..40,
            unlockStage = 40,
            theme = TableTheme.CYBER_BASEBALL,
            puckSkin = PuckSkin.CYBER_BASEBALL,
            malletSkin = MalletSkin.SLUGGER_STRIKER,
            titleRes = R.string.theme_preset_baseball,
            descRes = R.string.theme_preset_baseball_desc,
            puckTitleRes = R.string.skin_puck_baseball,
            malletTitleRes = R.string.skin_mallet_baseball,
            audioProfile = ThemeAudioProfile(bpm = 120.0, totalBars = 8)
        ),
        ArenaPreset(
            tierIndex = 5,
            stageRange = 41..50,
            unlockStage = 50,
            theme = TableTheme.GRIDIRON_FOOTBALL,
            puckSkin = PuckSkin.GRIDIRON_FOOTBALL,
            malletSkin = MalletSkin.LINEBACKER_STRIKER,
            titleRes = R.string.theme_preset_gridiron,
            descRes = R.string.theme_preset_gridiron_desc,
            puckTitleRes = R.string.skin_puck_gridiron,
            malletTitleRes = R.string.skin_mallet_gridiron,
            audioProfile = ThemeAudioProfile(bpm = 110.0, totalBars = 8)
        ),
        ArenaPreset(
            tierIndex = 6,
            stageRange = 51..60,
            unlockStage = 60,
            theme = TableTheme.GRAVITY_VOLLEY,
            puckSkin = PuckSkin.GRAVITY_VOLLEY,
            malletSkin = MalletSkin.SPIKE_EMITTER,
            titleRes = R.string.theme_preset_gravity_volley,
            descRes = R.string.theme_preset_gravity_volley_desc,
            puckTitleRes = R.string.skin_puck_volley,
            malletTitleRes = R.string.skin_mallet_volley,
            audioProfile = ThemeAudioProfile(bpm = 135.0, totalBars = 8)
        ),
        ArenaPreset(
            tierIndex = 7,
            stageRange = 61..70,
            unlockStage = 70,
            theme = TableTheme.RETRO_SYNTHWAVE,
            puckSkin = PuckSkin.SYNTH_DISC,
            malletSkin = MalletSkin.OUTRUN_STRIKER,
            titleRes = R.string.theme_preset_synthwave,
            descRes = R.string.theme_preset_synthwave_desc,
            puckTitleRes = R.string.skin_puck_synthwave,
            malletTitleRes = R.string.skin_mallet_synthwave,
            audioProfile = ThemeAudioProfile(bpm = 118.0, totalBars = 8)
        ),
        ArenaPreset(
            tierIndex = 8,
            stageRange = 71..80,
            unlockStage = 80,
            theme = TableTheme.WASTELAND_APOCALYPSE,
            puckSkin = PuckSkin.SAWBLADE,
            malletSkin = MalletSkin.RIPPER_STRIKER,
            titleRes = R.string.theme_preset_wasteland,
            descRes = R.string.theme_preset_wasteland_desc,
            puckTitleRes = R.string.skin_puck_wasteland,
            malletTitleRes = R.string.skin_mallet_wasteland,
            audioProfile = ThemeAudioProfile(bpm = 132.0, totalBars = 8)
        ),
        ArenaPreset(
            tierIndex = 9,
            stageRange = 81..90,
            unlockStage = 90,
            theme = TableTheme.TOKAMAK_REACTOR,
            puckSkin = PuckSkin.ATOMIC_CORE,
            malletSkin = MalletSkin.MAGNETIC_COIL,
            titleRes = R.string.theme_preset_tokamak,
            descRes = R.string.theme_preset_tokamak_desc,
            puckTitleRes = R.string.skin_puck_tokamak,
            malletTitleRes = R.string.skin_mallet_tokamak,
            audioProfile = ThemeAudioProfile(bpm = 134.0, totalBars = 8)
        ),
        ArenaPreset(
            tierIndex = 10,
            stageRange = 91..100,
            unlockStage = 100,
            theme = TableTheme.CYBER_COLOSSEUM,
            puckSkin = PuckSkin.GLADIATOR_SHIELD,
            malletSkin = MalletSkin.SPARTAN_AEGIS,
            titleRes = R.string.theme_preset_colosseum,
            descRes = R.string.theme_preset_colosseum_desc,
            puckTitleRes = R.string.skin_puck_colosseum,
            malletTitleRes = R.string.skin_mallet_colosseum,
            audioProfile = ThemeAudioProfile(bpm = 114.0, totalBars = 8)
        )
    )

    val ALL_PRESETS: List<ArenaPreset> = listOf(PRESET_CLASSIC, PRESET_KIARA_CAT) + TIERS

    fun getPresetForStage(stageNumber: Int): ArenaPreset {
        val validStage = stageNumber.coerceIn(1, 100)
        return TIERS.firstOrNull { validStage in it.stageRange } ?: TIERS.first()
    }

    fun getPresetForTheme(theme: TableTheme): ArenaPreset {
        val canonicalTheme = theme.canonical()
        return ALL_PRESETS.firstOrNull { it.theme == canonicalTheme } ?: PRESET_CLASSIC
    }

    fun getThemeForStage(stageNumber: Int): TableTheme = getPresetForStage(stageNumber).theme

    fun getPuckSkinForStage(stageNumber: Int): PuckSkin = getPresetForStage(stageNumber).puckSkin

    fun getMalletSkinForStage(stageNumber: Int): MalletSkin = getPresetForStage(stageNumber).malletSkin

    fun getUnlockStageForTheme(theme: TableTheme): Int = getPresetForTheme(theme).unlockStage

    fun getUnlockStageForPuck(skin: PuckSkin): Int =
        ALL_PRESETS.firstOrNull { it.puckSkin == skin }?.unlockStage ?: 0

    fun getUnlockStageForMallet(skin: MalletSkin): Int =
        ALL_PRESETS.firstOrNull { it.malletSkin == skin }?.unlockStage ?: 0

    fun isDefaultUnlocked(theme: TableTheme): Boolean = getUnlockStageForTheme(theme) == 0

    fun isDefaultUnlocked(skin: PuckSkin): Boolean = getUnlockStageForPuck(skin) == 0

    fun isDefaultUnlocked(skin: MalletSkin): Boolean = getUnlockStageForMallet(skin) == 0

    @StringRes
    fun resolveActivePresetTitleRes(theme: TableTheme, puck: PuckSkin, mallet: MalletSkin): Int {
        val canonicalTheme = theme.canonical()
        val match = ALL_PRESETS.firstOrNull {
            it.theme == canonicalTheme && it.puckSkin == puck && it.malletSkin == mallet
        }
        return match?.titleRes ?: R.string.customization_custom_preset
    }
}
