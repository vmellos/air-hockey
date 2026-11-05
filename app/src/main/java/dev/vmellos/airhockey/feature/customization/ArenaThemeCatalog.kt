package dev.vmellos.airhockey.feature.customization

import androidx.compose.ui.graphics.Color
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.game.ui.*

data class ThemeVisuals(
    val backgroundColor: Color,
    val primaryColor: Color,
    val secondaryColor: Color,
    val primaryRimColor: Color = primaryColor,
    val secondaryRimColor: Color = secondaryColor
)

data class MalletSkinVisuals(
    val color: Color,
    val rimColor: Color
)

/**
 * Catálogo Unificado do Conjunto Temático Esportivo (GLOSSARY.md).
 *
 * Centraliza os descritores de estilo, paletas de cores e temas visuais
 * eliminando verificações dispersas de [TableTheme] e [MalletSkin].
 */
object ArenaThemeCatalog {

    fun getVisuals(theme: TableTheme): ThemeVisuals = when (theme) {
        TableTheme.CYBER_SOCCER -> ThemeVisuals(CyberTurfDark, NeonEmerald, NeonEmeraldBright)
        TableTheme.NEO_BASKETBALL -> ThemeVisuals(CourtDark, NeonAmber, NeonAmberBright)
        TableTheme.GRAVITY_VOLLEY -> ThemeVisuals(VolleySpaceDark, NeonViolet, NeonVioletBright)
        TableTheme.GRIDIRON_FOOTBALL -> ThemeVisuals(GridironDark, GridironCyan, GridironGold)
        TableTheme.CYBER_BASEBALL -> ThemeVisuals(BaseballDark, BaseballRed, BaseballRedBright)
        TableTheme.WASTELAND_APOCALYPSE -> ThemeVisuals(WastelandDark, WastelandFlame, WastelandFlameBright)
        TableTheme.RETRO_SYNTHWAVE -> ThemeVisuals(SynthDark, SynthPink, SynthPinkBright)
        TableTheme.TOKAMAK_REACTOR -> ThemeVisuals(TokamakDark, TokamakRadiation, TokamakRadiationBright)
        TableTheme.CYBER_PONG -> ThemeVisuals(PongDark, PongGreen, PongGreenBright)
        TableTheme.CYBER_COLOSSEUM -> ThemeVisuals(ColosseumDark, ColosseumGold, ColosseumGoldBright)
        TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D -> ThemeVisuals(TableBgDark, NeonCyan, NeonOrange)
    }

    fun getMalletSkinVisuals(skin: MalletSkin, isPlayer1: Boolean): MalletSkinVisuals {
        return if (isPlayer1) {
            when (skin) {
                MalletSkin.MAGNETIC_STRIKER -> MalletSkinVisuals(NeonEmerald, NeonEmeraldBright)
                MalletSkin.DUNK_STRIKER -> MalletSkinVisuals(NeonAmber, NeonAmberBright)
                MalletSkin.SPIKE_EMITTER -> MalletSkinVisuals(NeonViolet, NeonVioletBright)
                MalletSkin.LINEBACKER_STRIKER -> MalletSkinVisuals(GridironCyan, GridironGold)
                MalletSkin.SLUGGER_STRIKER -> MalletSkinVisuals(BaseballRed, BaseballRedBright)
                MalletSkin.RIPPER_STRIKER -> MalletSkinVisuals(WastelandFlame, WastelandFlameBright)
                MalletSkin.OUTRUN_STRIKER -> MalletSkinVisuals(SynthPink, SynthPinkBright)
                MalletSkin.MAGNETIC_COIL -> MalletSkinVisuals(TokamakRadiation, TokamakRadiationBright)
                MalletSkin.PONG_PADDLE -> MalletSkinVisuals(PongGreen, PongGreenBright)
                MalletSkin.SPARTAN_AEGIS -> MalletSkinVisuals(ColosseumGold, ColosseumGoldBright)
                MalletSkin.CLASSIC_NEON -> MalletSkinVisuals(NeonCyan, NeonCyanBright)
            }
        } else {
            when (skin) {
                MalletSkin.MAGNETIC_STRIKER -> MalletSkinVisuals(NeonRuby, NeonRubyBright)
                MalletSkin.DUNK_STRIKER -> MalletSkinVisuals(NeonCyan, NeonCyanBright)
                MalletSkin.SPIKE_EMITTER -> MalletSkinVisuals(NeonMagenta, NeonMagentaBright)
                MalletSkin.LINEBACKER_STRIKER -> MalletSkinVisuals(GridironGold, GridironCyan)
                MalletSkin.SLUGGER_STRIKER -> MalletSkinVisuals(BaseballCyan, BaseballCyanBright)
                MalletSkin.RIPPER_STRIKER -> MalletSkinVisuals(WastelandFlameBright, WastelandFlame)
                MalletSkin.OUTRUN_STRIKER -> MalletSkinVisuals(SynthCyan, SynthCyanBright)
                MalletSkin.MAGNETIC_COIL -> MalletSkinVisuals(TokamakPlasma, TokamakPlasmaBright)
                MalletSkin.PONG_PADDLE -> MalletSkinVisuals(PongGreenBright, PongGreen)
                MalletSkin.SPARTAN_AEGIS -> MalletSkinVisuals(ColosseumCrimson, ColosseumGold)
                MalletSkin.CLASSIC_NEON -> MalletSkinVisuals(NeonOrange, NeonOrangeBright)
            }
        }
    }

    fun getPuckAccent(skin: PuckSkin): Color = when (skin) {
        PuckSkin.CYBER_SOCCER_BALL -> NeonEmerald
        PuckSkin.NEO_BASKETBALL -> NeonAmber
        PuckSkin.GRAVITY_VOLLEY -> NeonViolet
        PuckSkin.GRIDIRON_FOOTBALL -> GridironCyan
        PuckSkin.CYBER_BASEBALL -> BaseballRed
        PuckSkin.SAWBLADE -> WastelandFlame
        PuckSkin.SYNTH_DISC -> SynthPink
        PuckSkin.ATOMIC_CORE -> TokamakRadiation
        PuckSkin.PONG_SQUARE -> PongGreen
        PuckSkin.GLADIATOR_SHIELD -> ColosseumGold
        PuckSkin.CLASSIC_PLASMA -> NeonCyan
    }
}
