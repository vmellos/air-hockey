package dev.vmellos.airhockey.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.TableTheme
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Motor de áudio do jogo com síntese procedural temática adaptativa.
 * Fornece BGM, contagem regressiva, impactos e fanfarras de gols exclusivos por arena.
 */
class GameAudioEngine(private val context: Context) {

    private val soundPool: SoundPool
    private val vibrator: Vibrator?

    private val loadedSoundIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())

    // Controles de Efeitos Sonoros
    var isSoundEnabled: Boolean = true
    var volume: Float = 0.8f
    var isHapticsEnabled: Boolean = true

    // Controles de Tema e Skins
    var currentTheme: TableTheme = TableTheme.CLASSIC_NEON
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                ensureThemeLoaded(value)
                if (bgmPlayer != null && isMusicEnabled) {
                    playBgm(value)
                }
            }
        }

    var currentMalletSkinP1: MalletSkin = MalletSkin.CLASSIC_NEON
    var currentMalletSkinP2: MalletSkin = MalletSkin.CLASSIC_NEON

    // Controles de Música de Fundo (BGM)
    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                pauseBgm()
            } else {
                resumeBgm()
            }
        }

    var musicVolume: Float = 0.10f
        set(value) {
            field = value.coerceIn(0f, 1f)
            try {
                bgmPlayer?.setVolume(field, field)
            } catch (_: Exception) {}
        }

    private var bgmPlayer: MediaPlayer? = null
    private var playingBgmTheme: TableTheme? = null

    // Cache de IDs de som por tema
    data class ThemeSoundSet(
        val hitPaddleId: Int,
        val hitWallId: Int,
        val goalScoredId: Int,
        val goalConcededId: Int,
        val countdown3Id: Int,
        val countdown2Id: Int,
        val countdown1Id: Int,
        val countdownGoId: Int,
        val scoreBlipId: Int
    )

    private val themeSounds = ConcurrentHashMap<TableTheme, ThemeSoundSet>()

    // Sons padrão (fallback instantâneo)
    private var defaultPaddleHitId: Int = 0
    private var defaultWallHitId: Int = 0
    private var defaultGoalScoredId: Int = 0
    private var defaultGoalConcededId: Int = 0
    private var defaultCountdown3Id: Int = 0
    private var defaultCountdown2Id: Int = 0
    private var defaultCountdown1Id: Int = 0
    private var defaultCountdownGoId: Int = 0
    private var defaultScoreBlipId: Int = 0

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(12)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds.add(sampleId)
            }
        }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        // Carrega tema clássico imediatamente para prover fallbacks
        ensureThemeLoaded(TableTheme.CLASSIC_NEON)
    }

    @Synchronized
    fun ensureThemeLoaded(theme: TableTheme): ThemeSoundSet {
        themeSounds[theme]?.let { return it }

        val cacheDir = context.cacheDir
        val prefix = "sfx_${theme.name.lowercase()}"

        val paddleWav = File(cacheDir, "${prefix}_paddle.wav")
        val wallWav = File(cacheDir, "${prefix}_wall.wav")
        val goalScoredWav = File(cacheDir, "${prefix}_goal_scored.wav")
        val goalConcededWav = File(cacheDir, "${prefix}_goal_conceded.wav")
        val c3Wav = File(cacheDir, "${prefix}_c3.wav")
        val c2Wav = File(cacheDir, "${prefix}_c2.wav")
        val c1Wav = File(cacheDir, "${prefix}_c1.wav")
        val cGoWav = File(cacheDir, "${prefix}_c_go.wav")
        val scoreWav = File(cacheDir, "${prefix}_score.wav")

        try {
            if (!paddleWav.exists() || paddleWav.length() == 0L) ProceduralAudioSynthesizer.generatePaddleHitWav(paddleWav, theme)
            if (!wallWav.exists() || wallWav.length() == 0L) ProceduralAudioSynthesizer.generateWallHitWav(wallWav, theme)
            if (!goalScoredWav.exists() || goalScoredWav.length() == 0L) ProceduralAudioSynthesizer.generateGoalScoredWav(goalScoredWav, theme)
            if (!goalConcededWav.exists() || goalConcededWav.length() == 0L) ProceduralAudioSynthesizer.generateGoalConcededWav(goalConcededWav, theme)
            if (!c3Wav.exists() || c3Wav.length() == 0L) ProceduralAudioSynthesizer.generateCountdownWav(c3Wav, theme, 3)
            if (!c2Wav.exists() || c2Wav.length() == 0L) ProceduralAudioSynthesizer.generateCountdownWav(c2Wav, theme, 2)
            if (!c1Wav.exists() || c1Wav.length() == 0L) ProceduralAudioSynthesizer.generateCountdownWav(c1Wav, theme, 1)
            if (!cGoWav.exists() || cGoWav.length() == 0L) ProceduralAudioSynthesizer.generateCountdownWav(cGoWav, theme, 0)
            if (!scoreWav.exists() || scoreWav.length() == 0L) ProceduralAudioSynthesizer.generateScoreBlipWav(scoreWav, theme)

            val pId = soundPool.load(paddleWav.absolutePath, 1)
            val wId = soundPool.load(wallWav.absolutePath, 1)
            val gsId = soundPool.load(goalScoredWav.absolutePath, 1)
            val gcId = soundPool.load(goalConcededWav.absolutePath, 1)
            val c3Id = soundPool.load(c3Wav.absolutePath, 1)
            val c2Id = soundPool.load(c2Wav.absolutePath, 1)
            val c1Id = soundPool.load(c1Wav.absolutePath, 1)
            val cGoId = soundPool.load(cGoWav.absolutePath, 1)
            val sId = soundPool.load(scoreWav.absolutePath, 1)

            val soundSet = ThemeSoundSet(
                hitPaddleId = pId,
                hitWallId = wId,
                goalScoredId = gsId,
                goalConcededId = gcId,
                countdown3Id = c3Id,
                countdown2Id = c2Id,
                countdown1Id = c1Id,
                countdownGoId = cGoId,
                scoreBlipId = sId
            )

            if (defaultPaddleHitId == 0) {
                defaultPaddleHitId = pId
                defaultWallHitId = wId
                defaultGoalScoredId = gsId
                defaultGoalConcededId = gcId
                defaultCountdown3Id = c3Id
                defaultCountdown2Id = c2Id
                defaultCountdown1Id = c1Id
                defaultCountdownGoId = cGoId
                defaultScoreBlipId = sId
            }

            themeSounds[theme] = soundSet
            return soundSet
        } catch (_: Exception) {
            val fallback = ThemeSoundSet(
                defaultPaddleHitId, defaultWallHitId, defaultGoalScoredId, defaultGoalConcededId,
                defaultCountdown3Id, defaultCountdown2Id, defaultCountdown1Id, defaultCountdownGoId, defaultScoreBlipId
            )
            themeSounds[theme] = fallback
            return fallback
        }
    }

    private fun calculateEffectiveVolume(baseMultiplier: Float = 1.0f): Float {
        val normalized = volume.coerceIn(0f, 1f)
        return (normalized * normalized * baseMultiplier).coerceIn(0f, 1f)
    }

    fun playPaddleHit(intensity: Float = 1.0f, isPlayer1: Boolean = true) {
        if (isSoundEnabled) {
            val currentSet = themeSounds[currentTheme] ?: ensureThemeLoaded(currentTheme)
            val soundId = if (currentSet.hitPaddleId != 0) currentSet.hitPaddleId else defaultPaddleHitId
            if (soundId != 0) {
                val effVol = calculateEffectiveVolume(intensity.coerceIn(0.25f, 1.0f))
                val rate = if (isPlayer1) 1.0f else 0.92f
                soundPool.play(soundId, effVol, effVol, 1, 0, rate)
            }
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = 15, amplitude = (intensity * 255).toInt().coerceIn(60, 255))
        }
    }

    fun playWallHit() {
        if (isSoundEnabled) {
            val currentSet = themeSounds[currentTheme] ?: ensureThemeLoaded(currentTheme)
            val soundId = if (currentSet.hitWallId != 0) currentSet.hitWallId else defaultWallHitId
            if (soundId != 0) {
                val effVol = calculateEffectiveVolume(0.7f)
                soundPool.play(soundId, effVol, effVol, 1, 0, 1.0f)
            }
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = 10, amplitude = 50)
        }
    }

    /**
     * Ponto marcado pelo jogador (Ponto Pró).
     * Toca fanfarra triunfante exclusiva do tema + progressão harmônica proporcional aos pontos.
     */
    fun playGoalScored(currentScore: Int = 0, targetScore: Int = 3) {
        if (isSoundEnabled) {
            val currentSet = themeSounds[currentTheme] ?: ensureThemeLoaded(currentTheme)
            val soundId = if (currentSet.goalScoredId != 0) currentSet.goalScoredId else defaultGoalScoredId
            if (soundId != 0) {
                val effVol = calculateEffectiveVolume(1.0f)
                soundPool.play(soundId, effVol, effVol, 2, 0, 1.0f)
            }
            if (currentScore > 0) {
                playScoreAnnouncement(currentScore, targetScore)
            }
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = 250, amplitude = 255)
        }
    }

    /**
     * Ponto sofrido pelo jogador (Ponto Contra / Oponente marcou).
     * Toca alarme / som descendente exclusivo do tema.
     */
    fun playGoalConceded(currentScore: Int = 0, targetScore: Int = 3) {
        if (isSoundEnabled) {
            val currentSet = themeSounds[currentTheme] ?: ensureThemeLoaded(currentTheme)
            val soundId = if (currentSet.goalConcededId != 0) currentSet.goalConcededId else defaultGoalConcededId
            if (soundId != 0) {
                val effVol = calculateEffectiveVolume(0.95f)
                soundPool.play(soundId, effVol, effVol, 2, 0, 1.0f)
            }
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = 180, amplitude = 180)
        }
    }

    /**
     * Método de compatibilidade legado.
     */
    fun playGoal() {
        playGoalScored()
    }

    /**
     * Toca o chime com elevação harmônica proporcional à pontuação.
     */
    fun playScoreAnnouncement(currentScore: Int, targetScore: Int) {
        if (!isSoundEnabled) return
        val currentSet = themeSounds[currentTheme] ?: ensureThemeLoaded(currentTheme)
        val scoreId = if (currentSet.scoreBlipId != 0) currentSet.scoreBlipId else defaultScoreBlipId
        if (scoreId != 0) {
            val progress = (currentScore.coerceAtLeast(1) - 1).coerceAtMost(10)
            val rate = (1.0f + progress * 0.10f).coerceIn(0.5f, 2.0f)
            val effVol = calculateEffectiveVolume(0.80f)
            soundPool.play(scoreId, effVol, effVol, 1, 0, rate)
        }
    }

    fun playCountdownBeep(countdownNumber: Int) {
        if (!isSoundEnabled) return
        val currentSet = themeSounds[currentTheme] ?: ensureThemeLoaded(currentTheme)
        val soundId = when (countdownNumber) {
            3 -> currentSet.countdown3Id
            2 -> currentSet.countdown2Id
            1 -> currentSet.countdown1Id
            else -> currentSet.countdownGoId
        }
        val effSoundId = if (soundId != 0) soundId else when (countdownNumber) {
            3 -> defaultCountdown3Id
            2 -> defaultCountdown2Id
            1 -> defaultCountdown1Id
            else -> defaultCountdownGoId
        }
        if (effSoundId != 0) {
            val effVol = calculateEffectiveVolume(0.85f)
            soundPool.play(effSoundId, effVol, effVol, 1, 0, 1.0f)
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = if (countdownNumber <= 0) 50 else 25, amplitude = 180)
        }
    }

    fun playCountdownBeep(isFinal: Boolean = false) {
        playCountdownBeep(if (isFinal) 0 else 3)
    }

    // --- Reprodução e Controle da Música de Fundo (BGM) Temática ---

    fun playBgm(theme: TableTheme = currentTheme) {
        if (!isMusicEnabled) return
        try {
            if (bgmPlayer != null && playingBgmTheme == theme) {
                val player = bgmPlayer ?: return
                val vol = musicVolume.coerceIn(0f, 1f)
                player.setVolume(vol, vol)
                if (!player.isPlaying) {
                    player.start()
                }
                return
            }

            stopBgm()
            playingBgmTheme = theme

            val bgmFileName = "bgm_${theme.name.lowercase()}_v1.wav"
            val file = File(context.cacheDir, bgmFileName)
            if (!file.exists() || file.length() == 0L) {
                ProceduralAudioSynthesizer.generateBgmWav(file, theme)
            }

            bgmPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(file.absolutePath)
                isLooping = true
                prepare()
                val vol = musicVolume.coerceIn(0f, 1f)
                setVolume(vol, vol)
                start()
            }
        } catch (_: Exception) {
            // Silently recover if audio subsystem fails
        }
    }

    fun pauseBgm() {
        try {
            if (bgmPlayer?.isPlaying == true) {
                bgmPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    fun resumeBgm() {
        if (!isMusicEnabled) return
        try {
            if (bgmPlayer != null && bgmPlayer?.isPlaying == false) {
                val vol = musicVolume.coerceIn(0f, 1f)
                bgmPlayer?.setVolume(vol, vol)
                bgmPlayer?.start()
            } else if (bgmPlayer == null) {
                playBgm(currentTheme)
            }
        } catch (_: Exception) {}
    }

    fun stopBgm() {
        try {
            bgmPlayer?.stop()
            bgmPlayer?.release()
            bgmPlayer = null
            playingBgmTheme = null
        } catch (_: Exception) {}
    }

    private fun triggerVibration(durationMs: Long, amplitude: Int) {
        vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    }

    fun stopAll() {
        soundPool.autoPause()
        pauseBgm()
        vibrator?.cancel()
    }

    fun resumeAudio() {
        soundPool.autoResume()
        if (isMusicEnabled) {
            resumeBgm()
        }
    }

    fun release() {
        stopAll()
        stopBgm()
        soundPool.release()
    }
}
