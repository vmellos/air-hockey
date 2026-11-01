package dev.vmellos.airhockey.feature.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer

class UdpTelemetrySocket(
    private val localPort: Int = NetworkConstants.DEFAULT_UDP_PORT
) {
    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private var sendJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Fila de envio conflated: só o quadro mais recente importa em telemetria contínua.
    // O envio acontece sempre em Dispatchers.IO, nunca na thread de UI.
    private var outbox = Channel<ByteArray>(Channel.CONFLATED)

    @Volatile private var targetAddress: InetAddress? = null
    @Volatile private var targetPort: Int = NetworkConstants.DEFAULT_UDP_PORT
    private var sequenceCounter = 0L

    @Volatile var lastPacketReceivedTimestamp: Long = 0L
        private set

    var onHostTelemetryReceived: ((HostTelemetryPacket) -> Unit)? = null
    var onGuestInputReceived: ((GuestInputPacket) -> Unit)? = null

    fun startListening() {
        stop()
        val sock = try {
            DatagramSocket(null).apply {
                reuseAddress = true
                bind(InetSocketAddress(localPort))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao abrir socket UDP na porta $localPort", e)
            return
        }
        socket = sock
        outbox = Channel(Channel.CONFLATED)

        receiveJob = scope.launch {
            val buffer = ByteArray(256)
            val packet = DatagramPacket(buffer, buffer.size)
            while (isActive) {
                try {
                    packet.length = buffer.size
                    sock.receive(packet)
                } catch (e: Exception) {
                    if (isActive) Log.w(TAG, "Recepção UDP encerrada", e)
                    break
                }
                lastPacketReceivedTimestamp = System.currentTimeMillis()
                val byteBuffer = ByteBuffer.wrap(packet.data, packet.offset, packet.length)
                when (packet.data[packet.offset]) {
                    0x01.toByte() -> HostTelemetryPacket.fromByteBuffer(byteBuffer)
                        ?.let { onHostTelemetryReceived?.invoke(it) }
                    0x02.toByte() -> GuestInputPacket.fromByteBuffer(byteBuffer)
                        ?.let { onGuestInputReceived?.invoke(it) }
                }
            }
        }

        val channel = outbox
        sendJob = scope.launch {
            for (bytes in channel) {
                val dest = targetAddress ?: continue
                try {
                    sock.send(DatagramPacket(bytes, bytes.size, dest, targetPort))
                } catch (e: Exception) {
                    if (isActive) Log.w(TAG, "Falha ao enviar datagrama para $dest:$targetPort", e)
                }
            }
        }
    }

    fun setTarget(targetIp: String, port: Int = NetworkConstants.DEFAULT_UDP_PORT) {
        scope.launch {
            try {
                val cleanIp = targetIp.removePrefix("/").let { raw ->
                    if (raw.contains(":") && raw.contains(".")) {
                        raw.substringAfterLast(":")
                    } else {
                        raw
                    }
                }
                targetAddress = InetAddress.getByName(cleanIp)
                targetPort = port
            } catch (e: Exception) {
                Log.w(TAG, "Endereço UDP inválido: $targetIp", e)
            }
        }
    }

    fun sendHostTelemetry(
        puckX: Float, puckY: Float,
        puckVx: Float, puckVy: Float,
        hostMalletX: Float, hostMalletY: Float,
        guestMalletX: Float, guestMalletY: Float
    ) {
        val telemetry = HostTelemetryPacket(
            sequenceNumber = ++sequenceCounter,
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
        outbox.trySend(telemetry.toByteArray())
    }

    fun sendGuestInput(targetX: Float, targetY: Float) {
        val input = GuestInputPacket(
            sequenceNumber = ++sequenceCounter,
            timestampNano = System.nanoTime(),
            guestTargetX = targetX,
            guestTargetY = targetY
        )
        outbox.trySend(input.toByteArray())
    }

    fun stop() {
        receiveJob?.cancel()
        receiveJob = null
        sendJob?.cancel()
        sendJob = null
        outbox.close()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }

    private companion object {
        const val TAG = "UdpTelemetrySocket"
    }
}
