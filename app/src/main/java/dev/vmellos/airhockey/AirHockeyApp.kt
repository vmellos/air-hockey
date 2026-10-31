package dev.vmellos.airhockey

import android.app.Application
import dev.vmellos.airhockey.core.audio.GameAudioEngine
import dev.vmellos.airhockey.core.preferences.UserPreferencesRepository

class AirHockeyApp : Application() {

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var audioEngine: GameAudioEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = UserPreferencesRepository(this)
        audioEngine = GameAudioEngine(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        audioEngine.release()
    }

    companion object {
        lateinit var instance: AirHockeyApp
            private set
    }
}
