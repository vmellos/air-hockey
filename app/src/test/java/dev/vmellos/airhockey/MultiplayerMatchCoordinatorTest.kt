package dev.vmellos.airhockey

import dev.vmellos.airhockey.feature.lobby.MultiplayerMatchCoordinator
import dev.vmellos.airhockey.feature.network.ControlMessages
import dev.vmellos.airhockey.feature.network.InMemoryLoopbackTransportAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiplayerMatchCoordinatorTest {

    @Test
    fun testLargadaSincronizadaBetweenHostAndGuest() = runBlocking {
        val (hostTransport, guestTransport) = InMemoryLoopbackTransportAdapter.createConnectedPair()
        val hostCoordinator = MultiplayerMatchCoordinator(hostTransport, isHost = true)
        val guestCoordinator = MultiplayerMatchCoordinator(guestTransport, isHost = false)
        delay(50)

        // Host inicia a partida (Largada Sincronizada direta per ADR-0025)
        hostCoordinator.startMatch(12.0f)
        delay(50)

        assertTrue("Host deve disparar startMatchTrigger", hostCoordinator.state.value.startMatchTrigger > 0)
        assertTrue("Guest deve disparar startMatchTrigger simultaneamente", guestCoordinator.state.value.startMatchTrigger > 0)
        assertEquals(12.0f, guestCoordinator.state.value.calibratedGapMm, 0.01f)

        hostCoordinator.terminateSession()
        guestCoordinator.terminateSession()
    }

    @Test
    fun testStateBroadcastingFromHostToGuest() = runBlocking {
        val (hostTransport, guestTransport) = InMemoryLoopbackTransportAdapter.createConnectedPair()
        val hostCoordinator = MultiplayerMatchCoordinator(hostTransport, isHost = true)
        val guestCoordinator = MultiplayerMatchCoordinator(guestTransport, isHost = false)
        delay(50)

        val state = ControlMessages.RemoteMatchState(
            statusName = "GOAL_PAUSE",
            scorePlayer1 = 3,
            scorePlayer2 = 1,
            remainingTimeSeconds = 95,
            countdownNumber = 0,
            winner = 0,
            lastScorer = 1
        )
        hostCoordinator.broadcastState(state)
        delay(50)

        assertEquals(state, guestCoordinator.state.value.remoteMatchState)

        hostCoordinator.terminateSession()
        guestCoordinator.terminateSession()
    }

    @Test
    fun testPeerTerminationTriggersPeerExited() = runBlocking {
        val (hostTransport, guestTransport) = InMemoryLoopbackTransportAdapter.createConnectedPair()
        val hostCoordinator = MultiplayerMatchCoordinator(hostTransport, isHost = true)
        val guestCoordinator = MultiplayerMatchCoordinator(guestTransport, isHost = false)
        delay(50)

        hostCoordinator.terminateSession()
        delay(50)

        assertTrue("Guest deve detectar que o peer saiu da partida", guestCoordinator.state.value.peerExited)
        guestCoordinator.terminateSession()
    }
}
