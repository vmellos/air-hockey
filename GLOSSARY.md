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

**Jogada Lateral**:
O golpe angular aplicado no disco contra as bordas laterais da mesa com o objetivo de produzir ricochete veloz em direção à meta oposta.
_Avoid_: Bank shot, tabela, jogada de canto, ricochete intencional

**Velocidade do Disco**:
A taxa máxima de deslocamento admitida para o disco durante a partida, parametrizada por predefinições de ritmo de jogo.
_Avoid_: Rapidez da bola, velocidade do jogo, puck speed

**Empate**:
O resultado oficial de uma partida em que o tempo limite se esgota com pontuação idêntica entre os dois competidores, sem declaração de vitória.
_Avoid_: Tie, draw, igualdade, sem vencedor

**Retomada com Contagem Regressiva**:
O intervalo preparatório de 3 segundos disparado ao despausar a partida antes de restabelecer o movimento físico do disco.
_Avoid_: Unpause countdown, re-start, contagem de despause

**Posicionamento Preparatório**:
A movimentação tática livre do rebatedor permitida aos participantes em seu próprio meio-campo durante a contagem regressiva enquanto o disco permanece imóvel na saída.
_Avoid_: Pre-match movement, warm-up move, ajuste pré-jogo

## Modos de Jogo

**Modo Solo**:
Partida disputada por um jogador humano contra um oponente controlado por inteligência artificial em um único dispositivo.
_Avoid_: Modo treino, single player, vs bot

**Mesa Dividida**:
Partida para dois jogadores no mesmo dispositivo físico, dividindo a tela da mesa ao meio.
_Avoid_: Modo local compartilhado, tela dividida, pass-and-play

**Mesa Estendida**:
Partida para dois jogadores em dois dispositivos físicos dispostos em Disposição Topo-a-Topo, formando uma única mesa contínua onde o disco transita entre as telas.
_Avoid_: Dual screen, tela dupla, big board

**Mesa Conectada**:
Partida para dois jogadores em dois dispositivos físicos pareados via rede sem fio, na qual cada aparelho projeta a mesa inteira e sincroniza os movimentos em tempo real.
_Avoid_: Modo online, duelo sem fio, campo inteiro p2p, versus wifi, multiplayer normal

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

**Contador de FPS** (Descontinuado):
O antigo indicador na arena que exibia a taxa de quadros por segundo entregue pelo motor de renderização, removido na preparação para publicação na loja.
_Avoid_: Frame counter, medidor de taxa, taxa de quadros, medidor de fps
