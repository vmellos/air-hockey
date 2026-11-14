# Air Hockey

Air Hockey é um jogo arcade multijogador em tempo real executado localmente em um ou dois dispositivos móveis.

## Entidades de Jogo

**Aparelho**:
O dispositivo móvel físico (smartphone, tablet ou dobrável) que executa a aplicação individualmente ou pareado em rede com outro participante.
_Avoid_: Celular, telefone, smartphone (quando restringe indevidamente a experiência a celulares de bolso)

**Disco**:
O objeto circular deslizante impulsionado pelos jogadores para marcar pontos nas metas.
_Avoid_: Bola, puck, esfera

**Rebatedor**:
O acessório circular controlado pelo jogador por toque na tela para defender sua meta e golpear o disco.
_Avoid_: Raquete, taco, paddle, batedor, striker

**Mesa**:
A arena retangular delimitada por bordas onde o disco desliza e colide, contendo as áreas de meta em extremidades opostas.
_Avoid_: Campo, tabuleiro, pista, rink

**Meta**:
O abertura na extremidade do campo de cada jogador onde a entrada do disco concede um ponto ao adversário.
_Avoid_: Gol, caçapa, baliza, pocket

**Área da Meta**:
A demarcação no piso da mesa em frente à meta que delimita visualmente o arco defensivo e a zona de perigo.
_Avoid_: Pequena área, grande área, goal crease, área de gol

**Vão Físico**:
O intervalo físico de separação correspondente às molduras e bordas dos dois aparelhos colocados em contato na Mesa Estendida, configurado com valor padrão padronizado (8,0 mm) sem requerer calibração manual em tela intermediária.
_Avoid_: Bezel, bezel gap, borda plástica, gap, tela de calibração

**Ponto**:
A unidade de contagem de pontuação concedida quando o disco cruza integralmente a meta adversária.
_Avoid_: Gol, score, contagem

**Autogol**:
O evento indesejado em que um jogador ou a inteligência artificial impulsiona o disco diretamente para dentro de sua própria meta.
_Avoid_: Gol contra, own goal, ponto contra

**Deslocamento de Toque**:
O espaçamento ergonômico vertical aplicado entre o ponto de toque do dedo na tela e o centro do rebatedor para desobstruir a visão do disco.
_Avoid_: Touch offset, finger offset, margem de dedo, folga de toque

**Raio de Agarre do Rebatedor**:
A zona circular de tolerância ao redor do centro do rebatedor que condiciona o início do controle por toque, bloqueando saltos e deslocamentos anômalos disparados por cliques em áreas distantes da mesa.
_Avoid_: Grab radius, margem de toque, gatilho de clique, toque solto

**Higienização de Ponteiros de Toque**:
O mecanismo de segurança no controle da arena que redefine incondicionalmente os identificadores de toque retidos no motor gráfico sempre que a partida transita para fora do estado ativo de jogo (pausa, contagem regressiva, celebração de ponto ou encerramento) ou quando a aplicação perde o foco do sistema operacional, prevenindo o congelamento dos rebatedores por referência a ponteiros órfãos.
_Avoid_: Limpeza de toque, reset de dedo, touch unfreeze, desbloqueio manual

**Jogada Lateral**:
O golpe angular aplicado no disco contra as bordas laterais da mesa com o objetivo de produzir ricochete veloz em direção à meta oposta.
_Avoid_: Bank shot, tabela, jogada de canto, ricochete intencional

**Velocidade do Disco**:
A taxa máxima de deslocamento admitida para o disco durante a partida, acoplada diretamente ao nível de Dificuldade da IA nos confrontos individuais (Fácil = Lento, Médio = Normal, Difícil = Rápido).
_Avoid_: Rapidez da bola, velocidade do jogo, puck speed, ajuste manual de velocidade

**Empate** (Descontinuado):
O antigo resultado admitido quando a partida operava sob cronômetro regressivo. Com a remoção do limite de tempo em favor da vitória por Pontuação Alvo soberana, todas as partidas prosseguem continuamente até que um jogador alcance a meta de pontos, eliminando encerramentos arbitrários aos 00:00.
_Avoid_: Tie, draw, igualdade, sem vencedor, tempo esgotado

**Pontuação Alvo**:
A meta quantitativa de pontos definida para a partida (3, 5, 7 ou 10 pontos) cuja conquista pelo primeiro jogador encerra o confronto e declara o vencedor imediato e definitivo de forma soberana, sem interferência de cronômetro ou tempo limite.
_Avoid_: Placar final, limite de gols, meta de gols, target score

**Placar Lateral**:
A representação visual e minimalista da contagem de pontos posicionada na borda lateral da mesa na altura da linha central divisória (ancorada adjacente ao trilho direito da arena virtual, mantendo proximidade à mesa mesmo em telas largas de tablets e dobráveis), apresentando os dígitos dos participantes em tipografia neon de alto contraste (Ciano e Laranja) acompanhados pelo indicador da Pontuação Alvo da partida, desobstruindo as áreas de movimento dos rebatedores.
_Avoid_: Scoreboard do topo, placar superior, painel de pontos, score hud

**Modo Espectador**:
O estado operacional da interface e dos controles acionado durante o Modo IA x IA no qual o toque manual sobre a mesa é desativado em favor da simulação autônoma dos robôs, permitindo ao usuário acompanhar o duelo e interagir apenas através do menu de pausa.
_Avoid_: Tela travada, modo observador, bot view

**Contagem Regressiva**:
O intervalo preparatório em três tempos (~2,7 segundos em tempo real) antes de iniciar o movimento do disco na largada da partida, após cada ponto marcado ou ao retomar de uma pausa.
_Avoid_: Timer de início, delay inicial, espera de 3 segundos fixa

**Retomada com Contagem Regressiva**:
A aplicação da Contagem Regressiva especificamente ao despausar a partida antes de restabelecer o movimento físico do disco. Em partidas multijogador em rede (Mesa Estendida e Mesa Conectada), a retomada é simétrica e imediata: quando qualquer um dos participantes aciona o comando para continuar no Menu de Pausa da Arena, ambos os dispositivos encerram a pausa e executam sincronizadamente a contagem de saída.
_Avoid_: Unpause countdown, re-start, contagem de despause

**Posicionamento Preparatório**:
A movimentação tática livre do rebatedor permitida aos participantes estritamente em seu próprio meio-campo durante a contagem regressiva enquanto o disco permanece imóvel na saída, bloqueando qualquer travessia da linha central divisória.
_Avoid_: Pre-match movement, warm-up move, ajuste pré-jogo

**Saque de Início**:
O posicionamento do disco na metade da mesa do jogador que detém a posse de saída no começo da partida e após cada ponto marcado, permitindo a tacada inicial imediata sem estagnação.
_Avoid_: Tiro de saída, início parado, saque neutro

