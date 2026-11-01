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
A abertura na extremidade do campo de cada jogador onde a entrada do disco concede um ponto ao adversário.
_Avoid_: Gol, caçapa, baliza, pocket

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
