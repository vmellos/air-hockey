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
    data class Start(val gapMm: Float) : TableControlEvent()
    data class StateSync(val state: ControlMessages.RemoteMatchState) : TableControlEvent()
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
            is Start -> "${ControlMessages.START}:$gapMm"
            is StateSync -> ControlMessages.encodeState(state)
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
                    val gap = line.substringAfter(":", "8.0").toFloatOrNull() ?: 8.0f
                    Start(gap)
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