**Mira Tática Anti-Oponente**:
A análise posicional contínua executada pela inteligência artificial sobre as coordenadas do rebatedor adversário para direcionar o golpe deliberadamente no lado desprotegido da meta rival.
_Avoid_: Tiro mirado, mira automática, exploit de IA

## Experiência Visual e Câmera 3D

**Dimensão de Renderização**:
A seleção de apresentação visual da arena que alterna entre a perspectiva plana 2D (baseada em Jetpack Compose Canvas com gradientes e shaders) e a perspectiva espacial 3D imersiva (baseada em Sceneview e motor Filament com malhas PBR `.glb`), desacoplada da escolha estética do Tema de Mesa e configurável em todas as modalidades esportivas (Solo, Fases, Mesa Dividida, Mesa Conectada, Modo IA x IA e Mesa Estendida com Meia-Mesa 3D e Câmera Zenital Ancorada).
_Avoid_: Modo de câmera, visual 3d isolado, tema 3d, switch de gráfico

**Meia-Mesa 3D**:
A malha tridimensional otimizada (`.glb`) representativa de uma metade da arena esportiva na Mesa Estendida, construída na proporção geométrica 1:2 (1.08m de largura por 2.16m de comprimento) com a extremidade central aberta e arco neon de passagem para cobrir a extensão vertical da tela de cada dispositivo na Disposição Topo-a-Topo.
_Avoid_: Mesa cortada, modelo parcial, meia arena, mesa 3d curta, mini mesa

**Ancoragem de Topo na Mesa Estendida 3D**:
O alinhamento analítico da Câmera 3D Zenital que ancora a extremidade aberta da Meia-Mesa 3D no limite superior físico do display (`offsetY = 0`), mantendo a linha central colada na borda física do aparelho para continuidade com o segundo dispositivo e absorvendo variações de proporção de tela ou Níveis de Enquadramento 3D na margem inferior da meta.
_Avoid_: Centralização vertical, câmera solta, offset fixo, topo flutuante

**Kit de Ativos 3D**:
A coleção integrada de arquivos binários glTF otimizados (`.glb`) composta por Mesa Temática, Disco Temático e Rebatedores P1/P2 correspondentes a uma identidade visual de Conjunto Temático Esportivo, carregada dinamicamente pelo motor Sceneview com fallback automático para o conjunto clássico.
_Avoid_: Pasta de modelos, pack 3D, skins 3D, bundle glb

**Apresentação de Abertura da Mesa 3D**:
O voo panorâmico cinematográfico estilo drone que sobrevoa a extensão da arena com foco dinâmico antes do início da partida, iniciando em tomada elevada sobre a meta adversária, descendo em arco rasante ao longo da calha lateral com estabilização vetorial no eixo vertical do mundo (vetor UP em +Y sem roll ou rotação tipo turntable) e aterrissando com suavidade na perspectiva do Jogador 1 (ou no ângulo zenital na Mesa Dividida). Sincroniza a execução da Música de Fundo (BGM) desde o primeiro segundo do voo e permanece ativa ininterruptamente sem descarte por toques na tela, retendo o Saque de Início e a contagem regressiva até sua finalização completa, preservando o acionamento do botão Voltar para exibição do menu de pausa com opções de continuar, reiniciar ou sair para o menu.
_Avoid_: Animação inicial, intro 3d, câmera giratória, flyover, volta 360, tap to skip

**Níveis de Enquadramento 3D**:
Os três patamares de distância e amplitude de visão da câmera 3D calibrados para a perspectiva do Jogador 1, para a inversão espelhada de 180° do Dispositivo Convidado na Mesa Conectada (`z` negativo e guinada de `180°`, inclusive sob recuo de enquadramento para arenas com mascote), para a Câmera 3D Zenital no Modo Tela Dividida e para a Mesa Estendida com ancoragem no topo físico (Próximo, Padrão e Amplo), permitindo enquadrar a arena com ergonomia e segundo a preferência visual do usuário, alternáveis por controle no Menu de Pausa (preservando o HUD da arena despoluído durante a partida ativa) e persistidos nas preferências.
_Avoid_: Níveis de zoom, distâncias de câmera, câmera longe, câmera perto, botão de zoom

**Desprojeção em Perspectiva 3D**:
O cálculo de interseção geométrica que projeta o raio do ponto de toque na tela diretamente sobre o plano da mesa de jogo (Y = 0), convertendo com precisão coordenadas de tela em coordenadas métricas do motor físico sob qualquer um dos Níveis de Enquadramento 3D para controle preciso dos rebatedores.
_Avoid_: Raycast 2D, touch 3D aproximado, clique na tela, conversão ortográfica em 3D

**Piso da Arena 3D**:
O plano espacial perimetral posicionado no plano horizontal (`XZ`) abaixo e ao redor da mesa 3D estilizado com grade cibernética e balizadores de iluminação neon, servindo de âncora óptica fixa no mundo para evidenciar a cinemática de câmera durante a Apresentação de Abertura da Mesa 3D e ao longo da partida (renderizado via `piso_arena_cyber.glb` na Mesa Clássica e via piso de estádio temático integrado `ArenaStadiumFloor` nas 10 arenas de conjuntos esportivos).
_Avoid_: Chão escuro, fundo vazio, piso simples, arena vazia, piso vertical

**Barreira de Energia da Meta 3D**:
O portal luminoso em arco com emissores laser posicionado na embocadura de cada meta na Mesa 3D, simulando um campo de força de contenção arcade com iluminação temática da facção correspondente.
_Avoid_: Trave de gol, rede 3D, poste de gol, baliza 3d

**Rastro de Energia 3D**:
A esteira luminescente translúcida projetada dinamicamente atrás do disco em perspectiva tridimensional, desenhada com atenuação suave proporcional à velocidade instantânea de deslocamento na arena.
_Avoid_: Cauda da bola, rastro 2D estático, linha do disco, rastro simples

**Efeitos de Impacto 3D**:
O conjunto de partículas cintilantes e ondas de choque circulares geradas nos pontos exatos de colisão do disco contra os rebatedores ou trilhos da mesa e projetadas matematicamente na perspectiva da câmera 3D.
_Avoid_: Faíscas comuns, explosão 2D, efeito de batida, colisão simples

**Rotação Giroscópica do Disco**:
O giro angular contínuo do modelo tridimensional do disco ao redor de seu eixo vertical (Y) no motor Sceneview, calibrado proceduralmente com taxa de rotação proporcional à velocidade do deslocamento físico.
_Avoid_: Spin 2D, giro solto, rotação fake, giro aleatório

