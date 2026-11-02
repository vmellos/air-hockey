# 12. Google Play Release Packaging, Assinatura de Produção, ASO e Conformidade com a Loja

## Status
Aceito

## Contexto
1. O aplicativo foi concebido e estruturado com uma proposta de valor única no mercado global: física arcade de [Mesa Estendida](file:///home/vinicius/AndroidStudioProjects/AirHockey/GLOSSARY.md#L73-L76) unindo dois dispositivos físicos topo-a-topo via rede local Wi-Fi ou Ponto de Acesso Móvel com compensação de vão físico (bezel), sem requerer servidores em nuvem ou internet externa.
2. A publicação na **Google Play Store** impõe exigências técnicas e regulatórias rigorosas:
   - Formato obrigatório de **Android App Bundle (`.aab`)** compilado via `./gradlew bundleRelease` e assinado por chave de produção (RSA 2048+ bits, válida até 2050+).
   - Título estrito de no máximo 30 caracteres, que deve sintetizar a oferta multijogador e o diferencial da mesa estendida.
   - Ativos de arte obrigatórios: Ícone de alta resolução de 512x512 pixels sem transparência e Gráfico de Recursos (Feature Graphic) de 1024x500 pixels.
   - Declaração transparente de permissões (`CAMERA` para escaneamento de QR Code offline, `NEARBY_WIFI_DEVICES` e `ACCESS_FINE_LOCATION` para conexão local) com política de privacidade pública comprovando zero coleta de dados pessoais (Zero Data Collection).
3. Testes em aparelhos físicos de 120 Hz e 60 Hz revelaram que o antigo contador de FPS permanecia cravado no teto do vsync do display, tornando-se uma poluição visual supérflua para a versão final de produção.

## Decisão
1. **Remoção do Contador de FPS e Limpeza da Interface**:
   - Eliminou-se o indicador de FPS da arena e das telas de configuração, removendo a chave correspondente do DataStore e das strings de internacionalização, descontinuando o termo no [GLOSSARY.md](file:///home/vinicius/AndroidStudioProjects/AirHockey/GLOSSARY.md).
2. **Identidade do Produto e ASO Multilíngue**:
   - Adotou-se o título oficial **`Air Hockey 2P: Mesa Estendida`** (29 caracteres) para a loja brasileira, **`Air Hockey 2P: Dual Screen`** (26 caracteres) para os Estados Unidos e países de língua inglesa, e **`Air Hockey 2P: Mesa Extendida`** (29 caracteres) para países hispanófonos, preservando o nome simples `Air Hockey` no launcher do dispositivo.
   - Estruturou-se o dossiê completo de ASO em [`docs/PLAY_STORE_LISTING.md`](file:///home/vinicius/AndroidStudioProjects/AirHockey/docs/PLAY_STORE_LISTING.md) com palavras-chave de alta conversão orgânica focadas em jogos para 2 celulares, jogos offline e air hockey multiplayer.
3. **Identidade Visual e Ativos Gráficos Automatizados**:
   - Criou-se o script [`scripts/generate-store-assets.py`](file:///home/vinicius/AndroidStudioProjects/AirHockey/scripts/generate-store-assets.py) para renderizar a paleta arcade oficial (arena escura profunda, disco em neon âmbar com rastro veloz, rebatedores ciano e carmesim).
   - Geraram-se o ícone da Play Store (512x512 PNG), os banners de topo (1024x500 PNG) em PT, EN e ES, e as camadas de ícone adaptativo vetorial e rasterizado (`res/mipmap-*` e `res/drawable/ic_launcher_*`).
4. **Arquitetura de Assinatura Release Segura**:
   - Implementou-se o script [`scripts/generate-release-keystore.sh`](file:///home/vinicius/AndroidStudioProjects/AirHockey/scripts/generate-release-keystore.sh) que gera `keystore/airhockey-release.jks` dedicado e registra credenciais em `local.properties`.
   - Configurou-se `signingConfigs.release` em [`app/build.gradle.kts`](file:///home/vinicius/AndroidStudioProjects/AirHockey/app/build.gradle.kts) consumindo `local.properties` ou variáveis de ambiente, com fallback seguro para assinatura debug caso as chaves não estejam presentes, blindando o segredo via `.gitignore`.
5. **Conformidade Regulatória e Política de Privacidade**:
   - Saneou-se o manifesto [`AndroidManifest.xml`](file:///home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/AndroidManifest.xml) removendo permissões protegidas desnecessárias (`ACCESS_LOCAL_NETWORK`).
   - Publicou-se a [`PRIVACY_POLICY.md`](file:///home/vinicius/AndroidStudioProjects/AirHockey/PRIVACY_POLICY.md) declarando categoricamente a arquitetura local-only, zero rastreamento de dados e uso exclusivo da câmera para leitura offline de Códigos QR.

## Consequências
- **Positiva**: O projeto atinge 100% de conformidade com todos os requisitos técnicos, de design e de políticas do Google Play Console.
- **Positiva**: O pacote `.aab` de produção é gerado e assinado de forma totalmente reproduzível por meio de um único comando Gradle (`./gradlew bundleRelease`).
- **Positiva**: Máximo posicionamento em ASO para capitalizar o ineditismo da Mesa Estendida em dois aparelhos.
