package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.audio.ProceduralAudioSynthesizer
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream

class ThemeAudioAndPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDefaultTargetScoreIsThree() {
        val prefs = UserPreferences()
        assertEquals("Target score padrão deve ser 3", 3, prefs.targetScore)
    }

    @Test
    fun testProceduralAudioSynthesizerGeneratesValidWavForAllThemes() {
        val testDir = tempFolder.newFolder("audio_test")

        // Valida que todos os 11 temas possuem síntese funcional de SFX e BGM
        for (theme in TableTheme.values()) {
            val paddleWav = File(testDir, "sfx_${theme.name}_paddle.wav")
            val wallWav = File(testDir, "sfx_${theme.name}_wall.wav")
            val goalScoredWav = File(testDir, "sfx_${theme.name}_goal_scored.wav")
            val goalConcededWav = File(testDir, "sfx_${theme.name}_goal_conceded.wav")
            val countdownWav = File(testDir, "sfx_${theme.name}_countdown.wav")
            val scoreWav = File(testDir, "sfx_${theme.name}_score.wav")

            ProceduralAudioSynthesizer.generatePaddleHitWav(paddleWav, theme)
            ProceduralAudioSynthesizer.generateWallHitWav(wallWav, theme)
            ProceduralAudioSynthesizer.generateGoalScoredWav(goalScoredWav, theme)
            ProceduralAudioSynthesizer.generateGoalConcededWav(goalConcededWav, theme)
            ProceduralAudioSynthesizer.generateCountdownWav(countdownWav, theme, 3)
            ProceduralAudioSynthesizer.generateScoreBlipWav(scoreWav, theme)

            assertValidWavHeader(paddleWav)
            assertValidWavHeader(wallWav)
            assertValidWavHeader(goalScoredWav)
            assertValidWavHeader(goalConcededWav)
            assertValidWavHeader(countdownWav)
            assertValidWavHeader(scoreWav)
        }
    }

    @Test
    fun testBgmLoopGenerationForSampleThemes() {
        val testDir = tempFolder.newFolder("bgm_test")
        val classicBgm = File(testDir, "bgm_classic.wav")
        val pongBgm = File(testDir, "bgm_pong.wav")

        ProceduralAudioSynthesizer.generateBgmWav(classicBgm, TableTheme.CLASSIC_NEON)
        ProceduralAudioSynthesizer.generateBgmWav(pongBgm, TableTheme.CYBER_PONG)

        assertValidWavHeader(classicBgm)
        assertValidWavHeader(pongBgm)
        assertTrue("BGM clássico deve conter dados substanciais de áudio", classicBgm.length() > 50000)
        assertTrue("BGM pong deve conter dados substanciais de áudio", pongBgm.length() > 50000)
    }

    private fun assertValidWavHeader(file: File) {
        assertTrue("Arquivo deve existir: ${file.name}", file.exists())
        assertTrue("Arquivo deve ter pelo menos 44 bytes de cabeçalho WAV: ${file.name}", file.length() >= 44)

        val header = ByteArray(44)
        FileInputStream(file).use { it.read(header) }

        val riff = String(header, 0, 4, Charsets.US_ASCII)
        val wave = String(header, 8, 4, Charsets.US_ASCII)
        val fmt = String(header, 12, 4, Charsets.US_ASCII)

        assertEquals("Cabeçalho RIFF esperado", "RIFF", riff)
        assertEquals("Cabeçalho WAVE esperado", "WAVE", wave)
        assertEquals("Sub-bloco fmt esperado", "fmt ", fmt)
    }
}
