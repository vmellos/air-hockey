package dev.vmellos.airhockey.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
    private var soundCountdownId: Int = 0

    var isSoundEnabled: Boolean = true
    var volume: Float = 0.8f
    var isHapticsEnabled: Boolean = true

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

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
            val countdownWav = File(cacheDir, "sfx_countdown.wav")

            if (!hitPaddleWav.exists()) generateWav(hitPaddleWav, frequency = 480.0, durationMs = 60, isDecay = true)
            if (!hitWallWav.exists()) generateWav(hitWallWav, frequency = 240.0, durationMs = 45, isDecay = true)
            if (!goalWav.exists()) generateFanfareWav(goalWav)
            if (!countdownWav.exists()) generateWav(countdownWav, frequency = 880.0, durationMs = 80, isDecay = false)

            soundHitPaddleId = soundPool.load(hitPaddleWav.absolutePath, 1)
            soundHitWallId = soundPool.load(hitWallWav.absolutePath, 1)
            soundGoalId = soundPool.load(goalWav.absolutePath, 1)
            soundCountdownId = soundPool.load(countdownWav.absolutePath, 1)
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

    fun playCountdownBeep(isFinal: Boolean = false) {
        if (isSoundEnabled && soundCountdownId != 0) {
            val rate = if (isFinal) 1.5f else 1.0f
            soundPool.play(soundCountdownId, volume, volume, 1, 0, rate)
        }
        if (isHapticsEnabled) {
            triggerVibration(durationMs = if (isFinal) 50 else 25, amplitude = 180)
        }
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

    fun release() {
        soundPool.release()
    }
}
