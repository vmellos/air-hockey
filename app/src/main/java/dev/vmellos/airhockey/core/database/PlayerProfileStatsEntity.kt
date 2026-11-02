package dev.vmellos.airhockey.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile_stats")
data class PlayerProfileStatsEntity(
    @PrimaryKey
    val playerName: String,
    val ratingElo: Int = 1000,
    val soloWins: Int = 0,
    val soloLosses: Int = 0,
    val soloDraws: Int = 0,
    val multiplayerWins: Int = 0,
    val multiplayerLosses: Int = 0,
    val multiplayerDraws: Int = 0,
    val currentWinStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val goalsScored: Int = 0,
    val goalsConceded: Int = 0,
    val lastMatchTimestamp: Long = System.currentTimeMillis()
) {
    val totalMatches: Int
        get() = soloWins + soloLosses + soloDraws + multiplayerWins + multiplayerLosses + multiplayerDraws

    val totalWins: Int
        get() = soloWins + multiplayerWins

    val winRatePercent: Int
        get() = if (totalMatches > 0) ((totalWins.toDouble() / totalMatches) * 100).toInt() else 0

    val goalDifferential: Int
        get() = goalsScored - goalsConceded
}
