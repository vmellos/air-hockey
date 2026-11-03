# 17. Mira Tática Anti-Oponente na IA, BGM Arcade Procedural e Modo Imersivo de Jogo

## Status
Aceito

## Contexto
1. **Estagnação no Modo IA x IA**: No início da partida ou em bolas mortas centralizadas, o disco era posicionado exatamente na linha divisória central (`y = 1000f`). Como ambas as IAs operavam com restrição estrita de meio-campo (`puckY < centerLineY`) e o rebatedor é fisicamente limitado a uma distância do raio da linha média, ambos os bots avaliavam o disco como fora de alcance ou em território neutro, recuando indefinidamente para suas posições de defesa e travando a partida.
2. **Ausência de Trilha Sonora Dinâmica**: As partidas continham apenas efeitos sonoros de colisão sintetizados, carecendo de ambientação musical imersiva para o clima retrô arcade.
3. **Controle Independente de Canais de Áudio**: Era necessária a separação entre efeitos sonoros de mesa (impactos de rebatedores e paredes) e música de fundo, tanto nas Configurações quanto no Diálogo de Pausa durante a partida.
4. **Comportamento Tático da IA (Leitura de Oponente)**: Nos níveis mais desafiadores (Médio e Difícil), os bots miravam estritamente no disco ou executavam ricochetes aleatórios sem considerar onde o rebatedor adversário estava posicionado, falhando em explorar os cantos desguarnecidos da meta ou adversários adiantados.
5. **Interferência de Controles do Sistema**: Durante partidas acirradas, toques velozes próximos às extremidades do display frequentemente acionavam acidentalmente a barra de navegação do Android (botões Início, Voltar e Aplicativos Recentes) ou a barra de status, interrompendo a concentração e a fluidez do jogo.

## Decisão
1. **Regra de Saque de Início e Quebra de Deadlock**:
   - No início da partida em tela única, o saque de abertura é concedido ao Jogador 1 / Bot 1 (`centerLineY + 300f`), posicionando o disco claramente em sua área de posse.
   - O controlador `AirHockeyAiController` detecta discos estacionários (`speed < 35 px/s`) em seu meio-campo e avança imediatamente em investida ofensiva para realizar o saque.
   - Foi implementado um temporizador de estagnação (`idleTimer`): caso o disco permaneça quase imóvel por mais de 0.8s a 1.0s próximo à linha divisória, a IA avança até o limite de meio-campo para chocar e desbloquear a disputa.
2. **Mira Tática Anti-Oponente**:
   - O controlador de IA passa a receber o estado do rebatedor oponente normalizado (`opponentX`, `opponentY`).
   - Avalia a vulnerabilidade do adversário: se o oponente guarda o poste esquerdo, a IA golpeia o disco pelo flanco esquerdo para direcionar a bola ao poste direito livre; se o oponente guarda a direita, ataca pelo flanco direito rumo ao poste esquerdo.
   - Se o adversário estiver adiantado em relação à linha de meta (`opponentY < totalHeight - 380f`), a IA executa jogadas laterais anguladas (bank shots) pelas laterais para encobrir e superar o adversário pela retaguarda.
3. **Trilha Sonora Procedural Arcade (BGM)**:
   - Síntese pura determinística em memória e cache (`bgm_arcade_loop.wav`) a 22050 Hz, 125 BPM na tonalidade de Lá menor (Am - F - C - G).
   - Arranjo completo contendo: bumbo four-on-the-floor, caixa/palmas arcade, chimbal em contratempo, linha de baixo sintetizada pulsante em semicolcheias (rolling bass) e arpejos melódicos nostálgicos.
   - Transição com crossfade harmônico de 15ms nas extremidades para loop infinito 100% contínuo e sem estalos perceptíveis.
   - Reprodução via `MediaPlayer` com hardware audio offload e gerenciamento automático de ciclo de vida (inicia na contagem/partida, pausa no menu de pausa e encerra no fim de jogo).
   - Zero acréscimo de peso em megabytes ao APK/AAB do projeto.
4. **Desacoplamento de Canais de Áudio (SFX vs Música)**:
   - Adicionadas chaves independentes `music_enabled` e `music_volume` no DataStore de preferências.
   - Controles de alternância e sliders dedicados adicionados ao menu "Áudio e Sensores" das Configurações e ao `PauseDialog` em jogo.
5. **Modo Imersivo Sticky com Ocultação de Controles do Dispositivo**:
   - Na tela da partida (`GameScreen`), utiliza-se `WindowInsetsControllerCompat` com `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` e `hide(Type.systemBars())`.
   - As barras de navegação do sistema e de status ficam completamente invisíveis e insensíveis a toques normais de jogo.
   - Ao sair da partida ou retornar aos menus, as barras e controles do sistema são restaurados imediatamente no `onDispose`.

## Consequências
- **Positiva**: Partidas no Modo IA x IA fluem imediatamente sem travamentos, com saque inicial dinâmico e resolução automática de deadlocks.
- **Positiva**: A IA nos níveis Médio e Difícil demonstra inteligência tática autêntica, explorando cantos abertos e rebotes nas paredes quando o jogador avança.
- **Positiva**: Experiência arcade autêntica com trilha sonora synthwave envolvente e controle independente de volume.
- **Positiva**: Nenhum risco de toques acidentais nos botões de navegação do dispositivo durante jogadas rápidas.