**Vitrine de Pré-Visualização 3D**:
O componente visual interativo na tela de customização que renderiza a Mesa, Disco e Rebatedores em tempo real via Sceneview/Filament, permitindo inspeção estética e espacial completa antes de equipar os itens.
_Avoid_: Preview 3D, caixa 3D, janela 3D, visualizador de skins

**Giro de Apresentação Contínuo**:
A rotação orbital lenta e automática da cena ao redor do eixo vertical na Vitrine de Pré-Visualização 3D, simulando um expositor giratório (turntable) que é temporariamente pausado durante qualquer manipulação tátil e restabelecido suavemente após período de inatividade.
_Avoid_: Auto-rotate, giro fake, rotação infinita, giro solto

**Manipulação por Câmera Orbital**:
O sistema de controle por gestos táteis multitoque (arraste com um dedo para órbita em azimute/elevação e pinça com dois dedos para zoom) na Vitrine de Pré-Visualização 3D, delimitado por limites de distância e altura para prevenir colisões ou ângulos cegos abaixo do solo.
_Avoid_: Pinch zoom simples, touch livre, arrastar câmera, gesto solto

**Controle Direto de Bloom Neon**:
O ajuste em tempo real da intensidade de dispersão luminosa (bloom) e luminescência dos materiais emissivos na Vitrine de Pré-Visualização 3D e no Menu de Pausa da Arena, subordinado em partida à ativação do Perfil de Efeitos Visuais.
_Avoid_: Slider de brilho, ajuste de luz, efeito neon, bloom slider, brilho do neon descolado

**Mascote da Mesa**:
O elemento cênico e animado que patrulha autonomamente o piso externo adjacente à arena virtual durante a partida na Dimensão de Renderização 3D (e representado por silhueta móvel com rastro de passos na perspectiva 2D), conferindo dinamismo orgânico ao ambiente esportivo ao alternar caminhada quadrúpede e paradas para limpeza facial.
_Avoid_: Mascote solto, pet decorativo, npc da mesa, enfeite de campo

**Circuito de Patrulha da Mascote**:
O percurso perimetral fechado no piso da arena que contorna as calhas laterais e o topo superior atrás da meta adversária, percorrido continuamente pela Mascote da Mesa em velocidade constante com paradas contemplativas em mirantes estratégicos onde a visibilidade é ampla e desobstruída do toque do jogador.
_Avoid_: Caminho aleatório, rota livre, passeio do gato, volta da mesa

**Interferência Felina (Mascote Ativa)**:
A mecânica lúdica e opcional na qual a Mascote da Mesa rastreia a proximidade do disco em relação às bordas laterais do campo e reage com alerta visual e movimento preparatório de patada esportiva, configurável nas opções para coexistir harmoniosamente com a integridade física do jogo competitivo tradicional.
_Avoid_: Cheat do gato, bug de colisão, pata aleatória, sabotagem do disco

## Modos de Jogo

**Modo Solo**:
Partida disputada por um jogador humano contra um oponente controlado por inteligência artificial em um único dispositivo.
_Avoid_: Modo treino, single player, vs bot

**Modo Fases**:
A modalidade de progressão do Modo Solo estruturada em uma jornada de 100 níveis sequenciais com dificuldade gradativa e metas de vitória, na qual cada triunfo concede estrelas de desempenho e destrava o estágio seguinte.
_Avoid_: Modo campanha, stage mode, levels, fases infinitas

**Estrelas de Desempenho**:
A classificação de 1 a 3 estrelas concedida ao concluir uma fase no Modo Fases, avaliando a eficiência defensiva do jogador pelo número de pontos cedidos ao adversário sob escala proporcional à Pontuação Alvo da fase (Alvo 3: 0 pontos cedidos para 3⭐; Alvo 5: até 1 ponto; Alvo 7: até 2 pontos; Alvo 10: até 3 pontos).
_Avoid_: Stars, rating de fase, nota da fase

**Tela de Desfecho da Fase**:
A interface modal translúcida com bordas chanfradas neon exibida ao término de uma fase no Modo Fases, destacando as estrelas conquistadas e os controles de avanço em caso de vitória, ou a opção de nova tentativa imediata em caso de derrota.
_Avoid_: Win screen, tela de parabéns, resultado da fase, tela de vitória da fase, lose screen

**Disco Flamejante**:
O elemento gráfico e animado de destaque na tela inicial, apresentando o disco metálico envolto por labaredas de plasma posicionado como plano de fundo atrás dos modos de jogo.
_Avoid_: Flaming puck, fogo de fundo, bola de fogo, puck animado

**Mesa Dividida**:
Partida para dois jogadores no mesmo dispositivo físico, dividindo a tela da mesa ao meio.
_Avoid_: Modo local compartilhado, tela dividida, pass-and-play

**Mesa Estendida**:
Partida para dois jogadores em dois dispositivos físicos dispostos em Disposição Topo-a-Topo, formando uma única mesa contínua onde o disco transita entre as telas.
_Avoid_: Dual screen, tela dupla, big board, mesa dividida

**Mesa Conectada**:
Partida para dois jogadores em dois dispositivos físicos pareados via rede sem fio, na qual cada aparelho projeta a mesa inteira e sincroniza os movimentos em tempo real. No modo 3D, o Dispositivo Convidado inverte a câmera em 180° com desprojeção de toque adaptada, permitindo que ambos os competidores defendam a meta na base da tela sob perspectiva nativa do seu próprio lado.
_Avoid_: Modo online, duelo sem fio, campo inteiro p2p, versus wifi, multiplayer normal

**Modo IA x IA**:
Partida de demonstração disputada exclusivamente entre dois oponentes virtuais controlados por inteligência artificial em um único dispositivo (Local) ou em dois dispositivos (Mesa Estendida).
_Avoid_: Modo bot x bot, modo espectador, simulação ia, cpu vs cpu

**Disposição Topo-a-Topo**:
A configuração física de dois aparelhos dispostos verticalmente com as bordas superiores em contato mútuo, unindo as duas metades da mesa.
_Avoid_: Alinhamento vertical, ponta a ponta, tela dupla

## Conectividade e Papéis

**Dispositivo Anfitrião**:
O aparelho que gerencia a sessão de jogo, publica a presença na rede local e executa a autoridade física da partida.
_Avoid_: Host, servidor, master

**Dispositivo Convidado**:
O aparelho secundário que descobre e se conecta ao dispositivo anfitrião para disputar a partida em Mesa Estendida.
_Avoid_: Client, peer, slave

**Perfil de Jogador**:
A identidade local do participante composta por seu nome de exibição para identificação em sessões multijogador e no placar.
_Avoid_: Conta, usuário, login, player profile, gamer tag

