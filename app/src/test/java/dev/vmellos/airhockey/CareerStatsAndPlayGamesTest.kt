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
import org.junit.Assert.assertNull
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

        assertNotNull(record)
        assertEquals("PLAYER_A", record!!.winner)
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

        assertNotNull(record)
        assertEquals("PLAYER_A", record!!.winner)
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
    fun testSpectatorAiVsAiDoesNotPersistMatchOrAlterElo() = runBlocking {
        val fakeDao = FakeLeaderboardDao()
        val repository = LeaderboardRepository(fakeDao)

        val localSpec = repository.recordMatch(
            gameMode = "AI_VS_AI_LOCAL",
            playerAName = "Bot 1",
            playerBName = "Bot 2",
            scoreA = 7,
            scoreB = 3,
            durationSeconds = 55
        )
        val extSpec = repository.recordMatch(
            gameMode = "AI_VS_AI_EXTENDED_HOST",
            playerAName = "Bot 1",
            playerBName = "Bot 2",
            scoreA = 5,
            scoreB = 7,
            durationSeconds = 42
        )

        assertNull(localSpec)
        assertNull(extSpec)
        assertTrue(repository.recentMatches.first().isEmpty())
        assertNull(repository.getPlayerStatsDirect("Bot 1"))
        assertNull(repository.getPlayerStatsDirect("Bot 2"))
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

    @Test
    fun testPlayGamesSaveManagerSyncWithPortMergesAndPersistsAtomically() = runBlocking {
        val fakeLeaderboardDao = FakeLeaderboardDao()
        val fakeStageDao = FakeStageProgressDao()
        fakeStageDao.insertAll(
            listOf(
                StageProgressEntity(stageNumber = 1, starsEarned = 2, isUnlocked = true),
                StageProgressEntity(stageNumber = 2, starsEarned = 0, isUnlocked = true)
            )
        )
        fakeLeaderboardDao.upsertProfileStats(
            PlayerProfileStatsEntity(
                playerName = "AtletaLocal",
                ratingElo = 1120,
                soloWins = 4
            )
        )

        val testDb = object : AppDatabase() {
            override fun leaderboardDao(): LeaderboardDao = fakeLeaderboardDao
            override fun stageProgressDao(): StageProgressDao = fakeStageDao
            override fun clearAllTables() {}
            override fun createInvalidationTracker(): InvalidationTracker {
                return InvalidationTracker(this, "match_records")
            }
            override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
                throw UnsupportedOperationException()
            }
        }

        val remoteOnlyPayload = dev.vmellos.airhockey.core.playgames.PlayGamesSavePayload(
            stages = listOf(
                StageProgressEntity(stageNumber = 1, starsEarned = 3, isUnlocked = true),
                StageProgressEntity(stageNumber = 3, starsEarned = 3, isUnlocked = true)
            ),
            stats = PlayerProfileStatsEntity(
                playerName = "AtletaLocal",
                ratingElo = 1280,
                soloWins = 9
            ),
            preferences = dev.vmellos.airhockey.core.playgames.PlayGamesPreferencesPayload(
                tableTheme = "TOKAMAK_REACTOR",
                puckSkin = "KIARA_PAW"
            )
        )

        var transactionCount = 0
        var prefWriteCount = 0
        var writtenTheme: String? = null
        var committedBytes: ByteArray? = null
        var submittedElo: Long? = null
        var submittedStars: Long? = null

        val fakeSnapshotPort = object : dev.vmellos.airhockey.core.playgames.CloudSnapshotPort {
            override suspend fun openAndResolveSnapshot(
                snapshotName: String,
                resolveConflict: (ByteArray, ByteArray) -> ByteArray
            ): ByteArray = remoteOnlyPayload.toByteArray()

            override suspend fun commitSnapshot(
                snapshotName: String,
                bytes: ByteArray,
                description: String,
                progressScore: Long
            ) {
                committedBytes = bytes
            }

            override suspend fun submitLeaderboards(eloRating: Long?, totalStars: Long) {
                submittedElo = eloRating
                submittedStars = totalStars
            }
        }

        val fakePrefsPort = object : dev.vmellos.airhockey.core.playgames.CloudPreferencesPort {
            override suspend fun readSyncSnapshot(): Pair<String, dev.vmellos.airhockey.core.playgames.PlayGamesPreferencesPayload?> {
                return "AtletaLocal" to null
            }

            override suspend fun applyCloudSyncedPreferences(
                playerName: String?,
                syncedPrefs: dev.vmellos.airhockey.core.playgames.PlayGamesPreferencesPayload?
            ) {
                prefWriteCount++
                writtenTheme = syncedPrefs?.tableTheme
            }
        }

        val saveManager = PlayGamesSaveManager(
            database = testDb,
            transactionRunner = { block ->
                transactionCount++
                block()
            },
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
            mainDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
            clock = { 987654L }
        )

        val result = saveManager.syncWithPort(fakeSnapshotPort, fakePrefsPort)

        assertTrue(result.isSuccess)
        assertEquals(1, transactionCount)
        assertEquals(1, prefWriteCount)
        assertEquals("TOKAMAK_REACTOR", writtenTheme)
        assertNotNull(committedBytes)
        assertEquals(1280L, submittedElo)
        assertEquals(6L, submittedStars)
        assertEquals(3, fakeStageDao.getStageDirect(1)?.starsEarned)
        assertEquals(true, fakeStageDao.getStageDirect(3)?.isUnlocked)
        assertEquals(1280, fakeLeaderboardDao.getProfileStatsDirect("AtletaLocal")?.ratingElo)
        assertEquals(
            dev.vmellos.airhockey.core.playgames.CloudSyncState.Success(987654L, "Fase 3 • 6 ★"),
            saveManager.syncState.value
        )
    }
}
