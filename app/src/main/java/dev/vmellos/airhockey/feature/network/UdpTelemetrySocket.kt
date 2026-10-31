package dev.vmellos.airhockey.feature.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class UdpTelemetrySocket(
    private val localPort: Int = NetworkConstants.DEFAULT_UDP_PORT
) {
    private var socket: DatagramSocket? = null
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private var targetAddress: InetAddress? = null
    private var targetPort: Int = NetworkConstants.DEFAULT_UDP_PORT
    private var sequenceCounter = 0L

    var onHostTelemetryReceived: ((HostTelemetryPacket) -> Unit)? = null
    var onGuestInputReceived: ((GuestInputPacket) -> Unit)? = null

    fun startListening() {
        stop()
        receiveJob = scope.launch {
            try {
                socket = DatagramSocket(localPort)
                val buffer = ByteArray(256)
                val packet = DatagramPacket(buffer, buffer.size)

                while (isActive) {
                    socket?.receive(packet) ?: break
                    val byteBuffer = ByteBuffer.wrap(packet.data, packet.offset, packet.length)

                    when (packet.data[packet.offset]) {
                        0x01.toByte() -> {
                            val telemetry = HostTelemetryPacket.fromByteBuffer(byteBuffer)
                            if (telemetry != null) {
                                onHostTelemetryReceived?.invoke(telemetry)
                            }
                        }
                        0x02.toByte() -> {
                            val input = GuestInputPacket.fromByteBuffer(byteBuffer)
                            if (input != null) {
                                onGuestInputReceived?.invoke(input)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Fechamento normal de socket
            }
        }
    }

    fun setTarget(targetIp: String, port: Int = NetworkConstants.DEFAULT_UDP_PORT) {
        scope.launch {
            try {
                targetAddress = InetAddress.getByName(targetIp)
                targetPort = port
            } catch (_: Exception) {}
        }
    }

    fun sendHostTelemetry(
        puckX: Float, puckY: Float,
        puckVx: Float, puckVy: Float,
        hostMalletX: Float, hostMalletY: Float
    ) {
        val dest = targetAddress ?: return
        val sock = socket ?: return

        try {
            val telemetry = HostTelemetryPacket(
                sequenceNumber = ++sequenceCounter,
                timestampNano = System.nanoTime(),
                puckX = puckX,
                puckY = puckY,
                puckVx = puckVx,
                puckVy = puckVy,
                hostMalletX = hostMalletX,
                hostMalletY = hostMalletY
            )
            val bytes = telemetry.toByteArray()
            val packet = DatagramPacket(bytes, bytes.size, dest, targetPort)
            sock.send(packet)
        } catch (_: Exception) {}
    }

    fun sendGuestInput(targetX: Float, targetY: Float) {
        val dest = targetAddress ?: return
        val sock = socket ?: return

        try {
            val input = GuestInputPacket(
                sequenceNumber = ++sequenceCounter,
                timestampNano = System.nanoTime(),
                guestTargetX = targetX,
                guestTargetY = targetY
            )
            val bytes = input.toByteArray()
            val packet = DatagramPacket(bytes, bytes.size, dest, targetPort)
            sock.send(packet)
        } catch (_: Exception) {}
    }

    fun stop() {
        receiveJob?.cancel()
        receiveJob = null
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }
}
