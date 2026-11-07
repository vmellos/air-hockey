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
    fun testHandshakeAcceptedBetweenAiExtendedHostAndExtendedGuest() = runBlocking {
        val (hostTransport, guestTransport) = InMemoryLoopbackTransportAdapter.createConnectedPair()

        val hostCoord = MultiplayerMatchCoordinator(
            transport = hostTransport,
            isHost = true,
            expectedTableCode = "5544",
            selfPlayerName = "HostAI",
            selfMode = "AI_EXTENDED"
        )
        val guestCoord = MultiplayerMatchCoordinator(
            transport = guestTransport,
            isHost = false,
            expectedTableCode = "5544",
            selfPlayerName = "GuestSpectator",
            selfMode = "EXTENDED"
        )

        delay(80)

        assertFalse("Handshake entre AI_EXTENDED e EXTENDED não deve ser rejeitado", guestCoord.state.value.isHandshakeRejected)
        assertEquals("HostAI", guestCoord.state.value.peerPlayerName)
        assertEquals("GuestSpectator", hostCoord.state.value.peerPlayerName)
        assertEquals("AI_EXTENDED", guestCoord.state.value.mode)

        // Sincronização de Start com IAs e dimensão 2D
        hostCoord.startMatch(11.0f, 7000f, "EASY", "HARD", themeName = "CLASSIC_NEON", arenaDimension = "TWO_DIMENSIONAL")
        delay(50)

        assertEquals("EASY", guestCoord.state.value.aiDifficulty1)
        assertEquals("HARD", guestCoord.state.value.aiDifficulty2)
        assertEquals(11.0f, guestCoord.state.value.calibratedGapMm, 0.01f)
        assertEquals("TWO_DIMENSIONAL", guestCoord.state.value.hostArenaDimension)

        hostCoord.terminateSession()
        guestCoord.terminateSession()
    }

    @Test
    fun testCompatibleModeLogic() {
        assertTrue(MultiplayerMatchCoordinator.isCompatibleMode("AI_EXTENDED", "EXTENDED"))
        assertTrue(MultiplayerMatchCoordinator.isCompatibleMode("EXTENDED", "AI_EXTENDED"))
        assertTrue(MultiplayerMatchCoordinator.isCompatibleMode("CONNECTED", "CONNECTED"))
        assertTrue(MultiplayerMatchCoordinator.isCompatibleMode("EXTENDED", "EXTENDED"))
        assertTrue(MultiplayerMatchCoordinator.isCompatibleMode("CONNECTED", "ANY"))
        assertFalse(MultiplayerMatchCoordinator.isCompatibleMode("CONNECTED", "EXTENDED"))
        assertFalse(MultiplayerMatchCoordinator.isCompatibleMode("CONNECTED", "AI_EXTENDED"))
        assertFalse(MultiplayerMatchCoordinator.isCompatibleMode("AI_EXTENDED", "CONNECTED"))
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
        assertTrue(table.lastSeenMs > 0L)
    }

    @Test
    fun testTableDiscoveryRegistryBeaconParsingAndRegistration() {
        var simulatedTime = 1_000_000L
        val registry = dev.vmellos.airhockey.feature.network.TableDiscoveryRegistry(
            clock = { simulatedTime },
            ttlMillis = 3500L
        )

        // Recebe beacon de mesa aberta
        val tables = registry.handleBeacon("AIRHOCKEY_TABLE:9988:192.168.1.150:8888:CONNECTED:Mesa_Galaxy")
        assertEquals(1, tables.size)
        val table = tables.first()
        assertEquals("9988", table.tableCode)
        assertEquals("192.168.1.150", table.hostIp)
        assertEquals(8888, table.port)
        assertEquals("CONNECTED", table.mode)
        assertEquals("Mesa_Galaxy", table.hostName)
        assertEquals(1_000_000L, table.lastSeenMs)

        // Atualização pelo mesmo IP/código com novo hostName
        val updatedTables = registry.handleBeacon("AIRHOCKEY_TABLE:9988:192.168.1.150:8888:CONNECTED:Mesa_Galaxy_Updated")
        assertEquals(1, updatedTables.size)
        assertEquals("Mesa_Galaxy_Updated", updatedTables.first().hostName)
    }

    @Test
    fun testTableDiscoveryRegistryTtlExpiry() {
        var simulatedTime = 100_000L
        val registry = dev.vmellos.airhockey.feature.network.TableDiscoveryRegistry(
            clock = { simulatedTime },
            ttlMillis = 3500L
        )

        registry.handleBeacon("AIRHOCKEY_TABLE:1111:192.168.1.10:8888:EXTENDED:Mesa1")
        simulatedTime += 2000L // 2 segundos depois -> mesa ainda ativa
        registry.handleBeacon("AIRHOCKEY_TABLE:2222:192.168.1.20:8888:EXTENDED:Mesa2")

        assertEquals(2, registry.getTables().size)

        simulatedTime += 2000L // Mais 2 segundos (Mesa1 tem 4s sem beacon, Mesa2 tem 2s)
        val activeAfterTtl = registry.pruneExpired()

        assertEquals(1, activeAfterTtl.size)
        assertEquals("2222", activeAfterTtl.first().tableCode)
    }

    @Test
    fun testTableDiscoveryRegistryClosedRemoval() {
        val registry = dev.vmellos.airhockey.feature.network.TableDiscoveryRegistry()
        registry.handleBeacon("AIRHOCKEY_TABLE:1111:192.168.1.10:8888:EXTENDED:Mesa1")
        registry.handleBeacon("AIRHOCKEY_TABLE:2222:192.168.1.20:8888:EXTENDED:Mesa2")
        assertEquals(2, registry.getTables().size)

        // Mesa 1 fecha graciosamente
        val remaining = registry.handleBeacon("AIRHOCKEY_TABLE_CLOSED:1111:192.168.1.10")
        assertEquals(1, remaining.size)
        assertEquals("2222", remaining.first().tableCode)
    }
}
