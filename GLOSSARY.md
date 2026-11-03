# Air Hockey

Air Hockey é um jogo arcade multijogador em tempo real executado localmente em um ou dois dispositivos móveis.

## Entidades de Jogo

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
O intervalo físico de separação correspondente às molduras e bordas dos dois aparelhos colocados em contato.
_Avoid_: Bezel, bezel gap, borda plástica, gap

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

**Modo Espectador**:
O estado operacional da interface e dos controles acionado durante o Modo IA x IA no qual o toque manual sobre a mesa é desativado em favor da simulação autônoma dos robôs, permitindo ao usuário acompanhar o duelo e interagir apenas através do menu de pausa.
_Avoid_: Tela travada, modo observador, bot view

**Retomada com Contagem Regressiva**:
O intervalo preparatório de 3 segundos disparado ao despausar a partida antes de restabelecer o movimento físico do disco.
_Avoid_: Unpause countdown, re-start, contagem de despause

**Posicionamento Preparatório**:
A movimentação tática livre do rebatedor permitida aos participantes em seu próprio meio-campo durante a contagem regressiva enquanto o disco permanece imóvel na saída.
_Avoid_: Pre-match movement, warm-up move, ajuste pré-jogo

**Saque de Início**:
O posicionamento do disco na metade da mesa do jogador que detém a posse de saída no começo da partida e após cada ponto marcado, permitindo a tacada inicial imediata sem estagnação.
_Avoid_: Tiro de saída, início parado, saque neutro

**Mira Tática Anti-Oponente**:
A análise posicional contínua executada pela inteligência artificial sobre as coordenadas do rebatedor adversário para direcionar o golpe deliberadamente no lado desprotegido da meta rival.
_Avoid_: Tiro mirado, mira automática, exploit de IA

## Modos de Jogo

**Modo Solo**:
Partida disputada por um jogador humano contra um oponente controlado por inteligência artificial em um único dispositivo.
_Avoid_: Modo treino, single player, vs bot

**Modo Fases**:
A modalidade de progressão do Modo Solo estruturada em uma jornada de 100 níveis sequenciais com dificuldade gradativa e metas de vitória, na qual cada triunfo concede estrelas de desempenho e destrava o estágio seguinte.
_Avoid_: Modo campanha, stage mode, levels, fases infinitas

**Estrelas de Desempenho**:
A classificação de 1 a 3 estrelas concedida ao concluir uma fase no Modo Fases, avaliando a eficiência defensiva do jogador pelo número de pontos cedidos ao adversário.
_Avoid_: Stars, rating de fase, nota da fase

**Tela de Vitória da Fase**:
A interface de encerramento exibida após superar uma fase no Modo Fases, destacando as estrelas conquistadas e os controles de avanço direto (Lista de Fases, Reiniciar e Próxima Fase).
_Avoid_: Win screen, tela de parabéns, resultado da fase

**Disco Flamejante**:
O elemento gráfico e animado de destaque na tela inicial, apresentando o disco metálico envolto por labaredas de plasma posicionado como plano de fundo atrás dos modos de jogo.
_Avoid_: Flaming puck, fogo de fundo, bola de fogo, puck animado

**Mesa Dividida**:
Partida para dois jogadores no mesmo dispositivo físico, dividindo a tela da mesa ao meio.
_Avoid_: Modo local compartilhado, tela dividida, pass-and-play

**Mesa Estendida**:
Partida para dois jogadores em dois dispositivos físicos dispostos em Disposição Topo-a-Topo, formando uma única mesa contínua onde o disco transita entre as telas.
_Avoid_: Dual screen, tela dupla, big board

**Mesa Conectada**:
Partida para dois jogadores em dois dispositivos físicos pareados via rede sem fio, na qual cada aparelho projeta a mesa inteira e sincroniza os movimentos em tempo real.
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
O ciclo de vida unificado de uma partida em Mesa Estendida que abrange o pareamento, calibração do vão físico, largada sincronizada, transmissão contínua de telemetria e encerramento coordenado da conexão entre os dispositivos.
_Avoid_: Room, partida online, socket session

