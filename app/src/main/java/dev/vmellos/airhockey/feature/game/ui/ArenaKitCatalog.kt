package dev.vmellos.airhockey.feature.game.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.vmellos.airhockey.core.catalog.ArenaPreset
import dev.vmellos.airhockey.core.catalog.ArenaPresetCatalog
import dev.vmellos.airhockey.core.catalog.canonical
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.TablePerspective

@Immutable
data class ThemeVisuals(
    val backgroundColor: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val primaryRimColor: Color = primaryColor,
    val secondaryRimColor: Color = secondaryColor,
    val wallHitColor: Color = Color(0xFFE2E8F0)
)

@Immutable
data class MalletSkinVisuals(
    val color: Color,
    val rimColor: Color
)

@Immutable
data class ArenaOverlayStyle(
    val shape: Shape,
    val background: Color,
    val border: BorderStroke,
    val p1Color: Color,
    val p2Color: Color,
    val fontFamily: FontFamily
)

/**
 * Kit visual completo de uma Mesa de Arena, unificando estética 2D, HUD e modelos 3D Filament.
 */
@Immutable
data class ArenaKit(
    val preset: ArenaPreset,
    val icon: ImageVector,
    val visuals: ThemeVisuals,
    val overlayStyle: ArenaOverlayStyle,
    val tableFullAsset: String,
    val tableHalfP1Asset: String,
    val tableHalfP2Asset: String,
    val floor3dAsset: String? = null,
    val hasMascot: Boolean = false,
    val needsWideMascotFraming: Boolean = false,
    val mascot3dAsset: String? = null
) {
    val theme: TableTheme get() = preset.theme
    val titleRes: Int get() = preset.titleRes
    val descRes: Int get() = preset.descRes
    val puckSkin: PuckSkin get() = preset.puckSkin
    val malletSkin: MalletSkin get() = preset.malletSkin
    val unlockStage: Int get() = preset.unlockStage

    /**
     * Resolve o modelo 3D da Mesa conforme a perspectiva ativa na partida.
     */
    fun tableAsset(perspective: TablePerspective): String = when (perspective) {
        TablePerspective.EXTENDED_HOST -> tableHalfP1Asset
        TablePerspective.EXTENDED_GUEST -> tableHalfP2Asset
        TablePerspective.LOCAL_FULL,
        TablePerspective.SPLIT_DUAL,
        TablePerspective.CONNECTED_HOST,
        TablePerspective.CONNECTED_GUEST -> tableFullAsset
    }

    fun tableAsset(isExtended: Boolean, isPlayer1: Boolean): String =
        if (!isExtended) tableFullAsset else if (isPlayer1) tableHalfP1Asset else tableHalfP2Asset
}

/**
 * Descritor visual e 3D de uma skin de [PuckSkin].
 */
@Immutable
data class PuckSpec(
    val skin: PuckSkin,
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
    val accentColor: Color,
    val hit1Accent: Color,
    val hit1Core: Color,
    val hit2Accent: Color,
    val hit2Core: Color,
    val neutralAccent: Color,
    val neutralCore: Color,
    val model3dAsset: String
) {
    fun trailColors(lastHitBy: Int): Pair<Color, Color> = when (lastHitBy) {
        1 -> Pair(hit1Accent, hit1Core)
        2 -> Pair(hit2Accent, hit2Core)
        else -> Pair(neutralAccent, neutralCore)
    }
}

/**
 * Descritor visual e 3D de uma skin de [MalletSkin].
 */
@Immutable
data class MalletSpec(
    val skin: MalletSkin,
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
    val p1Visuals: MalletSkinVisuals,
    val p2Visuals: MalletSkinVisuals,
    val p1Model3dAsset: String,
    val p2Model3dAsset: String
) {
    fun visuals(isPlayer1: Boolean): MalletSkinVisuals = if (isPlayer1) p1Visuals else p2Visuals
    fun model3dAsset(isPlayer1: Boolean): String = if (isPlayer1) p1Model3dAsset else p2Model3dAsset
}

fun TableTheme.kit(): ArenaKit = ArenaKitCatalog.getKit(this)
fun PuckSkin.spec(): PuckSpec = ArenaKitCatalog.getPuckSpec(this)
fun MalletSkin.spec(): MalletSpec = ArenaKitCatalog.getMalletSpec(this)

/**
 * Conjunto efetivo de disco e rebatedores resolvido para renderização em cena 2D/3D.
 */