**Partida Privada**:
A sessão multijogador cujo acesso exige a validação do Código de Pareamento pelo dispositivo convidado antes de ingressar.
_Avoid_: Sala protegida, sala com senha, private room, sala fechada

**Código de Pareamento**:
O identificador visual (Código QR) ou numérico (4 dígitos) exibido pelo dispositivo anfitrião para conexão direta pelo convidado.
_Avoid_: Pin, senha, room code, token, chave de acesso

**Largada Sincronizada**:
O início de uma partida em Mesa Estendida, comandado pelo dispositivo anfitrião somente depois que os dois aparelhos sinalizam que estão prontos, com a mesma contagem regressiva exibida em ambos.
_Avoid_: Start, início automático, kickoff

**Sessão Multijogador**:
O ciclo de vida unificado de uma partida multijogador em rede local (Mesa Estendida ou Mesa Conectada) que abrange o anúncio de presença, pareamento com validação de modalidade, largada sincronizada direta a partir do lobby com vão físico padrão, transmissão contínua de telemetria e encerramento coordenado da conexão entre os dispositivos.
_Avoid_: Room, partida online, socket session, tela de calibração manual

**Descoberta Unificada de Mesas**:
O mecanismo de escaneamento local (via mDNS/NSD e beacon UDP) no qual o dispositivo convidado visualiza todas as mesas ativas na rede local com seus respectivos selos de modalidade (Mesa Estendida, Mesa Conectada ou Modo IA x IA), adaptando automaticamente sua interface, calibração e fluxo de entrada às regras estabelecidas pelo dispositivo anfitrião.
_Avoid_: Busca por modo restrito, filtro cego de lobby, salas isoladas por tela

**Expiração de Mesas Descobertas**:
O mecanismo de higienização contínua executado no dispositivo convidado que remove automaticamente mesas da lista do lobby caso nenhum beacon periódico seja recebido dentro da janela de tolerância (TTL de 7 segundos), prevenindo a exibição de salas inativas decorrentes de encerramento abrupto ou oscilações de sinal.
_Avoid_: Mesa fantasma, lista estática, salas órfãs, cache infinito de mesas

**Aviso de Encerramento de Mesa**:
A mensagem em broadcast UDP disparada imediatamente pelo dispositivo anfitrião ao cancelar a criação da mesa ou fechar a aplicação, notificando os dispositivos convidados no lobby para que expurguem a entrada correspondente de forma instantânea sem aguardar a expiração por tempo limite.
_Avoid_: Goodbye packet, tombstone, pacote de cancelamento, sinal de saída

**Ponto de Acesso Móvel**:
O recurso sem fio do dispositivo anfitrião que emite uma rede Wi-Fi local direta para conexão do dispositivo convidado sem requerer roteador externo.
_Avoid_: Roteador do telefone, hotspot pessoal, ancoragem, tethering

**Árbitro Virtual**:
O componente central de autoridade esportiva responsável por governar a linha do tempo determinística da partida em ritmo real (contagem regressiva, pausas, cronômetro e disco preso), validar impactos, coordenar a inteligência artificial e registrar pontos, desacoplado da taxa de quadros do aparelho.
_Avoid_: Engine, juiz, supervisor, fiscal

**Previsão no Cliente**:
A projeção imediata e soberana do rebatedor no dispositivo convidado durante o toque ativo local (blindada contra retrocessos por pacotes defasados em RTT), combinada à extrapolação cinemática contínua a 120 Hz (*dead reckoning*) da trajetória do disco, reflexão elástica nas tabelas e interpolação do rebatedor adversário entre pacotes UDP de telemetria, conciliada suavemente com o estado autoritativo do dispositivo anfitrião.
_Avoid_: Client-side prediction, toque instantâneo, lag compensation

**Telemetria de Colisão**:
A transmissão de eventos de impacto e intensidade física no pacote contínuo emitido pelo dispositivo anfitrião para sincronização simultânea de áudio e vibração no dispositivo convidado.
_Avoid_: Pacote de som, broadcast de áudio, sync de áudio

## Interface e Apresentação

**Ícone de Mesa Estendida**:
A representação gráfica oficial do aplicativo na loja e no lançador exibindo dois dispositivos móveis dispostos lado a lado com o disco flamejante cruzando o vão físico eletrizado, adotada como licença visual comercial para maximizar a proporção quadrada 1:1.
_Avoid_: Ícone simples, logo padrão, ícone genérico

**Título Arcade Volumétrico**:
O letreiro visual oficial do jogo estilizado em chanfro metálico cromado, labaredas de plasma em "AIR", raios elétricos ciano em "HOCKEY" e o subtítulo "ULTIMATE CHAMPIONSHIP", renderizado como cabeçalho gráfico no menu principal.
_Avoid_: Título em texto puro, texto do menu, logo estático

**Miniatura da Mesa** (Descontinuado no Menu Inicial):
A antiga representação esquemática da arena desenhada em Canvas nos cartões do menu principal, descontinuada no redesenho para eliminar poluição visual e priorizar a legibilidade tipográfica dos modos de jogo e a visibilidade do Disco Flamejante.
_Avoid_: Ícone de campo, desenho do modo, preview da mesa

**Cartão de Modo de Jogo**:
O componente interativo translúcido no menu inicial com bordas chanfradas neon que apresenta o título destacado e a descrição sucinta de cada modalidade esportiva (Modo Fases, Mesa Conectada, Mesa Estendida, Mesa Dividida e Modo IA x IA), projetado com perfil rebaixado e glassmorphism escuro para preservar a visibilidade do Disco Flamejante ao fundo.
_Avoid_: Botão comum, card opaco, menu tile, lista padrão

**Aviso de Reinício para Atualização**:
O cartão interativo no menu principal estilizado em vidro escuro com indicador luminoso verde esmeralda e cantos chanfrados que notifica o jogador sobre o download concluído de uma nova versão do aplicativo pronta para instalação imediata.
_Avoid_: Card de update, banner de reinício, update snackbar, restart card

**Menu de Pausa da Arena**:
O painel modal translúcido com bordas chanfradas neon acionado durante a partida para suspender temporariamente a disputa, alternar ajustes rápidos de áudio e música, reiniciar o confronto ou retornar ao menu principal. Em sessões multijogador em rede (Mesa Estendida e Mesa Conectada), a ativação e a liberação da pausa são simétricas e coordenadas entre o Dispositivo Anfitrião e o Dispositivo Convidado, garantindo que o destravamento por qualquer jogador sincronize a Retomada com Contagem Regressiva em ambos os aparelhos.
_Avoid_: Pause menu, tela de pausa, popup de pause, pause dialog

