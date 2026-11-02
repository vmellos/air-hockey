package dev.vmellos.airhockey.core.ads

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.vmellos.airhockey.core.billing.BillingManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.adDataStore by preferencesDataStore(name = "airhockey_ad_settings")

class AdManager(
    private val context: Context,
    private val billingManager: BillingManager? = null
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingInterstitial = false
    private var rewardedAd: RewardedAd? = null
    private var isLoadingRewarded = false
    private val cooldownTracker = AdCooldownTracker()

    private val _isAdsRemovedPermanently = MutableStateFlow(false)
    val isAdsRemovedPermanently: StateFlow<Boolean> = _isAdsRemovedPermanently.asStateFlow()

    private val _isAdFreeTemporarily = MutableStateFlow(false)
    val isAdFreeTemporarily: StateFlow<Boolean> = _isAdFreeTemporarily.asStateFlow()

    private val _isRewardedLoaded = MutableStateFlow(false)
    val isRewardedLoaded: StateFlow<Boolean> = _isRewardedLoaded.asStateFlow()

    companion object {
        private val AD_FREE_UNTIL_TIMESTAMP = longPreferencesKey("ad_free_until_timestamp")
        const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L
    }

    init {
        scope.launch {
            checkAdFreeExpiration()
        }
        billingManager?.let { bm ->
            scope.launch {
                bm.isAdsRemoved.collect { removed ->
                    _isAdsRemovedPermanently.value = removed
                }
            }
        }
    }

    fun isAdSuppressed(): Boolean = _isAdsRemovedPermanently.value || _isAdFreeTemporarily.value

    private suspend fun checkAdFreeExpiration() {
        val prefs = context.adDataStore.data.first()
        val until = prefs[AD_FREE_UNTIL_TIMESTAMP] ?: 0L
        _isAdFreeTemporarily.value = System.currentTimeMillis() < until
    }

    fun initialize(activity: Activity) {
        if (isInitialized) return

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (consentInformation.canRequestAds()) {
                        startAdsInitialization(activity.applicationContext)
                    }
                }
            },
            {
                // Fallback on error: allow ad request if canRequestAds
                if (consentInformation.canRequestAds()) {
                    startAdsInitialization(activity.applicationContext)
                }
            }
        )
    }

    private fun startAdsInitialization(appContext: Context) {
        if (isInitialized) return
        isInitialized = true
        MobileAds.initialize(appContext) {
            preloadInterstitial(appContext)
            preloadRewarded(appContext)
        }
    }

    fun preloadInterstitial(appContext: Context) {
        if (interstitialAd != null || isLoadingInterstitial || isAdSuppressed()) return
        isLoadingInterstitial = true

        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            appContext,
            AdConstants.getInterstitialAdUnitId(),
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    fun preloadRewarded(appContext: Context) {
        if (rewardedAd != null || isLoadingRewarded || _isAdsRemovedPermanently.value) return
        isLoadingRewarded = true

        val request = AdRequest.Builder().build()
        RewardedAd.load(
            appContext,
            AdConstants.getRewardedAdUnitId(),
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    _isRewardedLoaded.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    _isRewardedLoaded.value = false
                }
            }
        )
    }

    fun recordMatchFinished() {
        cooldownTracker.recordMatchFinished()
    }

    fun showInterstitialIfAllowed(activity: Activity, onDismiss: () -> Unit = {}) {
        scope.launch {
            checkAdFreeExpiration()
            if (isAdSuppressed() || !cooldownTracker.canShowInterstitial(_isAdFreeTemporarily.value)) {
                withContext(Dispatchers.Main) { onDismiss() }
                return@launch
            }

            val ad = interstitialAd
            if (ad == null) {
                withContext(Dispatchers.Main) {
                    preloadInterstitial(activity.applicationContext)
                    onDismiss()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        interstitialAd = null
                        cooldownTracker.recordInterstitialShown()
                        preloadInterstitial(activity.applicationContext)
                        onDismiss()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        interstitialAd = null
                        onDismiss()
                    }
                }
                ad.show(activity)
            }
        }
    }

    fun showRewarded(activity: Activity, onRewardGranted: () -> Unit, onDismiss: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            preloadRewarded(activity.applicationContext)
            onDismiss()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                _isRewardedLoaded.value = false
                preloadRewarded(activity.applicationContext)
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                _isRewardedLoaded.value = false
                onDismiss()
            }
        }

        ad.show(activity) { _ ->
            scope.launch {
                val newUntil = System.currentTimeMillis() + TWO_HOURS_MS
                context.adDataStore.edit { prefs ->
                    prefs[AD_FREE_UNTIL_TIMESTAMP] = newUntil
                }
                _isAdFreeTemporarily.value = true
                withContext(Dispatchers.Main) {
                    onRewardGranted()
                }
            }
        }
    }
}