data class EffectiveArenaCosmetics(
    val puckSkin: PuckSkin,
    val p1MalletSkin: MalletSkin,
    val p2MalletSkin: MalletSkin
)

/**
 * Resolve o kit efetivo da partida, garantindo paridade no multiplayer/seleção pré-jogo e
 * acoplando o disco temático + rebatedor do boss na campanha solo.
 */
fun ArenaKit.resolveCosmetics(
    activeTheme: TableTheme,
    explicitTableTheme: TableTheme?,
    mode: GameMode,
    currentStage: Int,
    prefs: UserPreferences
): EffectiveArenaCosmetics {
    if (mode == GameMode.SOLO) {
        val hasCustomP1Mallet = prefs.tableTheme == activeTheme &&
            prefs.player1MalletSkin != MalletSkin.CLASSIC_NEON
        return EffectiveArenaCosmetics(
            puckSkin = puckSkin,
            p1MalletSkin = if (hasCustomP1Mallet) prefs.player1MalletSkin else malletSkin,
            p2MalletSkin = ArenaPresetCatalog.getMalletSkinForStage(currentStage)
        )
    }
    if (explicitTableTheme != null) {
        return EffectiveArenaCosmetics(
            puckSkin = puckSkin,
            p1MalletSkin = malletSkin,
            p2MalletSkin = malletSkin
        )
    }
    return EffectiveArenaCosmetics(
        puckSkin = prefs.puckSkin,
        p1MalletSkin = prefs.player1MalletSkin,
        p2MalletSkin = prefs.player2MalletSkin
    )
}

/**
 * Catálogo visual unificado para kits de Arena, Discos e Rebatedores em 2D, 3D e HUD.
 */
object ArenaKitCatalog {

    private val KIT_CLASSIC = ArenaKit(
        preset = ArenaPresetCatalog.PRESET_CLASSIC,
        icon = Icons.Default.SportsHockey,
        visuals = ThemeVisuals(
            backgroundColor = TableBgDark,
            primaryColor = NeonCyan,
            secondaryColor = NeonOrange,
            wallHitColor = Color(0xFFE2E8F0)
        ),
        overlayStyle = ArenaOverlayStyle(
            shape = RoundedCornerShape(12.dp),
            background = Color(0x33000000),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            p1Color = NeonCyan,
            p2Color = NeonOrange,
            fontFamily = FontFamily.Monospace
        ),
        tableFullAsset = "models/mesa_arcade.glb",
        tableHalfP1Asset = "models/mesa_arcade_half_p1.glb",
        tableHalfP2Asset = "models/mesa_arcade_half_p2.glb",
        floor3dAsset = "models/piso_arena_cyber.glb"
    )

    private val KIT_KIARA_CAT = ArenaKit(
        preset = ArenaPresetCatalog.PRESET_KIARA_CAT,
        icon = Icons.Default.Pets,
        visuals = ThemeVisuals(
            backgroundColor = Color(0xFF0F0B18),
            primaryColor = Color(0xFFFF1493),
            secondaryColor = NeonCyan,
            wallHitColor = Color(0xFFFFC107)
        ),
        overlayStyle = ArenaOverlayStyle(
            shape = RoundedCornerShape(12.dp),
            background = Color(0xDD12081C),
            border = BorderStroke(1.2.dp, Color(0xFFFF1493).copy(alpha = 0.8f)),
            p1Color = Color(0xFFFF1493),
            p2Color = NeonCyan,
            fontFamily = FontFamily.SansSerif
        ),
        tableFullAsset = "models/mesa_kiara_cat.glb",
        tableHalfP1Asset = "models/mesa_kiara_cat_half_p1.glb",
        tableHalfP2Asset = "models/mesa_kiara_cat_half_p2.glb",
        hasMascot = true,
        needsWideMascotFraming = true,
        mascot3dAsset = "models/kiara_3d.glb"
    )

