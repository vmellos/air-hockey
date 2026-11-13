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

/**
 * Interface de porta de reprodução de áudio e resposta tátil,
 * desacoplando [GameAudioEngine] de [SoundPool], [MediaPlayer] e [Vibrator] do Android.
 */
interface AudioPlaybackPort {
    fun loadSample(file: File): Int
    fun playSample(sampleId: Int, volume: Float, priority: Int, rate: Float)
    fun startLoopingBgm(file: File, volume: Float)
    fun setLoopingBgmVolume(volume: Float)
    fun hasLoadedBgm(): Boolean
    fun isLoopingBgmPlaying(): Boolean
    fun resumeLoopingBgm(volume: Float)
    fun pauseLoopingBgm()
    fun stopLoopingBgm()
    fun pauseAllSamples()
    fun resumeAllSamples()
    fun vibrate(durationMs: Long, amplitude: Int)
    fun release()
}

/**
 * Adaptador Android para [AudioPlaybackPort] baseado em [SoundPool], [MediaPlayer] e [Vibrator].
 */
class AndroidAudioPlaybackAdapter(context: Context) : AudioPlaybackPort {

    private val soundPool: SoundPool
    private val vibrator: Vibrator?
    private val loadedSoundIds = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private var bgmPlayer: MediaPlayer? = null

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
    }

    override fun loadSample(file: File): Int = soundPool.load(file.absolutePath, 1)

    override fun playSample(sampleId: Int, volume: Float, priority: Int, rate: Float) {
        soundPool.play(sampleId, volume, volume, priority, 0, rate)
    }

    @Synchronized
    override fun startLoopingBgm(file: File, volume: Float) {
        try {
            stopLoopingBgm()
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
                val clamped = volume.coerceIn(0f, 1f)
                setVolume(clamped, clamped)
                start()
            }
        } catch (_: Exception) {
            stopLoopingBgm()
        }
    }

    override fun setLoopingBgmVolume(volume: Float) {
        try {
            val clamped = volume.coerceIn(0f, 1f)
            bgmPlayer?.setVolume(clamped, clamped)
        } catch (_: Exception) {}
    }

    override fun hasLoadedBgm(): Boolean = bgmPlayer != null

    override fun isLoopingBgmPlaying(): Boolean = try {
        bgmPlayer?.isPlaying == true
    } catch (_: Exception) {
        false
    }

    override fun resumeLoopingBgm(volume: Float) {
        try {
            val player = bgmPlayer ?: return
            val clamped = volume.coerceIn(0f, 1f)
            player.setVolume(clamped, clamped)
            if (!player.isPlaying) {
                player.start()
            }
        } catch (_: Exception) {}
    }

    override fun pauseLoopingBgm() {
        try {
            if (bgmPlayer?.isPlaying == true) {
                bgmPlayer?.pause()
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    override fun stopLoopingBgm() {
        try {
            bgmPlayer?.stop()
            bgmPlayer?.release()
        } catch (_: Exception) {
        } finally {
            bgmPlayer = null
        }
    }

    override fun pauseAllSamples() {
        soundPool.autoPause()
    }

    override fun resumeAllSamples() {
        soundPool.autoResume()
    }

    override fun vibrate(durationMs: Long, amplitude: Int) {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))
            vib.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(durationMs)
        }
    }

    override fun release() {
        stopLoopingBgm()
        soundPool.release()
    }
}
