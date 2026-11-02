# 14. Remoção Permanente de Anúncios e Google Play Billing

## Status
Aceito

## Contexto
1. O jogo possui uma estratégia de monetização híbrida com Google AdMob (banners em menus e intersticiais no pós-jogo), além de permitir supressão temporária de 2 horas via anúncio recompensado.
2. Para oferecer uma experiência premium definitiva e atender a jogadores que preferem não visualizar anúncios comerciais, é necessária uma opção de compra in-app permanente, segura e integrada ao ecossistema do Google Play.
3. O jogo suporta partidas locais offline e multijogador P2P via Wi-Fi/Ponto de Acesso. Portanto, o status de compra deve funcionar de forma confiável mesmo quando o usuário estiver sem conexão à internet.

## Decisão
1. **Produto Não Consumível e Termo Canônico**:
   - Formalização do termo de domínio **Remoção Permanente de Anúncios**, referindo-se ao produto digital vitalício da Google Play Store.
   - O identificador oficial do produto in-app é `remove_ads`, configurado como produto único não consumível (one-time purchase).
2. **Arquitetura Desacoplada com `BillingManager`**:
   - Criação do gerenciador dedicado `BillingManager` em `core/billing/`, responsável único pelo ciclo de vida da biblioteca `com.android.billingclient:billing-ktx` (versão 7.x).
   - O `BillingManager` expõe o estado de compra e o preço localizado formatado via `StateFlow`.
   - O `AdManager` atua como consumidor desse estado, consolidando a supressão de anúncios em banner e intersticiais.
3. **Persistência Local e Resiliência Offline**:
   - Armazenamento em DataStore local do status de posse reconhecido (`acknowledgePurchase`), permitindo que o usuário desfrute do app sem anúncios mesmo em modo avião ou sem rede.
   - Sincronização assíncrona com `queryPurchasesAsync` e verificação de licença na inicialização sempre que o Google Play Services estiver acessível.
   - Tratamento do código de retorno `ITEM_ALREADY_OWNED` como sucesso automático, concedendo o benefício sem cobrança duplicada.
4. **Experiência do Usuário e Ajuste de Layout**:
   - Colapso total (altura zero) do contêiner de anúncio em banner nos menus e na tela de configurações quando a compra estiver ativa.
   - Ocultação do botão de anúncio recompensado de 2 horas no menu principal para usuários com a remoção permanente ativa.
   - Ponto de entrada com preço dinâmico e botão de restauração na tela de Configurações, além de atalho de compra no Menu Principal que desaparece após a aquisição.
   - Toda comunicação visual e textual é estritamente limpa e internacionalizada (en, pt-BR, es), sem o emprego de caracteres de emoji unicode.
5. **Monetização Local no Multijogador**:
   - Em partidas em Mesa Estendida ou Mesa Conectada, a decisão de exibição de anúncios no encerramento do confronto permanece 100% restrita ao estado local de cada aparelho, sem tráfego de licenças por socket de rede.

## Consequências
- **Positiva**: Experiência premium sem fricção para o jogador, mantendo o funcionamento offline integral do jogo.
- **Positiva**: Arquitetura desacoplada onde `BillingManager` e `AdManager` possuem fronteiras e responsabilidades claras.
- **Positiva**: Interface limpa e responsiva que recupera espaço de tela sem resíduos de layout.
