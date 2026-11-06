package dev.vmellos.airhockey

import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.sqlite.db.SupportSQLiteOpenHelper
import dev.vmellos.airhockey.core.database.AppDatabase
import dev.vmellos.airhockey.core.database.LeaderboardDao
import dev.vmellos.airhockey.core.database.MatchRecordEntity
import dev.vmellos.airhockey.core.database.PlayerProfileStatsEntity
import dev.vmellos.airhockey.core.database.StageProgressDao
import dev.vmellos.airhockey.core.database.StageProgressEntity
import dev.vmellos.airhockey.core.playgames.PlayGamesManager
import dev.vmellos.airhockey.core.playgames.PlayGamesSaveManager
import dev.vmellos.airhockey.feature.leaderboard.LeaderboardRepository
import dev.vmellos.airhockey.feature.stage.StageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeLeaderboardDao : LeaderboardDao {
    val matches = mutableListOf<MatchRecordEntity>()
    val profiles = mutableMapOf<String, PlayerProfileStatsEntity>()
    val matchesFlow = MutableStateFlow<List<MatchRecordEntity>>(emptyList())
    val profilesFlow = MutableStateFlow<Map<String, PlayerProfileStatsEntity>>(emptyMap())

    override suspend fun insertMatch(match: MatchRecordEntity): Long {
        matches.add(0, match)
        matchesFlow.value = matches.toList()
        return matches.size.toLong()
    }

    override fun getRecentMatches(limit: Int): Flow<List<MatchRecordEntity>> {
        return matchesFlow.map { it.take(limit) }
    }

    override fun getMatchesByMode(gameMode: String, limit: Int): Flow<List<MatchRecordEntity>> {
        return matchesFlow.map { list -> list.filter { it.gameMode == gameMode }.take(limit) }
    }

    override suspend fun upsertProfileStats(stats: PlayerProfileStatsEntity) {
        profiles[stats.playerName] = stats
        profilesFlow.value = profiles.toMap()
    }

    override suspend fun getProfileStatsDirect(name: String): PlayerProfileStatsEntity? {
        return profiles[name]
    }

    override fun getProfileStats(name: String): Flow<PlayerProfileStatsEntity?> {
        return profilesFlow.map { it[name] }
    }

    override fun getTopPlayersByRating(limit: Int): Flow<List<PlayerProfileStatsEntity>> {
        return profilesFlow.map { map ->
            map.values.sortedByDescending { it.ratingElo }.take(limit)
        }
    }

    override fun getTopSoloPlayers(limit: Int): Flow<List<PlayerProfileStatsEntity>> {
        return profilesFlow.map { map ->
            map.values.sortedByDescending { it.soloWins }.take(limit)
        }
    }

    override fun getTopMultiplayerPlayers(limit: Int): Flow<List<PlayerProfileStatsEntity>> {
        return profilesFlow.map { map ->
            map.values.sortedByDescending { it.multiplayerWins }.take(limit)
        }
    }
}

class CareerStatsAndPlayGamesTest {

    @Test
    fun testSoloMatchRecordsCareerStatsAndUpdatesElo() = runBlocking {
        val fakeDao = FakeLeaderboardDao()
        val repository = LeaderboardRepository(fakeDao)

        val record = repository.recordMatch(
            gameMode = "SOLO",
            aiDifficulty = "MEDIUM",
            playerAName = "TestStriker",
            playerBName = "IA",
            scoreA = 5,
            scoreB = 2,
            durationSeconds = 48
        )

        assertEquals("PLAYER_A", record.winner)
        assertTrue(record.ratingDeltaA > 0)
        assertEquals(5, record.playerAScore)
        assertEquals(2, record.playerBScore)

        val profile = repository.getPlayerStatsDirect("TestStriker")
        assertNotNull(profile)
        assertEquals("TestStriker", profile!!.playerName)
        assertEquals(1, profile.soloWins)
        assertEquals(0, profile.soloLosses)
        assertEquals(1, profile.currentWinStreak)
        assertEquals(1, profile.bestWinStreak)
        assertEquals(5, profile.goalsScored)
        assertEquals(2, profile.goalsConceded)
        assertEquals(3, profile.goalDifferential)
        assertEquals(1, profile.totalMatches)
        assertEquals(1, profile.totalWins)
        assertEquals(100, profile.winRatePercent)
        assertTrue(profile.ratingElo > 1000)

        val recentMatches = repository.recentMatches.first()
        assertEquals(1, recentMatches.size)
        assertEquals("TestStriker", recentMatches[0].playerAName)
    }

