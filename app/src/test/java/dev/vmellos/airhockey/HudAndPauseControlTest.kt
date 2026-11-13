package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.game.GameMode
import dev.vmellos.airhockey.feature.lobby.MultiplayerMatchCoordinator
import dev.vmellos.airhockey.feature.network.InMemoryLoopbackTransportAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HudAndPauseControlTest {

    @Test
    fun testAiVsAiExtendedGuestPauseCoordination() = runBlocking {
        val (hostTransport, guestTransport) = InMemoryLoopbackTransportAdapter.createConnectedPair()
        val hostCoordinator = MultiplayerMatchCoordinator(hostTransport, isHost = true)
        val guestCoordinator = MultiplayerMatchCoordinator(
            guestTransport,
            isHost = false,
            selfMode = "AI_EXTENDED"
        )
        delay(50)

        var hostPauseReceived = false
        var hostResumeReceived = false
        var hostRestartReceived = false

        hostCoordinator.onPeerPauseRequested = { hostPauseReceived = true }
        hostCoordinator.onPeerResumeRequested = { hostResumeReceived = true }
        hostCoordinator.onPeerRestartRequested = { hostRestartReceived = true }

        // Espectador 2 solicita pausa
        guestCoordinator.requestPause()
        delay(50)
        assertTrue("Host deve receber pedido de pausa do espectador 2", hostPauseReceived)

        // Espectador 2 solicita retomada
        guestCoordinator.requestResume()
        delay(50)
        assertTrue("Host deve receber pedido de retomada do espectador 2", hostResumeReceived)

        // Espectador 2 solicita reiniciar
        guestCoordinator.requestRestart()
        delay(50)
        assertTrue("Host deve receber pedido de reinício do espectador 2", hostRestartReceived)

        hostCoordinator.terminateSession()
        guestCoordinator.terminateSession()
    }

    @Test
    fun testAiVsAiExtendedGuestModeClassification() {
        val mode = GameMode.AI_VS_AI_EXTENDED_GUEST
        val capabilities = mode.capabilities

        assertTrue("AI_VS_AI_EXTENDED_GUEST deve ser classificado como modo convidado para delegação de pausa", capabilities.isGuest)
        assertTrue(capabilities.isExtended)
        assertTrue(capabilities.isSpectator)
        assertTrue(!capabilities.recordsStats)
    }

    @Test
    fun testBotNameFormattingLogic() {
        val botName = "Bot 1"
        val diffName = "Medium"
        val formatted = String.format("%s (%s)", botName, diffName)
        assertEquals("Bot 1 (Medium)", formatted)
    }
}
