package dev.vmellos.airhockey.feature.game.ui

import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.game.LocalPlayerSlot
import dev.vmellos.airhockey.feature.physics.MatchFrameSnapshot
import dev.vmellos.airhockey.feature.physics.TablePoint
import kotlin.math.hypot

enum class PlayerSlot {
    P1,
    P2,
    GUEST
}

fun interface MalletTargetSink {
    fun submitTarget(slot: PlayerSlot, targetX: Float, targetY: Float)
}

fun interface ScreenToTableMapper {
    fun map(screenX: Float, screenY: Float): TablePoint?
}

/**
 * Controlador de toque multi-ponteiro desacoplado da árvore de renderização 2D/3D.
 *
 * Isola regras de captura de rebatedor (raio de captura), desvio de polegar e roteamento por
 * [LocalPlayerSlot] sem reter referências a Canvas, Filament ou engine de física.
 */
class ArenaInputController(
    private val grabRadiusMultiplier: Float = 2.2f
) {
    var activePointerP1: PointerId? = null
    var activePointerP2: PointerId? = null

    fun resetPointers() {
        activePointerP1 = null
        activePointerP2 = null
    }

    fun processPointerEvents(
        changes: List<PointerInputChange>,
        frame: MatchFrameSnapshot,
        mode: GameMode,
        mapper: ScreenToTableMapper,
        sink: MalletTargetSink,
        touchOffset: Float = 0f
    ) {
        val capabilities = mode.capabilities
        if (capabilities.isSpectator) {
            resetPointers()
            return
        }

        val grabRadius = frame.dimensions.malletRadius * grabRadiusMultiplier
        val centerLineY = frame.dimensions.centerLineY

        for (change in changes) {
            // Libera o ponteiro antes de tentar mapear coordenadas da tela para evitar ponteiro preso
            // caso o toque saia da área válida da mesa durante o movimento de soltura.
            if (!change.pressed) {
                if (change.id == activePointerP1) activePointerP1 = null
                if (change.id == activePointerP2) activePointerP2 = null
                continue
            }

            val tablePt = mapper.map(change.position.x, change.position.y) ?: continue

            when (capabilities.localPlayer) {
                LocalPlayerSlot.NONE -> continue
                LocalPlayerSlot.PLAYER_1 -> {
                    if (activePointerP1 == null) {
                        val dist = hypot(tablePt.x - frame.player1MalletX, tablePt.y - frame.player1MalletY)
                        if (dist <= grabRadius) {
                            activePointerP1 = change.id
                        }
                    }
                    if (change.id == activePointerP1) {
                        sink.submitTarget(PlayerSlot.P1, tablePt.x, tablePt.y - touchOffset)
                        change.consume()
                    }
                }
                LocalPlayerSlot.PLAYER_2 -> {
                    if (activePointerP1 == null) {
                        val dist = hypot(tablePt.x - frame.player2MalletX, tablePt.y - frame.player2MalletY)
                        if (dist <= grabRadius) {
                            activePointerP1 = change.id
                        }
                    }
                    if (change.id == activePointerP1) {
                        sink.submitTarget(PlayerSlot.GUEST, tablePt.x, tablePt.y + touchOffset)
                        change.consume()
                    }
                }
                LocalPlayerSlot.BOTH_SPLIT -> {
                    // Verifica propriedade ativa antes da metade da tela para não trocar de dono ao cruzar o meio.
                    when (change.id) {
                        activePointerP1 -> {
                            sink.submitTarget(PlayerSlot.P1, tablePt.x, tablePt.y - touchOffset)
                            change.consume()
                        }
                        activePointerP2 -> {
                            sink.submitTarget(PlayerSlot.P2, tablePt.x, tablePt.y + touchOffset)
                            change.consume()
                        }
                        else -> {
                            val isBottomHalf = tablePt.y >= centerLineY
                            if (isBottomHalf && activePointerP1 == null) {
                                val dist = hypot(tablePt.x - frame.player1MalletX, tablePt.y - frame.player1MalletY)
                                if (dist <= grabRadius) {
                                    activePointerP1 = change.id
                                    sink.submitTarget(PlayerSlot.P1, tablePt.x, tablePt.y - touchOffset)
                                    change.consume()
                                }
                            } else if (!isBottomHalf && activePointerP2 == null) {
                                val dist = hypot(tablePt.x - frame.player2MalletX, tablePt.y - frame.player2MalletY)
                                if (dist <= grabRadius) {
                                    activePointerP2 = change.id
                                    sink.submitTarget(PlayerSlot.P2, tablePt.x, tablePt.y + touchOffset)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
            }
        }

        if (changes.none { it.pressed }) {
            resetPointers()
        }
    }
}
