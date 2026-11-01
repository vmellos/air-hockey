package dev.vmellos.airhockey.feature.game

import dev.vmellos.airhockey.feature.physics.MalletSnapshot
import dev.vmellos.airhockey.feature.physics.PuckSnapshot

enum class GameMode {
    SOLO,
    SPLIT_SCREEN,
    EXTENDED_HOST,
    EXTENDED_GUEST,
    CONNECTED_HOST,
    CONNECTED_GUEST
}

enum class MatchStatus {
    IDLE,
    COUNTDOWN,
    PLAYING,
    GOAL_PAUSE,
    PAUSED,
    GAME_OVER,
    RECONNECTING
}

data class MatchState(
    val mode: GameMode = GameMode.SOLO,
    val status: MatchStatus = MatchStatus.IDLE,
    val scorePlayer1: Int = 0,
    val scorePlayer2: Int = 0,
    val targetScore: Int = 7,
    val countdownNumber: Int = 3,
    val lastScorer: Int = 0, // 1 ou 2
    val winner: Int = 0, // 1 ou 2
    val remainingTimeSeconds: Int = 180,
    val isHost: Boolean = true,
    val peerExited: Boolean = false,
    val player1Name: String = "P1",
    val player2Name: String = "P2"
)
