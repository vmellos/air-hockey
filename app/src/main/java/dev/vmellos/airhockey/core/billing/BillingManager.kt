package dev.vmellos.airhockey.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface canônica para gerenciamento de faturamento e remoção permanente de anúncios.
 *
 * Desacopla a camada de apresentação (Compose) e de anúncios (AdManager)
 * das implementações específicas de cada loja de distribuição (Google Play vs Samsung Galaxy Store).
 */
interface BillingManager {
    /**
     * Fluxo reativo do estado permanente de remoção de anúncios.
     */
    val isAdsRemoved: StateFlow<Boolean>

    /**
     * Preço formatado e localizado do produto (ex.: "R$ 4,99").
     */
    val formattedPrice: StateFlow<String?>

    /**
     * Indicador de carregamento de preços na loja.
     */
    val isLoadingPrice: StateFlow<Boolean>

    /**
     * Indica se a variante ativa suporta compras in-app (true no Google, false na Samsung).
     */
    val isPurchaseSupported: Boolean

    /**
     * Eventos pontuais de faturamento (sucesso, cancelamento, erros).
     */
    val events: SharedFlow<BillingEvent>

    /**
     * Inicia a conexão com o serviço de faturamento da loja se necessário.
     */
    fun startConnection(onConnected: (() -> Unit)? = null)

    /**
     * Dispara o fluxo de compra da remoção permanente de anúncios.
     */
    fun launchPurchaseFlow(activity: Activity)

    /**
     * Restaura compras anteriores associadas à conta do usuário.
     */
    fun restorePurchases()
}
