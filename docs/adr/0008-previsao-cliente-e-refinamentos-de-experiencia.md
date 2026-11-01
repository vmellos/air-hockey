# 0008 - Previsão no Cliente, Fallback Híbrido e Refinamentos de Experiência

## Contexto e Decisão
Nos testes práticos dos modos multijogador com dois aparelhos em rede sem fio (Mesa Conectada e Mesa Estendida), foram identificados problemas críticos de usabilidade e arquitetura de rede:
1. O socket UDP truncava datagramas após ler pacotes menores (o `DatagramPacket.length` é mutado pela JVM no `receive`), descartando toda a telemetria seguinte.
2. O Dispositivo Convidado não possuía Previsão no Cliente para o seu próprio Rebatedor, dependendo exclusivamente do round-trip de rede para atualizar a renderização, gerando percepção de congelamento/travamento do Jogador 2. Em redes Wi-Fi ou pontos de acesso com descarte ou isolamento de portas UDP, não havia contingência.
3. A Calibração de Vão Físico era exibida erroneamente no modo Mesa Conectada (onde a mesa é inteira e não há junção de telas), e na Mesa Estendida não deixava claro o conceito de moldura física dos aparelhos.
4. O Disco atingia velocidades extremas sem possibilidade de ajuste de ritmo pelos jogadores, e o menu principal não ilustrava visualmente as diferenças entre os quatro modos.

Decidimos:
1. **Reset do Buffer UDP e Fallback Dinâmico TCP**: Restaurar `packet.length = buffer.size` a cada leitura no `UdpTelemetrySocket` e comutar dinamicamente para o canal de controle TCP caso o receptor passe mais de 150 ms sem receber datagramas UDP válidos.
2. **Previsão no Cliente com Reconciliação Suave**: O Dispositivo Convidado move o Rebatedor do Jogador 2 localmente de forma imediata (com restrição à sua metade da Mesa), reconciliando de maneira suave com a telemetria autoritativa recebida do Anfitrião.
3. **Fluxo Direto na Mesa Conectada e Calibração Didática na Mesa Estendida**: Eliminar a etapa de calibração na Mesa Conectada (iniciando o jogo diretamente) e adicionar ilustração esquemática topo-a-topo e botões de predefinição rápida (Fina, Padrão, Com Capa) na tela de calibração da Mesa Estendida.
4. **Velocidade do Disco Parametrizável**: Adicionar a configuração de ritmo do disco (Lenta: 6.500, Normal: 9.500, Rápida: 12.000 unidades/s) em `UserPreferences`, sincronizada pelo Anfitrião na mensagem de início da partida multijogador.
5. **Miniaturas Neon da Mesa no Menu Principal**: Substituir os ícones genéricos dos cartões do menu por representações gráficas estilizadas em Canvas de cada disposição de jogo.