    private val KIT_BY_THEME: Map<TableTheme, ArenaKit> = mapOf(
        TableTheme.CLASSIC_NEON to KIT_CLASSIC,
        TableTheme.KIARA_CAT to KIT_KIARA_CAT,
        TableTheme.CYBER_PONG to ArenaKit(
            preset = ArenaPresetCatalog.getPresetForTheme(TableTheme.CYBER_PONG),
            icon = Icons.Default.VideogameAsset,
            visuals = ThemeVisuals(PongDark, PongGreen, PongAmber, wallHitColor = PongGreenBright),
            overlayStyle = ArenaOverlayStyle(
                shape = CutCornerShape(0.dp),
                background = Color(0xFF000000),
                border = BorderStroke(1.5.dp, PongGreen),
                p1Color = PongGreen,
                p2Color = PongAmber,
                fontFamily = FontFamily.Monospace
            ),
            tableFullAsset = "models/mesa_cyber_pong.glb",
            tableHalfP1Asset = "models/mesa_cyber_pong_half_p1.glb",
            tableHalfP2Asset = "models/mesa_cyber_pong_half_p2.glb"
        ),
        TableTheme.CYBER_SOCCER to ArenaKit(
            preset = ArenaPresetCatalog.getPresetForTheme(TableTheme.CYBER_SOCCER),
            icon = Icons.Default.SportsSoccer,
            visuals = ThemeVisuals(CyberTurfDark, NeonEmerald, NeonRuby, wallHitColor = CyberSoccerGold),
            overlayStyle = ArenaOverlayStyle(
                shape = RoundedCornerShape(12.dp),
                background = Color(0xCC05180D),
                border = BorderStroke(1.dp, NeonEmerald.copy(alpha = 0.6f)),
                p1Color = NeonEmerald,
                p2Color = NeonRuby,
                fontFamily = FontFamily.SansSerif
            ),
            tableFullAsset = "models/mesa_cyber_soccer.glb",
            tableHalfP1Asset = "models/mesa_cyber_soccer_half_p1.glb",
            tableHalfP2Asset = "models/mesa_cyber_soccer_half_p2.glb"
        ),
        TableTheme.NEO_BASKETBALL to ArenaKit(
            preset = ArenaPresetCatalog.getPresetForTheme(TableTheme.NEO_BASKETBALL),
            icon = Icons.Default.SportsBasketball,
            visuals = ThemeVisuals(CourtDark, NeonAmber, NeonCyan, wallHitColor = NeonAmberBright),
            overlayStyle = ArenaOverlayStyle(
                shape = CutCornerShape(8.dp),
                background = Color(0xDD140E08),
                border = BorderStroke(1.2.dp, NeonAmber.copy(alpha = 0.7f)),
                p1Color = NeonAmber,
                p2Color = NeonCyan,
                fontFamily = FontFamily.SansSerif
            ),
            tableFullAsset = "models/mesa_neo_basketball.glb",
            tableHalfP1Asset = "models/mesa_neo_basketball_half_p1.glb",
            tableHalfP2Asset = "models/mesa_neo_basketball_half_p2.glb"
        ),
        TableTheme.CYBER_BASEBALL to ArenaKit(
            preset = ArenaPresetCatalog.getPresetForTheme(TableTheme.CYBER_BASEBALL),
            icon = Icons.Default.SportsBaseball,
            visuals = ThemeVisuals(BaseballDark, BaseballRed, BaseballCyan, wallHitColor = BaseballRedBright),
            overlayStyle = ArenaOverlayStyle(
                shape = CutCornerShape(12.dp),
                background = Color(0xCC18080C),
                border = BorderStroke(1.dp, BaseballRed.copy(alpha = 0.7f)),
                p1Color = BaseballRed,
                p2Color = BaseballCyan,
                fontFamily = FontFamily.SansSerif
            ),
            tableFullAsset = "models/mesa_cyber_baseball.glb",
            tableHalfP1Asset = "models/mesa_cyber_baseball_half_p1.glb",
            tableHalfP2Asset = "models/mesa_cyber_baseball_half_p2.glb"
        ),
        TableTheme.GRIDIRON_FOOTBALL to ArenaKit(
            preset = ArenaPresetCatalog.getPresetForTheme(TableTheme.GRIDIRON_FOOTBALL),
            icon = Icons.Default.SportsFootball,
            visuals = ThemeVisuals(GridironDark, GridironCyan, GridironGold, wallHitColor = GridironGoldBright),
            overlayStyle = ArenaOverlayStyle(
                shape = CutCornerShape(6.dp),
                background = Color(0xDD081420),
                border = BorderStroke(1.5.dp, GridironCyan.copy(alpha = 0.7f)),
                p1Color = GridironCyan,
                p2Color = GridironGold,
                fontFamily = FontFamily.SansSerif
            ),
            tableFullAsset = "models/mesa_gridiron_football.glb",
            tableHalfP1Asset = "models/mesa_gridiron_football_half_p1.glb",
            tableHalfP2Asset = "models/mesa_gridiron_football_half_p2.glb"
        ),
        TableTheme.GRAVITY_VOLLEY to ArenaKit(
            preset = ArenaPresetCatalog.getPresetForTheme(TableTheme.GRAVITY_VOLLEY),
            icon = Icons.Default.SportsVolleyball,
            visuals = ThemeVisuals(VolleySpaceDark, NeonViolet, NeonMagenta, wallHitColor = VolleyYellow),
            overlayStyle = ArenaOverlayStyle(
                shape = RoundedCornerShape(16.dp),
                background = Color(0xCC0E0A1E),
                border = BorderStroke(1.dp, NeonViolet.copy(alpha = 0.6f)),
                p1Color = NeonViolet,
                p2Color = NeonMagenta,
                fontFamily = FontFamily.SansSerif
            ),
            tableFullAsset = "models/mesa_gravity_volley.glb",
            tableHalfP1Asset = "models/mesa_gravity_volley_half_p1.glb",
            tableHalfP2Asset = "models/mesa_gravity_volley_half_p2.glb"
        ),
        TableTheme.RETRO_SYNTHWAVE to ArenaKit(
            preset = ArenaPresetCatalog.getPresetForTheme(TableTheme.RETRO_SYNTHWAVE),
            icon = Icons.Default.WbSunny,
            visuals = ThemeVisuals(SynthDark, SynthPink, SynthCyan, wallHitColor = SynthSunYellow),
            overlayStyle = ArenaOverlayStyle(
                shape = RoundedCornerShape(10.dp),
                background = Color(0xDD1A0A1C),
                border = BorderStroke(1.2.dp, SynthPink.copy(alpha = 0.7f)),
                p1Color = SynthPink,
                p2Color = SynthCyan,
                fontFamily = FontFamily.SansSerif
            ),
            tableFullAsset = "models/mesa_retro_synthwave.glb",
            tableHalfP1Asset = "models/mesa_retro_synthwave_half_p1.glb",
            tableHalfP2Asset = "models/mesa_retro_synthwave_half_p2.glb"
        ),
        TableTheme.WASTELAND_APOCALYPSE to ArenaKit(
            preset = ArenaPresetCatalog.getPresetForTheme(TableTheme.WASTELAND_APOCALYPSE),
            icon = Icons.Default.Whatshot,
            visuals = ThemeVisuals(WastelandDark, WastelandFlame, WastelandFlameBright, wallHitColor = WastelandHazardYellow),
            overlayStyle = ArenaOverlayStyle(
                shape = CutCornerShape(4.dp),
                background = Color(0xEE160C06),
                border = BorderStroke(1.5.dp, WastelandFlame.copy(alpha = 0.8f)),
                p1Color = WastelandFlame,
                p2Color = WastelandFlameBright,
                fontFamily = FontFamily.SansSerif
            ),
            tableFullAsset = "models/mesa_wasteland_apocalypse.glb",
            tableHalfP1Asset = "models/mesa_wasteland_apocalypse_half_p1.glb",
            tableHalfP2Asset = "models/mesa_wasteland_apocalypse_half_p2.glb"
        ),
        TableTheme.TOKAMAK_REACTOR to ArenaKit(
            preset = ArenaPresetCatalog.getPresetForTheme(TableTheme.TOKAMAK_REACTOR),
            icon = Icons.Default.Bolt,
            visuals = ThemeVisuals(TokamakDark, TokamakRadiation, TokamakPlasma, wallHitColor = TokamakRadiationBright),
            overlayStyle = ArenaOverlayStyle(
                shape = CutCornerShape(10.dp),
                background = Color(0xDD081608),
                border = BorderStroke(1.2.dp, TokamakRadiation.copy(alpha = 0.7f)),
                p1Color = TokamakRadiation,
                p2Color = TokamakPlasma,
                fontFamily = FontFamily.SansSerif
            ),
            tableFullAsset = "models/mesa_tokamak_reactor.glb",
            tableHalfP1Asset = "models/mesa_tokamak_reactor_half_p1.glb",
            tableHalfP2Asset = "models/mesa_tokamak_reactor_half_p2.glb"
        ),
        TableTheme.CYBER_COLOSSEUM to ArenaKit(
            preset = ArenaPresetCatalog.getPresetForTheme(TableTheme.CYBER_COLOSSEUM),
            icon = Icons.Default.Shield,
            visuals = ThemeVisuals(ColosseumDark, ColosseumGold, ColosseumCrimson, wallHitColor = ColosseumGoldBright),
            overlayStyle = ArenaOverlayStyle(
                shape = RoundedCornerShape(6.dp),
                background = Color(0xDD1C1408),
                border = BorderStroke(1.2.dp, ColosseumGold.copy(alpha = 0.7f)),
                p1Color = ColosseumGold,
                p2Color = ColosseumCrimson,
                fontFamily = FontFamily.Serif
            ),
            tableFullAsset = "models/mesa_cyber_colosseum.glb",
            tableHalfP1Asset = "models/mesa_cyber_colosseum_half_p1.glb",
            tableHalfP2Asset = "models/mesa_cyber_colosseum_half_p2.glb"
        )
    )

