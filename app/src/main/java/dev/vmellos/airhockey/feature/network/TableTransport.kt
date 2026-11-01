package dev.vmellos.airhockey.feature.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class TransportConnectionState {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED
}

sealed class TableControlEvent {
    data class HandshakeOk(val tableCode: String) : TableControlEvent()
    object NavigateCalibration : TableControlEvent()
    object Ready : TableControlEvent()
    data class Start(val gapMm: Float, val maxSpeed: Float = 9500f) : TableControlEvent()
    data class StateSync(val state: ControlMessages.RemoteMatchState) : TableControlEvent()
    data class TelemetryFallback(val packet: HostTelemetryPacket) : TableControlEvent()
    data class GuestInputFallback(val packet: GuestInputPacket) : TableControlEvent()
    object Pause : TableControlEvent()
    object Resume : TableControlEvent()
    object Restart : TableControlEvent()
    object Exit : TableControlEvent()
    object PeerDisconnected : TableControlEvent()
    data class Raw(val payload: String) : TableControlEvent()

    fun toWireString(): String {
        return when (this) {
            is HandshakeOk -> "HANDSHAKE_OK:$tableCode"
            NavigateCalibration -> "NAVIGATE_CALIBRATION"
            Ready -> ControlMessages.READY
            is Start -> "${ControlMessages.START}:$gapMm:$maxSpeed"
            is StateSync -> ControlMessages.encodeState(state)
            is TelemetryFallback -> "TELEM:${packet.sequenceNumber},${packet.puckX},${packet.puckY},${packet.puckVx},${packet.puckVy},${packet.hostMalletX},${packet.hostMalletY},${packet.guestMalletX},${packet.guestMalletY}"
            is GuestInputFallback -> "GINPUT:${packet.sequenceNumber},${packet.guestTargetX},${packet.guestTargetY}"
            Pause -> ControlMessages.PAUSE
            Resume -> ControlMessages.RESUME
            Restart -> ControlMessages.RESTART
            Exit -> ControlMessages.EXIT
            PeerDisconnected -> ControlMessages.EXIT
            is Raw -> payload
        }
    }

    companion object {
        fun fromWireString(line: String): TableControlEvent {
            return when {
                line.startsWith("HANDSHAKE_OK") -> {
                    val code = line.substringAfter(":", "")
                    HandshakeOk(code)
                }
                line == "NAVIGATE_CALIBRATION" -> NavigateCalibration
                line == ControlMessages.READY -> Ready
                line.startsWith(ControlMessages.START) -> {
                    val parts = line.removePrefix(ControlMessages.START).removePrefix(":").split(":")
                    val gap = parts.getOrNull(0)?.toFloatOrNull() ?: 8.0f
                    val speed = parts.getOrNull(1)?.toFloatOrNull() ?: 9500f
                    Start(gap, speed)
                }
                line.startsWith("TELEM:") -> {
                    val parts = line.removePrefix("TELEM:").split(",")
                    if (parts.size >= 9) {
                        val seq = parts[0].toLongOrNull() ?: 0L
                        val px = parts[1].toFloatOrNull() ?: 500f
                        val py = parts[2].toFloatOrNull() ?: 1000f
                        val pvx = parts[3].toFloatOrNull() ?: 0f
                        val pvy = parts[4].toFloatOrNull() ?: 0f
                        val hmx = parts[5].toFloatOrNull() ?: 500f
                        val hmy = parts[6].toFloatOrNull() ?: 1750f
                        val gmx = parts[7].toFloatOrNull() ?: 500f
                        val gmy = parts[8].toFloatOrNull() ?: 250f
                        TelemetryFallback(
                            HostTelemetryPacket(seq, System.nanoTime(), px, py, pvx, pvy, hmx, hmy, gmx, gmy)
                        )
                    } else Raw(line)
                }
                line.startsWith("GINPUT:") -> {
                    val parts = line.removePrefix("GINPUT:").split(",")
                    if (parts.size >= 3) {
                        val seq = parts[0].toLongOrNull() ?: 0L
                        val gx = parts[1].toFloatOrNull() ?: 500f
                        val gy = parts[2].toFloatOrNull() ?: 250f
                        GuestInputFallback(GuestInputPacket(seq, System.nanoTime(), gx, gy))
                    } else Raw(line)
                }
                line.startsWith("STATE:") -> {
                    val state = ControlMessages.decodeState(line)
                    if (state != null) StateSync(state) else Raw(line)
                }
                line == ControlMessages.PAUSE -> Pause
                line == ControlMessages.RESUME -> Resume
                line == ControlMessages.RESTART -> Restart
                line == ControlMessages.EXIT -> Exit
                else -> Raw(line)
            }
        }
    }
}

/**
 * Seam de transporte de rede para a Mesa Estendida (ADR 0002).
 *
 * Apresenta uma interface única e tipada, ocultando a complexidade dos canais duplos
 * (TCP para controle e UDP para telemetria contínua).
 */
interface TableTransport {
    val controlEvents: Flow<TableControlEvent>
    val telemetryFrames: Flow<HostTelemetryPacket>
    val guestInputs: Flow<GuestInputPacket>
    val connectionState: StateFlow<TransportConnectionState>

    suspend fun sendControl(event: TableControlEvent)
    fun sendTelemetry(packet: HostTelemetryPacket)
    fun sendGuestInput(packet: GuestInputPacket)
    suspend fun close()
}
