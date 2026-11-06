package dev.vmellos.airhockey.feature.leaderboard

import dev.vmellos.airhockey.core.database.LeaderboardDao
import dev.vmellos.airhockey.core.database.MatchRecordEntity
import dev.vmellos.airhockey.core.database.PlayerProfileStatsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.roundToInt

class LeaderboardRepository(
    private val dao: LeaderboardDao
) {
    val topPlayersByRating: Flow<List<PlayerProfileStatsEntity>> = dao.getTopPlayersByRating()
    val topSoloPlayers: Flow<List<PlayerProfileStatsEntity>> = dao.getTopSoloPlayers()
    val topMultiplayerPlayers: Flow<List<PlayerProfileStatsEntity>> = dao.getTopMultiplayerPlayers()
    val recentMatches: Flow<List<MatchRecordEntity>> = dao.getRecentMatches()

    fun getPlayerStats(name: String): Flow<PlayerProfileStatsEntity?> = dao.getProfileStats(name)

    suspend fun getPlayerStatsDirect(name: String): PlayerProfileStatsEntity? = withContext(Dispatchers.IO) {
        dao.getProfileStatsDirect(name)
    }

    fun getMatchesByMode(mode: String): Flow<List<MatchRecordEntity>> = dao.getMatchesByMode(mode)

    suspend fun recordMatch(
        gameMode: String,
        aiDifficulty: String? = null,
        playerAName: String,
        playerBName: String,
        scoreA: Int,
        scoreB: Int,
        durationSeconds: Int
    ): MatchRecordEntity = withContext(Dispatchers.IO) {
        val winner = when {
            scoreA > scoreB -> "PLAYER_A"
            scoreB > scoreA -> "PLAYER_B"
            else -> "DRAW"
        }

        val profileA = dao.getProfileStatsDirect(playerAName) ?: PlayerProfileStatsEntity(playerName = playerAName)
        val profileB = dao.getProfileStatsDirect(playerBName) ?: PlayerProfileStatsEntity(playerName = playerBName)

        val opponentRating = if (gameMode == "SOLO") {
            when (aiDifficulty?.uppercase()) {
                "HARD" -> 1250
                "MEDIUM" -> 1000
                "EASY" -> 800
                else -> 1000
            }
        } else {
            profileB.ratingElo
        }

        // Elo Rating Calculation (K-Factor = 32)
        val expectedA = 1.0 / (1.0 + 10.0.pow((opponentRating - profileA.ratingElo) / 400.0))
        val actualA = when (winner) {
            "PLAYER_A" -> 1.0
            "PLAYER_B" -> 0.0
            else -> 0.5
        }
        val deltaA = (32.0 * (actualA - expectedA)).roundToInt().coerceIn(-40, 40)
        val deltaB = -deltaA

        val isSolo = gameMode == "SOLO"

        // Update Profile A
        val newEloA = (profileA.ratingElo + deltaA).coerceAtLeast(100)
        val newStreakA = if (winner == "PLAYER_A") profileA.currentWinStreak + 1 else 0
        val updatedA = profileA.copy(
            ratingElo = newEloA,
            soloWins = if (isSolo && winner == "PLAYER_A") profileA.soloWins + 1 else profileA.soloWins,
            soloLosses = if (isSolo && winner == "PLAYER_B") profileA.soloLosses + 1 else profileA.soloLosses,
            soloDraws = if (isSolo && winner == "DRAW") profileA.soloDraws + 1 else profileA.soloDraws,
            multiplayerWins = if (!isSolo && winner == "PLAYER_A") profileA.multiplayerWins + 1 else profileA.multiplayerWins,
            multiplayerLosses = if (!isSolo && winner == "PLAYER_B") profileA.multiplayerLosses + 1 else profileA.multiplayerLosses,
            multiplayerDraws = if (!isSolo && winner == "DRAW") profileA.multiplayerDraws + 1 else profileA.multiplayerDraws,
            currentWinStreak = newStreakA,
            bestWinStreak = maxOf(profileA.bestWinStreak, newStreakA),
            goalsScored = profileA.goalsScored + scoreA,
            goalsConceded = profileA.goalsConceded + scoreB,
            lastMatchTimestamp = System.currentTimeMillis()
        )
        dao.upsertProfileStats(updatedA)

        // Update Profile B if not AI
        if (!isSolo) {
            val newStreakB = if (winner == "PLAYER_B") profileB.currentWinStreak + 1 else 0
            val updatedB = profileB.copy(
                ratingElo = (profileB.ratingElo + deltaB).coerceAtLeast(100),
                multiplayerWins = if (winner == "PLAYER_B") profileB.multiplayerWins + 1 else profileB.multiplayerWins,
                multiplayerLosses = if (winner == "PLAYER_A") profileB.multiplayerLosses + 1 else profileB.multiplayerLosses,
                multiplayerDraws = if (winner == "DRAW") profileB.multiplayerDraws + 1 else profileB.multiplayerDraws,
                currentWinStreak = newStreakB,
                bestWinStreak = maxOf(profileB.bestWinStreak, newStreakB),
                goalsScored = profileB.goalsScored + scoreB,
                goalsConceded = profileB.goalsConceded + scoreA,
                lastMatchTimestamp = System.currentTimeMillis()
            )
            dao.upsertProfileStats(updatedB)
        }

        val record = MatchRecordEntity(
            gameMode = gameMode,
            aiDifficulty = aiDifficulty,
            playerAName = playerAName,
            playerBName = playerBName,
            playerAScore = scoreA,
            playerBScore = scoreB,
            durationSeconds = durationSeconds,
            winner = winner,
            ratingDeltaA = deltaA,
            ratingDeltaB = deltaB,
            timestamp = System.currentTimeMillis()
        )
        dao.insertMatch(record)
        record
    }
}
