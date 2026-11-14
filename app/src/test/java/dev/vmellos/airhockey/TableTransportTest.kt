package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.network.GuestInputPacket
import dev.vmellos.airhockey.feature.network.HostTelemetryPacket
import dev.vmellos.airhockey.feature.network.InMemoryLoopbackTransportAdapter
import dev.vmellos.airhockey.feature.network.TableControlEvent
import dev.vmellos.airhockey.feature.network.TransportConnectionState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableTransportTest {

    @Test
    fun testInMemoryLoopbackControlEvents() = runBlocking {
        val (host, guest) = InMemoryLoopbackTransportAdapter.createConnectedPair()

        assertEquals(TransportConnectionState.CONNECTED, host.connectionState.value)
        assertEquals(TransportConnectionState.CONNECTED, guest.connectionState.value)

        // Guest envia READY para o Host
        guest.sendControl(TableControlEvent.Ready)
        val hostReceived = host.controlEvents.first()
        assertTrue(hostReceived is TableControlEvent.Ready)

        // Host envia START com vão de 9.5mm para o Guest
        host.sendControl(TableControlEvent.Start(9.5f))
        val guestReceived = guest.controlEvents.first()
        assertTrue(guestReceived is TableControlEvent.Start)
        assertEquals(9.5f, (guestReceived as TableControlEvent.Start).gapMm, 0.01f)

        // Host fecha -> Guest detecta desconexão
        host.close()
        val disconnectEvent = guest.controlEvents.first()
        assertEquals(TableControlEvent.PeerDisconnected, disconnectEvent)
        assertEquals(TransportConnectionState.DISCONNECTED, guest.connectionState.value)
    }

    @Test
    fun testInMemoryLoopbackTelemetryAndInputFlow() = runBlocking {
        val (host, guest) = InMemoryLoopbackTransportAdapter.createConnectedPair()

        val telemetry = HostTelemetryPacket(
            sequenceNumber = 1L,
            timestampNano = 1000L,
            puckX = 500f,
            puckY = 1200f,
            puckVx = 10f,
            puckVy = -20f,
            hostMalletX = 500f,
            hostMalletY = 1800f,
            guestMalletX = 500f,
            guestMalletY = 200f
        )
        host.sendTelemetry(telemetry)
        val receivedTelemetry = guest.telemetryFrames.first()
        assertEquals(1L, receivedTelemetry.sequenceNumber)
        assertEquals(500f, receivedTelemetry.puckX, 0.01f)

        val input = GuestInputPacket(
            sequenceNumber = 2L,
            timestampNano = 2000L,
            guestTargetX = 450f,
            guestTargetY = 300f
        )
        guest.sendGuestInput(input)
        val receivedInput = host.guestInputs.first()
        assertEquals(2L, receivedInput.sequenceNumber)
        assertEquals(450f, receivedInput.guestTargetX, 0.01f)
    }

    @Test
    fun testControlEventWireStringRoundTrip() {
        val events = listOf(
            TableControlEvent.HandshakeOk("1234"),
            TableControlEvent.Ready,
            TableControlEvent.Start(14.2f),
            TableControlEvent.Pause,
            TableControlEvent.Resume,
            TableControlEvent.Restart,
            TableControlEvent.Exit,
            TableControlEvent.StateSync(
                ControlMessages.RemoteMatchState(
                    statusName = "PLAYING",
                    scorePlayer1 = 4,
                    scorePlayer2 = 2,
                    remainingTimeSeconds = 120,
                    countdownNumber = 0,
                    winner = 0,
                    lastScorer = 1
                )
            )
        )

        for (event in events) {
            val wire = event.toWireString()
            val parsed = TableControlEvent.fromWireString(wire)
            assertEquals("Event roundtrip failed for: $wire", event, parsed)
        }
    }

    @Test
    fun testStartEventWithAiDifficultiesSerialization() {
        val startEvent = TableControlEvent.Start(10.5f, 7200f, "EASY", "HARD")
        val wire = startEvent.toWireString()
        assertEquals("START:10.5:7200.0:EASY:HARD", wire)

        val parsed = TableControlEvent.fromWireString(wire) as TableControlEvent.Start
        assertEquals(10.5f, parsed.gapMm, 0.01f)
        assertEquals(7200f, parsed.maxSpeed, 0.01f)
        assertEquals("EASY", parsed.aiDiff1)
        assertEquals("HARD", parsed.aiDiff2)
    }

    @Test
    fun testStartEventWithThemeSerialization() {
        val startEvent = TableControlEvent.Start(10.5f, 7200f, "MEDIUM", "HARD", "NEO_BASKETBALL")
        val wire = startEvent.toWireString()
        assertEquals("START:10.5:7200.0:MEDIUM:HARD:NEO_BASKETBALL", wire)

        val parsed = TableControlEvent.fromWireString(wire) as TableControlEvent.Start
        assertEquals(10.5f, parsed.gapMm, 0.01f)
        assertEquals(7200f, parsed.maxSpeed, 0.01f)
        assertEquals("MEDIUM", parsed.aiDiff1)
        assertEquals("HARD", parsed.aiDiff2)
        assertEquals("NEO_BASKETBALL", parsed.themeName)
    }

    @Test
    fun testStartEventWithDimensionSerialization() {
        val startEvent = TableControlEvent.Start(8.0f, 6800f, "", "", "CYBER_SOCCER", "THREE_DIMENSIONAL")
        val wire = startEvent.toWireString()
        assertEquals("START:8.0:6800.0:::CYBER_SOCCER:THREE_DIMENSIONAL", wire)

        val parsed = TableControlEvent.fromWireString(wire) as TableControlEvent.Start
        assertEquals(8.0f, parsed.gapMm, 0.01f)
        assertEquals(6800f, parsed.maxSpeed, 0.01f)
        assertEquals("CYBER_SOCCER", parsed.themeName)
        assertEquals("THREE_DIMENSIONAL", parsed.arenaDimension)
    }

    @Test
    fun testStartEventBackwardCompatibilityWithoutAiDifficulties() {
        val wireLegacy = "START:8.0:6800.0"
        val parsed = TableControlEvent.fromWireString(wireLegacy) as TableControlEvent.Start
        assertEquals(8.0f, parsed.gapMm, 0.01f)
        assertEquals(6800f, parsed.maxSpeed, 0.01f)
        assertEquals("", parsed.aiDiff1)
        assertEquals("", parsed.aiDiff2)
    }

    @Test
    fun testTelemetryFallbackWireRoundTrip() {
        val event = TableControlEvent.TelemetryFallback(
            HostTelemetryPacket(
                sequenceNumber = 42L,
                puckX = 500f,
                puckY = 1000f,
                puckVx = 100f,
                puckVy = -50f,
                hostMalletX = 500f,
                hostMalletY = 1800f,
                guestMalletX = 500f,
                guestMalletY = 200f,
                collisionEvent = 2.toByte(),
                collisionIntensity = 0.85f
            )
        )

        val wire = event.toWireString()
        val parsed = TableControlEvent.fromWireString(wire) as TableControlEvent.TelemetryFallback
        assertEquals(42L, parsed.packet.sequenceNumber)
        assertEquals(500f, parsed.packet.puckX, 0.01f)
        assertEquals(1000f, parsed.packet.puckY, 0.01f)
        assertEquals(100f, parsed.packet.puckVx, 0.01f)
        assertEquals(-50f, parsed.packet.puckVy, 0.01f)
        assertEquals(500f, parsed.packet.hostMalletX, 0.01f)
        assertEquals(1800f, parsed.packet.hostMalletY, 0.01f)
        assertEquals(500f, parsed.packet.guestMalletX, 0.01f)
        assertEquals(200f, parsed.packet.guestMalletY, 0.01f)
        assertEquals(2.toByte(), parsed.packet.collisionEvent)
        assertEquals(0.85f, parsed.packet.collisionIntensity, 0.01f)
    }

    @Test
    fun testInMemoryLoopbackAutoMonotonicSequencing() = runBlocking {
        val (host, guest) = InMemoryLoopbackTransportAdapter.createConnectedPair()

        // Envia pacote com sequenceNumber = 0L (padrão)
        val t1 = HostTelemetryPacket(
            puckX = 500f, puckY = 1000f, puckVx = 0f, puckVy = 0f,
            hostMalletX = 500f, hostMalletY = 1750f, guestMalletX = 500f, guestMalletY = 250f
        )
        host.sendTelemetry(t1)
        val received1 = guest.telemetryFrames.first()
        assertEquals(1L, received1.sequenceNumber)

        val t2 = t1.copy(puckX = 510f)
        host.sendTelemetry(t2)
        val received2 = guest.telemetryFrames.first()
        assertEquals(2L, received2.sequenceNumber)

        val i1 = GuestInputPacket(guestTargetX = 490f, guestTargetY = 260f)
        guest.sendGuestInput(i1)
        val recInput1 = host.guestInputs.first()
        assertEquals(1L, recInput1.sequenceNumber)
    }

    @Test
    fun testStartEventWithModeSerializationRoundTrip() {
        val startEvent = TableControlEvent.Start(
            gapMm = 0.0f,
            maxSpeed = 6800f,
            aiDiff1 = "",
            aiDiff2 = "",
            themeName = "KIARA_CAT",
            arenaDimension = "THREE_DIMENSIONAL",
            mode = "CONNECTED"
        )
        val wire = startEvent.toWireString()
        assertEquals("START:0.0:6800.0:::KIARA_CAT:THREE_DIMENSIONAL:CONNECTED", wire)

        val parsed = TableControlEvent.fromWireString(wire) as TableControlEvent.Start
        assertEquals("CONNECTED", parsed.mode)
        assertEquals("KIARA_CAT", parsed.themeName)
        assertEquals("THREE_DIMENSIONAL", parsed.arenaDimension)
    }
}
