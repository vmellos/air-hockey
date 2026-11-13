package dev.vmellos.airhockey.feature.game

import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.core.preferences.ArenaDimension
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.PuckSpeedPreset
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchSpecTest {

    @Test
    fun forStage_configuresSoloConquestMatch() {
        val prefs = UserPreferences(
            playerName = "PlayerAlpha",
            bezelGapMm = 10f,
            arenaDimension = ArenaDimension.THREE_DIMENSIONAL,
            arena3dIntroEnabled = true
        )

        val spec = MatchSpec.forStage(
            stageNumber = 5,
            userPrefs = prefs,
            player1Name = "PlayerAlpha",
            player2Name = "Robô IA"
        )

        assertEquals(GameMode.SOLO, spec.mode)
        assertEquals(5, spec.stageNumber)
        assertEquals("PlayerAlpha", spec.player1Name)
        assertEquals("Robô IA", spec.player2Name)
        assertEquals(10f, spec.bezelGapMm, 1e-4f)
        assertTrue("3D match with intro enabled should have intro presentation", spec.hasIntroPresentation)
        assertEquals(ArenaDimension.THREE_DIMENSIONAL, spec.arenaDimension)
    }

    @Test
    fun forStage_with2DDebouncing_disablesIntroPresentation() {
        val prefs = UserPreferences(
            playerName = "PlayerAlpha",
            arenaDimension = ArenaDimension.TWO_DIMENSIONAL,
            arena3dIntroEnabled = true
        )

        val spec = MatchSpec.forStage(
            stageNumber = 1,
            userPrefs = prefs,
            player1Name = "PlayerAlpha",
            player2Name = "Robô IA"
        )

        assertFalse("2D match should not have 3D intro presentation", spec.hasIntroPresentation)
        assertEquals(ArenaDimension.TWO_DIMENSIONAL, spec.arenaDimension)
    }

    @Test
    fun forSplitScreen_configuresSymmetricLocalMatch() {
        val prefs = UserPreferences(
            targetScore = 7,
            matchTimeSeconds = 240,
            bezelGapMm = 12f,
            arena3dIntroEnabled = false
        )

        val spec = MatchSpec.forSplitScreen(
            userPrefs = prefs,
            chosenTheme = TableTheme.CYBER_SOCCER,
            chosenDimension = ArenaDimension.THREE_DIMENSIONAL,
            player1Name = "Lado 1",
            player2Name = "Lado 2"
        )

        assertEquals(GameMode.SPLIT_SCREEN, spec.mode)
        assertEquals(7, spec.targetScore)
        assertEquals(240, spec.matchTimeSeconds)
        assertEquals(12f, spec.bezelGapMm, 1e-4f)
        assertEquals(TableTheme.CYBER_SOCCER, spec.tableTheme)
        assertEquals(PuckSpeedPreset.NORMAL.maxSpeed, spec.puckMaxSpeed, 1e-4f)
        assertEquals(PuckSpeedPreset.NORMAL.airFriction, spec.airFriction, 1e-4f)
        assertFalse("Intro was disabled in userPrefs", spec.hasIntroPresentation)
    }

    @Test
    fun forAiVsAiLocal_calibratesSpeedsAccordingToDifficulty() {
        val prefs = UserPreferences(targetScore = 5)

        val specHard = MatchSpec.forAiVsAiLocal(
            userPrefs = prefs,
            diff1 = AiDifficulty.HARD,
            diff2 = AiDifficulty.EASY,
            chosenTheme = TableTheme.RETRO_SYNTHWAVE,
            chosenDimension = ArenaDimension.THREE_DIMENSIONAL,
            player1Name = "Bot Master",
            player2Name = "Bot Novice"
        )

        assertEquals(GameMode.AI_VS_AI_LOCAL, specHard.mode)
        assertEquals(AiDifficulty.HARD, specHard.aiDifficultyPlayer1)
        assertEquals(AiDifficulty.EASY, specHard.aiDifficultyPlayer2)
        assertEquals(PuckSpeedPreset.FAST.maxSpeed, specHard.puckMaxSpeed, 1e-4f)
        assertEquals(TableTheme.RETRO_SYNTHWAVE, specHard.tableTheme)

        val specEasy = MatchSpec.forAiVsAiLocal(
            userPrefs = prefs,
            diff1 = AiDifficulty.EASY,
            diff2 = AiDifficulty.MEDIUM,
            chosenTheme = TableTheme.NEO_BASKETBALL,
            chosenDimension = ArenaDimension.TWO_DIMENSIONAL,
            player1Name = "Bot 1",
            player2Name = "Bot 2"
        )
        assertEquals(PuckSpeedPreset.SLOW.maxSpeed, specEasy.puckMaxSpeed, 1e-4f)
    }

    @Test
    fun forMultiplayer_propagatesAllNegotiatedSettings() {
        val prefs = UserPreferences(targetScore = 10)

        val spec = MatchSpec.forMultiplayer(
            gameMode = GameMode.EXTENDED_HOST,
            userPrefs = prefs,
            bezelGapMm = 14.5f,
            targetScore = 10,
            matchTimeSeconds = 0,
            aiDifficultyPlayer1 = AiDifficulty.MEDIUM,
            aiDifficultyPlayer2 = AiDifficulty.HARD,
            puckMaxSpeed = 7200f,
            airFriction = 0.35f,
            player1Name = "HostPhone",
            player2Name = "GuestTablet",
            tableTheme = TableTheme.TOKAMAK_REACTOR,
            arenaDimension = ArenaDimension.THREE_DIMENSIONAL,
            hasIntroPresentation = false
        )

        assertEquals(GameMode.EXTENDED_HOST, spec.mode)
        assertEquals(14.5f, spec.bezelGapMm, 1e-4f)
        assertEquals(10, spec.targetScore)
        assertEquals(0, spec.matchTimeSeconds)
        assertEquals(7200f, spec.puckMaxSpeed, 1e-4f)
        assertEquals(0.35f, spec.airFriction, 1e-4f)
        assertEquals("HostPhone", spec.player1Name)
        assertEquals("GuestTablet", spec.player2Name)
        assertEquals(TableTheme.TOKAMAK_REACTOR, spec.tableTheme)
        assertFalse(spec.hasIntroPresentation)
    }

    @Test
    fun forMultiplayer_preservesLocalPuckAndMalletSkinsUnderDifferentHostTheme() {
        val guestPrefs = UserPreferences(
            tableTheme = TableTheme.CLASSIC_NEON,
            puckSkin = PuckSkin.KIARA_PAW,
            player1MalletSkin = MalletSkin.NEKO_STRIKER
        )

        val spec = MatchSpec.forMultiplayer(
            gameMode = GameMode.CONNECTED_GUEST,
            userPrefs = guestPrefs,
            bezelGapMm = 0f,
            targetScore = 5,
            matchTimeSeconds = 180,
            aiDifficultyPlayer1 = AiDifficulty.MEDIUM,
            aiDifficultyPlayer2 = AiDifficulty.MEDIUM,
            puckMaxSpeed = 6800f,
            airFriction = 0.40f,
            player1Name = "Host",
            player2Name = "Guest",
            tableTheme = TableTheme.TOKAMAK_REACTOR,
            arenaDimension = ArenaDimension.THREE_DIMENSIONAL,
            hasIntroPresentation = false
        )

        assertEquals(TableTheme.TOKAMAK_REACTOR, spec.tableTheme)
        assertEquals(PuckSkin.KIARA_PAW, spec.localPuckSkin)
        assertEquals(MalletSkin.NEKO_STRIKER, spec.localMalletSkin)
    }
}
