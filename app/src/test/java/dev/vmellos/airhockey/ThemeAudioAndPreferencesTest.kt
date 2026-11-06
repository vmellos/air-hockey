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
        assertEquals("CameraZoom3D padrão deve ser STANDARD", dev.vmellos.airhockey.core.preferences.CameraZoom3D.STANDARD, prefs.cameraZoom3d)
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

    @Test
    fun testArenaThemeCatalogProvidesVisualsForAllThemes() {
        for (theme in TableTheme.entries) {
            val visuals = dev.vmellos.airhockey.feature.customization.ArenaThemeCatalog.getVisuals(theme)
            org.junit.Assert.assertNotNull(visuals.backgroundColor)
            org.junit.Assert.assertNotNull(visuals.primaryColor)
            org.junit.Assert.assertNotNull(visuals.secondaryColor)
        }
    }

    @Test
    fun testArenaThemeCatalogProvidesMalletSkinVisuals() {
        for (skin in dev.vmellos.airhockey.core.preferences.MalletSkin.entries) {
            val p1 = dev.vmellos.airhockey.feature.customization.ArenaThemeCatalog.getMalletSkinVisuals(skin, isPlayer1 = true)
            val p2 = dev.vmellos.airhockey.feature.customization.ArenaThemeCatalog.getMalletSkinVisuals(skin, isPlayer1 = false)
            org.junit.Assert.assertNotNull(p1.color)
            org.junit.Assert.assertNotNull(p2.color)
        }
    }

    @Test
    fun testArenaThemeCatalogProvidesPuckAccents() {
        for (puck in dev.vmellos.airhockey.core.preferences.PuckSkin.entries) {
            val accent = dev.vmellos.airhockey.feature.customization.ArenaThemeCatalog.getPuckAccent(puck)
            org.junit.Assert.assertNotNull(accent)
        }
    }

    @Test
    fun testTheme3DAssetResolverResolvesExistingAssetsForAllThemes() {
        val assetsDir = File("src/main/assets")
        assertTrue("Diretório assets deve existir", assetsDir.exists())

        for (theme in TableTheme.entries) {
            val tableModel = dev.vmellos.airhockey.feature.game.ui.Theme3DAssetResolver.getTable3DAsset(theme)
            assertTrue("Arquivo da mesa 3D deve existir: $tableModel", File(assetsDir, tableModel).exists())

            assertTrue("Todos os temas devem possuir suporte 3D nativo",
                dev.vmellos.airhockey.feature.game.ui.Theme3DAssetResolver.hasNative3DSupport(theme))
        }

        for (puck in dev.vmellos.airhockey.core.preferences.PuckSkin.entries) {
            val puckModel = dev.vmellos.airhockey.feature.game.ui.Theme3DAssetResolver.getPuck3DAsset(puck, TableTheme.CLASSIC_NEON)
            assertTrue("Arquivo do disco 3D deve existir: $puckModel", File(assetsDir, puckModel).exists())
        }

        for (mallet in dev.vmellos.airhockey.core.preferences.MalletSkin.entries) {
            val m1 = dev.vmellos.airhockey.feature.game.ui.Theme3DAssetResolver.getMallet3DAsset(mallet, isPlayer1 = true, TableTheme.CLASSIC_NEON)
            val m2 = dev.vmellos.airhockey.feature.game.ui.Theme3DAssetResolver.getMallet3DAsset(mallet, isPlayer1 = false, TableTheme.CLASSIC_NEON)
            assertTrue("Arquivo do rebatedor p1 deve existir: $m1", File(assetsDir, m1).exists())
            assertTrue("Arquivo do rebatedor p2 deve existir: $m2", File(assetsDir, m2).exists())
        }
    }

    @Test
    fun testNeonBloomIntensityAndArenaDimensionPreferences() {
        val prefs = UserPreferences()
        assertEquals("ArenaDimension padrão deve ser THREE_DIMENSIONAL",
            dev.vmellos.airhockey.core.preferences.ArenaDimension.THREE_DIMENSIONAL, prefs.arenaDimension)
        assertEquals("Intensidade padrão do bloom de neon deve ser 0.05f (50%)", 0.05f, prefs.neonBloomIntensity, 0.0001f)
        assertTrue("Animação inicial 3D deve vir ativada por padrão", prefs.arena3dIntroEnabled)

        val updatedPrefs = prefs.copy(
            arenaDimension = dev.vmellos.airhockey.core.preferences.ArenaDimension.TWO_DIMENSIONAL,
            neonBloomIntensity = 0.08f,
            arena3dIntroEnabled = false
        )
        assertEquals(dev.vmellos.airhockey.core.preferences.ArenaDimension.TWO_DIMENSIONAL, updatedPrefs.arenaDimension)
        assertEquals(0.08f, updatedPrefs.neonBloomIntensity, 0.0001f)
        org.junit.Assert.assertFalse(updatedPrefs.arena3dIntroEnabled)
    }
}
