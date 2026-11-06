# ADR 0030: Vitrine 3D Interativa com Giro Contínuo, Câmera Orbital e Bloom Neon em Tempo Real

## Status
Aceito (Complementa a ADR 0028 e ADR 0029)

## Contexto
1. A **Vitrine de Pré-Visualização 3D** (`Arena3DLivePreview`) na tela de Customização (`ArenaCustomizationScreen`) apresentava os modelos tridimensionais da mesa, disco e rebatedores de forma totalmente estática e passiva. O manipulador de câmera estava explicitamente desativado (`cameraManipulator = null`), impedindo qualquer inspeção espacial por toque ou gestos de pinça (zoom).
2. O controle deslizante de intensidade de neon (`neonBloomIntensity`), localizado em um bloco logo abaixo do seletor 2D/3D, gravava assincronamente no DataStore a cada micro-deslocamento do slider durante o arraste, provocando contenção de I/O em tempo de execução.
3. Além disso, as opções de pós-processamento de Bloom no Filament operavam sob os valores padrão da biblioteca (`threshold = true` com corte severo em `highlight = 1000.0f`). Como os materiais 3D gerados no Blender possuem intensidade emissiva de 5.0, a dispersão de luz era virtualmente imperceptível, frustrando o feedback visual esperado ao manipular a barra de intensidade tanto na vitrine quanto durante a partida (`AirHockeyArena3DScene`).

## Decisão
1. **Giro de Apresentação Contínuo (Turntable Procedural)**:
   - Agrupar a mesa, o disco e os rebatedores sob um nó raiz espacial comum (`arenaRootNode: Node`).
   - Aplicar rotação procedural contínua e suave no eixo vertical Y (~12°/s) no callback `onFrame` do Sceneview.
   - Pausar instantaneamente a rotação no primeiro contato tátil do jogador (`isTouching = true`) e restabelecer o giro de forma suave a partir do ângulo exato onde o usuário parou após 3 segundos de inatividade tátil (`now - lastTouchTime >= 3000L`).
2. **Manipulação por Câmera Orbital e Pinça com Isolamento de Gestos**:
   - Ativar o manipulador orbital de câmera oficial do Sceneview (`rememberCameraManipulator` com órbita em volta do centro da mesa `(0, 0, 0)`).
   - Permitir rotação em 360° com 1 dedo e zoom por pinça com 2 dedos, desativando o deslizamento lateral acidental (`isPanEnabled = false`).
   - Bloquear a interceptação de toque pelo contêiner pai (`v.parent?.requestDisallowInterceptTouchEvent(true)`) para que gestos de pinça e rotação não provoquem a rolagem vertical da tela de customização.
3. **Controle Direto de Bloom Neon em Tempo Real**:
   - Implementar estado local reativo imediato (`liveNeonBloomIntensity`) desacoplado da persistência em disco. A cada frame de arraste do slider, o valor é propagado instantaneamente ao renderizador 3D, adiando a gravação no DataStore exclusivamente para o evento de desprendimento do dedo (`onValueChangeFinished`).
   - Calibrar o pipeline de Bloom do Filament (`threshold = false`, `highlight = 1.0f`, `strength = (intensity / 0.10f) * 0.45f` e modo `ADD`), liberando o resplendor luminoso real dos materiais emissivos tanto na vitrine quanto na arena de partida oficial (`AirHockeyArena3DScene`).
4. **Preservação Ergonômica de Interface**:
   - Manter a chave da Apresentação de Abertura 3D (`arena3dIntroEnabled`) preservada em sua posição original para controle do sobrevoo da partida, sem criar botões sobrepostos intrusivos na vitrine.

## Consequências
- **Positiva**: Experiência visual de vitrine moderna, interativa e fluida, permitindo inspecionar cada detalhe dos kits 3D de qualquer ângulo e distância.
- **Positiva**: Feedback visual imediato e de alto impacto ao calibrar a iluminação neon, sem engasgos de gravação no disco.
- **Positiva**: Coerência estética total entre a pré-visualização e o gameplay em 3D.
