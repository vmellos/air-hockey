package dev.vmellos.airhockey

import dev.vmellos.airhockey.core.billing.BillingConstants
import dev.vmellos.airhockey.core.billing.BillingEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingAndAdsTest {

    @Test
    fun `product id conforms to canonical domain specification`() {
        assertEquals("remove_ads", BillingConstants.PRODUCT_REMOVE_ADS)
    }

    @Test
    fun `billing events can be instantiated properly`() {
        val successEvent: BillingEvent = BillingEvent.PurchaseSuccess
        val restoreSuccessEvent: BillingEvent = BillingEvent.RestoreSuccess
        val notFoundEvent: BillingEvent = BillingEvent.RestoreNotFound
        val canceledEvent: BillingEvent = BillingEvent.PurchaseCanceled
        val errorEvent: BillingEvent = BillingEvent.BillingError("Network error")

        assertNotNull(successEvent)
        assertNotNull(restoreSuccessEvent)
        assertNotNull(notFoundEvent)
        assertNotNull(canceledEvent)
        assertTrue(errorEvent is BillingEvent.BillingError)
        assertEquals("Network error", (errorEvent as BillingEvent.BillingError).message)
    }
}
