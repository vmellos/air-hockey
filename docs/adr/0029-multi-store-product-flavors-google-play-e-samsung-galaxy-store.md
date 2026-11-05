# 29. Multi-Store Product Flavors: Google Play Store e Samsung Galaxy Store

## Status
Aceito

## Contexto
1. **Expansão de Distribuição Comercial**: O Air Hockey foi inicialmente concebido e publicado na Google Play Store com modelo de monetização híbrido: exibição de anúncios Google AdMob (banners e intersticiais), supressão temporária via anúncios recompensados e compra in-app única (IAP) não consumível para a **Remoção Permanente de Anúncios** gerenciada pelo Google Play Billing (`com.android.billingclient:billing-ktx`).
2. **Requisitos Rígidos da Samsung Galaxy Store**: A Samsung Galaxy Store realiza verificação estática automatizada de binários e rejeita sumariamente qualquer aplicativo que contenha referências a bibliotecas da Google Play Store (`com.android.billingclient`) ou botões/telas de compra inoperantes.
3. **Estratégia 100% Free Ad-Supported na Galaxy Store**: Para expandir a distribuição sem incorrer em custos recorrentes de manutenção de SDKs proprietários de billing (como o Samsung IAP SDK), definiu-se a distribuição do jogo na Samsung Galaxy Store como 100% gratuito suportado por anúncios AdMob, com supressão temporária de 2h via anúncio premiado, porém sem compras in-app nem botões de faturamento.
4. **Manutenibilidade de Base Única**: A coexistência das duas lojas deve ocorrer sob um único repositório e base de código, sem bifurcação de branches nem proliferação de condicionais na camada de interface e no ciclo de vida de anúncios.

## Decisão

1. **Configuração de Product Flavors no Gradle (`store` dimension)**:
   - Configuração da dimensão `store` no `app/build.gradle.kts` com duas variantes de produto: `google` e `samsung`.
   - Isolamento estrito do SDK de Billing à variante Google através da configuração `"googleImplementation"(libs.play.billing.ktx)`.
   - A variante `samsung` possui zero dependências de bibliotecas de billing (`samsungImplementation` não inclui billing).
   - O Google Mobile Ads (`libs.play.services.ads`) e a plataforma de consentimento UMP (`libs.user.messaging.platform`) permanecem compartilhados em `implementation`.
   - Ambas as variantes mantêm o mesmo `applicationId = "dev.vmellos.airhockey"` e as mesmas configurações de assinatura de release.

2. **Abstração Canônica via Interface `BillingManager` e Substituição por Source Sets**:
   - Criação da interface de domínio `BillingManager` em `app/src/main/java/dev/vmellos/airhockey/core/billing/BillingManager.kt`, expondo:
     - `val isAdsRemoved: StateFlow<Boolean>`
     - `val formattedPrice: StateFlow<String?>`
     - `val isLoadingPrice: StateFlow<Boolean>`
     - `val isPurchaseSupported: Boolean`
     - `val events: SharedFlow<BillingEvent>`
     - `fun startConnection(onConnected: (() -> Unit)?)`
     - `fun launchPurchaseFlow(activity: Activity)`
     - `fun restorePurchases()`
   - Fornecimento de implementações concretas sob o mesmo nome e pacote (`DefaultBillingManager`) em cada source set:
     - `src/google/`: Conecta-se ao `BillingClient`, consulta o SKU `remove_ads`, processa compras e restaurações, e define `isPurchaseSupported = true`.
     - `src/samsung/`: Fornece um stub estático com `isAdsRemoved = false`, `isPurchaseSupported = false` e métodos no-op, sem incluir nenhuma classe da biblioteca do Google Play Billing.
   - No `AirHockeyApp.kt`, a injeção ocorre de forma transparente via `billingManager = DefaultBillingManager(this)`.

3. **Adaptação de Interface do Usuário e Supressão de Paywall**:
   - Na `MainMenuScreen`: O botão de compra rápida (`Icons.Default.ShoppingBag`) é condicionado a `if (billingManager.isPurchaseSupported && !isAdsRemovedPermanently)`.
   - Na `SettingsScreen`: A seção inteira de "Remoção Permanente de Anúncios" (título, descrição, preço dinâmico, botões de compra e restauração) é encapsulada em `if (billingManager.isPurchaseSupported)`.
   - A observação de eventos de compra (`billingManager.events`) é desativada quando `isPurchaseSupported == false`.
   - O botão de anúncio recompensado de 2 horas no menu principal permanece totalmente ativo e funcional em ambos os flavors, permitindo alívio temporário de anúncios sem transações financeiras.

4. **Isolamento de Atualizações Google Play In-App Updates**:
   - Na `MainActivity`, a verificação e o callback do Google Play In-App Update (`InAppUpdateEngine`) são contornados quando `BuildConfig.FLAVOR == "samsung"`, prevenindo falhas de verificação ou chamadas anômalas ao Google Play Services em dispositivos onde o app foi instalado pela Galaxy Store.
   - Na `MainMenuScreen`, o banner de atualização flexível baixada é suprimido na variante Samsung.

## Consequências
- **Positiva**: Base de código 100% unificada com compilação determinística e limpa para ambas as lojas (`assembleGoogleRelease` e `assembleSamsungRelease`).
- **Positiva**: Zero bytes e zero símbolos da biblioteca `com.android.billingclient` empacotados nos APKs e AABs da Samsung Galaxy Store, garantindo aprovação imediata nos testes estáticos automatizados da Samsung.
- **Positiva**: Usuários da Samsung desfrutam de experiência de jogo arcade completa e sem atritos, monetizada de forma sustentável via AdMob, sem telas de erro nem botões inativos.
- **Positiva**: Modularização desacoplada que permite plugar novos gateways ou lojas futuras no ecossistema sem tocar no código de jogo ou física da arena.
