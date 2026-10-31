package dev.vmellos.airhockey.feature.network

import java.nio.ByteBuffer

object NetworkConstants {
    const val DEFAULT_TCP_PORT = 8888
    const val DEFAULT_UDP_PORT = 8889
    const val DISCOVERY_UDP_PORT = 8887
    const val SERVICE_TYPE = "_airhockey._tcp."
    const val SERVICE_NAME_PREFIX = "AirHockeyTable_"
}

enum class MessageType(val id: Byte) {
    HANDSHAKE(1),
    HANDSHAKE_ACK(2),
    TIME_SYNC_REQUEST(3),
    TIME_SYNC_RESPONSE(4),
    GOAL_EVENT(5),
    MATCH_PAUSE(6),
    MATCH_RESUME(7),
    MATCH_RESTART(8)
}

data class HostTelemetryPacket(
    val sequenceNumber: Long,
    val timestampNano: Long,
    val puckX: Float,
    val puckY: Float,
    val puckVx: Float,
    val puckVy: Float,
    val hostMalletX: Float,
    val hostMalletY: Float
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(36)
        buffer.put(0x01) // Tipo TELEMETRY
        buffer.putLong(sequenceNumber)
        buffer.putFloat(puckX)
        buffer.putFloat(puckY)
        buffer.putFloat(puckVx)
        buffer.putFloat(puckVy)
        buffer.putFloat(hostMalletX)
        buffer.putFloat(hostMalletY)
        return buffer.array()
    }

    companion object {
        fun fromByteBuffer(buffer: ByteBuffer): HostTelemetryPacket? {
            if (buffer.remaining() < 33) return null
            val type = buffer.get()
            if (type != 0x01.toByte()) return null
            val seq = buffer.getLong()
            val px = buffer.getFloat()
            val py = buffer.getFloat()
            val pvx = buffer.getFloat()
            val pvy = buffer.getFloat()
            val hmx = buffer.getFloat()
            val hmy = buffer.getFloat()
            return HostTelemetryPacket(seq, System.nanoTime(), px, py, pvx, pvy, hmx, hmy)
        }
    }
}

data class GuestInputPacket(
    val sequenceNumber: Long,
    val timestampNano: Long,
    val guestTargetX: Float,
    val guestTargetY: Float
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(17)
        buffer.put(0x02) // Tipo INPUT
        buffer.putLong(sequenceNumber)
        buffer.putFloat(guestTargetX)
        buffer.putFloat(guestTargetY)
        return buffer.array()
    }

    companion object {
        fun fromByteBuffer(buffer: ByteBuffer): GuestInputPacket? {
            if (buffer.remaining() < 17) return null
            val type = buffer.get()
            if (type != 0x02.toByte()) return null
            val seq = buffer.getLong()
            val gx = buffer.getFloat()
            val gy = buffer.getFloat()
            return GuestInputPacket(seq, System.nanoTime(), gx, gy)
        }
    }
}