**Diálogo de Fim de Partida**:
A interface modal translúcida com bordas chanfradas neon exibida ao término de confrontos individuais ou multijogador, apresentando o participante vencedor, o placar final e opções de revanche ou saída para o menu.
_Avoid_: Game over, tela de vencedor, match end popup, fim de jogo

**Contêiner Responsivo de Menus**:
O envoltório de layout centralizado aplicado às telas de navegação (Menu Principal, Lobby, Configurações, Fases e Placar de Líderes) que impõe um limite de largura máxima ergonômico (até 520 dp), preservando a estética de painel arcade e evitando o estiramento horizontal excessivo de cartões e botões em tablets e dispositivos dobráveis abertos (Galaxy Fold).
_Avoid_: Layout esticado, tela cheia em tablet, cards estendidos sem limite

**Bordas Chanfradas Neon**:
O traçado perimetral da mesa com cantos angulados em 45 graus e gradiente luminoso em neon ciano e laranja, substituindo cantos arredondados simples.
_Avoid_: Borda redonda, moldura do campo, cantos da mesa

**Rastro de Energia do Disco**:
O efeito visual fluido de plasma emitido atrás do disco em alta velocidade, amostrado por deslocamento espacial mínimo entre quadros para evitar repetições sob latência de rede, cuja tonalidade cromática reflete o último rebatedor a golpeá-lo (ciano para o Jogador 1 e laranja para o Jogador 2).
_Avoid_: Fogo da bola, rastro da bola, rastro de fumaça, cauda do puck

**Vórtice de Contagem Regressiva**:
A animação central composta por anéis de energia concêntricos e pulsantes que circundam os números da contagem regressiva no início e reinício da partida.
_Avoid_: Círculo do countdown, relógio central, animação de início

**Celebração de Ponto**:
O efeito de impacto visual e partículas incandescentes desencadeado quando o disco cruza a meta adversária, iluminando a arena com faíscas e ondas de choque na cor do marcador.
_Avoid_: Animação de gol, comemoração de gol, goal fx

**Onda de Choque Radial**:
O efeito visual luminoso em formato de anel com feixes de plasma expansivos e raios irradiados emitido no centro da mesa ou na meta durante a contagem regressiva e celebração de ponto.
_Avoid_: Onda de impacto, explosão de gol, círculo com raios, choque neon

**Perfil de Efeitos Visuais**:
A configuração gráfica ajustável pelo usuário nas configurações que ativa ou desativa efeitos visuais em partida nas arenas 2D e 3D — incluindo rastro de energia do disco, vórtice de contagem regressiva, ondas de choque, faíscas incandescentes e o bloom do Brilho do Neon em jogo, preservando a intensidade salva para quando os efeitos forem reativados ou pré-visualizados na vitrine.
_Avoid_: Gráficos, qualidade gráfica, resolução, modo batata, modos de desempenho

**Música de Fundo Arcade**:
A trilha sonora temática instrumental em estilo synthwave sintetizada proceduralmente e reproduzida em loop contínuo durante as partidas, com controle independente de volume em relação aos efeitos sonoros.
_Avoid_: Trilha sonora estática, música mp3, som do jogo

**Modo Imersivo de Partida**:
A ocultação estrita das barras de navegação do sistema operacional e da barra de status durante a partida na arena para blindar a área de jogo contra toques acidentais nos controles do aparelho.
_Avoid_: Fullscreen comum, tela cheia simples, modo sem borda

**Contador de FPS** (Descontinuado):
O antigo indicador na arena que exibia a taxa de quadros por segundo entregue pelo motor de renderização, removido na preparação para publicação na loja.
_Avoid_: Frame counter, medidor de taxa, taxa de quadros, medidor de fps

## Customização e Temas Visuais

**Tema de Mesa**:
A estilização visual, paleta cromática, iluminação de fundo e demarcações táticas aplicadas proceduralmente à arena retangular de jogo, preservando dimensões físicas e proporções de colisão.
_Avoid_: Campo, skin de mesa, textura de chão, fundo de campo

**Aparência de Disco**:
A estilização visual do disco que define o relevo geométrico central, o anel de contenção de plasma e as partículas do rastro de energia emitido em alta velocidade.
_Avoid_: Skin de bola, bola diferente, sprite de puck, skin de puck

**Aparência de Rebatedor**:
A composição cosmética e de relevo pseudo-3D do acessório de controle do jogador, incluindo manopla ergonômica, textura de aderência e halo luminoso de impacto.
_Avoid_: Skin de batedor, skin de raquete, batedor customizado

**Conjunto Temático Esportivo**:
A coleção integrada pré-configurada que combina Tema de Mesa, Aparência de Disco e Aparência de Rebatedor sob uma mesma identidade esportiva retro-futurista (ex: Cyber Futebol, Neo-Tokyo Basquete, Vôlei Gravitacional, Gridiron Sci-Fi e Beisebol Cibernético).
_Avoid_: Kit de time, pacote de skins, theme pack, bundle de campo

**Conjunto Temático Kiara Cat**:
A coleção integrada de identidade felina arcade composta pela Mesa Kiara Cat (`KIARA_CAT`), Aparência de Disco Patinha Neon (`KIARA_PAW`) e Aparência de Rebatedor Neko Striker (`NEKO_STRIKER`), configurada como Livre por Padrão para uso imediato em todas as modalidades de jogo sem vínculo com a progressão do Modo Fases.
_Avoid_: Tema de gatinho avulso, kit kiara simples, skin fofa, Kiki (utilizar canonicamente Conjunto Temático Kiara Cat)

**Desbloqueio Progressivo de Arenas**:
O sistema de progressão esportiva vinculado ao Modo Fases que mantém as arenas temáticas bloqueadas até que o jogador conclua o respectivo Bloco de Fases Temático de 10 níveis, mantendo a Mesa Clássica permanentemente acessível desde o primeiro momento.
_Avoid_: Unlock de skins, compras de fases, liberação paga

**Conquista de Arena**:
O marco competitivo oficial registrado quando o jogador vence a 10ª fase de um bloco temático no Modo Fases (ex: Fases 10, 20, 30... até 100), liberando em definitivo o uso global daquela arena e de seu respectivo Conjunto Temático Esportivo para criação de partidas e customização.
_Avoid_: Compra de arena, skin ganha, prêmio de fase

**Autoridade de Criação Temática**:
A diretriz de conectividade em sessões multijogador na qual apenas o Dispositivo Anfitrião necessita ter a Conquista de Arena correspondente para abrir a mesa de jogo, habilitando a participação imediata do Dispositivo Convidado naquela arena mesmo que este ainda não a tenha desbloqueado em sua campanha individual.
_Avoid_: Restrição cega de sala, trava de convidado, lobby bloqueado

