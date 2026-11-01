# 0009 - Perfil de Jogador, Partida Privada por Código e Isolamento de Descoberta

## Contexto e Decisão
Com múltiplos jogadores utilizando o Air Hockey na mesma rede local (Wi-Fi doméstico ou Ponto de Acesso Móvel), surgiram conflitos de conectividade:
1. Jogadores em busca de Mesa Conectada descobriam e entravam inadvertidamente em mesas configuradas como Mesa Estendida (que exigem calibração de vão físico e Disposição Topo-a-Topo).
2. Qualquer dispositivo na rede local conseguia entrar imediatamente em mesas anunciadas sem autorização prévia do anfitrião.
3. Não havia identificação humana dos participantes nem nas listas de descoberta nem no placar da partida.
4. Ao recriar sessões consecutivas de dois jogadores, resquícios de gatilhos assíncronos e vazamento de conexões fechadas provocavam transições imediatas sem pareamento e congelamento de tela.

Decidimos:
1. **Perfil de Jogador com Detecção de Nome do Dispositivo**: Introduzir o Perfil de Jogador persistido no DataStore de preferências, inicializado automaticamente via `Settings.Global.device_name` (com fallback para modelo do aparelho e "Jogador"). O nome é exibido no cabeçalho do Lobby, configurável nas Preferências e transmitido para identificação nas mesas e no placar em jogo.
2. **Isolamento de Descoberta por Modo de Jogo**: Estender o protocolo mDNS/NSD e os pacotes de beacon UDP para incluir expressamente o modo ativo (`EXTENDED` vs `CONNECTED`) e o nome do anfitrião. Convidados descobrem e visualizam apenas mesas correspondentes ao modo de jogo que estão buscando.
3. **Partida Privada com Código de Pareamento**: O Código de Pareamento de 4 dígitos atua como chave de acesso à sessão. Ao selecionar uma mesa descoberta na rede, o Convidado deve informar o código exibido na tela do Anfitrião; o handshake TCP valida o código e o modo antes de aceitar a conexão.
4. **Ciclo de Vida Determinístico e Consumo Atômico de Gatilhos**: Eliminar vazamentos entre sessões multijogador cancelando explicitamente coletores de corrotinas anteriores, fechando sockets síncronamente no encerramento e convertendo o gatilho de largada em evento de consumo único.
5. **Aprimoramentos de Apresentação e Áudio**: Implementar a demarcação gráfica neon da Área da Meta no piso da arena e sintetizar uma contagem regressiva harmônica arcade com decaimento exponencial (Dó5, Mi5, Sol5 e acorde Dó6), substituindo o ruído senoidal estático anterior.
