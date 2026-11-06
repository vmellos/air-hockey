package dev.vmellos.airhockey

import android.app.Application
import dev.vmellos.airhockey.core.ads.AdManager
import dev.vmellos.airhockey.core.billing.BillingManager
import dev.vmellos.airhockey.core.audio.GameAudioEngine
import dev.vmellos.airhockey.core.database.AppDatabase
import dev.vmellos.airhockey.core.preferences.UserPreferencesRepository
import dev.vmellos.airhockey.feature.leaderboard.LeaderboardRepository
import dev.vmellos.airhockey.feature.stage.StageRepository
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

    lateinit var stageRepository: StageRepository
        private set

    lateinit var billingManager: BillingManager
        private set

    lateinit var adManager: AdManager
        private set

    lateinit var inAppUpdateEngine: dev.vmellos.airhockey.core.update.InAppUpdateEngine
        private set

    lateinit var playGamesSaveManager: dev.vmellos.airhockey.core.playgames.PlayGamesSaveManager
        private set

    lateinit var playGamesManager: dev.vmellos.airhockey.core.playgames.PlayGamesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = UserPreferencesRepository(this)
        audioEngine = GameAudioEngine(this)
        database = AppDatabase.getInstance(this)
        leaderboardRepository = LeaderboardRepository(database.leaderboardDao())
        stageRepository = StageRepository(database.stageProgressDao())
        playGamesSaveManager = dev.vmellos.airhockey.core.playgames.PlayGamesSaveManager(database)
        playGamesManager = dev.vmellos.airhockey.core.playgames.PlayGamesManager(playGamesSaveManager)
        playGamesManager.initialize(this)
        billingManager = BillingManager(this)
        billingManager.startConnection()
        adManager = AdManager(this, billingManager)
        inAppUpdateEngine = dev.vmellos.airhockey.core.update.PlayInAppUpdateEngine(this)

        applicationScope.launch {
            stageRepository.ensureStagesInitialized()
        }

        applicationScope.launch {
            preferencesRepository.userPreferencesFlow.collectLatest { prefs ->
                audioEngine.isSoundEnabled = prefs.soundEnabled
                audioEngine.volume = prefs.soundVolume
                audioEngine.isMusicEnabled = prefs.musicEnabled
                audioEngine.musicVolume = prefs.musicVolume
                audioEngine.isHapticsEnabled = prefs.hapticsEnabled
                audioEngine.currentTheme = prefs.tableTheme
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
        audioEngine.release()
        inAppUpdateEngine.unregister()
    }

    companion object {
        lateinit var instance: AirHockeyApp
            private set
    }
}
