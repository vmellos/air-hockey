package dev.vmellos.airhockey.feature.game

import dev.vmellos.airhockey.FakeLeaderboardDao
import dev.vmellos.airhockey.FakeStageProgressDao
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.catalog.ArenaPresetCatalog
import dev.vmellos.airhockey.feature.leaderboard.LeaderboardRepository
import dev.vmellos.airhockey.feature.stage.StageRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchOutcomeRecorderTest {

    private class RecordingPlayGamesPort : PlayGamesScorePort {
        val submissions = mutableListOf<Pair<String, Long>>()
        override fun submitScore(leaderboardId: String, score: Long) {
            submissions.add(leaderboardId to score)
        }
    }

    @Test
    fun recordOutcome_soloSavesRecordUpdatesEloAndSubmitsStars() = runBlocking {
        val fakeLeaderboardDao = FakeLeaderboardDao()
        val fakeStageDao = FakeStageProgressDao()
        val leaderboardRepo = LeaderboardRepository(fakeLeaderboardDao)
        val stageRepo = StageRepository(fakeStageDao)
        val playGamesPort = RecordingPlayGamesPort()

        val recorder = MatchOutcomeRecorder(
            leaderboardRepository = leaderboardRepo,
            stageRepository = stageRepo,
            playGamesPort = playGamesPort,
            eloLeaderboardId = "leaderboard_elo",
            stageLeaderboardId = "leaderboard_stages"
        )

        val state = MatchState(
            mode = GameMode.SOLO,
            status = MatchStatus.GAME_OVER,
            scorePlayer1 = 5,
            scorePlayer2 = 2,
            targetScore = 5,
            remainingTimeSeconds = 45,
            player1Name = "Champion",
            player2Name = "AI",
            currentStage = 1,
            winner = 1
        )

        val result = recorder.recordOutcome(state, aiDifficulty = "MEDIUM")

        assertNotNull(result.matchRecord)
        assertEquals("PLAYER_A", result.matchRecord?.winner)
        assertTrue("Stars earned for 5-2 win should be > 0", result.starsEarned > 0)
        assertNotNull(result.eloRating)
        assertTrue(result.eloRating!! > 1000)

        // Submissions to both Play Games leaderboards
        assertEquals(2, playGamesPort.submissions.size)
        assertEquals("leaderboard_elo", playGamesPort.submissions[0].first)
        assertEquals(result.eloRating!!.toLong(), playGamesPort.submissions[0].second)
        assertEquals("leaderboard_stages", playGamesPort.submissions[1].first)
        assertEquals(result.starsEarned.toLong(), playGamesPort.submissions[1].second)
    }

    @Test
    fun recordOutcome_bossStageVictoryUnlocksTheme() = runBlocking {
        val fakeLeaderboardDao = FakeLeaderboardDao()
        val fakeStageDao = FakeStageProgressDao()
        val leaderboardRepo = LeaderboardRepository(fakeLeaderboardDao)
        val stageRepo = StageRepository(fakeStageDao)
        val playGamesPort = RecordingPlayGamesPort()

        val recorder = MatchOutcomeRecorder(
            leaderboardRepository = leaderboardRepo,
            stageRepository = stageRepo,
            playGamesPort = playGamesPort
        )

        val bossStage = 10
        val bossTheme = ArenaPresetCatalog.getThemeForStage(bossStage)

        val state = MatchState(
            mode = GameMode.SOLO,
            status = MatchStatus.GAME_OVER,
            scorePlayer1 = 5,
            scorePlayer2 = 3,
            targetScore = 5,
            remainingTimeSeconds = 70,
            player1Name = "Hero",
            player2Name = "BossAI",
            currentStage = bossStage,
            winner = 1
        )

        val result = recorder.recordOutcome(state, aiDifficulty = "HARD")

        assertTrue(result.starsEarned > 0)
        assertEquals(bossTheme, result.newlyConqueredTheme)
    }

    @Test
    fun recordOutcome_multiplayerSkipsStageConquest() = runBlocking {
        val fakeLeaderboardDao = FakeLeaderboardDao()
        val fakeStageDao = FakeStageProgressDao()
        val leaderboardRepo = LeaderboardRepository(fakeLeaderboardDao)
        val stageRepo = StageRepository(fakeStageDao)
        val playGamesPort = RecordingPlayGamesPort()

        val recorder = MatchOutcomeRecorder(
            leaderboardRepository = leaderboardRepo,
            stageRepository = stageRepo,
            playGamesPort = playGamesPort,
            eloLeaderboardId = "elo_board"
        )

        val state = MatchState(
            mode = GameMode.EXTENDED_HOST,
            status = MatchStatus.GAME_OVER,
            scorePlayer1 = 7,
            scorePlayer2 = 5,
            targetScore = 7,
            remainingTimeSeconds = 120,
            player1Name = "PlayerA",
            player2Name = "PlayerB",
            winner = 1
        )

        val result = recorder.recordOutcome(state, aiDifficulty = null)

        assertNotNull(result.matchRecord)
        assertEquals(0, result.starsEarned)
        assertNull(result.newlyConqueredTheme)
        assertNull(result.totalStars)

        // Only ELO leaderboard submitted, stage leaderboard is skipped
        assertEquals(1, playGamesPort.submissions.size)
        assertEquals("elo_board", playGamesPort.submissions[0].first)
    }

    @Test
    fun recordOutcome_spectatorAiVsAiSkipsStatsAndPlayGames() = runBlocking {
        val fakeLeaderboardDao = FakeLeaderboardDao()
        val fakeStageDao = FakeStageProgressDao()
        val leaderboardRepo = LeaderboardRepository(fakeLeaderboardDao)
        val stageRepo = StageRepository(fakeStageDao)
        val playGamesPort = RecordingPlayGamesPort()

        val recorder = MatchOutcomeRecorder(
            leaderboardRepository = leaderboardRepo,
            stageRepository = stageRepo,
            playGamesPort = playGamesPort,
            eloLeaderboardId = "elo_board",
            stageLeaderboardId = "stage_board"
        )

        val state = MatchState(
            mode = GameMode.AI_VS_AI_LOCAL,
            status = MatchStatus.GAME_OVER,
            scorePlayer1 = 7,
            scorePlayer2 = 2,
            targetScore = 7,
            remainingTimeSeconds = 60,
            player1Name = "Bot 1",
            player2Name = "Bot 2",
            winner = 1
        )

        val result = recorder.recordOutcome(state, aiDifficulty = null)

        assertNull(result.matchRecord)
        assertNull(result.eloRating)
        assertEquals(0, result.starsEarned)
        assertTrue(fakeLeaderboardDao.matches.isEmpty())
        assertTrue(playGamesPort.submissions.isEmpty())
    }

    @Test
    fun recordOutcome_guestUsesPlayer2NameAndDeltaBForElo() = runBlocking {
        val fakeLeaderboardDao = FakeLeaderboardDao()
        val fakeStageDao = FakeStageProgressDao()
        val leaderboardRepo = LeaderboardRepository(fakeLeaderboardDao)
        val stageRepo = StageRepository(fakeStageDao)
        val playGamesPort = RecordingPlayGamesPort()

        val recorder = MatchOutcomeRecorder(
            leaderboardRepository = leaderboardRepo,
            stageRepository = stageRepo,
            playGamesPort = playGamesPort,
            eloLeaderboardId = "elo_board"
        )

        val state = MatchState(
            mode = GameMode.CONNECTED_GUEST,
            status = MatchStatus.GAME_OVER,
            scorePlayer1 = 3,
            scorePlayer2 = 5,
            targetScore = 5,
            remainingTimeSeconds = 80,
            isHost = false,
            player1Name = "HostPlayer",
            player2Name = "GuestPlayer",
            localPlayerName = "GuestPlayer",
            winner = 2
        )

        val result = recorder.recordOutcome(state, aiDifficulty = null)

        assertNotNull(result.matchRecord)
        assertNotNull(result.eloRating)
        assertTrue("Vitória do convidado deve elevar seu Elo acima de 1000", result.eloRating!! > 1000)
        assertEquals(1, playGamesPort.submissions.size)
        assertEquals("elo_board", playGamesPort.submissions[0].first)
        assertEquals(result.eloRating!!.toLong(), playGamesPort.submissions[0].second)
    }
}
