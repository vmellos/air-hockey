package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.TableTheme

/**
 * Renderizador 2D unificado para superfícies de jogo e preview de personalização.
 */
object Arena2DRenderer {

    fun DrawScope.drawTableSurface(
        theme: TableTheme,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        scale: Float,
        isExtendedHost: Boolean = false,
        isExtendedGuest: Boolean = false,
        isConnectedGuest: Boolean = false
    ) {
        when (theme) {
            TableTheme.CYBER_SOCCER -> drawCyberSoccerTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
            TableTheme.NEO_BASKETBALL -> drawNeoBasketballTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
            TableTheme.GRAVITY_VOLLEY -> drawGravityVolleyTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
            TableTheme.GRIDIRON_FOOTBALL -> drawGridironTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
            TableTheme.CYBER_BASEBALL -> drawCyberBaseballTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
            TableTheme.WASTELAND_APOCALYPSE -> drawWastelandTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
            TableTheme.RETRO_SYNTHWAVE -> drawRetroSynthwaveTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
            TableTheme.TOKAMAK_REACTOR -> drawTokamakTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
            TableTheme.CYBER_PONG -> drawCyberPongTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
            TableTheme.CYBER_COLOSSEUM -> drawColosseumTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
            TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.REALIDADE_AUMENTADA_AR -> drawChamferedTable(left, top, width, height, scale, isExtendedHost, isExtendedGuest, isConnectedGuest)
        }
    }

    fun DrawScope.drawTableGoals(
        theme: TableTheme,
        goalLeft: Float,
        goalWidth: Float,
        topY: Float,
        bottomY: Float,
        scale: Float,
        topGoalColor: Color,
        bottomGoalColor: Color,
        drawTopGoal: Boolean = true
    ) {
        when (theme) {
            TableTheme.CYBER_SOCCER -> {
                if (drawTopGoal) drawCyberSoccerGoal(goalLeft, goalWidth, topY, scale, topGoalColor, isTop = true)
                drawCyberSoccerGoal(goalLeft, goalWidth, bottomY, scale, bottomGoalColor, isTop = false)
            }
            TableTheme.NEO_BASKETBALL -> {
                if (drawTopGoal) drawBasketballGoal(goalLeft, goalWidth, topY, scale, topGoalColor, isTop = true)
                drawBasketballGoal(goalLeft, goalWidth, bottomY, scale, bottomGoalColor, isTop = false)
            }
            TableTheme.GRAVITY_VOLLEY -> {
                if (drawTopGoal) drawVolleyGoal(goalLeft, goalWidth, topY, scale, topGoalColor, isTop = true)
                drawVolleyGoal(goalLeft, goalWidth, bottomY, scale, bottomGoalColor, isTop = false)
            }
            TableTheme.GRIDIRON_FOOTBALL -> {
                if (drawTopGoal) drawGridironGoal(goalLeft, goalWidth, topY, scale, topGoalColor, isTop = true)
                drawGridironGoal(goalLeft, goalWidth, bottomY, scale, bottomGoalColor, isTop = false)
            }
            TableTheme.CYBER_BASEBALL -> {
                if (drawTopGoal) drawBaseballGoal(goalLeft, goalWidth, topY, scale, topGoalColor, isTop = true)
                drawBaseballGoal(goalLeft, goalWidth, bottomY, scale, bottomGoalColor, isTop = false)
            }
            TableTheme.WASTELAND_APOCALYPSE -> {
                if (drawTopGoal) drawWastelandGoal(goalLeft, goalWidth, topY, scale, topGoalColor, isTop = true)
                drawWastelandGoal(goalLeft, goalWidth, bottomY, scale, bottomGoalColor, isTop = false)
            }
            TableTheme.RETRO_SYNTHWAVE -> {
                if (drawTopGoal) drawSynthwaveGoal(goalLeft, goalWidth, topY, scale, topGoalColor, isTop = true)
                drawSynthwaveGoal(goalLeft, goalWidth, bottomY, scale, bottomGoalColor, isTop = false)
            }
            TableTheme.TOKAMAK_REACTOR -> {
                if (drawTopGoal) drawTokamakGoal(goalLeft, goalWidth, topY, scale, topGoalColor, isTop = true)
                drawTokamakGoal(goalLeft, goalWidth, bottomY, scale, bottomGoalColor, isTop = false)
            }
            TableTheme.CYBER_PONG -> {
                if (drawTopGoal) drawPongGoal(goalLeft, goalWidth, topY, scale, topGoalColor, isTop = true)
                drawPongGoal(goalLeft, goalWidth, bottomY, scale, bottomGoalColor, isTop = false)
            }
            TableTheme.CYBER_COLOSSEUM -> {
                if (drawTopGoal) drawColosseumGoal(goalLeft, goalWidth, topY, scale, topGoalColor, isTop = true)
                drawColosseumGoal(goalLeft, goalWidth, bottomY, scale, bottomGoalColor, isTop = false)
            }
            TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D, TableTheme.REALIDADE_AUMENTADA_AR -> {
                if (drawTopGoal) drawAngledGoal(goalLeft, goalWidth, topY, scale, NeonOrange, isTop = true)
                drawAngledGoal(goalLeft, goalWidth, bottomY, scale, NeonCyan, isTop = false)
            }
        }
    }

    fun DrawScope.drawMalletSkin(
        skin: MalletSkin,
        x: Float,
        y: Float,
        radius: Float,
        color: Color,
        rimColor: Color,
        scale: Float
    ) {
        when (skin) {
            MalletSkin.MAGNETIC_STRIKER -> drawCyberStrikerMallet(x, y, radius, color, rimColor, scale)
            MalletSkin.DUNK_STRIKER -> drawDunkStrikerMallet(x, y, radius, color, rimColor, scale)
            MalletSkin.SPIKE_EMITTER -> drawSpikeEmitterMallet(x, y, radius, color, rimColor, scale)
            MalletSkin.LINEBACKER_STRIKER -> drawGridironMallet(x, y, radius, color, rimColor, scale)
            MalletSkin.SLUGGER_STRIKER -> drawSluggerMallet(x, y, radius, color, rimColor, scale)
            MalletSkin.RIPPER_STRIKER -> drawRipperMallet(x, y, radius, color, rimColor, scale)
            MalletSkin.OUTRUN_STRIKER -> drawOutrunStrikerMallet(x, y, radius, color, rimColor, scale)
            MalletSkin.MAGNETIC_COIL -> drawMagneticCoilMallet(x, y, radius, color, rimColor, scale)
            MalletSkin.PONG_PADDLE -> drawPongMallet(x, y, radius, color, rimColor, scale)
            MalletSkin.SPARTAN_AEGIS -> drawSpartanAegisMallet(x, y, radius, color, rimColor, scale)
            MalletSkin.CLASSIC_NEON -> drawPseudo3DMallet(x, y, radius, color, rimColor, scale)
        }
    }
}
