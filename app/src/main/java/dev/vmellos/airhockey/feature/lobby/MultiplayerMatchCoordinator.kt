package dev.vmellos.airhockey.feature.lobby

import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.network.GuestInputPacket
import dev.vmellos.airhockey.feature.network.HostTelemetryPacket
import dev.vmellos.airhockey.feature.network.TableControlEvent
import dev.vmellos.airhockey.feature.network.TableTransport
import dev.vmellos.airhockey.feature.network.TransportConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado da sessão multijogador na Mesa Estendida.
 */
data class MultiplayerSessionState(
    val isHost: Boolean,
    val connectionState: TransportConnectionState = TransportConnectionState.CONNECTED,
    val isSelfReady: Boolean = false,
    val isPeerReady: Boolean = false,
    val startMatchTrigger: Long = 0L,
    val navigateToCalibrationTrigger: Long = 0L,
    val calibratedGapMm: Float = 8.0f,
    val peerExited: Boolean = false,
    val remoteMatchState: ControlMessages.RemoteMatchState? = null
)

/**
 * Módulo profundo que encapsula o ciclo de vida da Sessão Multijogador (GLOSSARY.md).
 *
 * Elimina o vazamento de encanamento de rede na MainActivity, gerenciando
 * pareamento, calibração, Largada Sincronizada, telemetria contínua e encerramento.
 */
class MultiplayerMatchCoordinator(
    val transport: TableTransport,
    val isHost: Boolean,
    initialGapMm: Float = 8.0f,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private val _state = MutableStateFlow(
        MultiplayerSessionState(
            isHost = isHost,
            calibratedGapMm = initialGapMm
        )
    )
    val state: StateFlow<MultiplayerSessionState> = _state.asStateFlow()

    // Callbacks opcionais de partida para quando mensagens de controle chegarem
    var onPeerPauseRequested: (() -> Unit)? = null
    var onPeerResumeRequested: (() -> Unit)? = null
    var onPeerRestartRequested: (() -> Unit)? = null

    init {
        listenToTransportEvents()
    }

    private fun listenToTransportEvents() {
        scope.launch {
            transport.connectionState.collect { connState ->
                _state.update { it.copy(connectionState = connState) }
                if (connState == TransportConnectionState.DISCONNECTED) {
                    _state.update { it.copy(peerExited = true) }
                }
            }
        }

        scope.launch {
            transport.controlEvents.collect { event ->
                handleControlEvent(event)
            }
        }
    }

    private fun handleControlEvent(event: TableControlEvent) {
        when (event) {
            TableControlEvent.NavigateCalibration -> {
                _state.update { it.copy(navigateToCalibrationTrigger = System.currentTimeMillis()) }
            }
            TableControlEvent.Ready -> {
                _state.update { it.copy(isPeerReady = true) }
            }
            is TableControlEvent.Start -> {
                _state.update {
                    it.copy(
                        calibratedGapMm = event.gapMm,
                        startMatchTrigger = System.currentTimeMillis()
                    )
                }
            }
            is TableControlEvent.StateSync -> {
                _state.update { it.copy(remoteMatchState = event.state) }
            }
            TableControlEvent.Pause -> {
                onPeerPauseRequested?.invoke()
            }
            TableControlEvent.Resume -> {
                onPeerResumeRequested?.invoke()
            }
            TableControlEvent.Restart -> {
                onPeerRestartRequested?.invoke()
            }
            TableControlEvent.Exit, TableControlEvent.PeerDisconnected -> {
                _state.update { it.copy(peerExited = true) }
            }
            else -> {}
        }
    }

    fun notifyNavigateToCalibration() {
        if (isHost) {
            scope.launch {
                transport.sendControl(TableControlEvent.NavigateCalibration)
            }
        }
    }

    fun signalCalibrationReady(gapMm: Float) {
        _state.update { it.copy(isSelfReady = true, calibratedGapMm = gapMm) }
        scope.launch {
            transport.sendControl(TableControlEvent.Ready)
        }
    }

    fun startMatch(gapMm: Float) {
        _state.update {
            it.copy(
                calibratedGapMm = gapMm,
                startMatchTrigger = System.currentTimeMillis()
            )
        }
        scope.launch {
            transport.sendControl(TableControlEvent.Start(gapMm))
        }
    }

    fun broadcastState(remoteState: ControlMessages.RemoteMatchState) {
        if (isHost) {
            scope.launch {
                transport.sendControl(TableControlEvent.StateSync(remoteState))
            }
        }
    }

    fun broadcastTelemetry(
        puckX: Float, puckY: Float,
        puckVx: Float, puckVy: Float,
        hostMalletX: Float, hostMalletY: Float,
        guestMalletX: Float, guestMalletY: Float,
        sequence: Long = 0L
    ) {
        if (isHost) {
            val packet = HostTelemetryPacket(
                sequenceNumber = sequence,
                timestampNano = System.nanoTime(),
                puckX = puckX,
                puckY = puckY,
                puckVx = puckVx,
                puckVy = puckVy,
                hostMalletX = hostMalletX,
                hostMalletY = hostMalletY,
                guestMalletX = guestMalletX,
                guestMalletY = guestMalletY
            )
            transport.sendTelemetry(packet)
        }
    }

    fun sendGuestInput(targetX: Float, targetY: Float, sequence: Long = 0L) {
        if (!isHost) {
            val packet = GuestInputPacket(
                sequenceNumber = sequence,
                timestampNano = System.nanoTime(),
                guestTargetX = targetX,
                guestTargetY = targetY
            )
            transport.sendGuestInput(packet)
        }
    }

    fun requestPause() {
        scope.launch {
            transport.sendControl(TableControlEvent.Pause)
        }
    }

    fun requestResume() {
        scope.launch {
            transport.sendControl(TableControlEvent.Resume)
        }
    }

    fun requestRestart() {
        scope.launch {
            transport.sendControl(TableControlEvent.Restart)
        }
    }

    fun terminateSession() {
        scope.launch {
            try {
                transport.sendControl(TableControlEvent.Exit)
                transport.close()
            } catch (_: Exception) {}
            scope.cancel()
        }
    }
}