**Sincronização de Arena do Anfitrião**:
A transmissão do identificador de Tema de Mesa ativo enviada pelo Dispositivo Anfitrião ao Dispositivo Convidado no handshake de abertura da sessão multijogador, resolvendo o **Kit Cosmético da Arena** completo (Mesa, Disco Temático e Rebatedores P1/P2) de forma determinística a partir do `ArenaKit`, garantindo paridade visual absoluta entre anfitrião e convidado independentemente das preferências locais ou do progresso individual do convidado.
_Avoid_: Sync de skin, pacote de campo, tema compartilhado

**Seletor Pré-Jogo de Arenas**:
A interface de confirmação exibida antes da inicialização de partidas locais (P1 x P2 Split-Screen ou IA x IA) ou da abertura de mesas multijogador pelo Anfitrião, permitindo inspecionar as arenas conquistadas com pré-visualização interativa (2D/3D), alternar a dimensão gráfica e confirmar o palco do confronto.
_Avoid_: Tela de skin, menu de mapa, pré-jogo avulso

**Selo de Bloqueio Minimalista**:
A representação visual discreta aplicada aos itens cosméticos e arenas ainda não conquistados, constituída exclusivamente pelo ícone vetorial de cadeado, postergando detalhes de nível e instruções de desbloqueio para o diálogo interativo disparado ao toque.
_Avoid_: Badge com texto de fase, selo com número, tarja de bloqueio

**Câmera 3D Zenital**:
O enquadramento tridimensional ortogonal calibrado a 90° acima do centro da mesa no Modo Tela Dividida (Split Screen) com suporte aos Níveis de Enquadramento 3D, que projeta os modelos 3D volumétricos (disco, rebatedores e arena) e iluminação dinâmica PBR com simetria geométrica absoluta, preenchimento otimizado da tela e sem distorção angular de perspectiva entre os dois competidores locais no mesmo aparelho.
_Avoid_: Câmera inclinada, visão torta, 3D assimétrico, top-down fake

## Competição e Classificação

**Central de Estatísticas & Histórico**:
A interface dedicada no aplicativo que consolida as estatísticas acumuladas da carreira do jogador (Pontuação Elo, taxa de vitórias, saldo de gols, sequência de vitórias) e o Registro de Partida detalhado das últimas 50 disputas locais, integrando o botão de lançamento dos Placares Globais da Google Play.
_Avoid_: Tela de ranking local, placar local, tabela de líderes, ranking tab

**Placar Global da Google Play**:
O sistema oficial de classificação competitivo mundial e entre amigos provido pelo Google Play Games Services v2, no qual as pontuações oficiais (Pontuação Elo e Conquistas de Fases) são sincronizadas e exibidas na sobreposição nativa em nuvem, condicionado à autorização prévia das impressões digitais de certificado (SHA-1) e da lista de testadores cadastrados no Google Play Console.
_Avoid_: Ranking interno, ranking próprio, tabela web, leaderboard do servidor, card da play game

**Pontuação Elo**:
A pontuação numérica dinâmica atribuída a cada perfil (iniciada em 1.000 pontos) ajustada ao final de cada partida com base na probabilidade matemática de vitória e nível do oponente.
_Avoid_: MMR, rank points, troféus, score

**Registro de Partida**:
A entrada histórica imutável gravada no banco de dados local contendo os participantes, modo de jogo, placar final, duração e variação de Elo.
_Avoid_: Histórico, log de jogo, match log, súmula

**Abas de Classificação Local** (Descontinuado):
As antigas abas da tela de classificação que tentavam ranquear jogadores locais no mesmo aparelho, descontinuadas em favor da Central de Estatísticas & Histórico pessoal e da delegação da liderança competitiva ao Placar Global da Google Play.
_Avoid_: Abas de ranking, top players local, listas de jogadores offline

## Monetização e Infraestrutura Web

**Anúncio em Banner**:
O formato publicitário retangular ancorado de forma estática e fixa no rodapé de todas as telas de navegação (Menu Principal, Configurações, Placar de Líderes, Seleção de Fases, Customização e Lobby Multijogador), garantindo que nunca role junto com listas e conteúdos verticais, com supressão estrita durante a partida na arena e durante o escaneamento ativo de câmera no leitor QR.
_Avoid_: Propaganda, banner ad, anúncio de rodapé rolante

**Anúncio Intersticial de Pós-Partida**:
O anúncio de tela cheia disparado no encerramento de confrontos, protegido por temporizador de respiro (cooldown de 3 minutos) e contagem mínima de partidas para evitar intrusão, com supressão estrita na revanche e no botão de nova tentativa imediata após derrota no Modo Fases para blindar a retenção do jogador contra interrupções frustrantes.
_Avoid_: Pop-up, interstitial, comercial pós-jogo

**Anúncio Recompensado** (Pausado na Interface Principal):
O formato publicitário assistido por decisão do usuário em troca de recompensas temporárias, descontinuado temporariamente da interface inicial enquanto a loja de customizações e skins está em desenvolvimento.
_Avoid_: Pop-up, anúncio invasivo, propaganda fixa

**Remoção Permanente de Anúncios**:
O produto digital não consumível adquirido na Google Play Store que suprime em definitivo anúncios em banner e intersticiais na conta do jogador.
_Avoid_: Versão paga, premium, versão vip, assinatura, compra pro, pacote ad-free

**Portal Oficial**:
O sítio institucional estático hospedado no Cloudflare Pages contendo a apresentação do jogo, documentação legal de privacidade e arquivo de validação `app-ads.txt`.
_Avoid_: Site, homepage, web portal

**Trava de Segurança de Teste Fechado**:
A diretriz de proteção de conta que mantém os identificadores de blocos de anúncios roteados estritamente para os blocos de teste oficiais do Google AdMob durante o teste fechado de 14 dias do Google Play Console, prevenindo deduções e suspensão por tráfego inválido.
_Avoid_: Anúncio real em teste, modo produção antecipado, auto-clique

**Roteamento de Anúncios por Ambiente**:
A política arquitetural (`USE_PRODUCTION_ADS`) que serve blocos comerciais de anúncios exclusivamente após o lançamento público em produção, isolando builds de depuração e testes fechados em ambiente sandbox.
_Avoid_: Ad toggle manual, hardcoded ads, anúncio fixo

**Internacionalização do Placar**:
A padronização visual e textual do Placar de Líderes regida por recursos de strings (`strings.xml` nos idiomas en, pt-BR e es) e iconografia vetorial limpa do Material Design, sem dependência de glifos de emoji unicode.
_Avoid_: Emoji no ranking, strings hardcoded, placar fixo em português

