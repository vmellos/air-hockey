package dev.vmellos.airhockey.feature.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Adaptador de teste em memória que implementa o seam TableTransport.
 *
 * Utiliza filas Kotlin Flow/Channel sem sockets de rede, garantindo execução
 * determinística em milissegundos e eliminando conflitos de portas do SO.
 */
class InMemoryLoopbackTransportAdapter private constructor() : TableTransport {

    var peer: InMemoryLoopbackTransportAdapter? = null

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

    private val _connectionState = MutableStateFlow(TransportConnectionState.CONNECTED)
    override val connectionState: StateFlow<TransportConnectionState> = _connectionState.asStateFlow()

    override suspend fun sendControl(event: TableControlEvent) {
        val target = peer ?: return
        target._controlEvents.emit(event)
    }

    override fun sendTelemetry(packet: HostTelemetryPacket) {
        peer?._telemetryFrames?.tryEmit(packet)
    }

    override fun sendGuestInput(packet: GuestInputPacket) {
        peer?._guestInputs?.tryEmit(packet)
    }

    override suspend fun close() {
        _connectionState.value = TransportConnectionState.DISCONNECTED
        peer?._controlEvents?.emit(TableControlEvent.PeerDisconnected)
        peer?._connectionState?.value = TransportConnectionState.DISCONNECTED
        peer = null
    }

    companion object {
        /**
         * Cria um par de transportes em memória mutuamente conectados.
         * Retorna (hostTransport, guestTransport).
         */
        fun createConnectedPair(): Pair<InMemoryLoopbackTransportAdapter, InMemoryLoopbackTransportAdapter> {
            val host = InMemoryLoopbackTransportAdapter()
            val guest = InMemoryLoopbackTransportAdapter()
            host.peer = guest
            guest.peer = host
            return Pair(host, guest)
        }
    }
}
