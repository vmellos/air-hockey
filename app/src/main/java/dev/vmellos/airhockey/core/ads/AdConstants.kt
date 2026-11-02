package dev.vmellos.airhockey.core.ads

import dev.vmellos.airhockey.BuildConfig

object AdConstants {
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-6935322996335458/1067268060"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6935322996335458/8754186398"
    const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-6935322996335458/6057104396"

    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private val isDebug: Boolean
        get() = BuildConfig.DEBUG

    fun getBannerAdUnitId(): String {
        return if (isDebug) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID
    }

    fun getInterstitialAdUnitId(): String {
        return if (isDebug) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID
    }

    fun getRewardedAdUnitId(): String {
        return if (isDebug) TEST_REWARDED_AD_UNIT_ID else PROD_REWARDED_AD_UNIT_ID
    }
}