    val ALL_KITS: List<ArenaKit> = ArenaPresetCatalog.ALL_PRESETS.map { getKit(it.theme) }

    private val PUCK_SPECS: Map<PuckSkin, PuckSpec> = mapOf(
        PuckSkin.CLASSIC_PLASMA to PuckSpec(
            skin = PuckSkin.CLASSIC_PLASMA,
            titleRes = ArenaPresetCatalog.PRESET_CLASSIC.puckTitleRes,
            icon = Icons.Default.SportsHockey,
            accentColor = NeonCyan,
            hit1Accent = NeonCyan, hit1Core = NeonCyanCore,
            hit2Accent = NeonOrange, hit2Core = NeonOrangeCore,
            neutralAccent = NeonCyanBright, neutralCore = WhiteHot,
            model3dAsset = "models/disco_cyber.glb"
        ),
        PuckSkin.KIARA_PAW to PuckSpec(
            skin = PuckSkin.KIARA_PAW,
            titleRes = ArenaPresetCatalog.PRESET_KIARA_CAT.puckTitleRes,
            icon = Icons.Default.Pets,
            accentColor = Color(0xFFFF1493),
            hit1Accent = Color(0xFFFF1493), hit1Core = Color(0xFFFF69B4),
            hit2Accent = NeonCyan, hit2Core = NeonCyanBright,
            neutralAccent = Color(0xFFFFC107), neutralCore = WhiteHot,
            model3dAsset = "models/disco_kiara_paw.glb"
        ),
        PuckSkin.PONG_SQUARE to PuckSpec(
            skin = PuckSkin.PONG_SQUARE,
            titleRes = KIT_BY_THEME.getValue(TableTheme.CYBER_PONG).preset.puckTitleRes,
            icon = Icons.Default.VideogameAsset,
            accentColor = PongGreen,
            hit1Accent = PongGreen, hit1Core = PongGreenCore,
            hit2Accent = PongAmber, hit2Core = PongAmberCore,
            neutralAccent = PongGreenBright, neutralCore = WhiteHot,
            model3dAsset = "models/disco_cyber_pong.glb"
        ),
        PuckSkin.CYBER_SOCCER_BALL to PuckSpec(
            skin = PuckSkin.CYBER_SOCCER_BALL,
            titleRes = KIT_BY_THEME.getValue(TableTheme.CYBER_SOCCER).preset.puckTitleRes,
            icon = Icons.Default.SportsSoccer,
            accentColor = NeonEmerald,
            hit1Accent = NeonEmerald, hit1Core = NeonEmeraldCore,
            hit2Accent = NeonRuby, hit2Core = NeonRubyCore,
            neutralAccent = CyberSoccerGold, neutralCore = WhiteHot,
            model3dAsset = "models/disco_cyber_soccer.glb"
        ),
        PuckSkin.NEO_BASKETBALL to PuckSpec(
            skin = PuckSkin.NEO_BASKETBALL,
            titleRes = KIT_BY_THEME.getValue(TableTheme.NEO_BASKETBALL).preset.puckTitleRes,
            icon = Icons.Default.SportsBasketball,
            accentColor = NeonAmber,
            hit1Accent = NeonAmber, hit1Core = NeonAmberCore,
            hit2Accent = NeonCyan, hit2Core = NeonCyanCore,
            neutralAccent = NeonAmberBright, neutralCore = WhiteHot,
            model3dAsset = "models/disco_neo_basketball.glb"
        ),
        PuckSkin.CYBER_BASEBALL to PuckSpec(
            skin = PuckSkin.CYBER_BASEBALL,
            titleRes = KIT_BY_THEME.getValue(TableTheme.CYBER_BASEBALL).preset.puckTitleRes,
            icon = Icons.Default.SportsBaseball,
            accentColor = BaseballRed,
            hit1Accent = BaseballRed, hit1Core = BaseballRedCore,
            hit2Accent = BaseballCyan, hit2Core = BaseballCyanCore,
            neutralAccent = BaseballRedBright, neutralCore = WhiteHot,
            model3dAsset = "models/disco_cyber_baseball.glb"
        ),
        PuckSkin.GRIDIRON_FOOTBALL to PuckSpec(
            skin = PuckSkin.GRIDIRON_FOOTBALL,
            titleRes = KIT_BY_THEME.getValue(TableTheme.GRIDIRON_FOOTBALL).preset.puckTitleRes,
            icon = Icons.Default.SportsFootball,
            accentColor = GridironCyan,
            hit1Accent = GridironCyan, hit1Core = GridironCyanCore,
            hit2Accent = GridironGold, hit2Core = GridironGoldCore,
            neutralAccent = GridironGoldBright, neutralCore = WhiteHot,
            model3dAsset = "models/disco_gridiron_football.glb"
        ),
        PuckSkin.GRAVITY_VOLLEY to PuckSpec(
            skin = PuckSkin.GRAVITY_VOLLEY,
            titleRes = KIT_BY_THEME.getValue(TableTheme.GRAVITY_VOLLEY).preset.puckTitleRes,
            icon = Icons.Default.SportsVolleyball,
            accentColor = NeonViolet,
            hit1Accent = NeonViolet, hit1Core = NeonVioletCore,
            hit2Accent = NeonMagenta, hit2Core = NeonMagentaCore,
            neutralAccent = VolleyYellow, neutralCore = WhiteHot,
            model3dAsset = "models/disco_gravity_volley.glb"
        ),
        PuckSkin.SYNTH_DISC to PuckSpec(
            skin = PuckSkin.SYNTH_DISC,
            titleRes = KIT_BY_THEME.getValue(TableTheme.RETRO_SYNTHWAVE).preset.puckTitleRes,
            icon = Icons.Default.WbSunny,
            accentColor = SynthPink,
            hit1Accent = SynthPink, hit1Core = SynthPinkCore,
            hit2Accent = SynthCyan, hit2Core = SynthCyanCore,
            neutralAccent = SynthSunYellow, neutralCore = WhiteHot,
            model3dAsset = "models/disco_retro_synthwave.glb"
        ),
        PuckSkin.SAWBLADE to PuckSpec(
            skin = PuckSkin.SAWBLADE,
            titleRes = KIT_BY_THEME.getValue(TableTheme.WASTELAND_APOCALYPSE).preset.puckTitleRes,
            icon = Icons.Default.Whatshot,
            accentColor = WastelandFlame,
            hit1Accent = WastelandFlame, hit1Core = WastelandFlameCore,
            hit2Accent = WastelandFlameBright, hit2Core = WastelandFlameCore,
            neutralAccent = WastelandHazardYellow, neutralCore = WhiteHot,
            model3dAsset = "models/disco_wasteland_apocalypse.glb"
        ),
        PuckSkin.ATOMIC_CORE to PuckSpec(
            skin = PuckSkin.ATOMIC_CORE,
            titleRes = KIT_BY_THEME.getValue(TableTheme.TOKAMAK_REACTOR).preset.puckTitleRes,
            icon = Icons.Default.Bolt,
            accentColor = TokamakRadiation,
            hit1Accent = TokamakRadiation, hit1Core = TokamakRadiationCore,
            hit2Accent = TokamakPlasma, hit2Core = TokamakPlasmaCore,
            neutralAccent = TokamakRadiationBright, neutralCore = WhiteHot,
            model3dAsset = "models/disco_tokamak_reactor.glb"
        ),
        PuckSkin.GLADIATOR_SHIELD to PuckSpec(
            skin = PuckSkin.GLADIATOR_SHIELD,
            titleRes = KIT_BY_THEME.getValue(TableTheme.CYBER_COLOSSEUM).preset.puckTitleRes,
            icon = Icons.Default.Shield,
            accentColor = ColosseumGold,
            hit1Accent = ColosseumGold, hit1Core = ColosseumGoldCore,
            hit2Accent = ColosseumCrimson, hit2Core = ColosseumCrimsonCore,
            neutralAccent = ColosseumGoldBright, neutralCore = WhiteHot,
            model3dAsset = "models/disco_cyber_colosseum.glb"
        )
    )

