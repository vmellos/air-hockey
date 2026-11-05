package dev.vmellos.airhockey.core.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementação 100% gratuita para a Samsung Galaxy Store.
 *
 * Distribuído sem compras in-app e monetizado exclusivamente através do Google AdMob.
 * Zero dependências de bibliotecas de billing no binário final.
 */
class DefaultBillingManager(
    @Suppress("UNUSED_PARAMETER") context: Context
) : BillingManager {

    private val _isAdsRemoved = MutableStateFlow(false)
    override val isAdsRemoved: StateFlow<Boolean> = _isAdsRemoved.asStateFlow()

    private val _formattedPrice = MutableStateFlow<String?>(null)
    override val formattedPrice: StateFlow<String?> = _formattedPrice.asStateFlow()

    private val _isLoadingPrice = MutableStateFlow(false)
    override val isLoadingPrice: StateFlow<Boolean> = _isLoadingPrice.asStateFlow()

    override val isPurchaseSupported: Boolean = false

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8)
    override val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    override fun startConnection(onConnected: (() -> Unit)?) {
        onConnected?.invoke()
    }

    override fun launchPurchaseFlow(activity: Activity) {
        // No-op na Samsung Galaxy Store (compras desativadas)
    }

    override fun restorePurchases() {
        // No-op na Samsung Galaxy Store (compras desativadas)
    }
}
