# 0045. Sincronização Simétrica de Pausa e Retomada em Rede, Deduplicação de Largada e Geometria da Meia-Mesa Kiara Cat

Implementamos a **Retomada com Contagem Regressiva** simétrica imediata na **Mesa Estendida** e na **Mesa Conectada**, deduplicamos o gatilho de início de partida no **Lobby Multijogador**, protegemos o ciclo de vida contra pausas transitórias espúrias e corrigimos a proporção métrica 1:2 da **Meia-Mesa 3D** do **Conjunto Temático Kiara Cat**.

## Contexto e Decisão

1. **Bloqueio do Convidado após Retomada da Pausa:**
   No `ArbitroVirtual.applyRemoteState`, uma guarda defensiva forçava `effectiveStatus = MatchStatus.PAUSED` sempre que o estado anterior local fosse `PAUSED` e o novo estado recebido do Anfitrião fosse `COUNTDOWN` ou `PLAYING`. Com isso, quando o Anfitrião despausava a partida e entrava em contagem regressiva, o Dispositivo Convidado permanecia irremediavelmente travado no diálogo de pausa. Além disso, se o Convidado tocasse em "Continuar", seu `GameViewModel.requestResume()` apenas emitia o evento de rede sem atualizar seu próprio estado nem garantir a transição caso o Anfitrião já tivesse despausado.
2. **Pausa Automática Involuntária na Largada da Partida:**
   Em `LobbyViewModel.consumeStartMatchTrigger()`, apenas o estado do ViewModel do Lobby tinha o gatilho zerado. O `MultiplayerMatchCoordinator` mantinha o valor do timestamp em `MultiplayerSessionState.startMatchTrigger`. Qualquer atualização de estado de transporte (ex.: `StateSync` periódico de abertura) provocava uma nova coleta em `LobbyViewModel`, reatribuindo o gatilho e re-navegando para a rota de jogo. Paralelamente, o `DisposableEffect` de ciclo de vida em `GameScreen` interceptava qualquer evento transitório de `ON_PAUSE` durante a transição Compose / navegação ou exibição de avisos de permissão de rede local, pausando imediatamente a partida recém-iniciada.
3. **Geometria e Enquadramento da Meia-Mesa 3D Kiara Cat:**
   A malha de meia-mesa do Kiara Cat (`mesa_kiara_cat_half_p1.glb` e `_p2.glb`) fora gerada incorretamente com comprimento `l = 1.08m` (ocupando apenas `Z in [0, 1.08]`), sem a abertura e semipórtico em `Z = -1.08m`. Como o sistema de projeção 3D zênite (`Table3DCameraProjection`) ancora a extremidade superior aberta da Mesa Estendida em `Z_TOP_EXTENDED = -1.08m`, a metade superior da tela ficava em vácuo escuro com o círculo central cortado.

Decidimos:
1. **Despausa Simétrica Imediata em Rede:**
   - Em `ArbitroVirtual.applyRemoteState`, quando o estado remoto recebido é `COUNTDOWN` ou `PLAYING`, o Dispositivo Convidado aceita a transição imediata para fora de `PAUSED`.
   - Em `GameViewModel.requestResume()`, se o Dispositivo Convidado solicitar retomada, o evento de controle `Resume` é transmitido ao Anfitrião. O Anfitrião, ao receber `TableControlEvent.Resume`, executa `requestResume()`, transitando de `PAUSED` para `COUNTDOWN` e propagando `broadcastState()`, sincronizando a contagem de 3 segundos em ambos os aparelhos.
2. **Deduplicação do Gatilho de Partida e Filtro de Ciclo de Vida:**
   - O `MultiplayerMatchCoordinator` passa a expor método de consumo/limpeza de `startMatchTrigger`, garantindo que o gatilho só dispare a navegação uma única vez por sessão iniciada.
   - O observador de ciclo de vida em `GameScreen` adota filtro defensivo: não pausa automaticamente a partida durante a fase de contagem inicial imediata caso a atividade não tenha entrado em `ON_STOP` efetivo.
3. **Meia-Mesa 3D Kiara Cat Proporcional 1:2:**
   - Regeneramos `mesa_kiara_cat_half_p1.glb` e `mesa_kiara_cat_half_p2.glb` em estrita conformidade com a ADR 0035: comprimento total `2.16m` (`Z in [-1.08, +1.08]`), extremidade superior aberta em `Z = -1.08m` com arco semicircular e orelhinhas neon, eletro-borda de transição contínua e meta com barreira laser na extremidade inferior (`Z = +1.08m`).
   - Calibramos o material da superfície (`DARK_SURFACE`) com rugosidade e metalicidade equilibradas para evitar superexposição esbranquiçada sob a iluminação direcional Filament.

## Consequências

- **Positiva:** Partidas em Mesa Estendida e Conectada despausam com sincronismo bilateral perfeito, permitindo que tanto o Anfitrião quanto o Convidado continuem a disputa sem bloqueios.
- **Positiva:** Partidas multijogador iniciam sem pausar sozinhas durante a contagem regressiva inicial.
- **Positiva:** A Meia-Mesa 3D da Kiara Cat renderiza completa, alinhada com as bordas da tela e com proporção idêntica às demais 11 arenas do catálogo.
