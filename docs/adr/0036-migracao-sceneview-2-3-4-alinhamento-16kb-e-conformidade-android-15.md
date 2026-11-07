# 36. Migração para SceneView 2.3.4, Alinhamento Nativo de 16 KB e Conformidade Android 15/16 no Google Play Console

**Data:** 2026-11-07  
**Status:** Aceito

---

## Contexto

Após a publicação da versão `1.8.0` (`versionCode = 17`), o Google Play Console reportou cinco apontamentos técnicos e de experiência do usuário:

1. **Observação crítica do SDK SceneView (`io.github.sceneview:sceneview:2.2.1`):** O autor do SDK registrou no Google Play SDK Index a recomendação de atualizar para a versão `2.3.4` (última release da série `2.x`), corrigindo ANRs causados por espera ilimitada na thread principal ao destruir o `Engine` do Filament quando a `SceneView` deixa a janela, além de falhas fatais em atualizações de frame e atualização da API de `CameraManipulator`.
2. **Alinhamento à biblioteca nativa de 16 KB:** A inspeção dos cabeçalhos ELF (`objdump -p`) das bibliotecas nativas empacotadas no app identificou que apenas `libfilament-jni.so`, `libgltfio-jni.so` e `libfilament-utils-jni.so` (oriundas do Filament `1.52.0` embutido no SceneView `2.2.1`) possuíam alinhamento de segmento `LOAD` de `4 KB` (`2**12`), enquanto todas as demais bibliotecas do projeto já operavam em `16 KB` (`2**14`).
3. **Dois alertas de Exibição de Ponta a Ponta (Edge-to-Edge) no Android 15:** Mesmo invocando `enableEdgeToEdge()` em `MainActivity.onCreate`, o otimizador R8 realizou o *inline* e remoção da classe `androidx.activity.EdgeToEdge` (`R8$$REMOVED$$CLASS$$3`) e ofuscou as classes auxiliares `androidx.activity.EdgeToEdgeApi26/28/29/30` para identificadores curtos (`sc0`, `tc0`, `uc0`, `vc0`). Como o analisador estático do Google Play Console valida a presença nominal de `androidx.activity.EdgeToEdge` e só isenta chamadas internas legadas (`setStatusBarColor`, `setNavigationBarColor` e `layoutInDisplayCutoutMode`) quando pertencem ao pacote `androidx.activity.EdgeToEdge*`, a ofuscação provocou ambos os falsos-positivos.
4. **Restrições de redimensionamento e orientação em dispositivos de tela grande:** A presença de `android:screenOrientation="portrait"` no `AndroidManifest.xml` e `requestedOrientation = SCREEN_ORIENTATION_PORTRAIT` na `MainActivity` acionou o alerta de qualidade para tablets e dobráveis.

---

## Decisão

1. **Atualização para SceneView `2.3.4` e Kotlin `2.2.21` (Preservando a Série `2.x`):**
   - Atualizamos `io.github.sceneview:sceneview` de `2.2.1` para `2.3.4` e o compilador Kotlin de `2.2.10` para `2.2.21` (compatível com o `kotlin-stdlib:2.2.21` exigido pelo manifesto POM do SceneView `2.3.4`).
   - Rejeitamos a migração imediata para a linha `4.x` do SceneView por tratar-se de uma reescrita arquitetural profunda desnecessária para sanar os problemas de estabilidade e conformidade da série `2.x`.
   - Adaptamos a **Manipulação por Câmera Orbital** na **Vitrine de Pré-Visualização 3D** (`Arena3DLivePreview`) para a nova implementação de `CameraManipulator` da versão `2.3.4`, preservando a órbita centralizada, o zoom por pinça e a retomada do **Giro de Apresentação Contínuo** após 3 segundos de inatividade.

2. **Resolução Unificada do Alinhamento de Página de Memória de 16 KB via Filament `1.68.2`:**
   - A atualização para o SceneView `2.3.4` atualiza transitivamente o motor nativo Google Filament de `1.52.0` para `1.68.2`, cujos binários nativos (`libfilament-jni.so`, `libgltfio-jni.so` e `libfilament-utils-jni.so`) são compilados com alinhamento ELF `LOAD` de `16 KB` (`2**14`), tornando 100% das bibliotecas nativas do aplicativo compatíveis com dispositivos Android de páginas de 16 KB.

3. **Preservação Nominal do `EdgeToEdge` no R8 e Estilo Escuro Explícito:**
   - Adicionamos a diretiva `-keep class androidx.activity.EdgeToEdge* { *; }` em `app/proguard-rules.pro` para impedir que o R8 remova ou renomeie as classes de compatibilidade do AndroidX, garantindo que o scanner estático do Google Play Console reconheça a chamada a `enableEdgeToEdge()` e a whitelist oficial de retrocompatibilidade.
   - Configuramos `enableEdgeToEdge` em `MainActivity` com `SystemBarStyle.dark(Color.TRANSPARENT)` para a barra de status e de navegação, alinhado à identidade visual integralmente escura do jogo.

4. **Remoção de Travas de Orientação para Compatibilidade com Telas Grandes:**
   - Removemos `android:screenOrientation="portrait"` do `AndroidManifest.xml` e `requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT` da `MainActivity`, apoiando-nos no **Contêiner Responsivo de Menus** e no cálculo dinâmico de proporção de tela (`BoxWithConstraints` / `screenAspect`) das arenas 2D e 3D.

---

## Consequências

- **Eliminação dos 5 Alertas no Google Play Console:** O pacote de release (`versionCode = 18` / `1.8.1`) atende simultaneamente às diretrizes do SDK Index, páginas de memória de 16 KB, Edge-to-Edge no Android 15/16 e dispositivos de tela grande.
- **Estabilidade Gráfica 3D:** Eliminação do risco de ANR no encerramento da superfície nativa do Filament e proteção contra exceções de frame.
