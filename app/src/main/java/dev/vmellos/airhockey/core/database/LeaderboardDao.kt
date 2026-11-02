package dev.vmellos.airhockey.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderboardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchRecordEntity): Long

    @Query("SELECT * FROM match_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMatches(limit: Int = 50): Flow<List<MatchRecordEntity>>

    @Query("SELECT * FROM match_records WHERE gameMode = :gameMode ORDER BY timestamp DESC LIMIT :limit")
    fun getMatchesByMode(gameMode: String, limit: Int = 50): Flow<List<MatchRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfileStats(stats: PlayerProfileStatsEntity)

    @Query("SELECT * FROM player_profile_stats WHERE playerName = :name LIMIT 1")
    suspend fun getProfileStatsDirect(name: String): PlayerProfileStatsEntity?

    @Query("SELECT * FROM player_profile_stats WHERE playerName = :name LIMIT 1")
    fun getProfileStats(name: String): Flow<PlayerProfileStatsEntity?>

    @Query("SELECT * FROM player_profile_stats ORDER BY ratingElo DESC, (soloWins + multiplayerWins) DESC LIMIT :limit")
    fun getTopPlayersByRating(limit: Int = 100): Flow<List<PlayerProfileStatsEntity>>

    @Query("SELECT * FROM player_profile_stats ORDER BY soloWins DESC, ratingElo DESC LIMIT :limit")
    fun getTopSoloPlayers(limit: Int = 100): Flow<List<PlayerProfileStatsEntity>>

    @Query("SELECT * FROM player_profile_stats ORDER BY multiplayerWins DESC, ratingElo DESC LIMIT :limit")
    fun getTopMultiplayerPlayers(limit: Int = 100): Flow<List<PlayerProfileStatsEntity>>
}
