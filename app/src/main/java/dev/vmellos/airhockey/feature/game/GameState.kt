package dev.vmellos.airhockey.feature.game

import dev.vmellos.airhockey.feature.physics.MalletSnapshot
import dev.vmellos.airhockey.feature.physics.PuckSnapshot

enum class GameMode {
    SOLO,
    SPLIT_SCREEN,
    EXTENDED_HOST,
    EXTENDED_GUEST,
    CONNECTED_HOST,
    CONNECTED_GUEST,
    AI_VS_AI_LOCAL,
    AI_VS_AI_EXTENDED,
    AI_VS_AI_EXTENDED_HOST,
    AI_VS_AI_EXTENDED_GUEST
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
    val targetScore: Int = 3,
    val countdownNumber: Int = 3,
    val lastScorer: Int = 0, // 1 ou 2
    val winner: Int = 0, // 1 ou 2
    val remainingTimeSeconds: Int = 0,
    val isHost: Boolean = true,
    val peerExited: Boolean = false,
    val player1Name: String = "P1",
    val player2Name: String = "P2",
    val currentStage: Int = 1,
    val starsEarned: Int = 0
) {
    val isSpectator: Boolean
        get() = mode == GameMode.AI_VS_AI_LOCAL ||
                mode == GameMode.AI_VS_AI_EXTENDED ||
                mode == GameMode.AI_VS_AI_EXTENDED_HOST ||
                mode == GameMode.AI_VS_AI_EXTENDED_GUEST

    val isExtendedMode: Boolean
        get() = mode == GameMode.EXTENDED_HOST ||
                mode == GameMode.EXTENDED_GUEST ||
                mode == GameMode.AI_VS_AI_EXTENDED ||
                mode == GameMode.AI_VS_AI_EXTENDED_HOST ||
                mode == GameMode.AI_VS_AI_EXTENDED_GUEST
}