**Internacionalização do Portal Oficial**:
A apresentação multilíngue do sítio estático e da política de privacidade nos 3 idiomas suportados (Inglês, Português e Espanhol), com carregamento padrão em Inglês, persistência em armazenamento local do navegador e seletor no cabeçalho.
_Avoid_: Site somente em português, landing page estática monolíngue

**Botão Oficial de Loja**:
O botão de chamada para ação (CTA) principal do Portal Oficial que direciona os jogadores à Google Play Store oficial (`dev.vmellos.airhockey`) com o logotipo vetorial de 4 cores do Google Play, suprimindo links manuais de APK e repositório GitHub.
_Avoid_: Download de APK direto, link para GitHub, release button

**Capturas Oficiais de Loja**:
O conjunto diagramado de imagens verticais de alta conversão (1080 x 2340 px, proporção 19.5:9) para a ficha do aplicativo na Google Play Store, apresentando mockups 3D realistas de smartphones com chassi de titânio escuro, iluminação radial neon, grade tecnológica e cabeçalhos tipográficos traduzidos nos 3 idiomas suportados (Português, Inglês e Espanhol).
_Avoid_: Prints soltos, telas brutas, screenshots sem tratamento, capturas 2D antigas

**Imagem de Destaque**:
A peça gráfica panorâmica oficial de topo de ficha na Google Play Store (1024 x 500 px, proporção 2:1), combinando o Título Arcade Volumétrico ("AIR HOCKEY 2P"), o Disco Flamejante e a representação de dispositivos em Mesa Estendida com Meia-Mesa 3D e pílulas de recursos multilíngues em alta conversão.
_Avoid_: Foto de destaque, banner comum, capa do jogo, feature graphic genérica

## Áudio Procedural e Acústica Temática

**Síntese Procedural Temática**:
O mecanismo matemático de geração contínua de áudio PCM mono (22.050 Hz, 16 bits) que sintetiza trilhas de fundo (BGM) e efeitos físicos (SFX) sem inclusão de arquivos estáticos pesados no APK, mantendo a pegada ultraleve da aplicação.
_Avoid_: Arquivos MP3 pesados, áudio estático gravado, sound pack externo

**Acústica Temática de Colisão**:
O perfil sonoro individualizado para impactos do disco contra os rebatedores e contra as bordas perimetrais, adaptado aos materiais e identidade acústica de cada uma das 11 arenas temáticas (como estalos secos de taco, ressonância oca em piso de madeira, choques de escudo de bronze, desvios gravitacionais e ondas quadradas vintage).
_Avoid_: Som genérico de colisão, mesmo bipe para todas as mesas

**Progressão Harmônica de Pontuação**:
O chime sonoro de celebração executado a cada gol com taxa de afinação (pitch scale) ascendentemente modulada em tempo real conforme a pontuação aproxima-se do alvo da partida (`targetScore`), gerando crescente clímax musical.
_Avoid_: Tom fixo monótono de pontuação, bipe repetitivo

**Fanfarra Temática de Ponto Pró**:
O efeito sonoro triunfante e celebratório exclusivo sintetizado para cada arena quando o jogador marca um ponto na meta adversária.
_Avoid_: Som de gol genérico, apito padrão

**Alarme Temático de Ponto Contra**:
O sinal sonoro descendente ou aviso de falha de contenção exclusivo sintetizado para cada arena quando o jogador sofre um gol em sua meta.
_Avoid_: Buzzer padrão, som estático de derrota

**Equipamento Direto em Toque Único**:
O padrão de interação na tela de customização em que tocar em qualquer card de conjunto, mesa, disco ou rebatedor equipa e persiste a seleção instantaneamente com retorno tátil, suprimindo botões intermediários de confirmação e atualizando a moldura luminosa e indicador de seleção ativa em tempo real.
_Avoid_: Botão de equipar separado, modal de confirmação de skin, clique duplo para equipar

## Elementos Visuais em Jogo e Acesso Rápido

**Painel de Ponto Marcado Temático**:
O componente de sobreposição visual exibido no centro da mesa ao marcar um ponto, cujos formatos de moldura, paleta de cores, tipografia, brilho neon e efeitos gráficos adaptam-se dinamicamente à identidade visual da arena ativa.
_Avoid_: Banner estático genérico, aviso de gol padrão sem tema

**Contagem Regressiva Temática**:
A composição gráfica e tipográfica dos números da contagem (3, 2, 1) e do comando inicial ("VAI" / "GO") estilizada com as cores primárias, brilho de contorno e sombras decorativas da arena selecionada.
_Avoid_: Números simples monocromáticos sem estilo

**Placar Lateral Temático**:
A moldura do mostrador de pontuação fixada na borda lateral direita da mesa, personalizada com o acabamento de superfície, gradientes de borda e estilo numérico condizentes com o tema da arena ativa.
_Avoid_: Placar fixo transparente genérico

**Acesso Rápido de Áudio**:
O diálogo modal acionado pelo botão de volume do Menu Principal que centraliza com exclusividade a alternância e ajuste de volume dos efeitos sonoros, música de fundo e resposta tátil do aplicativo, eliminando duplicidade de controles na Tela de Configurações.
_Avoid_: Apenas alternar mudo, controle escondido em menus profundos, áudio nas configurações

**Tela de Configurações**:
A interface de ajustes globais do sistema dedicada estritamente ao gerenciamento de conta e jogabilidade técnica (Remoção Permanente de Anúncios, Perfil do Jogador, Idioma, Regras de Jogo / IA, Ergonomia de Toque e Perfil de Efeitos Visuais), desacoplada das preferências cosméticas de temas e preferências sensoriais de áudio/vibração concentradas no Menu Principal.
_Avoid_: Config, tela de opções geral, menu de temas

**Nomenclatura Concisa de Abas de Customização**:
O vocabulário padronizado para as abas na tela de personalização (`Kits`, `Mesas`, `Discos`, `Tacos` em PT e `Mazos` em ES), garantindo legibilidade em qualquer densidade de tela sem quebra ou compressão visual.
_Avoid_: Rebatedores espremido, strings longas em abas fixas

**Layout Bicolunar em Modo Paisagem**:
A organização espacial responsiva em duas colunas paralela e simétrica adotada nas telas de navegação (Menu Principal, Customização da Arena e Seletor Pré-Jogo de Arenas) quando a aplicação é executada na orientação horizontal (landscape), isolando os elementos volumétricos visuais (como o Título Arcade Volumétrico ou a Vitrine de Pré-Visualização 3D) na coluna esquerda e os fluxos de ação (cartões de modos, abas de itens e botões primários) na coluna direita, prevenindo o colapso e corte vertical de elementos em smartphones.
_Avoid_: Coluna única esticada em paisagem, tela quebrada em horizontal, scroll único achatado

