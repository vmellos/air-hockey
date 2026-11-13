package dev.vmellos.airhockey.feature.game

import dev.vmellos.airhockey.core.catalog.ArenaPresetCatalog
import dev.vmellos.airhockey.core.database.MatchRecordEntity
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.leaderboard.LeaderboardRepository
import dev.vmellos.airhockey.feature.stage.StageRepository

/**
 * Porta de saída para submissão de pontuação a serviços de jogos (Google Play Games / Game Center).
 */
fun interface PlayGamesScorePort {
    fun submitScore(leaderboardId: String, score: Long)
}

/**
 * Resultado da consolidação esportiva e persistência de término de partida.
 */
data class MatchOutcomeResult(
    val matchRecord: MatchRecordEntity?,
    val starsEarned: Int = 0,
    val newlyConqueredTheme: TableTheme? = null,
    val eloRating: Int? = null,
    val totalStars: Int? = null
)

/**
 * Gravador de Desfecho de Partida (ADR 0029 & GLOSSARY.md).
 *
 * Módulo profundo que encapsula e orquestra a finalização de uma partida esportiva:
 * - Persistência do registro histórico no banco de dados local com cálculo de variação Elo
 * - Submissão da nova pontuação Elo para as Tabelas de Classificação globais
 * - Cálculo e desbloqueio de estrelas na Conquista da Arena (Modo Fases / Campanha Solo)
 * - Desbloqueio temático de Arenas Chefão (Boss Stages a cada 10 níveis)
 * - Submissão do progresso acumulado de estrelas para os placares de liderança
 */
class MatchOutcomeRecorder(
    private val leaderboardRepository: LeaderboardRepository,
    private val stageRepository: StageRepository,
    private val playGamesPort: PlayGamesScorePort,
    private val eloLeaderboardId: String = "",
    private val stageLeaderboardId: String = ""
) {

    suspend fun recordOutcome(
        state: MatchState,
        aiDifficulty: String?
    ): MatchOutcomeResult {
        if (!state.capabilities.recordsStats) {
            return MatchOutcomeResult(matchRecord = null)
        }

        val duration = state.remainingTimeSeconds.coerceAtLeast(0)
        val modeStr = state.mode.name

        val record = try {
            leaderboardRepository.recordMatch(
                gameMode = modeStr,
                aiDifficulty = aiDifficulty,
                playerAName = state.player1Name,
                playerBName = state.player2Name,
                scoreA = state.scorePlayer1,
                scoreB = state.scorePlayer2,
                durationSeconds = duration
            )
        } catch (_: Exception) {
            null
        }

        var newElo: Int? = null
        if (record != null && eloLeaderboardId.isNotBlank()) {
            val isGuest = state.capabilities.isGuest
            val localName = state.localPlayerName.ifBlank {
                if (isGuest) state.player2Name else state.player1Name
            }
            val delta = if (isGuest) record.ratingDeltaB else record.ratingDeltaA
            val profile = leaderboardRepository.getPlayerStatsDirect(localName)
            val elo = profile?.ratingElo ?: (1000 + delta)
            newElo = elo
            playGamesPort.submitScore(eloLeaderboardId, elo.toLong())
        }

        var stars = 0
        var newlyConquered: TableTheme? = null
        var totalStars: Int? = null

        if (state.mode == GameMode.SOLO) {
            try {
                val currentTheme = ArenaPresetCatalog.getThemeForStage(state.currentStage)
                val previouslyUnlocked = stageRepository.isThemeUnlocked(currentTheme)
                stars = stageRepository.recordStageResult(
                    stageNumber = state.currentStage,
                    scorePlayer = state.scorePlayer1,
                    scoreAi = state.scorePlayer2
                )
                if (stars > 0 && state.currentStage % 10 == 0 && !previouslyUnlocked) {
                    newlyConquered = currentTheme
                }

                if (stars > 0 && stageLeaderboardId.isNotBlank()) {
                    val tot = stageRepository.getTotalStarsDirect()
                    totalStars = tot
                    playGamesPort.submitScore(stageLeaderboardId, tot.toLong())
                }
            } catch (_: Exception) {}
        }

        return MatchOutcomeResult(
            matchRecord = record,
            starsEarned = stars,
            newlyConqueredTheme = newlyConquered,
            eloRating = newElo,
            totalStars = totalStars
        )
    }
}
