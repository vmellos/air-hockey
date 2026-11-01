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

/**
 * Mensagens de controle TCP (texto, uma por linha) trocadas na Mesa Estendida.
 * O anfitrião é a fonte da verdade: ele publica o estado da partida e o convidado apenas espelha.
 */
object ControlMessages {
    const val READY = "READY"
    const val START = "START"
    const val PAUSE = "PAUSE"
    const val RESUME = "RESUME"
    const val RESTART = "RESTART"
    const val EXIT = "EXIT"
    private const val STATE_PREFIX = "STATE:"

    data class RemoteMatchState(
        val statusName: String,
        val scorePlayer1: Int,
        val scorePlayer2: Int,
        val remainingTimeSeconds: Int,
        val countdownNumber: Int,
        val winner: Int,
        val lastScorer: Int
    )

    fun encodeState(state: RemoteMatchState): String = with(state) {
        "$STATE_PREFIX$statusName,$scorePlayer1,$scorePlayer2,$remainingTimeSeconds,$countdownNumber,$winner,$lastScorer"
    }

    fun decodeState(line: String): RemoteMatchState? {
        if (!line.startsWith(STATE_PREFIX)) return null
        val parts = line.removePrefix(STATE_PREFIX).split(",")
        if (parts.size != 7) return null
        return RemoteMatchState(
            statusName = parts[0],
            scorePlayer1 = parts[1].toIntOrNull() ?: return null,
            scorePlayer2 = parts[2].toIntOrNull() ?: return null,
            remainingTimeSeconds = parts[3].toIntOrNull() ?: return null,
            countdownNumber = parts[4].toIntOrNull() ?: return null,
            winner = parts[5].toIntOrNull() ?: return null,
            lastScorer = parts[6].toIntOrNull() ?: return null
        )
    }
}

data class HostTelemetryPacket(
    val sequenceNumber: Long = 0L,
    val timestampNano: Long = 0L,
    val puckX: Float,
    val puckY: Float,
    val puckVx: Float,
    val puckVy: Float,
    val hostMalletX: Float,
    val hostMalletY: Float,
    val guestMalletX: Float,
    val guestMalletY: Float,
    val collisionEvent: Byte = 0,
    val collisionIntensity: Float = 0f
) {
    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(SIZE_BYTES)
        buffer.put(0x01) // Tipo TELEMETRY
        buffer.putLong(sequenceNumber)
        buffer.putFloat(puckX)
        buffer.putFloat(puckY)
        buffer.putFloat(puckVx)
        buffer.putFloat(puckVy)
        buffer.putFloat(hostMalletX)
        buffer.putFloat(hostMalletY)
        buffer.putFloat(guestMalletX)
        buffer.putFloat(guestMalletY)
        buffer.put(collisionEvent)
        buffer.putFloat(collisionIntensity)
        return buffer.array()
    }

    companion object {
        const val LEGACY_SIZE_BYTES = 1 + 8 + 8 * 4
        const val SIZE_BYTES = 1 + 8 + 8 * 4 + 1 + 4

        fun fromByteBuffer(buffer: ByteBuffer): HostTelemetryPacket? {
            if (buffer.remaining() < LEGACY_SIZE_BYTES) return null
            val type = buffer.get()
            if (type != 0x01.toByte()) return null
            val seq = buffer.getLong()
            val px = buffer.getFloat()
            val py = buffer.getFloat()
            val pvx = buffer.getFloat()
            val pvy = buffer.getFloat()
            val hmx = buffer.getFloat()
            val hmy = buffer.getFloat()
            val gmx = buffer.getFloat()
            val gmy = buffer.getFloat()
            val evt = if (buffer.remaining() >= 5) buffer.get() else 0.toByte()
            val intensity = if (buffer.remaining() >= 4) buffer.getFloat() else 0f
            return HostTelemetryPacket(seq, System.nanoTime(), px, py, pvx, pvy, hmx, hmy, gmx, gmy, evt, intensity)
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
