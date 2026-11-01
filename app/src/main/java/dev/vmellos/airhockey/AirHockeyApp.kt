package dev.vmellos.airhockey

import android.app.Application
import dev.vmellos.airhockey.core.audio.GameAudioEngine
import dev.vmellos.airhockey.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AirHockeyApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var audioEngine: GameAudioEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = UserPreferencesRepository(this)
        audioEngine = GameAudioEngine(this)

        applicationScope.launch {
            preferencesRepository.userPreferencesFlow.collectLatest { prefs ->
                audioEngine.isSoundEnabled = prefs.soundEnabled
                audioEngine.volume = prefs.soundVolume
                audioEngine.isHapticsEnabled = prefs.hapticsEnabled
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
        audioEngine.release()
    }

    companion object {
        lateinit var instance: AirHockeyApp
            private set
    }
}
