package dev.vmellos.airhockey.feature.physics

import dev.vmellos.airhockey.feature.game.GameMode
import kotlin.math.min

/**
 * Ponto no espaço de tela (em pixels físicos).
 */
data class ScreenPoint(val x: Float, val y: Float)

/**
 * Ponto no espaço de mesa global normalizado (0..1000 de largura).
 */
data class TablePoint(val x: Float, val y: Float)

/**
 * Módulo profundo de geometria e projeção da mesa de Air Hockey.
 *
 * Encapsula o cálculo de aspect-ratio, letterboxing, compensação do Vão Físico (ADR 0003)
 * e a rotação de 180° da Disposição Topo-a-Topo (ADR 0005).
 */
class TableProjection(
    val dimensions: TableDimensions,
    val mode: GameMode
) {
    val isExtendedHost: Boolean = mode == GameMode.EXTENDED_HOST
    val isExtendedGuest: Boolean = mode == GameMode.EXTENDED_GUEST
    val isExtended: Boolean = isExtendedHost || isExtendedGuest

    val isConnectedHost: Boolean = mode == GameMode.CONNECTED_HOST
    val isConnectedGuest: Boolean = mode == GameMode.CONNECTED_GUEST
    val isConnected: Boolean = isConnectedHost || isConnectedGuest

    val viewportVirtualWidth: Float = 1000f
    val viewportVirtualHeight: Float = 2000f

    /**
     * Calcula o fator de escala proporcional (ContentScale.Fit) para caber na tela.
     */
    fun calculateScale(screenW: Float, screenH: Float): Float {
        return min(screenW / viewportVirtualWidth, screenH / viewportVirtualHeight)
    }

    /**
     * Deslocamento horizontal para centralização na tela (letterbox).
     */
    fun calculateOffsetX(screenW: Float, screenH: Float): Float {
        val scale = calculateScale(screenW, screenH)
        return (screenW - viewportVirtualWidth * scale) / 2f
    }

    /**
     * Deslocamento vertical para posicionamento na tela.
     * Na Mesa Estendida (Disposição Topo-a-Topo), a metade da mesa é ancorada
     * diretamente no topo físico da tela (offsetY = 0) para que a borda de toque
     * encontre a moldura do celular vizinho sem faixas pretas intermediárias.
     */
    fun calculateOffsetY(screenW: Float, screenH: Float): Float {
        if (isExtended) {
            return 0f
        }
        val scale = calculateScale(screenW, screenH)
        return (screenH - viewportVirtualHeight * scale) / 2f
    }

    /**
     * Projeta uma coordenada global da mesa para pixels na tela.
     */
    fun tableToScreen(tableX: Float, tableY: Float, screenW: Float, screenH: Float): ScreenPoint {
        val scale = calculateScale(screenW, screenH)
        val offX = calculateOffsetX(screenW, screenH)
        val offY = calculateOffsetY(screenW, screenH)

        val effectiveX = when {
            isExtendedGuest || isConnectedGuest -> 1000f - tableX
            else -> tableX
        }
        val effectiveY = when {
            isExtendedHost -> tableY - dimensions.guestHalfTopY
            isExtendedGuest -> dimensions.hostHalfBottomY - tableY
            isConnectedGuest -> dimensions.totalHeight - tableY
            else -> tableY
        }

        return ScreenPoint(
            x = offX + effectiveX * scale,
            y = offY + effectiveY * scale
        )
    }

    /**
     * Converte um ponto tocado na tela (pixels) para a coordenada global da mesa.
     */
    fun screenToTable(screenX: Float, screenY: Float, screenW: Float, screenH: Float): TablePoint {
        val scale = calculateScale(screenW, screenH)
        if (scale <= 0f) return TablePoint(500f, 1000f)

        val offX = calculateOffsetX(screenW, screenH)
        val offY = calculateOffsetY(screenW, screenH)

        val localX = (screenX - offX) / scale
        val localY = (screenY - offY) / scale

        val globalX = when {
            isExtendedGuest || isConnectedGuest -> 1000f - localX
            else -> localX
        }
        val globalY = when {
            isExtendedHost -> dimensions.guestHalfTopY + localY
            isExtendedGuest -> dimensions.hostHalfBottomY - localY
            isConnectedGuest -> dimensions.totalHeight - localY
            else -> localY
        }

        return TablePoint(globalX, globalY)
    }

    /**
     * Verifica se um objeto com dado raio é visível no viewport deste dispositivo.
     */
    fun isVisibleInViewport(tableX: Float, tableY: Float, radius: Float): Boolean {
        if (tableX + radius < 0f || tableX - radius > dimensions.width) return false

        return when {
            isExtendedHost -> {
                tableY + radius >= dimensions.guestHalfTopY && tableY - radius <= dimensions.totalHeight
            }
            isExtendedGuest -> {
                tableY + radius >= 0f && tableY - radius <= dimensions.hostHalfBottomY
            }
            else -> {
                tableY + radius >= 0f && tableY - radius <= dimensions.totalHeight
            }
        }
    }

    /**
     * Restringe a posição do rebatedor à sua respectiva metade da mesa.
     */
    fun clampMallet(targetX: Float, targetY: Float, radius: Float, isPlayer1: Boolean): TablePoint {
        val clampedX = targetX.coerceIn(radius, dimensions.width - radius)

        val clampedY = if (isPlayer1) {
            val minY = if (dimensions.isExtendedMode) dimensions.guestHalfTopY + radius else (dimensions.centerLineY + radius)
            targetY.coerceIn(minY, dimensions.totalHeight - radius)
        } else {
            val maxY = if (dimensions.isExtendedMode) dimensions.hostHalfBottomY - radius else (dimensions.centerLineY - radius)
            targetY.coerceIn(radius, maxY)
        }

        return TablePoint(clampedX, clampedY)
    }

    /**
     * Retorna a posição ideal de reinício do disco com base em quem marcou o último ponto.
     */
    fun getServePosition(scorer: Int): TablePoint {
        val puckY = if (dimensions.isExtendedMode) {
            when (scorer) {
                1 -> dimensions.hostHalfBottomY - 400f // P1 marcou, P2 saca na metade superior
                2 -> dimensions.guestHalfTopY + 400f   // P2 marcou, P1 saca na metade inferior
                else -> dimensions.guestHalfTopY + 400f // Início da partida: P1 saca
            }
        } else {
            when (scorer) {
                1 -> dimensions.centerLineY - 300f
                2 -> dimensions.centerLineY + 300f
                else -> dimensions.centerLineY
            }
        }
        return TablePoint(dimensions.width / 2f, puckY)
    }
}
