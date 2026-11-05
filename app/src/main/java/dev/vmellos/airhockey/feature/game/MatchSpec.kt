package dev.vmellos.airhockey.feature.game

import dev.vmellos.airhockey.core.preferences.AiDifficulty

/**
 * Especificação Imutável de Partida de Air Hockey.
 *
 * Agrupa todos os parâmetros de configuração e calibração de uma partida,
 * eliminando parâmetros soltos em navegadores e controladores de ciclo de vida.
 */
data class MatchSpec(
    val mode: GameMode,
    val bezelGapMm: Float = 8.0f,
    val screenHeightPx: Float = 2400f,
    val yDpi: Float = 420f,
    val targetScore: Int = 3,
    val matchTimeSeconds: Int = 180,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val aiDifficultyPlayer1: AiDifficulty = aiDifficulty,
    val aiDifficultyPlayer2: AiDifficulty = aiDifficulty,
    val puckMaxSpeed: Float = 6800f,
    val airFriction: Float = 0.40f,
    val player1Name: String = "Jogador 1",
    val player2Name: String = "Jogador 2",
    val stageNumber: Int = 1,
    val hasIntroPresentation: Boolean = false
)
