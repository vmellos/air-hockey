package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.ads.AdConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AdConstantsTest {

    @Test
    fun `closed testing safe guard defaults to false`() {
        assertFalse(
            "USE_PRODUCTION_ADS must remain false during 14-day closed testing to avoid invalid traffic penalties",
            AdConstants.USE_PRODUCTION_ADS
        )
    }

    @Test
    fun `default getters route to test ad units when useProductionAds is disabled`() {
        assertEquals(
            AdConstants.TEST_BANNER_AD_UNIT_ID,
            AdConstants.getBannerAdUnitId(useProductionAds = false)
        )
        assertEquals(
            AdConstants.TEST_INTERSTITIAL_AD_UNIT_ID,
            AdConstants.getInterstitialAdUnitId(useProductionAds = false)
        )
        assertEquals(
            AdConstants.TEST_REWARDED_AD_UNIT_ID,
            AdConstants.getRewardedAdUnitId(useProductionAds = false)
        )
    }

    @Test
    fun `production getters return official ad unit IDs when explicitly enabled`() {
        assertEquals(
            AdConstants.PROD_BANNER_AD_UNIT_ID,
            AdConstants.getBannerAdUnitId(useProductionAds = true)
        )
        assertEquals(
            AdConstants.PROD_INTERSTITIAL_AD_UNIT_ID,
            AdConstants.getInterstitialAdUnitId(useProductionAds = true)
        )
        assertEquals(
            AdConstants.PROD_REWARDED_AD_UNIT_ID,
            AdConstants.getRewardedAdUnitId(useProductionAds = true)
        )
    }

    @Test
    fun `production ad unit IDs conform to standard format`() {
        val prodAdMobPrefix = "ca-app-pub-6935322996335458/"
        assert(AdConstants.PROD_BANNER_AD_UNIT_ID.startsWith(prodAdMobPrefix))
        assert(AdConstants.PROD_INTERSTITIAL_AD_UNIT_ID.startsWith(prodAdMobPrefix))
        assert(AdConstants.PROD_REWARDED_AD_UNIT_ID.startsWith(prodAdMobPrefix))
    }
}