    private val MALLET_SPECS: Map<MalletSkin, MalletSpec> = mapOf(
        MalletSkin.CLASSIC_NEON to MalletSpec(
            skin = MalletSkin.CLASSIC_NEON,
            titleRes = ArenaPresetCatalog.PRESET_CLASSIC.malletTitleRes,
            icon = Icons.Default.SportsHockey,
            p1Visuals = MalletSkinVisuals(NeonCyan, NeonCyanBright),
            p2Visuals = MalletSkinVisuals(NeonOrange, NeonOrangeBright),
            p1Model3dAsset = "models/rebatedor_p1.glb",
            p2Model3dAsset = "models/rebatedor_p2.glb"
        ),
        MalletSkin.NEKO_STRIKER to MalletSpec(
            skin = MalletSkin.NEKO_STRIKER,
            titleRes = ArenaPresetCatalog.PRESET_KIARA_CAT.malletTitleRes,
            icon = Icons.Default.Pets,
            p1Visuals = MalletSkinVisuals(Color(0xFFFF1493), Color(0xFFFF69B4)),
            p2Visuals = MalletSkinVisuals(NeonCyan, NeonCyanBright),
            p1Model3dAsset = "models/rebatedor_kiara_p1.glb",
            p2Model3dAsset = "models/rebatedor_kiara_p2.glb"
        ),
        MalletSkin.PONG_PADDLE to MalletSpec(
            skin = MalletSkin.PONG_PADDLE,
            titleRes = KIT_BY_THEME.getValue(TableTheme.CYBER_PONG).preset.malletTitleRes,
            icon = Icons.Default.VideogameAsset,
            p1Visuals = MalletSkinVisuals(PongGreen, PongGreenBright),
            p2Visuals = MalletSkinVisuals(PongAmber, PongAmberBright),
            p1Model3dAsset = "models/rebatedor_cyber_pong_p1.glb",
            p2Model3dAsset = "models/rebatedor_cyber_pong_p2.glb"
        ),
        MalletSkin.MAGNETIC_STRIKER to MalletSpec(
            skin = MalletSkin.MAGNETIC_STRIKER,
            titleRes = KIT_BY_THEME.getValue(TableTheme.CYBER_SOCCER).preset.malletTitleRes,
            icon = Icons.Default.SportsSoccer,
            p1Visuals = MalletSkinVisuals(NeonEmerald, NeonEmeraldBright),
            p2Visuals = MalletSkinVisuals(NeonRuby, NeonRubyBright),
            p1Model3dAsset = "models/rebatedor_cyber_soccer_p1.glb",
            p2Model3dAsset = "models/rebatedor_cyber_soccer_p2.glb"
        ),
        MalletSkin.DUNK_STRIKER to MalletSpec(
            skin = MalletSkin.DUNK_STRIKER,
            titleRes = KIT_BY_THEME.getValue(TableTheme.NEO_BASKETBALL).preset.malletTitleRes,
            icon = Icons.Default.SportsBasketball,
            p1Visuals = MalletSkinVisuals(NeonAmber, NeonAmberBright),
            p2Visuals = MalletSkinVisuals(NeonCyan, NeonCyanBright),
            p1Model3dAsset = "models/rebatedor_neo_basketball_p1.glb",
            p2Model3dAsset = "models/rebatedor_neo_basketball_p2.glb"
        ),
        MalletSkin.SLUGGER_STRIKER to MalletSpec(
            skin = MalletSkin.SLUGGER_STRIKER,
            titleRes = KIT_BY_THEME.getValue(TableTheme.CYBER_BASEBALL).preset.malletTitleRes,
            icon = Icons.Default.SportsBaseball,
            p1Visuals = MalletSkinVisuals(BaseballRed, BaseballRedBright),
            p2Visuals = MalletSkinVisuals(BaseballCyan, BaseballCyanBright),
            p1Model3dAsset = "models/rebatedor_cyber_baseball_p1.glb",
            p2Model3dAsset = "models/rebatedor_cyber_baseball_p2.glb"
        ),
        MalletSkin.LINEBACKER_STRIKER to MalletSpec(
            skin = MalletSkin.LINEBACKER_STRIKER,
            titleRes = KIT_BY_THEME.getValue(TableTheme.GRIDIRON_FOOTBALL).preset.malletTitleRes,
            icon = Icons.Default.SportsFootball,
            p1Visuals = MalletSkinVisuals(GridironCyan, GridironGold),
            p2Visuals = MalletSkinVisuals(GridironGold, GridironCyan),
            p1Model3dAsset = "models/rebatedor_gridiron_football_p1.glb",
            p2Model3dAsset = "models/rebatedor_gridiron_football_p2.glb"
        ),
        MalletSkin.SPIKE_EMITTER to MalletSpec(
            skin = MalletSkin.SPIKE_EMITTER,
            titleRes = KIT_BY_THEME.getValue(TableTheme.GRAVITY_VOLLEY).preset.malletTitleRes,
            icon = Icons.Default.SportsVolleyball,
            p1Visuals = MalletSkinVisuals(NeonViolet, NeonVioletBright),
            p2Visuals = MalletSkinVisuals(NeonMagenta, NeonMagentaBright),
            p1Model3dAsset = "models/rebatedor_gravity_volley_p1.glb",
            p2Model3dAsset = "models/rebatedor_gravity_volley_p2.glb"
        ),
        MalletSkin.OUTRUN_STRIKER to MalletSpec(
            skin = MalletSkin.OUTRUN_STRIKER,
            titleRes = KIT_BY_THEME.getValue(TableTheme.RETRO_SYNTHWAVE).preset.malletTitleRes,
            icon = Icons.Default.WbSunny,
            p1Visuals = MalletSkinVisuals(SynthPink, SynthPinkBright),
            p2Visuals = MalletSkinVisuals(SynthCyan, SynthCyanBright),
            p1Model3dAsset = "models/rebatedor_retro_synthwave_p1.glb",
            p2Model3dAsset = "models/rebatedor_retro_synthwave_p2.glb"
        ),
        MalletSkin.RIPPER_STRIKER to MalletSpec(
            skin = MalletSkin.RIPPER_STRIKER,
            titleRes = KIT_BY_THEME.getValue(TableTheme.WASTELAND_APOCALYPSE).preset.malletTitleRes,
            icon = Icons.Default.Whatshot,
            p1Visuals = MalletSkinVisuals(WastelandFlame, WastelandFlameBright),
            p2Visuals = MalletSkinVisuals(WastelandFlameBright, WastelandFlame),
            p1Model3dAsset = "models/rebatedor_wasteland_apocalypse_p1.glb",
            p2Model3dAsset = "models/rebatedor_wasteland_apocalypse_p2.glb"
        ),
        MalletSkin.MAGNETIC_COIL to MalletSpec(
            skin = MalletSkin.MAGNETIC_COIL,
            titleRes = KIT_BY_THEME.getValue(TableTheme.TOKAMAK_REACTOR).preset.malletTitleRes,
            icon = Icons.Default.Bolt,
            p1Visuals = MalletSkinVisuals(TokamakRadiation, TokamakRadiationBright),
            p2Visuals = MalletSkinVisuals(TokamakPlasma, TokamakPlasmaBright),
            p1Model3dAsset = "models/rebatedor_tokamak_reactor_p1.glb",
            p2Model3dAsset = "models/rebatedor_tokamak_reactor_p2.glb"
        ),
        MalletSkin.SPARTAN_AEGIS to MalletSpec(
            skin = MalletSkin.SPARTAN_AEGIS,
            titleRes = KIT_BY_THEME.getValue(TableTheme.CYBER_COLOSSEUM).preset.malletTitleRes,
            icon = Icons.Default.Shield,
            p1Visuals = MalletSkinVisuals(ColosseumGold, ColosseumGoldBright),
            p2Visuals = MalletSkinVisuals(ColosseumCrimson, ColosseumGold),
            p1Model3dAsset = "models/rebatedor_cyber_colosseum_p1.glb",
            p2Model3dAsset = "models/rebatedor_cyber_colosseum_p2.glb"
        )
    )

    fun getKit(theme: TableTheme): ArenaKit = KIT_BY_THEME[theme.canonical()] ?: KIT_CLASSIC

    fun getKitForStage(stageNumber: Int): ArenaKit =
        getKit(ArenaPresetCatalog.getPresetForStage(stageNumber).theme)

    fun getPuckSpec(skin: PuckSkin): PuckSpec = PUCK_SPECS.getValue(skin)

    fun getMalletSpec(skin: MalletSkin): MalletSpec = MALLET_SPECS.getValue(skin)
}
