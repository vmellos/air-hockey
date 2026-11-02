package dev.vmellos.airhockey.core.billing

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.billingDataStore by preferencesDataStore(name = "airhockey_billing_settings")

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isAdsRemoved = MutableStateFlow(false)
    val isAdsRemoved: StateFlow<Boolean> = _isAdsRemoved.asStateFlow()

    private val _formattedPrice = MutableStateFlow<String?>(null)
    val formattedPrice: StateFlow<String?> = _formattedPrice.asStateFlow()

    private val _isLoadingPrice = MutableStateFlow(false)
    val isLoadingPrice: StateFlow<Boolean> = _isLoadingPrice.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private var removeAdsProductDetails: ProductDetails? = null

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    companion object {
        private val ADS_REMOVED_KEY = booleanPreferencesKey("is_ads_removed_permanently")
    }

    init {
        scope.launch {
            loadPersistedPurchaseState()
        }
    }

    private suspend fun loadPersistedPurchaseState() {
        val prefs = context.billingDataStore.data.first()
        _isAdsRemoved.value = prefs[ADS_REMOVED_KEY] ?: false
    }

    private fun persistPurchaseState(isRemoved: Boolean) {
        scope.launch {
            context.billingDataStore.edit { prefs ->
                prefs[ADS_REMOVED_KEY] = isRemoved
            }
            _isAdsRemoved.value = isRemoved
        }
    }

    fun startConnection(onConnected: (() -> Unit)? = null) {
        if (billingClient.isReady) {
            onConnected?.invoke()
            return
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    queryPurchases(isRestoreExplicit = false)
                    onConnected?.invoke()
                } else {
                    _events.tryEmit(BillingEvent.BillingError(billingResult.debugMessage.ifBlank { "Falha ao conectar com o Google Play" }))
                }
            }

            override fun onBillingServiceDisconnected() {
                // Sincronização será tentada novamente na próxima interação
            }
        })
    }

    fun queryProductDetails(onComplete: ((ProductDetails?) -> Unit)? = null) {
        _isLoadingPrice.value = true
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BillingConstants.PRODUCT_REMOVE_ADS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
            _isLoadingPrice.value = false
            val details = if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                queryResult.productDetailsList?.firstOrNull { it.productId == BillingConstants.PRODUCT_REMOVE_ADS }
            } else {
                null
            }
            if (details != null) {
                removeAdsProductDetails = details
                _formattedPrice.value = details.oneTimePurchaseOfferDetails?.formattedPrice
            }
            onComplete?.invoke(details)
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        if (_isAdsRemoved.value) {
            _events.tryEmit(BillingEvent.PurchaseSuccess)
            return
        }

        val details = removeAdsProductDetails
        if (details != null) {
            launchFlowWithDetails(activity, details)
            return
        }

        _isLoadingPrice.value = true
        startConnection {
            queryProductDetails { updatedDetails ->
                _isLoadingPrice.value = false
                if (updatedDetails != null) {
                    launchFlowWithDetails(activity, updatedDetails)
                } else {
                    _events.tryEmit(BillingEvent.BillingError("Produto ainda não disponível no Google Play. Verifique se o produto está Ativo no Play Console."))
                }
            }
        }
    }

    private fun launchFlowWithDetails(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it, isRestore = false) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                persistPurchaseState(true)
                queryPurchases(isRestoreExplicit = false)
                _events.tryEmit(BillingEvent.PurchaseSuccess)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _events.tryEmit(BillingEvent.PurchaseCanceled)
            }
            else -> {
                _events.tryEmit(BillingEvent.BillingError(billingResult.debugMessage))
            }
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            startConnection {
                queryPurchases(isRestoreExplicit = true)
            }
            return
        }
        queryPurchases(isRestoreExplicit = true)
    }

    private fun queryPurchases(isRestoreExplicit: Boolean) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val removeAdsPurchase = purchases.firstOrNull { purchase ->
                    purchase.products.contains(BillingConstants.PRODUCT_REMOVE_ADS) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }

                if (removeAdsPurchase != null) {
                    handlePurchase(removeAdsPurchase, isRestore = isRestoreExplicit)
                } else {
                    if (isRestoreExplicit) {
                        _events.tryEmit(BillingEvent.RestoreNotFound)
                    }
                }
            } else {
                if (isRestoreExplicit) {
                    _events.tryEmit(BillingEvent.BillingError(billingResult.debugMessage))
                }
            }
        }
    }

    private fun handlePurchase(purchase: Purchase, isRestore: Boolean) {
        if (purchase.products.contains(BillingConstants.PRODUCT_REMOVE_ADS) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        ) {
            persistPurchaseState(true)
            if (isRestore) {
                _events.tryEmit(BillingEvent.RestoreSuccess)
            } else {
                _events.tryEmit(BillingEvent.PurchaseSuccess)
            }

            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { _ -> }
            }
        }
    }
}
