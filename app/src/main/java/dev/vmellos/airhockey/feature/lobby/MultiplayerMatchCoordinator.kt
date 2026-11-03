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
    val synchronizedPuckSpeed: Float = 6800f,
    val aiDifficulty1: String = "",
    val aiDifficulty2: String = "",
    val mode: String = "",
    val peerExited: Boolean = false,
    val remoteMatchState: ControlMessages.RemoteMatchState? = null,
    val peerPlayerName: String = "",
    val isHandshakeRejected: Boolean = false,
    val rejectReason: String? = null
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
    val expectedTableCode: String = "",
    val selfPlayerName: String = "",
    val selfMode: String = "EXTENDED",
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
                if (connState == TransportConnectionState.CONNECTED && !isHost) {
                    // Convidado envia a solicitação de Handshake com código e nome
                    transport.sendControl(
                        TableControlEvent.HandshakeRequest(
                            code = expectedTableCode,
                            guestName = selfPlayerName,
                            mode = selfMode
                        )
                    )
                } else if (connState == TransportConnectionState.DISCONNECTED) {
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
            is TableControlEvent.HandshakeRequest -> {
                if (isHost) {
                    val codeMatches = expectedTableCode.isBlank() || event.code == expectedTableCode
                    val modeMatches = isCompatibleMode(selfMode, event.mode)
                    if (!codeMatches) {
                        scope.launch {
                            transport.sendControl(TableControlEvent.HandshakeReject("INVALID_CODE"))
                        }
                    } else if (!modeMatches) {
                        scope.launch {
                            transport.sendControl(TableControlEvent.HandshakeReject("INVALID_MODE"))
                        }
                    } else {
                        _state.update { it.copy(peerPlayerName = event.guestName) }
                        scope.launch {
                            transport.sendControl(
                                TableControlEvent.HandshakeOk(
                                    tableCode = expectedTableCode,
                                    hostName = selfPlayerName,
                                    mode = selfMode
                                )
                            )
                        }
                    }
                }
            }
            is TableControlEvent.HandshakeOk -> {
                if (!isHost) {
                    _state.update {
                        it.copy(
                            peerPlayerName = event.hostName,
                            mode = if (event.mode.isNotBlank()) event.mode else it.mode
                        )
                    }
                }
            }
            is TableControlEvent.HandshakeReject -> {
                if (!isHost) {
                    _state.update {
                        it.copy(
                            isHandshakeRejected = true,
                            rejectReason = event.reason,
                            peerExited = true
                        )
                    }
                    terminateSession()
                }
            }
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
                        synchronizedPuckSpeed = event.maxSpeed,
                        aiDifficulty1 = event.aiDiff1,
                        aiDifficulty2 = event.aiDiff2,
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

    fun startMatch(gapMm: Float, maxSpeed: Float = 6800f, aiDiff1: String = "", aiDiff2: String = "") {
        _state.update {
            it.copy(
                calibratedGapMm = gapMm,
                synchronizedPuckSpeed = maxSpeed,
                aiDifficulty1 = aiDiff1,
                aiDifficulty2 = aiDiff2,
                startMatchTrigger = System.currentTimeMillis()
            )
        }
        scope.launch {
            transport.sendControl(TableControlEvent.Start(gapMm, maxSpeed, aiDiff1, aiDiff2))
        }
    }

    companion object {
        fun isCompatibleMode(hostMode: String, guestMode: String): Boolean {
            if (hostMode.isBlank() || guestMode.isBlank() || guestMode.equals("ANY", ignoreCase = true)) return true
            if (hostMode.equals(guestMode, ignoreCase = true)) return true
            val isHostExt = hostMode.equals("EXTENDED", ignoreCase = true) || hostMode.equals("AI_EXTENDED", ignoreCase = true)
            val isGuestExt = guestMode.equals("EXTENDED", ignoreCase = true) || guestMode.equals("AI_EXTENDED", ignoreCase = true)
            return isHostExt && isGuestExt
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
        sequence: Long = 0L,
        collisionEvent: Byte = 0,
        collisionIntensity: Float = 0f,
        puckLastHitBy: Byte = 0
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
                guestMalletY = guestMalletY,
                collisionEvent = collisionEvent,
                collisionIntensity = collisionIntensity,
                puckLastHitBy = puckLastHitBy
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
