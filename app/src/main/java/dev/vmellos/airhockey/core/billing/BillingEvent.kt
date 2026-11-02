package dev.vmellos.airhockey.core.billing

sealed interface BillingEvent {
    data object PurchaseSuccess : BillingEvent
    data object RestoreSuccess : BillingEvent
    data object RestoreNotFound : BillingEvent
    data object PurchaseCanceled : BillingEvent
    data class BillingError(val message: String? = null) : BillingEvent
}
