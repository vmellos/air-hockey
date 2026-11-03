package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.network.GuestInputPacket
import dev.vmellos.airhockey.feature.network.HostTelemetryPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer

class ProtocolMessagesTest {

    @Test
    fun testHostTelemetryPacketSerialization() {
        val original = HostTelemetryPacket(
            sequenceNumber = 42L,
            timestampNano = 123456789L,
            puckX = 523.5f,
            puckY = 874.2f,
            puckVx = -450.0f,
            puckVy = 1200.0f,
            hostMalletX = 510.0f,
            hostMalletY = 1780.0f,
            guestMalletX = 320.0f,
            guestMalletY = 240.0f
        )

        val bytes = original.toByteArray()
        assertEquals(HostTelemetryPacket.SIZE_BYTES, bytes.size)

        val deserialized = HostTelemetryPacket.fromByteBuffer(ByteBuffer.wrap(bytes))
        assertNotNull(deserialized)
        assertEquals(42L, deserialized!!.sequenceNumber)
        assertEquals(523.5f, deserialized.puckX, 0.01f)
        assertEquals(874.2f, deserialized.puckY, 0.01f)
        assertEquals(-450.0f, deserialized.puckVx, 0.01f)
        assertEquals(1200.0f, deserialized.puckVy, 0.01f)
        assertEquals(510.0f, deserialized.hostMalletX, 0.01f)
        assertEquals(1780.0f, deserialized.hostMalletY, 0.01f)
        assertEquals(320.0f, deserialized.guestMalletX, 0.01f)
        assertEquals(240.0f, deserialized.guestMalletY, 0.01f)
        assertEquals(0.toByte(), deserialized.collisionEvent)
        assertEquals(0f, deserialized.collisionIntensity, 0.01f)
        assertEquals(0.toByte(), deserialized.puckLastHitBy)
    }

    @Test
    fun testHostTelemetryPacketWithCollisionAndLastHitBySerialization() {
        val original = HostTelemetryPacket(
            sequenceNumber = 43L,
            timestampNano = 222333444L,
            puckX = 500.0f,
            puckY = 900.0f,
            puckVx = -300.0f,
            puckVy = 600.0f,
            hostMalletX = 500.0f,
            hostMalletY = 1750.0f,
            guestMalletX = 500.0f,
            guestMalletY = 250.0f,
            collisionEvent = 2.toByte(),
            collisionIntensity = 0.85f,
            puckLastHitBy = 2.toByte()
        )

        val bytes = original.toByteArray()
        assertEquals(HostTelemetryPacket.SIZE_BYTES, bytes.size)

        val deserialized = HostTelemetryPacket.fromByteBuffer(ByteBuffer.wrap(bytes))
        assertNotNull(deserialized)
        assertEquals(43L, deserialized!!.sequenceNumber)
        assertEquals(2.toByte(), deserialized.collisionEvent)
        assertEquals(0.85f, deserialized.collisionIntensity, 0.01f)
        assertEquals(2.toByte(), deserialized.puckLastHitBy)
    }

    @Test
    fun testHostTelemetryPacketBackwardCompatibilityWithoutCollisionFields() {
        // Formato legado com 41 bytes (sem collisionEvent e collisionIntensity)
        val legacyBuffer = ByteBuffer.allocate(41)
        legacyBuffer.put(0x01.toByte())
        legacyBuffer.putLong(99L)
        legacyBuffer.putFloat(100f) // puckX
        legacyBuffer.putFloat(200f) // puckY
        legacyBuffer.putFloat(10f)  // puckVx
        legacyBuffer.putFloat(20f)  // puckVy
        legacyBuffer.putFloat(300f) // hostMalletX
        legacyBuffer.putFloat(400f) // hostMalletY
        legacyBuffer.putFloat(500f) // guestMalletX
        legacyBuffer.putFloat(600f) // guestMalletY
        legacyBuffer.flip()

        val deserialized = HostTelemetryPacket.fromByteBuffer(legacyBuffer)
        assertNotNull(deserialized)
        assertEquals(99L, deserialized!!.sequenceNumber)
        assertEquals(100f, deserialized.puckX, 0.01f)
        assertEquals(0.toByte(), deserialized.collisionEvent)
        assertEquals(0f, deserialized.collisionIntensity, 0.01f)
    }

    @Test
    fun testTruncatedTelemetryIsRejected() {
        val bytes = ByteArray(HostTelemetryPacket.LEGACY_SIZE_BYTES - 1).also { it[0] = 0x01 }
        assertNull(HostTelemetryPacket.fromByteBuffer(ByteBuffer.wrap(bytes)))
    }

    @Test
    fun testGuestInputPacketSerialization() {
        val original = GuestInputPacket(
            sequenceNumber = 101L,
            timestampNano = 987654321L,
            guestTargetX = 650.0f,
            guestTargetY = 320.0f
        )

        val bytes = original.toByteArray()
        assertEquals(17, bytes.size)

        val deserialized = GuestInputPacket.fromByteBuffer(ByteBuffer.wrap(bytes))
        assertNotNull(deserialized)
        assertEquals(101L, deserialized!!.sequenceNumber)
        assertEquals(650.0f, deserialized.guestTargetX, 0.01f)
        assertEquals(320.0f, deserialized.guestTargetY, 0.01f)
    }

    @Test
    fun testMatchStateRoundTrip() {
        val state = ControlMessages.RemoteMatchState(
            statusName = "COUNTDOWN",
            scorePlayer1 = 3,
            scorePlayer2 = 5,
            remainingTimeSeconds = 97,
            countdownNumber = 2,
            winner = 0,
            lastScorer = 2
        )

        val line = ControlMessages.encodeState(state)
        assertEquals(state, ControlMessages.decodeState(line))
    }

    @Test
    fun testDecodeStateRejectsOtherMessagesAndGarbage() {
        assertNull(ControlMessages.decodeState(ControlMessages.READY))
        assertNull(ControlMessages.decodeState("STATE:PLAYING,1,2"))
        assertNull(ControlMessages.decodeState("STATE:PLAYING,a,2,3,4,5,6"))
    }

    @Test
    fun testControlMessageConstants() {
        assertEquals("READY", ControlMessages.READY)
        assertEquals("START", ControlMessages.START)
        assertEquals("PAUSE", ControlMessages.PAUSE)
        assertEquals("RESUME", ControlMessages.RESUME)
        assertEquals("RESTART", ControlMessages.RESTART)
        assertEquals("EXIT", ControlMessages.EXIT)
    }
}