**Ponto de Acesso Móvel**:
O recurso sem fio do dispositivo anfitrião que emite uma rede Wi-Fi local direta para conexão do dispositivo convidado sem requerer roteador externo.
_Avoid_: Roteador do telefone, hotspot pessoal, ancoragem, tethering

**Árbitro Virtual**:
O componente central de regras responsável por validar impactos, registrar pontos e controlar o cronômetro da partida.
_Avoid_: Engine, juiz, supervisor, fiscal

**Previsão no Cliente**:
A projeção imediata do rebatedor no dispositivo convidado em resposta ao toque local, conciliada subsequentemente com a telemetria do dispositivo anfitrião.
_Avoid_: Client-side prediction, toque instantâneo, lag compensation

**Telemetria de Colisão**:
A transmissão de eventos de impacto e intensidade física no pacote contínuo emitido pelo dispositivo anfitrião para sincronização simultânea de áudio e vibração no dispositivo convidado.
_Avoid_: Pacote de som, broadcast de áudio, sync de áudio

## Interface e Apresentação

**Miniatura da Mesa**:
A representação esquemática reduzida da arena desenhada nos cartões do menu principal para ilustrar a disposição física de cada modo de jogo.
_Avoid_: Ícone de campo, desenho do modo, preview da mesa

**Bordas Chanfradas Neon**:
O traçado perimetral da mesa com cantos angulados em 45 graus e gradiente luminoso em neon ciano e laranja, substituindo cantos arredondados simples.
_Avoid_: Borda redonda, moldura do campo, cantos da mesa

**Rastro de Energia do Disco**:
O efeito visual fluido de plasma emitido atrás do disco em alta velocidade, cuja tonalidade cromática reflete o último rebatedor a golpeá-lo (ciano para o Jogador 1 e laranja para o Jogador 2).
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
A configuração gráfica ajustável pelo usuário nas configurações que ativa ou desativa o rastro de plasma do disco, o vórtice de contagem regressiva e as faíscas incandescentes, permitindo alternar entre a fidelidade visual arcade completa e a renderização limpa para máxima fluidez e economia de bateria.
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

## Competição e Classificação

**Placar de Líderes**:
O sistema de classificação competitivo que ranqueia os participantes em abas por modo de jogo, ordenando por pontuação Elo, vitórias, sequência invicta e saldo de gols.
_Avoid_: Hanking, ranking, tabela de pontos, leaderboard

**Pontuação Elo**:
A pontuação numérica dinâmica atribuída a cada perfil (iniciada em 1.000 pontos) ajustada ao final de cada partida com base na probabilidade matemática de vitória e nível do oponente.
_Avoid_: MMR, rank points, troféus, score

**Registro de Partida**:
A entrada histórica imutável gravada no banco de dados local contendo os participantes, modo de jogo, placar final, duração e variação de Elo.
_Avoid_: Histórico, log de jogo, match log, súmula

## Monetização e Infraestrutura Web

**Anúncio em Banner**:
O formato publicitário retangular fixado na base dos menus e telas de configuração, com supressão estrita durante a partida na arena.
_Avoid_: Propaganda, banner ad, anúncio de rodapé

**Anúncio Intersticial de Pós-Partida**:
O anúncio de tela cheia disparado no encerramento de confrontos, protegido por temporizador de respiro (cooldown de 3 minutos) e contagem mínima de partidas para evitar intrusão.
_Avoid_: Pop-up, interstitial, comercial pós-jogo

**Anúncio Recompensado**:
O vídeo publicitário assistido por decisão ativa do jogador em troca de benefícios tangíveis, como 2 horas de supressão completa de anúncios ou desbloqueio de estilos cosméticos.
_Avoid_: Rewarded ad, propaganda premiada, vídeo de bônus

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