    @Test
    fun testMultiplayerMatchUpdatesBothProfilesZeroSumElo() = runBlocking {
        val fakeDao = FakeLeaderboardDao()
        val repository = LeaderboardRepository(fakeDao)

        val record = repository.recordMatch(
            gameMode = "EXTENDED_TABLE",
            playerAName = "Alice",
            playerBName = "Bob",
            scoreA = 7,
            scoreB = 4,
            durationSeconds = 60
        )

        assertEquals("PLAYER_A", record.winner)
        assertEquals(record.ratingDeltaA, -record.ratingDeltaB)

        val profileA = repository.getPlayerStatsDirect("Alice")
        val profileB = repository.getPlayerStatsDirect("Bob")

        assertNotNull(profileA)
        assertNotNull(profileB)

        assertEquals(1, profileA!!.multiplayerWins)
        assertEquals(0, profileA.multiplayerLosses)
        assertEquals(1, profileA.currentWinStreak)

        assertEquals(0, profileB!!.multiplayerWins)
        assertEquals(1, profileB.multiplayerLosses)
        assertEquals(0, profileB.currentWinStreak)
    }

    @Test
    fun testStageRepositoryGetTotalStarsDirect() = runBlocking {
        val fakeStageDao = FakeStageProgressDao()
        val stages = listOf(
            StageProgressEntity(stageNumber = 1, starsEarned = 3, isUnlocked = true),
            StageProgressEntity(stageNumber = 2, starsEarned = 2, isUnlocked = true),
            StageProgressEntity(stageNumber = 3, starsEarned = 3, isUnlocked = true),
            StageProgressEntity(stageNumber = 4, starsEarned = 1, isUnlocked = true)
        )
        fakeStageDao.insertAll(stages)

        val stageRepo = StageRepository(fakeStageDao)
        val totalStars = stageRepo.getTotalStarsDirect()

        assertEquals(9, totalStars)
    }

    private class DummyAppDatabase : AppDatabase() {
        override fun leaderboardDao(): LeaderboardDao = FakeLeaderboardDao()
        override fun stageProgressDao(): StageProgressDao = FakeStageProgressDao()
        override fun clearAllTables() {}
        override fun createInvalidationTracker(): InvalidationTracker {
            return InvalidationTracker(this, "match_records")
        }
        override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
            throw UnsupportedOperationException()
        }
    }

    @Test
    fun testPlayGamesManagerSubmitScoreHandlesBlankIdAndNoActivitySafely() {
        val dummySaveManager = PlayGamesSaveManager(DummyAppDatabase())
        val playGamesManager = PlayGamesManager(dummySaveManager)

        // Submissão com ID em branco deve retornar silenciosamente sem exceção
        playGamesManager.submitScore("", 1200L)
        playGamesManager.submitScore("   ", 1200L)

        // Submissão com ID preenchido mas sem Activity anexada também deve ser segura e tratada
        playGamesManager.submitScore("leaderboard_sample_id", 1200L)
    }

    @Test
    fun testPlayGamesManagerShowLeaderboardsAndAchievementsSafelyHandlesNoActivity() {
        val dummySaveManager = PlayGamesSaveManager(DummyAppDatabase())
        val playGamesManager = PlayGamesManager(dummySaveManager)

        var leaderboardsFeedback: String? = null
        playGamesManager.showLeaderboards(activity = null) { feedback ->
            leaderboardsFeedback = feedback
        }
        org.junit.Assert.assertNotNull(leaderboardsFeedback)

        var achievementsFeedback: String? = null
        playGamesManager.showAchievements(activity = null) { feedback ->
            achievementsFeedback = feedback
        }
        org.junit.Assert.assertNotNull(achievementsFeedback)
    }
}
