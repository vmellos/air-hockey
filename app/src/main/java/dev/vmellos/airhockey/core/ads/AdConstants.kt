package dev.vmellos.airhockey.core.ads

import dev.vmellos.airhockey.BuildConfig

object AdConstants {
    // Production Google AdMob IDs (dev.vmellos.airhockey)
    const val PROD_ADMOB_APP_ID = "ca-app-pub-6935322996335458~9861411597"
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-6935322996335458/4129526586"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6935322996335458/5570812799"
    const val PROD_REWARDED_AD_UNIT_ID = "ca-app-pub-6935322996335458/1670709190"

    // Official Google AdMob Test Ad Units
    const val TEST_ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    /**
     * Safe Monetization Guard: Set to false during Closed Testing (14 days) to prevent
     * invalid traffic penalties and AdMob account suspension from testers/internal tests.
     * Flip to true only when promoted to Public Production.
     */
    const val USE_PRODUCTION_ADS: Boolean = false

    fun getBannerAdUnitId(useProductionAds: Boolean = USE_PRODUCTION_ADS && !BuildConfig.DEBUG): String =
        if (useProductionAds) PROD_BANNER_AD_UNIT_ID else TEST_BANNER_AD_UNIT_ID

    fun getInterstitialAdUnitId(useProductionAds: Boolean = USE_PRODUCTION_ADS && !BuildConfig.DEBUG): String =
        if (useProductionAds) PROD_INTERSTITIAL_AD_UNIT_ID else TEST_INTERSTITIAL_AD_UNIT_ID

    fun getRewardedAdUnitId(useProductionAds: Boolean = USE_PRODUCTION_ADS && !BuildConfig.DEBUG): String =
        if (useProductionAds) PROD_REWARDED_AD_UNIT_ID else TEST_REWARDED_AD_UNIT_ID
}
