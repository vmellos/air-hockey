# 0048. Cofre Criptografado de Ativos 3D e Descriptografia Exclusiva em RAM

Status: Aceito

## Contexto e Decisão

Todos os 74 ativos autorais do jogo em `assets/models/*.glb` (~7,5 MB) estavam expostos diretamente dentro de `.apk` e `.aab` (ZIP), permitindo extração trivial dos modelos 3D, materiais e animações via `unzip` ou `apktool`. Auditoria completa confirmou que o projeto não possui áudios estáticos, shaders customizados, JSONs nem WebView/vídeo em `assets/` (todo áudio é sintetizado proceduralmente em tempo de execução, e as duas artes raster de interface permanecem como recursos Android em `res/drawable/`). Os arquivos em `assets/environments/` e `assets/materials/*.filamat` são injetados transitivamente pelo AAR do Sceneview.

Decidimos:
1. **Módulo JVM puro `:asset-crypto` (`java-library`) como fonte única de criptografia:**
   - Contêiner binário próprio (`.lpkg`) com cabeçalho mágico de 8 bytes, versão de formato, IV de 12 bytes e `AES/GCM/NoPadding` (256 bits, tag de 128 bits), removendo cabeçalhos reconhecíveis (`glTF`, `PNG`, `RIFF`, `OggS`, `{`).
   - Endereçamento opaco em `lspk/<20-hex>.lpkg` via SHA-256 do **Caminho Canônico** (ex.: `models/mesa_arcade.glb`).
   - Subchave única por ativo (`HMAC-SHA256`), IV determinístico de 96 bits (`HMAC-SHA256(subchave, caminho + sha256(conteúdo))`) e `AAD` vinculado ao caminho canônico, preservando cache incremental/reprodutibilidade no Gradle sem reutilizar `(chave, IV)` e impedindo troca/renomeação de blobs.
2. **Pipeline Gradle isolado por variante e compatível com Configuration Cache / `--offline`:**
   - Mover os 74 `.glb` para `app/src/main/raw_assets/` (**Ativos Brutos**), mantendo `noCompress += "lpkg"`.
   - Registrar task `@CacheableTask` por variante via `androidComponents { onVariants { ... } }` conectada a `variant.sources.assets` e `variant.sources.kotlin`, invocando `AssetPackerCli` via `ExecOperations.javaexec` com o segredo (`ASSET_VAULT_SECRET`) entregue exclusivamente via `stdin` e removendo `.lpkg` órfãos quando ativos são renomeados ou excluídos.
   - Gerar classe Kotlin ofuscada (`GeneratedAssetVaultKey`) reconstruindo a chave mestra a partir de literais `Long` (`0x...uL.toLong()`), rotações de bits e máscaras XOR (sem strings literais e sem vínculo à assinatura de upload por conta do Google Play App Signing). Como NDK/CMake não estão instalados no ambiente, optamos por reconstrução ofuscada em Kotlin + R8 em vez de biblioteca `.so` C++/JNI.
   - Adotar fallback determinístico quando `ASSET_VAULT_SECRET` estiver ausente em builds locais/debug, e falha rápida (`GradleException`) quando `USE_PRODUCTION_ADS=true` sem `ASSET_VAULT_SECRET`.
3. **Carregamento em memória via `ProtectedAssetLoader`:**
   - Descriptografar o `.lpkg` direto para `ByteArray`/`ByteBuffer` em RAM, passar para `ModelLoader.createModelInstance(buffer)` do Sceneview e zerar imediatamente os buffers intermediários com `Arrays.fill(..., 0)`, sem gravar arquivos temporários em disco/cache.

## Consequências

- **Positiva:** Nenhum modelo `.glb` original ou caminho legível permanece extraível via `unzip`/`apktool`, a integridade de cada blob contra adulteração ou troca de caminho é autenticada via GCM, e o build permanece incremental, cacheável e compatível com `--offline`.
- **Trade-off:** O IV determinístico garante cacheabilidade e builds reproduzíveis no Gradle enquanto mantém unicidade por `(chave, caminho, conteúdo)`; a proteção de chave em RAM/APK depende de ofuscação Kotlin + R8 em vez de JNI/C++ enquanto não houver NDK configurado no projeto.
