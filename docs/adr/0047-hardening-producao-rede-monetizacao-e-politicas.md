# 0047. Hardening de Produção, Parametrização de Monetização/Play Games e Alinhamento de Políticas

Status: Aceito (Complementa a [ADR 0012](0012-google-play-release-packaging-aso-e-store-conformity.md), a [ADR 0013](0013-admob-monetization-cloudflare-web-portal-e-leaderboard-ranking.md), a [ADR 0014](0014-remocao-permanente-de-anuncios-e-google-play-billing.md) e a [ADR 0034](0034-central-de-estatisticas-historico-de-partidas-e-placares-google-play.md))

## Contexto e Decisão

Na preparação do lançamento em **Produção** na Google Play Store (`versionCode = 28`, `versionName = "2.0.0"`), identificamos desvios críticos entre o estado real do binário e decisões anteriores:
1. A constante `USE_PRODUCTION_ADS` estava hardcoded como `false` em `AdConstants.kt`, expondo builds finais ao risco de exibir anúncios de teste ou de ativar anúncios reais em builds locais de validação.
2. A [ADR 0012](0012-google-play-release-packaging-aso-e-store-conformity.md) registrava a remoção de `ACCESS_LOCAL_NETWORK` e declarava o aplicativo como "Zero Ads / Zero Data Collection", mas o código passou a operar com AdMob + UMP, Google Play Billing (`remove_ads`), Google Play Games Services (`SnapshotsClient` e Placares Globais) e permissão `ACCESS_LOCAL_NETWORK` (reintroduzida para compatibilidade de descoberta local em Android recente/16+).
3. Os IDs de placares do Play Games (`leaderboard_elo_id` e `leaderboard_stages_id`) estavam vazios em `strings.xml`, e o botão de Conquistas aparecia na interface sem conquistas configuradas.
4. `AndroidManifest.xml` mantinha `usesCleartextTraffic="true"` global, `proguard-rules.pro` guardava nomes antigos de classes removidas e as regras de backup automático estavam vazias.

Decidimos:
1. **Parametrização de Produção com Trava Fail-Fast no Gradle:**
   - Expor `BuildConfig.USE_PRODUCTION_ADS` controlado por `-PUSE_PRODUCTION_ADS=true` (padrão `false` em builds locais e bloqueado em `BuildConfig.DEBUG`) e injetar `leaderboard_elo_id` e `leaderboard_stages_id` via `local.properties` / variáveis de ambiente em `app/build.gradle.kts`.
   - Bloquear a compilação quando `USE_PRODUCTION_ADS=true` sem keystore de release ou sem os dois IDs de placar preenchidos.
2. **Alinhamento de UI e Monetização:**
   - Desativar o pré-carregamento automático de `RewardedAd` até existir fluxo dedicado na interface e ocultar temporariamente o botão de Conquistas em `LeaderboardScreen.kt` até que conquistas reais sejam configuradas no Play Console.
   - Restringir o extra de teste `override_lang` em `MainActivity.kt` exclusivamente a builds `BuildConfig.DEBUG`.
3. **Hardening de Rede, R8 e Backup:**
   - Substituir `usesCleartextTraffic="true"` por `res/xml/network_security_config.xml` com `cleartextTrafficPermitted="false"`, preservando intacta a comunicação local via sockets TCP/UDP e mDNS (que não usam HTTP) e mantendo `ACCESS_LOCAL_NETWORK` no manifesto.
   - Atualizar `app/proguard-rules.pro` com as classes reais (`HostTelemetryPacket`, `GuestInputPacket`, `GameState`) mantendo `isMinifyEnabled = true` e `isShrinkResources = true`.
   - Configurar `backup_rules.xml` e `data_extraction_rules.xml` excluindo `datastore/airhockey_ad_settings.preferences_pb` e `datastore/airhockey_billing_settings.preferences_pb` para que temporizadores de anúncios e titularidade de `remove_ads` não sejam restaurados de backups defasados ou de outra conta.
4. **Portal Oficial e Políticas de Privacidade:**
   - Padronizar `https://vmellos.dev` como o **Portal Oficial** onde política de privacidade e `app-ads.txt` são publicados, atualizando `PRIVACY_POLICY.md`, `web/privacy.html` e `docs/PLAY_STORE_LISTING.md` para declarar AdMob/UMP, Google Play Billing (`remove_ads`) e Play Games Services no Data Safety.

## Consequências

- **Positiva:** Impede a geração acidental de `.aab` de produção com anúncios de teste, placares vazios ou assinatura debug, além de eliminar inconsistências entre o binário e as políticas da Google Play Store.
- **Trade-off:** Builds finais de produção exigem explicitamente `-PUSE_PRODUCTION_ADS=true` e as chaves configuradas em `local.properties` ou variáveis de ambiente (`KEYSTORE_PATH`, `LEADERBOARD_ELO_ID`, `LEADERBOARD_STAGES_ID`).
