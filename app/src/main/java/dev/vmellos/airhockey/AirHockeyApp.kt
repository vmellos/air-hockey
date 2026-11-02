package dev.vmellos.airhockey

import android.app.Application
import dev.vmellos.airhockey.core.ads.AdManager
import dev.vmellos.airhockey.core.billing.BillingManager
import dev.vmellos.airhockey.core.audio.GameAudioEngine
import dev.vmellos.airhockey.core.database.AppDatabase
import dev.vmellos.airhockey.core.preferences.UserPreferencesRepository
import dev.vmellos.airhockey.feature.leaderboard.LeaderboardRepository
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

    lateinit var database: AppDatabase
        private set

    lateinit var leaderboardRepository: LeaderboardRepository
        private set

    lateinit var billingManager: BillingManager
        private set

    lateinit var adManager: AdManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = UserPreferencesRepository(this)
        audioEngine = GameAudioEngine(this)
        database = AppDatabase.getInstance(this)
        leaderboardRepository = LeaderboardRepository(database.leaderboardDao())
        billingManager = BillingManager(this)
        billingManager.startConnection()
        adManager = AdManager(this, billingManager)

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
