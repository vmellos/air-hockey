package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.lobby.MultiplayerMatchCoordinator
import dev.vmellos.airhockey.feature.network.DiscoveredTable
import dev.vmellos.airhockey.feature.network.InMemoryLoopbackTransportAdapter
import dev.vmellos.airhockey.feature.network.TableControlEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateMatchAndDiscoveryTest {

    @Test
    fun testHandshakeEventsWireFormat() {
        val req = TableControlEvent.HandshakeRequest(code = "4821", guestName = "Convidado1", mode = "CONNECTED")
        val wireReq = req.toWireString()
        assertEquals("HANDSHAKE_REQ:4821:Convidado1:CONNECTED", wireReq)
        val parsedReq = TableControlEvent.fromWireString(wireReq)
        assertTrue(parsedReq is TableControlEvent.HandshakeRequest)
        assertEquals("4821", (parsedReq as TableControlEvent.HandshakeRequest).code)
        assertEquals("Convidado1", parsedReq.guestName)
        assertEquals("CONNECTED", parsedReq.mode)

        val ok = TableControlEvent.HandshakeOk(tableCode = "4821", hostName = "AnfitriaoX", mode = "CONNECTED")
        val wireOk = ok.toWireString()
        assertEquals("HANDSHAKE_OK:4821:AnfitriaoX:CONNECTED", wireOk)
        val parsedOk = TableControlEvent.fromWireString(wireOk)
        assertTrue(parsedOk is TableControlEvent.HandshakeOk)
        assertEquals("4821", (parsedOk as TableControlEvent.HandshakeOk).tableCode)
        assertEquals("AnfitriaoX", parsedOk.hostName)

        val reject = TableControlEvent.HandshakeReject("INVALID_CODE")
        val wireReject = reject.toWireString()
        assertEquals("HANDSHAKE_REJECT:INVALID_CODE", wireReject)
        val parsedReject = TableControlEvent.fromWireString(wireReject)
        assertTrue(parsedReject is TableControlEvent.HandshakeReject)
        assertEquals("INVALID_CODE", (parsedReject as TableControlEvent.HandshakeReject).reason)
    }

    @Test
    fun testPrivateMatchHandshakeAcceptedWhenCodeMatches() = runBlocking {
        val (hostTransport, guestTransport) = InMemoryLoopbackTransportAdapter.createConnectedPair()

        val hostCoord = MultiplayerMatchCoordinator(
            transport = hostTransport,
            isHost = true,
            expectedTableCode = "7392",
            selfPlayerName = "HostPlayer",
            selfMode = "CONNECTED"
        )
        val guestCoord = MultiplayerMatchCoordinator(
            transport = guestTransport,
            isHost = false,
            expectedTableCode = "7392",
            selfPlayerName = "GuestPlayer",
            selfMode = "CONNECTED"
        )

        delay(80)

        // Ambos devem ter trocado seus nomes de jogador
        assertEquals("GuestPlayer", hostCoord.state.value.peerPlayerName)
        assertEquals("HostPlayer", guestCoord.state.value.peerPlayerName)
        assertFalse(guestCoord.state.value.isHandshakeRejected)

        hostCoord.terminateSession()
        guestCoord.terminateSession()
    }

    @Test
    fun testPrivateMatchHandshakeRejectedWhenCodeIsIncorrect() = runBlocking {
        val (hostTransport, guestTransport) = InMemoryLoopbackTransportAdapter.createConnectedPair()

        val hostCoord = MultiplayerMatchCoordinator(
            transport = hostTransport,
            isHost = true,
            expectedTableCode = "7392",
            selfPlayerName = "HostPlayer",
            selfMode = "CONNECTED"
        )
        val guestCoord = MultiplayerMatchCoordinator(
            transport = guestTransport,
            isHost = false,
            expectedTableCode = "0000", // Código errado!
            selfPlayerName = "Intruder",
            selfMode = "CONNECTED"
        )

        delay(80)

        assertTrue("Convidado com código incorreto deve ser rejeitado", guestCoord.state.value.isHandshakeRejected)
        assertEquals("INVALID_CODE", guestCoord.state.value.rejectReason)

        hostCoord.terminateSession()
        guestCoord.terminateSession()
    }

    @Test
    fun testModeIsolationRejectionWhenGameModesDoNotMatch() = runBlocking {
        val (hostTransport, guestTransport) = InMemoryLoopbackTransportAdapter.createConnectedPair()

        val hostCoord = MultiplayerMatchCoordinator(
            transport = hostTransport,
            isHost = true,
            expectedTableCode = "7392",
            selfPlayerName = "HostPlayer",
            selfMode = "EXTENDED" // Host em Mesa Estendida
        )
        val guestCoord = MultiplayerMatchCoordinator(
            transport = guestTransport,
            isHost = false,
            expectedTableCode = "7392",
            selfPlayerName = "GuestPlayer",
            selfMode = "CONNECTED" // Convidado tentando Mesa Conectada
        )

        delay(80)

        assertTrue("Conexão entre modos incompatíveis deve ser rejeitada", guestCoord.state.value.isHandshakeRejected)
        assertEquals("INVALID_MODE", guestCoord.state.value.rejectReason)

        hostCoord.terminateSession()
        guestCoord.terminateSession()
    }

    @Test
    fun testDiscoveredTableModeAndHostName() {
        val table = DiscoveredTable(
            name = "AirHockeyTable_1234",
            hostIp = "192.168.1.50",
            port = 8888,
            tableCode = "1234",
            mode = "CONNECTED",
            hostName = "S24+ de Vinicius"
        )
        assertEquals("CONNECTED", table.mode)
        assertEquals("S24+ de Vinicius", table.hostName)
    }
}
