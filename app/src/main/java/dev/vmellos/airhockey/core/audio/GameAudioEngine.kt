package dev.vmellos.airhockey.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class GameAudioEngine(private val context: Context) {

    private val soundPool: SoundPool
    private val vibrator: Vibrator?

    private var soundHitPaddleId: Int = 0
    private var soundHitWallId: Int = 0
    private var soundGoalId: Int = 0
    private var soundCountdown3Id: Int = 0
    private var soundCountdown2Id: Int = 0
    private var soundCountdown1Id: Int = 0
    private var soundCountdownGoId: Int = 0
    private val loadedSoundIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())

    // Controles de Efeitos Sonoros
    var isSoundEnabled: Boolean = true
    var volume: Float = 0.8f
    var isHapticsEnabled: Boolean = true

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

    var musicVolume: Float = 0.6f
        set(value) {
            field = value.coerceIn(0f, 1f)
            try {
                bgmPlayer?.setVolume(field, field)
            } catch (_: Exception) {}
        }

    private var bgmPlayer: MediaPlayer? = null
    private var bgmFile: File? = null

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
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

        initializeSynthesizedSounds()
    }

    private fun initializeSynthesizedSounds() {
        try {
            val cacheDir = context.cacheDir
            val hitPaddleWav = File(cacheDir, "sfx_hit_paddle.wav")
            val hitWallWav = File(cacheDir, "sfx_hit_wall.wav")
            val goalWav = File(cacheDir, "sfx_goal.wav")
            val countdown3Wav = File(cacheDir, "sfx_countdown_3.wav")
            val countdown2Wav = File(cacheDir, "sfx_countdown_2.wav")
            val countdown1Wav = File(cacheDir, "sfx_countdown_1.wav")
            val countdownGoWav = File(cacheDir, "sfx_countdown_go.wav")

            if (!hitPaddleWav.exists() || hitPaddleWav.length() == 0L) generateWav(hitPaddleWav, frequency = 480.0, durationMs = 60, isDecay = true)
            if (!hitWallWav.exists() || hitWallWav.length() == 0L) generateWav(hitWallWav, frequency = 240.0, durationMs = 45, isDecay = true)
            if (!goalWav.exists() || goalWav.length() == 0L) generateFanfareWav(goalWav)
            if (!countdown3Wav.exists() || countdown3Wav.length() == 0L) generateHarmonicChimeWav(countdown3Wav, frequency = 523.25, durationMs = 140)
            if (!countdown2Wav.exists() || countdown2Wav.length() == 0L) generateHarmonicChimeWav(countdown2Wav, frequency = 659.25, durationMs = 140)
            if (!countdown1Wav.exists() || countdown1Wav.length() == 0L) generateHarmonicChimeWav(countdown1Wav, frequency = 783.99, durationMs = 140)
            if (!countdownGoWav.exists() || countdownGoWav.length() == 0L) generateGoChordWav(countdownGoWav)

            soundHitPaddleId = soundPool.load(hitPaddleWav.absolutePath, 1)
            soundHitWallId = soundPool.load(hitWallWav.absolutePath, 1)
            soundGoalId = soundPool.load(goalWav.absolutePath, 1)
            soundCountdown3Id = soundPool.load(countdown3Wav.absolutePath, 1)
            soundCountdown2Id = soundPool.load(countdown2Wav.absolutePath, 1)
            soundCountdown1Id = soundPool.load(countdown1Wav.absolutePath, 1)
            soundCountdownGoId = soundPool.load(countdownGoWav.absolutePath, 1)
        } catch (_: Exception) {
            // Silently fall back if storage fails
        }
    }

    fun playPaddleHit(intensity: Float = 1.0f) {
        if (isSoundEnabled && soundHitPaddleId != 0) {
            val clampedVol = (volume * intensity.coerceIn(0.3f, 1.0f)).coerceIn(0.1f, 1.0f)
            soundPool.play(soundHitPaddleId, clampedVol, clampedVol, 1, 0, 1.0f)
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = 15, amplitude = (intensity * 255).toInt().coerceIn(60, 255))
        }
    }

    fun playWallHit() {
        if (isSoundEnabled && soundHitWallId != 0) {
            val clampedVol = (volume * 0.7f).coerceIn(0.1f, 1.0f)
            soundPool.play(soundHitWallId, clampedVol, clampedVol, 1, 0, 1.0f)
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = 10, amplitude = 50)
        }
    }

    fun playGoal() {
        if (isSoundEnabled && soundGoalId != 0) {
            soundPool.play(soundGoalId, volume, volume, 2, 0, 1.0f)
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = 250, amplitude = 255)
        }
    }

    fun playCountdownBeep(countdownNumber: Int) {
        if (!isSoundEnabled) return
        val soundId = when (countdownNumber) {
            3 -> soundCountdown3Id
            2 -> soundCountdown2Id
            1 -> soundCountdown1Id
            else -> soundCountdownGoId
        }
        if (soundId != 0) {
            soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = if (countdownNumber <= 0) 50 else 25, amplitude = 180)
        }
    }

    fun playCountdownBeep(isFinal: Boolean = false) {
        playCountdownBeep(if (isFinal) 0 else 3)
    }

    // --- Reprodução e Controle da Música de Fundo (BGM) Arcade ---

    fun playBgm() {
        if (!isMusicEnabled) return
        try {
            if (bgmPlayer == null) {
                val file = bgmFile ?: File(context.cacheDir, "bgm_arcade_loop.wav").also { bgmFile = it }
                if (!file.exists() || file.length() == 0L) {
                    generateBgmArcadeLoopWav(file)
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
            } else {
                val player = bgmPlayer ?: return
                val vol = musicVolume.coerceIn(0f, 1f)
                player.setVolume(vol, vol)
                if (!player.isPlaying) {
                    player.start()
                }
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
                playBgm()
            }
        } catch (_: Exception) {}
    }

    fun stopBgm() {
        try {
            bgmPlayer?.stop()
            bgmPlayer?.release()
            bgmPlayer = null
        } catch (_: Exception) {}
    }



    private fun generateBgmArcadeLoopWav(file: File) {
        val sampleRate = 22050
        val bpm = 125.0
        val beatDuration = 60.0 / bpm // ~0.48s
        val totalBars = 8
        val beatsPerBar = 4
        val totalDuration = totalBars * beatsPerBar * beatDuration // 15.36s
        val numSamples = (sampleRate * totalDuration).toInt()
        val pcmData = ByteArray(numSamples * 2)

        var noiseSeed = 123456789L
        fun pseudoNoise(): Double {
            noiseSeed = (noiseSeed * 1103515245L + 12345L) and 0x7FFFFFFF
            return (noiseSeed.toDouble() / 0x7FFFFFFF) * 2.0 - 1.0
        }

        // Progressão harmônica: Am (compassos 0-1), F (2-3), C (4-5), G (6-7)
        val chordRoots = doubleArrayOf(110.0, 87.31, 130.81, 98.00) // A2, F2, C3, G2
        val chordFifths = doubleArrayOf(164.81, 130.81, 196.00, 146.83) // E3, C3, G3, D3
        val chordOctaves = doubleArrayOf(220.0, 174.61, 261.63, 196.00) // A3, F3, C4, G3

        // Frase melódica arcade retro em A minor
        val melodyNotes = doubleArrayOf(
            440.00, 523.25, 659.25, 783.99,
            880.00, 783.99, 659.25, 523.25,
            587.33, 659.25, 783.99, 880.00,
            1046.50, 880.00, 783.99, 659.25
        )

        var kickPhase = 0.0
        var bassPhase = 0.0
        var leadPhase = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val currentBar = ((t / (4.0 * beatDuration)).toInt()).coerceIn(0, totalBars - 1)
            val chordIdx = (currentBar / 2).coerceIn(0, 3)

            val rootFreq = chordRoots[chordIdx]
            val fifthFreq = chordFifths[chordIdx]
            val octFreq = chordOctaves[chordIdx]

            // 1. Bumbo / Kick (four-on-the-floor)
            val tBeat = t % beatDuration
            var kickSample = 0.0
            if (tBeat < 0.22) {
                val kickEnv = kotlin.math.exp(-22.0 * tBeat)
                val kickFreq = 48.0 + 120.0 * kotlin.math.exp(-35.0 * tBeat)
                kickPhase += 2.0 * Math.PI * kickFreq / sampleRate
                kickSample = sin(kickPhase) * kickEnv * 0.45
            }

            // 2. Caixa / Snare arcade (tempos 2 e 4)
            val beatIndex = (t / beatDuration).toInt()
            var snareSample = 0.0
            if (beatIndex % 2 == 1 && tBeat < 0.18) {
                val snareEnv = kotlin.math.exp(-20.0 * tBeat)
                val noise = pseudoNoise()
                val tone = sin(2.0 * Math.PI * 185.0 * tBeat)
                snareSample = (noise * 0.65 + tone * 0.35) * snareEnv * 0.30
            }

            // 3. Chimbal / Hi-Hat nos contratempos
            val tEighth = (t + beatDuration * 0.5) % beatDuration
            var hatSample = 0.0
            if (tEighth < 0.06) {
                val hatEnv = kotlin.math.exp(-65.0 * tEighth)
                hatSample = pseudoNoise() * hatEnv * 0.12
            }

            // 4. Synthwave Rolling Bass (arpejo em semicolcheias)
            val sixteenthDuration = beatDuration / 4.0
            val sixteenthIdx = ((t / sixteenthDuration).toInt()) % 4
            val bassFreq = when (sixteenthIdx) {
                0 -> rootFreq
                1 -> rootFreq
                2 -> octFreq
                else -> fifthFreq
            }
            val tSixteenth = t % sixteenthDuration
            val bassEnv = kotlin.math.exp(-12.0 * tSixteenth)
            bassPhase += 2.0 * Math.PI * bassFreq / sampleRate
            val bassWave = sin(bassPhase) + 0.35 * sin(bassPhase * 2.0)
            val bassSample = bassWave * bassEnv * 0.35

            // 5. Melodia sintetizada arcade retro
            val melodyIdx = ((t / sixteenthDuration).toInt()) % melodyNotes.size
            val leadFreq = melodyNotes[melodyIdx]
            val leadEnv = kotlin.math.exp(-14.0 * tSixteenth)
            leadPhase += 2.0 * Math.PI * leadFreq / sampleRate
            val leadWave = sin(leadPhase) + 0.25 * sin(leadPhase * 3.0)
            val leadSample = leadWave * leadEnv * 0.18

            var mixed = kickSample + snareSample + hatSample + bassSample + leadSample

            // Crossfade suave nos limites de início e fim para garantir loop contínuo sem estalos
            val fadeSamples = (0.015 * sampleRate).toInt()
            if (i < fadeSamples) {
                mixed *= (i.toDouble() / fadeSamples)
            } else if (i > numSamples - fadeSamples) {
                mixed *= ((numSamples - i).toDouble() / fadeSamples)
            }

            val clampedSample = (mixed * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
            val byteIndex = i * 2
            pcmData[byteIndex] = (clampedSample.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((clampedSample.toInt() shr 8) and 0xFF).toByte()
        }

        writeWavFile(file, pcmData, sampleRate)
    }

    private fun generateHarmonicChimeWav(file: File, frequency: Double, durationMs: Int) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val decay = kotlin.math.exp(-3.2 * (i.toDouble() / numSamples))
            val fundamental = sin(2.0 * Math.PI * frequency * time)
            val overtone = 0.25 * sin(4.0 * Math.PI * frequency * time)
            val sample = ((fundamental + overtone) / 1.25 * decay * 32767.0).toInt().coerceIn(-32768, 32767).toShort()

            val byteIndex = i * 2
            pcmData[byteIndex] = (sample.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        writeWavFile(file, pcmData, sampleRate)
    }

    private fun generateGoChordWav(file: File) {
        val sampleRate = 22050
        val durationMs = 280
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val decay = kotlin.math.exp(-2.5 * (i.toDouble() / numSamples))
            val note1 = sin(2.0 * Math.PI * 1046.50 * time) // C6
            val note2 = 0.6 * sin(2.0 * Math.PI * 1318.51 * time) // E6
            val note3 = 0.4 * sin(2.0 * Math.PI * 1567.98 * time) // G6
            val sample = ((note1 + note2 + note3) / 2.0 * decay * 32767.0).toInt().coerceIn(-32768, 32767).toShort()

            val byteIndex = i * 2
            pcmData[byteIndex] = (sample.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        writeWavFile(file, pcmData, sampleRate)
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

    private fun generateWav(file: File, frequency: Double, durationMs: Int, isDecay: Boolean) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val decay = if (isDecay) (1.0 - (i.toDouble() / numSamples)) else 1.0
            val angle = 2.0 * Math.PI * frequency * time
            val sample = (sin(angle) * decay * 32767.0).toInt().coerceIn(-32768, 32767).toShort()

            val byteIndex = i * 2
            pcmData[byteIndex] = (sample.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        writeWavFile(file, pcmData, sampleRate)
    }

    private fun generateFanfareWav(file: File) {
        val sampleRate = 22050
        val durationMs = 400
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val pcmData = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val time = i.toDouble() / sampleRate
            val freq = if (time < 0.15) 523.25 else if (time < 0.28) 659.25 else 783.99
            val decay = 1.0 - (i.toDouble() / numSamples)
            val sample = (sin(2.0 * Math.PI * freq * time) * decay * 32767.0).toInt().coerceIn(-32768, 32767).toShort()

            val byteIndex = i * 2
            pcmData[byteIndex] = (sample.toInt() and 0xFF).toByte()
            pcmData[byteIndex + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        writeWavFile(file, pcmData, sampleRate)
    }

    private fun writeWavFile(file: File, pcmData: ByteArray, sampleRate: Int) {
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalDataLen.toInt())
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16) // Subchunk1Size
        buffer.putShort(1) // AudioFormat (1 = PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort((channels * 2).toShort()) // BlockAlign
        buffer.putShort(16) // BitsPerSample
        buffer.put("data".toByteArray())
        buffer.putInt(totalAudioLen.toInt())

        FileOutputStream(file).use { out ->
            out.write(header)
            out.write(pcmData)
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
