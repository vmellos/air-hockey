package dev.vmellos.airhockey.feature.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Adaptador de produção que implementa o seam TableTransport sobre sockets reais do SO.
 *
 * Encapsula o protocolo híbrido TCP/UDP (ADR 0002):
 * - TCP para controle confiável (handshake, largada sincronizada, pausas, pontuações)
 * - UDP em background para telemetria contínua a 120 Hz sem bloqueio
 * - Endereçamento IP cruzado automático: o host mira no cliente assim que o handshake TCP é aceito.
 */
class HybridSocketTransportAdapter(
    val isHost: Boolean,
    val targetHostIp: String? = null,
    val tcpPort: Int = NetworkConstants.DEFAULT_TCP_PORT,
    val udpPort: Int = NetworkConstants.DEFAULT_UDP_PORT
) : TableTransport {

    private val tcpServer: TcpControlServer? = if (isHost) TcpControlServer(tcpPort) else null
    private val tcpClient: TcpControlClient? = if (!isHost) TcpControlClient() else null
    val udpSocket: UdpTelemetrySocket = UdpTelemetrySocket(udpPort)

    private val _controlEvents = MutableSharedFlow<TableControlEvent>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val controlEvents: Flow<TableControlEvent> = _controlEvents.asSharedFlow()

    private val _telemetryFrames = MutableSharedFlow<HostTelemetryPacket>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val telemetryFrames: Flow<HostTelemetryPacket> = _telemetryFrames.asSharedFlow()

    private val _guestInputs = MutableSharedFlow<GuestInputPacket>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val guestInputs: Flow<GuestInputPacket> = _guestInputs.asSharedFlow()

    private val _connectionState = MutableStateFlow(TransportConnectionState.IDLE)
    override val connectionState: StateFlow<TransportConnectionState> = _connectionState.asStateFlow()

    private var hostTelemetrySeq = 0L
    private var guestInputSeq = 0L

    private var lastReceivedTelemetrySeq = 0L
    private var lastReceivedGuestInputSeq = 0L

    init {
        setupTransportChannels()
    }

    internal fun isUdpHealthy(): Boolean {
        val now = System.currentTimeMillis()
        val last = udpSocket.lastPacketReceivedTimestamp
        return last > 0L && (now - last) <= 200L
    }

    private fun setupTransportChannels() {
        udpSocket.startListening()

        udpSocket.onHostTelemetryReceived = { packet ->
            if (packet.sequenceNumber > lastReceivedTelemetrySeq) {
                lastReceivedTelemetrySeq = packet.sequenceNumber
                _telemetryFrames.tryEmit(packet)
            }
        }
        udpSocket.onGuestInputReceived = { input ->
            if (input.sequenceNumber > lastReceivedGuestInputSeq) {
                lastReceivedGuestInputSeq = input.sequenceNumber
                _guestInputs.tryEmit(input)
            }
        }

        if (isHost) {
            val server = tcpServer ?: return
            server.onClientConnected = { clientIp ->
                _connectionState.value = TransportConnectionState.CONNECTED
                udpSocket.setTarget(clientIp, udpPort)
            }
            server.onClientDisconnected = {
                _connectionState.value = TransportConnectionState.DISCONNECTED
                _controlEvents.tryEmit(TableControlEvent.PeerDisconnected)
            }
            server.onMessageReceived = { line ->
                val event = TableControlEvent.fromWireString(line)
                when (event) {
                    is TableControlEvent.GuestInputFallback -> {
                        if (!isUdpHealthy() && event.packet.sequenceNumber > lastReceivedGuestInputSeq) {
                            lastReceivedGuestInputSeq = event.packet.sequenceNumber
                            _guestInputs.tryEmit(event.packet)
                        }
                    }
                    else -> _controlEvents.tryEmit(event)
                }
            }
            _connectionState.value = TransportConnectionState.CONNECTING
            server.start()
        } else {
            val client = tcpClient ?: return
            val ip = targetHostIp ?: ""
            if (ip.isNotBlank()) {
                udpSocket.setTarget(ip, udpPort)
            }
            client.onConnected = {
                _connectionState.value = TransportConnectionState.CONNECTED
            }
            client.onDisconnected = {
                _connectionState.value = TransportConnectionState.DISCONNECTED
                _controlEvents.tryEmit(TableControlEvent.PeerDisconnected)
            }
            client.onMessageReceived = { line ->
                val event = TableControlEvent.fromWireString(line)
                when (event) {
                    is TableControlEvent.TelemetryFallback -> {
                        if (!isUdpHealthy() && event.packet.sequenceNumber > lastReceivedTelemetrySeq) {
                            lastReceivedTelemetrySeq = event.packet.sequenceNumber
                            _telemetryFrames.tryEmit(event.packet)
                        }
                    }
                    else -> _controlEvents.tryEmit(event)
                }
            }
            if (ip.isNotBlank()) {
                _connectionState.value = TransportConnectionState.CONNECTING
                client.connect(ip, tcpPort)
            }
        }
    }

    override suspend fun sendControl(event: TableControlEvent) {
        val wireStr = event.toWireString()
        if (isHost) {
            tcpServer?.sendMessage(wireStr)
        } else {
            tcpClient?.sendMessage(wireStr)
        }
    }

    override fun sendTelemetry(packet: HostTelemetryPacket) {
        val seq = if (packet.sequenceNumber > 0L) packet.sequenceNumber else ++hostTelemetrySeq
        val outgoing = if (packet.sequenceNumber > 0L) packet else packet.copy(sequenceNumber = seq)
        udpSocket.sendHostTelemetryPacket(outgoing)
        // Fallback TCP somente quando o canal UDP não estiver saudável (~15 Hz)
        if (!isUdpHealthy() && seq % 8L == 0L) {
            tcpServer?.sendMessage(TableControlEvent.TelemetryFallback(outgoing).toWireString())
        }
    }

    override fun sendGuestInput(packet: GuestInputPacket) {
        val seq = if (packet.sequenceNumber > 0L) packet.sequenceNumber else ++guestInputSeq
        val outgoing = if (packet.sequenceNumber > 0L) packet else packet.copy(sequenceNumber = seq)
        udpSocket.sendGuestInputPacket(outgoing)
        // Fallback TCP somente quando o canal UDP não estiver saudável (~15 Hz)
        if (!isUdpHealthy() && seq % 8L == 0L) {
            tcpClient?.sendMessage(TableControlEvent.GuestInputFallback(outgoing).toWireString())
        }
    }

    override suspend fun close() {
        _connectionState.value = TransportConnectionState.DISCONNECTED
        tcpServer?.stop()
        tcpClient?.disconnect()
        udpSocket.stop()
    }
}
