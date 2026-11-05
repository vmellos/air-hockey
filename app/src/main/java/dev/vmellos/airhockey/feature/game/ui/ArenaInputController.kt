package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.physics.TableDimensions
import dev.vmellos.airhockey.feature.physics.TablePoint

/**
 * Controlador Unificado de Entrada Multi-toque na Arena (GLOSSARY.md).
 *
 * Centraliza o rastreamento e atribuição estável de dedos aos rebatedores (Player 1 e Player 2)
 * para a renderização 2D (Canvas) e 3D (Sceneview), garantindo:
 * - Raio de captura padronizado (2.2× do raio do rebatedor)
 * - Deslocamento ergonômico vertical de toque (touchOffset = 110f)
 * - Roteamento correto por modo de jogo (SOLO, HOST, GUEST, SPLIT_SCREEN)
 * - Proteção estrita contra movimentação em modo espectador (AI x AI)
 * - Higienização determinística de ponteiros em pausas, contagem e avanço de fases
 */
class ArenaInputController(
    val grabRadiusMultiplier: Float = 2.2f
) {
    var activePointerP1: PointerId? = null
    var activePointerP2: PointerId? = null

    fun resetPointers() {
        activePointerP1 = null
        activePointerP2 = null
    }

    fun processPointerEvents(
        changes: List<PointerInputChange>,
        dimensions: TableDimensions,
        p1MalletX: Float,
        p1MalletY: Float,
        p2MalletX: Float,
        p2MalletY: Float,
        malletRadius: Float,
        mode: GameMode,
        touchOffset: Float,
        isSpectator: Boolean,
        screenToTable: (Float, Float) -> TablePoint?,
        onTouchP1: (Float, Float) -> Unit,
        onTouchP2: (Float, Float) -> Unit,
        onTouchGuest: (Float, Float) -> Unit
    ) {
        if (changes.none { it.pressed }) {
            resetPointers()
        }

        if (isSpectator) return

        val grabRadiusSq = (malletRadius * grabRadiusMultiplier).let { it * it }

        for (change in changes) {
            val tablePt = screenToTable(change.position.x, change.position.y) ?: continue

            if (change.pressed) {
                when (mode) {
                    GameMode.EXTENDED_GUEST, GameMode.CONNECTED_GUEST -> {
                        val targetY = tablePt.y + touchOffset
                        val dx = tablePt.x - p2MalletX
                        val dy = targetY - p2MalletY
                        val distSq = dx * dx + dy * dy

                        if (activePointerP2 == change.id) {
                            onTouchGuest(tablePt.x, targetY)
                            change.consume()
                        } else if (activePointerP2 == null && distSq <= grabRadiusSq) {
                            activePointerP2 = change.id
                            onTouchGuest(tablePt.x, targetY)
                            change.consume()
                        }
                    }
                    GameMode.EXTENDED_HOST, GameMode.CONNECTED_HOST, GameMode.SOLO -> {
                        val targetY = tablePt.y - touchOffset
                        val dx = tablePt.x - p1MalletX
                        val dy = targetY - p1MalletY
                        val distSq = dx * dx + dy * dy

                        if (activePointerP1 == change.id) {
                            onTouchP1(tablePt.x, targetY)
                            change.consume()
                        } else if (activePointerP1 == null && distSq <= grabRadiusSq) {
                            activePointerP1 = change.id
                            onTouchP1(tablePt.x, targetY)
                            change.consume()
                        }
                    }
                    GameMode.SPLIT_SCREEN -> {
                        if (tablePt.y < dimensions.centerLineY) {
                            val targetY = tablePt.y + touchOffset
                            val dx = tablePt.x - p2MalletX
                            val dy = targetY - p2MalletY
                            val distSq = dx * dx + dy * dy

                            if (activePointerP2 == change.id) {
                                onTouchP2(tablePt.x, targetY)
                                change.consume()
                            } else if (activePointerP2 == null && distSq <= grabRadiusSq) {
                                activePointerP2 = change.id
                                onTouchP2(tablePt.x, targetY)
                                change.consume()
                            }
                        } else {
                            val targetY = tablePt.y - touchOffset
                            val dx = tablePt.x - p1MalletX
                            val dy = targetY - p1MalletY
                            val distSq = dx * dx + dy * dy

                            if (activePointerP1 == change.id) {
                                onTouchP1(tablePt.x, targetY)
                                change.consume()
                            } else if (activePointerP1 == null && distSq <= grabRadiusSq) {
                                activePointerP1 = change.id
                                onTouchP1(tablePt.x, targetY)
                                change.consume()
                            }
                        }
                    }
                    else -> {}
                }
            } else {
                if (activePointerP1 == change.id) activePointerP1 = null
                if (activePointerP2 == change.id) activePointerP2 = null
            }
        }
    }
}
