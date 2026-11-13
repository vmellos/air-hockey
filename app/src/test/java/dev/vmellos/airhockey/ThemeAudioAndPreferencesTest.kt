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
    fun testArenaKitCatalogProvidesVisualsForAllThemes() {
        for (theme in TableTheme.entries) {
            val visuals = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog.getKit(theme).visuals
            org.junit.Assert.assertNotNull(visuals.backgroundColor)
            org.junit.Assert.assertNotNull(visuals.primaryColor)
            org.junit.Assert.assertNotNull(visuals.secondaryColor)
        }
    }

    @Test
    fun testArenaKitCatalogProvidesMalletSkinVisuals() {
        for (skin in dev.vmellos.airhockey.core.preferences.MalletSkin.entries) {
            val spec = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog.getMalletSpec(skin)
            val p1 = spec.visuals(isPlayer1 = true)
            val p2 = spec.visuals(isPlayer1 = false)
            org.junit.Assert.assertNotNull(p1.color)
            org.junit.Assert.assertNotNull(p2.color)
        }
    }

    @Test
    fun testArenaKitCatalogProvidesPuckAccents() {
        for (puck in dev.vmellos.airhockey.core.preferences.PuckSkin.entries) {
            val accent = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog.getPuckSpec(puck).accentColor
            org.junit.Assert.assertNotNull(accent)
        }
    }

    @Test
    fun testArenaKitCatalogResolvesExistingAssetsForAllThemes() {
        val assetsDir = File("src/main/assets")
        assertTrue("Diretório assets deve existir", assetsDir.exists())

        for (theme in TableTheme.entries) {
            val kit = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog.getKit(theme)
            val tableModel = kit.tableFullAsset
            assertTrue("Arquivo da mesa 3D deve existir: $tableModel", File(assetsDir, tableModel).exists())

            val halfTableP1 = kit.tableAsset(isExtended = true, isPlayer1 = true)
            val halfTableP2 = kit.tableAsset(isExtended = true, isPlayer1 = false)
            assertTrue("Arquivo da meia-mesa P1 deve existir: $halfTableP1", File(assetsDir, halfTableP1).exists())
            assertTrue("Arquivo da meia-mesa P2 deve existir: $halfTableP2", File(assetsDir, halfTableP2).exists())
        }

        for (puck in dev.vmellos.airhockey.core.preferences.PuckSkin.entries) {
            val puckModel = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog.getPuckSpec(puck).model3dAsset
            assertTrue("Arquivo do disco 3D deve existir: $puckModel", File(assetsDir, puckModel).exists())
        }

        for (mallet in dev.vmellos.airhockey.core.preferences.MalletSkin.entries) {
            val spec = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog.getMalletSpec(mallet)
            val m1 = spec.model3dAsset(isPlayer1 = true)
            val m2 = spec.model3dAsset(isPlayer1 = false)
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

    @Test
    fun testMalletAndPuckSkinsDecoupledFromTableTheme() {
        // Garantir que skins equipadas não sejam sobrescritas pelo tema da arena (decisão Q2)
        val classicP1 = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog
            .getMalletSpec(dev.vmellos.airhockey.core.preferences.MalletSkin.CLASSIC_NEON)
            .model3dAsset(isPlayer1 = true)
        val classicP2 = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog
            .getMalletSpec(dev.vmellos.airhockey.core.preferences.MalletSkin.CLASSIC_NEON)
            .model3dAsset(isPlayer1 = false)
        assertEquals("models/rebatedor_p1.glb", classicP1)
        assertEquals("models/rebatedor_p2.glb", classicP2)

        val classicPuckAsset = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog
            .getPuckSpec(dev.vmellos.airhockey.core.preferences.PuckSkin.CLASSIC_PLASMA)
            .model3dAsset
        assertEquals("models/disco_cyber.glb", classicPuckAsset)

        val soccerMallet = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog
            .getMalletSpec(dev.vmellos.airhockey.core.preferences.MalletSkin.MAGNETIC_STRIKER)
            .model3dAsset(isPlayer1 = true)
        assertEquals("models/rebatedor_cyber_soccer_p1.glb", soccerMallet)
    }

    @Test
    fun testArenaKitCatalogProvidesThemeVisualsForAllThemes() {
        for (theme in TableTheme.entries) {
            val kit = dev.vmellos.airhockey.feature.game.ui.ArenaKitCatalog.getKit(theme)
            val visuals = kit.visuals
            org.junit.Assert.assertNotNull("Visuals para o tema $theme não podem ser nulos", visuals)
            org.junit.Assert.assertNotNull(visuals.primaryColor)
            org.junit.Assert.assertNotNull(visuals.secondaryColor)
            org.junit.Assert.assertNotNull(visuals.wallHitColor)
            org.junit.Assert.assertNotNull(kit.overlayStyle)
        }
        assertEquals(
            TableTheme.CLASSIC_NEON,
            with(dev.vmellos.airhockey.core.catalog.ArenaPresetCatalog) {
                dev.vmellos.airhockey.core.catalog.ArenaPresetCatalog.getPresetForTheme(TableTheme.ARCADE_3D).theme
            }
        )
    }

    @Test
    fun testSmartScrollTargetCalculation() {
        // Simula fases até o nível 50 desbloqueadas
        val stages = (1..100).map { stageNum ->
            dev.vmellos.airhockey.core.database.StageProgressEntity(
                stageNumber = stageNum,
                isUnlocked = stageNum <= 50,
                starsEarned = if (stageNum < 50) 3 else 0
            )
        }
        val highestUnlocked = stages.filter { it.isUnlocked }.maxOfOrNull { it.stageNumber } ?: 1
        val targetIndex = (highestUnlocked - 1).coerceAtLeast(0)
        assertEquals(50, highestUnlocked)
        assertEquals("O índice de rolagem do LazyGrid deve ser 49 para a fase 50", 49, targetIndex)
    }

    @Test
    fun testGameAudioEnginePreloadsWavsOffCallerThreadAndPlaysNonBlocking() = kotlinx.coroutines.runBlocking {
        val cacheDir = tempFolder.newFolder("audio_engine_test")
        var nextSampleId = 1
        val playedSamples = mutableListOf<Int>()
        val startedBgms = mutableListOf<File>()
        var isBgmPlaying = false

        val fakePort = object : dev.vmellos.airhockey.core.audio.AudioPlaybackPort {
            override fun loadSample(file: File): Int = nextSampleId++
            override fun playSample(sampleId: Int, volume: Float, priority: Int, rate: Float) {
                playedSamples.add(sampleId)
            }
            override fun startLoopingBgm(file: File, volume: Float) {
                startedBgms.add(file)
                isBgmPlaying = true
            }
            override fun setLoopingBgmVolume(volume: Float) {}
            override fun hasLoadedBgm(): Boolean = startedBgms.isNotEmpty()
            override fun isLoopingBgmPlaying(): Boolean = isBgmPlaying
            override fun resumeLoopingBgm(volume: Float) {
                isBgmPlaying = true
            }
            override fun pauseLoopingBgm() {
                isBgmPlaying = false
            }
            override fun stopLoopingBgm() {
                isBgmPlaying = false
            }
            override fun pauseAllSamples() {}
            override fun resumeAllSamples() {}
            override fun vibrate(durationMs: Long, amplitude: Int) {}
            override fun release() {}
        }

        val engine = dev.vmellos.airhockey.core.audio.GameAudioEngine(
            cacheDir = cacheDir,
            playbackPort = fakePort,
            ioDispatcher = kotlinx.coroutines.Dispatchers.IO,
            preloadDefaultSynchronously = false
        )
        engine.isHapticsEnabled = false

        // Chamada imediata não bloqueia sintetizando 9 WAVs síncronos
        engine.currentTheme = TableTheme.TOKAMAK_REACTOR
        engine.playPaddleHit(intensity = 0.8f, isPlayer1 = true)

        // Aguarda carga assíncrona em Dispatchers.IO completar
        engine.preloadThemeAsync(TableTheme.CLASSIC_NEON).join()
        engine.preloadThemeAsync(TableTheme.TOKAMAK_REACTOR).join()

        assertTrue(engine.isThemeLoaded(TableTheme.CLASSIC_NEON))
        assertTrue(engine.isThemeLoaded(TableTheme.TOKAMAK_REACTOR))

        engine.playPaddleHit(intensity = 0.9f, isPlayer1 = true)
        assertTrue("Após preload em segundo plano, efeito deve tocar imediatamente", playedSamples.isNotEmpty())

        engine.playBgm(TableTheme.TOKAMAK_REACTOR)
        var waitedMs = 0L
        while (startedBgms.isEmpty() && waitedMs < 3000L) {
            kotlinx.coroutines.delay(25)
            waitedMs += 25
        }
        assertEquals(1, startedBgms.size)
        assertTrue(startedBgms.first().name.contains("tokamak_reactor"))

        engine.release()
    }
}
