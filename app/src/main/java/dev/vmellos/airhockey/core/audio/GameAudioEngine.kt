package dev.vmellos.airhockey.core.audio

import android.content.Context
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.physics.AudioOutputPort
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Motor de áudio do jogo com síntese procedural temática adaptativa fora da thread principal.
 * Fornece BGM, contagem regressiva, impactos e fanfarras de gols exclusivos por arena.
 */
class GameAudioEngine(
    private val cacheDir: File,
    private val playbackPort: AudioPlaybackPort,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    preloadDefaultSynchronously: Boolean = false
) : AudioOutputPort {

    constructor(context: Context) : this(
        cacheDir = context.cacheDir,
        playbackPort = AndroidAudioPlaybackAdapter(context),
        ioDispatcher = Dispatchers.IO,
        preloadDefaultSynchronously = false
    )

    private val audioScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val themeLoadLock = Any()
    private val pendingThemeLoads = ConcurrentHashMap<TableTheme, Job>()

    @Volatile
    private var pendingBgmJob: Job? = null

    // Controles de Efeitos Sonoros
    @Volatile
    var isAppInForeground: Boolean = true
        set(value) {
            field = value
            if (!value) {
                stopAll()
            }
        }

    var isSoundEnabled: Boolean = true
    var volume: Float = 0.8f
    var isHapticsEnabled: Boolean = true

    // Controles de Tema e Skins
    var currentTheme: TableTheme = TableTheme.CLASSIC_NEON
        set(value) {
            val changed = field != value
            field = value
            if (changed) {
                preloadThemeAsync(value)
                if (isAppInForeground && playbackPort.isLoopingBgmPlaying() && isMusicEnabled) {
                    playBgm(value)
                }
            }
        }

    // Controles de Música de Fundo (BGM)
    var isMusicEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                stopBgm()
            }
        }

    var musicVolume: Float = 0.10f
        set(value) {
            field = value.coerceIn(0f, 1f)
            playbackPort.setLoopingBgmVolume(field)
        }

    @Volatile
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
    @Volatile private var defaultPaddleHitId: Int = 0
    @Volatile private var defaultWallHitId: Int = 0
    @Volatile private var defaultGoalScoredId: Int = 0
    @Volatile private var defaultGoalConcededId: Int = 0
    @Volatile private var defaultCountdown3Id: Int = 0
    @Volatile private var defaultCountdown2Id: Int = 0
    @Volatile private var defaultCountdown1Id: Int = 0
    @Volatile private var defaultCountdownGoId: Int = 0
    @Volatile private var defaultScoreBlipId: Int = 0

    init {
        if (preloadDefaultSynchronously) {
            ensureThemeLoaded(TableTheme.CLASSIC_NEON)
        } else {
            preloadThemeAsync(TableTheme.CLASSIC_NEON)
        }
    }

    /**
     * Agenda o pré-carregamento e síntese dos arquivos WAV do [theme] em background,
     * sem bloquear a thread chamadora.
     */
    fun preloadThemeAsync(theme: TableTheme): Job {
        if (themeSounds.containsKey(theme)) {
            return pendingThemeLoads[theme] ?: Job().apply { complete() }
        }
        val existing = pendingThemeLoads[theme]
        if (existing != null && existing.isActive) {
            return existing
        }
        val job = audioScope.launch {
            try {
                ensureThemeLoaded(theme)
            } finally {
                pendingThemeLoads.remove(theme)
            }
        }
        pendingThemeLoads[theme] = job
        return job
    }

    fun isThemeLoaded(theme: TableTheme): Boolean = themeSounds.containsKey(theme)

    fun ensureThemeLoaded(theme: TableTheme): ThemeSoundSet = synchronized(themeLoadLock) {
        themeSounds[theme]?.let { return it }

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

            val pId = playbackPort.loadSample(paddleWav)
            val wId = playbackPort.loadSample(wallWav)
            val gsId = playbackPort.loadSample(goalScoredWav)
            val gcId = playbackPort.loadSample(goalConcededWav)
            val c3Id = playbackPort.loadSample(c3Wav)
            val c2Id = playbackPort.loadSample(c2Wav)
            val c1Id = playbackPort.loadSample(c1Wav)
            val cGoId = playbackPort.loadSample(cGoWav)
            val sId = playbackPort.loadSample(scoreWav)

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

    private fun resolveSoundSetNonBlocking(): ThemeSoundSet? {
        val cached = themeSounds[currentTheme]
        if (cached != null) return cached
        preloadThemeAsync(currentTheme)
        return themeSounds[TableTheme.CLASSIC_NEON]
    }

    private fun calculateEffectiveVolume(baseMultiplier: Float = 1.0f): Float {
        val normalized = volume.coerceIn(0f, 1f)
        return (normalized * normalized * baseMultiplier).coerceIn(0f, 1f)
    }

    override fun playPaddleHit(intensity: Float, isPlayer1: Boolean) {
        if (!isAppInForeground) return
        if (isSoundEnabled) {
            val currentSet = resolveSoundSetNonBlocking()
            val soundId = currentSet?.hitPaddleId?.takeIf { it != 0 } ?: defaultPaddleHitId
            if (soundId != 0) {
                val effVol = calculateEffectiveVolume(intensity.coerceIn(0.25f, 1.0f))
                val rate = if (isPlayer1) 1.0f else 0.92f
                playbackPort.playSample(soundId, effVol, priority = 1, rate = rate)
            }
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = 15, amplitude = (intensity * 255).toInt().coerceIn(60, 255))
        }
    }

    override fun playWallHit() {
        if (!isAppInForeground) return
        if (isSoundEnabled) {
            val currentSet = resolveSoundSetNonBlocking()
            val soundId = currentSet?.hitWallId?.takeIf { it != 0 } ?: defaultWallHitId
            if (soundId != 0) {
                val effVol = calculateEffectiveVolume(0.7f)
                playbackPort.playSample(soundId, effVol, priority = 1, rate = 1.0f)
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
    override fun playGoalScored(score: Int, target: Int) {
        if (!isAppInForeground) return
        if (isSoundEnabled) {
            val currentSet = resolveSoundSetNonBlocking()
            val soundId = currentSet?.goalScoredId?.takeIf { it != 0 } ?: defaultGoalScoredId
            if (soundId != 0) {
                val effVol = calculateEffectiveVolume(1.0f)
                playbackPort.playSample(soundId, effVol, priority = 2, rate = 1.0f)
            }
            if (score > 0) {
                playScoreAnnouncement(score, target)
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
    override fun playGoalConceded(score: Int, target: Int) {
        if (!isAppInForeground) return
        if (isSoundEnabled) {
            val currentSet = resolveSoundSetNonBlocking()
            val soundId = currentSet?.goalConcededId?.takeIf { it != 0 } ?: defaultGoalConcededId
            if (soundId != 0) {
                val effVol = calculateEffectiveVolume(0.95f)
                playbackPort.playSample(soundId, effVol, priority = 2, rate = 1.0f)
            }
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = 180, amplitude = 180)
        }
    }

    /**
     * Método de compatibilidade legado.
     */
    override fun playGoal() {
        playGoalScored(0, 3)
    }

    /**
     * Toca o chime com elevação harmônica proporcional à pontuação.
     */
    fun playScoreAnnouncement(currentScore: Int, targetScore: Int) {
        if (!isAppInForeground || !isSoundEnabled) return
        val currentSet = resolveSoundSetNonBlocking()
        val scoreId = currentSet?.scoreBlipId?.takeIf { it != 0 } ?: defaultScoreBlipId
        if (scoreId != 0) {
            val progress = (currentScore.coerceAtLeast(1) - 1).coerceAtMost(10)
            val rate = (1.0f + progress * 0.10f).coerceIn(0.5f, 2.0f)
            val effVol = calculateEffectiveVolume(0.80f)
            playbackPort.playSample(scoreId, effVol, priority = 1, rate = rate)
        }
    }

    override fun playCountdownBeep(number: Int) {
        if (!isAppInForeground || !isSoundEnabled) return
        val currentSet = resolveSoundSetNonBlocking()
        val soundId = when (number) {
            3 -> currentSet?.countdown3Id ?: 0
            2 -> currentSet?.countdown2Id ?: 0
            1 -> currentSet?.countdown1Id ?: 0
            else -> currentSet?.countdownGoId ?: 0
        }
        val effSoundId = if (soundId != 0) soundId else when (number) {
            3 -> defaultCountdown3Id
            2 -> defaultCountdown2Id
            1 -> defaultCountdown1Id
            else -> defaultCountdownGoId
        }
        if (effSoundId != 0) {
            val effVol = calculateEffectiveVolume(0.85f)
            playbackPort.playSample(effSoundId, effVol, priority = 1, rate = 1.0f)
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = if (number <= 0) 50 else 25, amplitude = 180)
        }
    }

    override fun playCountdownBeep(isFinal: Boolean) {
        playCountdownBeep(if (isFinal) 0 else 3)
    }

    // --- Reprodução e Controle da Música de Fundo (BGM) Temática ---

    fun playBgm(theme: TableTheme = currentTheme) {
        if (!isMusicEnabled || !isAppInForeground) return
        val vol = musicVolume.coerceIn(0f, 1f)

        if (playbackPort.hasLoadedBgm() && playingBgmTheme == theme) {
            playbackPort.resumeLoopingBgm(vol)
            return
        }

        stopBgm()
        playingBgmTheme = theme

        val bgmFileName = "bgm_${theme.name.lowercase()}_v1.wav"
        val file = File(cacheDir, bgmFileName)

        pendingBgmJob = audioScope.launch {
            try {
                if (!file.exists() || file.length() == 0L) {
                    ProceduralAudioSynthesizer.generateBgmWav(file, theme)
                }
                if (!isActive || !isMusicEnabled || !isAppInForeground || playingBgmTheme != theme) {
                    return@launch
                }
                playbackPort.startLoopingBgm(file, musicVolume.coerceIn(0f, 1f))
            } catch (_: Exception) {
                // Recupera silenciosamente em caso de falha do subsistema de áudio
            }
        }
    }

    fun pauseBgm() {
        playbackPort.pauseLoopingBgm()
    }

    fun resumeBgm() {
        if (!isMusicEnabled || !isAppInForeground) return
        if (playbackPort.hasLoadedBgm() && !playbackPort.isLoopingBgmPlaying()) {
            playbackPort.resumeLoopingBgm(musicVolume.coerceIn(0f, 1f))
        }
    }

    fun stopBgm() {
        pendingBgmJob?.cancel()
        pendingBgmJob = null
        playingBgmTheme = null
        playbackPort.stopLoopingBgm()
    }

    private fun triggerVibration(durationMs: Long, amplitude: Int) {
        playbackPort.vibrate(durationMs, amplitude)
    }

    override fun stopAll() {
        playbackPort.pauseAllSamples()
        stopBgm()
    }

    fun resumeAudio() {
        if (!isAppInForeground) return
        playbackPort.resumeAllSamples()
        if (isMusicEnabled && playbackPort.hasLoadedBgm()) {
            resumeBgm()
        }
    }

    fun release() {
        stopAll()
        audioScope.cancel()
        playbackPort.release()
    }
}