**Calhas Laterais da Arena**:
Os espaços livres e escurecidos (letterbox) gerados automaticamente à esquerda e à direita da Mesa em telas ultrawide ou em smartphones na orientação horizontal (landscape), aproveitados ergonomicamente para ancoragem externa do Placar Lateral e botão de pausa sem invadir ou cobrir a superfície de jogo e os trilhos da arena virtual.
_Avoid_: Borda preta vazia, área morta, letterbox simples

## Regras Esportivas, Inteligência Artificial e Renderização

**Disco Preso**:
Regra esportiva oficial arbitrada pelo Árbitro Virtual que detecta quando o disco permanece inerte por mais de 3,5 segundos na meia-mesa controlada por inteligência artificial, no Modo IA x IA ou no Vão Físico da Mesa Estendida, reposicionando-o em local jogável sem interferir em discos parados no meio-campo alcançável por jogador humano.
_Avoid_: Deadlock, disco travado, bug de física, bola presa

**Resgate de Canto**:
Manobra tática de desempacamento executada pelo rebatedor autônomo ao detectar o disco em baixa velocidade nos cantos profundos de sua área defensiva, contornando o disco para projetá-lo de volta ao meio-campo sem expor a própria meta.
_Avoid_: Destravamento de IA, saída de escanteio, manobra de canto

**Curva Gradual de Dificuldade**:
A progressão paramétrica contínua e suave da inteligência artificial ao longo dos 100 níveis do Modo Fases, interpolando velocidade máxima, tempo de reação humana, dispersão de mira e propensão a tabelas sem saltos abruptos entre categorias.
_Avoid_: Dificuldade em bloco, salto de IA, degrau de fases

**Salvaguarda de Autogol**:
O filtro vetorial preventivo do controlador autônomo que atenua a aceleração e contorna o disco quando este se encontra entre o rebatedor e a própria meta, impedindo rebatidas acidentais contra as próprias redes.
_Avoid_: Gol contra de IA, frenesi defensivo, rebatida cega

**Sincronização Cosmética em Nuvem**:
A extensão do payload do Google Play Games Services v2 que persiste em nuvem a identidade competitiva e o equipamento visual ativo do atleta (nome, temas de arena, discos e rebatedores), mantendo calibrações de hardware e áudio no armazenamento local.
_Avoid_: Cloud save total, backup cego de datastore

**Curva Gama de Bloom**:
O mapeamento não linear quadrático de intensidade com patamar HDR ativo no motor Filament 3D, restringindo a emissão luminosa aos elementos incandescentes neon e assegurando visual arcade nítido sem ofuscamento ou superexposição.
_Avoid_: Bloom estourado, saturação aditiva cega, tela branca

**Auto-Rolagem de Fases**:
O mecanismo de ancoragem inteligente na grade de seleção de fases que localiza a fase ativa mais recente desbloqueada e rola suavemente a visão até sua posição ao abrir a interface, eliminando o esforço de rolagem manual desde o primeiro nível.
_Avoid_: Scroll manual de fases, reset de topo de grid

**Desacoplamento de Skin em Fases**:
A regra esportiva visual da campanha solo que acopla o disco temático do palco em disputa (`ArenaKit`) e atribui ao oponente autônomo (Boss da Arena) o rebatedor temático da fase, enquanto permite que o rebatedor do jogador (P1) preserve sua aparência personalizada não-padrão quando equipada para o tema correspondente.
_Avoid_: Forçar skin da fase, taco mutável, perda de customização

**Partículas Temáticas da Arena**:
O sistema de efeitos visuais de colisão (ondas de choque, faíscas incandescentes, explosões de gol e rastro de energia) cujas cores no motor 2D e 3D Filament são extraídas dinamicamente da paleta esportiva da arena ativa, eliminando tons genéricos fixos.
_Avoid_: Faíscas monocromáticas, efeitos fixos azul e vermelho

**Design System Cyber-Obsidian**:
O padrão de interface gráfica padronizado adotado nas telas de navegação, diálogos e botões, composto por superfícies escuras Obsidian (`#070B14`), glassmorphism translúcido, acentos luminosos em Ciano Elétrico (`#00E5FF`) e Ouro Arcade (`#FFB800`), e molduras chanfradas elegantes.
_Avoid_: Cores primárias genéricas, botões vermelho e azul estridentes

## Módulos Arquiteturais Profundos e Interfaces de Sistema

**Especificação de Partida (`MatchSpec`)**:
O contrato imutável construído na abertura da sessão esportiva que consolida modalidade, Vão Físico, Pontuação Alvo, limites cinemáticos do Disco, tema da Mesa e identidade local dos equipamentos de Disco e Rebatedor por Aparelho.

**Capacidades da Modalidade (`ModeCapabilities`)**:
O módulo declarativo de regras por `GameMode` que determina perspectiva da Mesa (`TablePerspective`), política de registro de placar (`StatsPolicy`), controle local/autônomo e papéis de rede sem ramificações espalhadas pela interface.

**Porta de Execução da Arena (`ArenaRuntime`)**:
A interface estável observada pelos renderizadores 2D/3D e pelo HUD de partida para leitura instantânea de estado (`StateFlow<MatchState>`), captura de snapshot físico, drenagem de eventos de colisão e submissão de comandos do Rebatedor e pausa.

**Catálogo de Presets e Kits da Arena (`ArenaPresetCatalog` / `ArenaKitCatalog`)**:
Os dois módulos complementares de identidade visual e acústica: `ArenaPresetCatalog` provê em Kotlin puro os parâmetros acústicos, tiers da Conquista da Arena e presets canônicos de cada Mesa; `ArenaKitCatalog` concentra paletas 2D, recursos 3D (.glb) da Mesa, Disco, Rebatedor e estilos de sobreposição com liberdade local de escolha entre Aparelhos.

**Base Vetorial da Câmera 3D (`CameraBasis3D`)**:
A estrutura imutável pré-calculada por quadro em `Table3DCameraProjection` que reaproveita seno/cosseno de pitch/yaw e proporção de tela entre a projeção de partículas, o rastro do Disco e a desprojeção de toque para a Mesa.

**Portas de Descoberta, Snapshot em Nuvem e Áudio (`LanDiscoveryPort`, `CloudSnapshotPort`, `AudioPlaybackPort`)**:
Os pontos de acoplamento estreitos (*seams*) que isolam respectivamente a descoberta em rede local (`TableDiscoveryRegistry` com expiração em TTL de 7 segundos), o sincronismo atômico de progresso em nuvem (`PlayGamesSaveManager` em transação única de banco Room e DataStore) e a reprodução de áudio cuja síntese WAV executa integralmente fora da thread principal.







