# 18. Remoção do Cronômetro Regressivo, Soberania da Pontuação Alvo e Sincronização Cromática do Disco

## Status
Aceito

## Contexto
1. **Interrupção Prematura de Partidas**: O Árbitro Virtual e a GameViewModel operavam sob um temporizador decrescente de 180 segundos (`matchTimeSeconds = 180`). Ao atingir exatamente `00:00`, a física era subitamente congelada (`status = GAME_OVER`), cancelando lances em andamento e encerrando a partida de forma arbitrária (ex: 6 a 5 a favor do jogador com mais pontos, mesmo quando o adversário estava em vias de igualar o placar).
2. **Divergência de Regras Tradicionais**: O Air Hockey de mesa física é historicamente um esporte regido por pontuação alvo (primeiro a atingir 7 pontos vence). O corte por tempo gerava frustração competitiva e sensação de "bug" ao impedir desfechos esportivos legítimos.
3. **Inconsistência Visual do Disco em Mesa Estendida e Conectada**: O pacote de telemetria UDP (`HostTelemetryPacket`) omitia o campo `lastHitBy`. Como consequência, no Dispositivo Convidado o disco e o rastro de plasma permaneciam sempre na cor branca neutra, não refletindo os tons Ciano (Jogador 1 / Anfitrião) ou Laranja (Jogador 2 / Convidado) ao serem golpeados.
4. **Fantasmas no Rastro de Energia (`puckTrail`)**: Ao marcar um ponto e reiniciar o jogo via contagem regressiva, o buffer visual `puckTrail` retinha pontos anteriores do disco, traçando vetores anômalos que cortavam a mesa durante a retomada.
5. **Inoperância do Modo IA x IA em Mesa Estendida**: Selecionar "Estendida (2 Telas)" no diálogo de IA x IA apenas encaminhava para o lobby multijogador de humanos, exigindo toque manual e desconsiderando a parametrização dos bots.
6. **Deficiência Visual e Internacionalização do Diálogo de IA x IA**: O diálogo de configuração de bots possuía visual plano, sem botão de fechar e strings estáticas fora dos recursos de idioma.

## Decisão
1. **Extinção do Cronômetro de Jogo Regressivo**:
   - O cronômetro limitador de partida é completamente removido da física (`ArbitroVirtual`), do loop de jogo (`GameViewModel`) e do HUD (`GameOverlayComponents`).
   - A partida prossegue continuamente até que um dos jogadores atinja a **Pontuação Alvo** (3, 5, 7 ou 10 pontos), momento no qual a vitória é declarada imediatamente.
   - O termo `Empate` é descontinuado no domínio do jogo.
   - O espaço central superior do HUD passa a exibir a insígnia da Pontuação Alvo (`ALVO: X`), mantendo a simetria visual.
   - O tempo decorrido total (cronômetro crescente interno) continua sendo medido estritamente para registro estatístico na súmula da partida e no Placar de Líderes.
2. **Sincronização Cromática do Disco na Telemetria UDP**:
   - O campo `lastHitBy: Byte` é incorporado ao pacote `HostTelemetryPacket` (47 bytes, com retrocompatibilidade no leitor de buffer).
   - O Dispositivo Convidado espelha `puck.lastHitBy` a cada frame recebido, assegurando que o anel do disco e o rastro de plasma adotem dinamicamente Ciano ou Laranja em ambos os telefones físicos.
3. **Higienização do Rastro de Energia na Retomada de Jogo**:
   - O buffer `puckTrail` é limpo explicitamente (`puckTrail.clear()`) nas transições para `GOAL_PAUSE`, `COUNTDOWN` e na restauração das posições físicas.
4. **Implementação Autônoma da IA x IA em Mesa Estendida**:
   - Adicionado fluxo dedicado para IA x IA em dois dispositivos físicos.
   - O Anfitrião executa deterministamente ambos os controladores de IA (Bot 1 e Bot 2) e transmite as coordenadas via telemetria UDP a 120 Hz para o Convidado.
   - Em ambos os dispositivos, o agarre de rebatedor por toque é travado sob o selo de "Modo Espectador", liberando apenas o botão de Pausa.
5. **Redesign Arcade Neon e Internacionalização do Diálogo de Bots**:
   - Diálogo reformulado com Glassmorphism translúcido, cartões dedicados para Bot 1 (Ciano) e Bot 2 (Laranja) com ícones arcade, botão "X" de fechar no topo e botão de ação com gradiente neon vibrante.
   - Todas as strings extraídas para `strings.xml` nos idiomas en, pt-BR e es.

## Consequências
- **Positiva**: Partidas justas e eletrizantes que terminam invariavelmente por mérito de pontuação (Pontuação Alvo), eliminando derrotas arbitrárias por estouro de cronômetro.
- **Positiva**: Fidelidade visual completa em Mesa Estendida e Conectada, com disco e rastro coloridos dinamicamente de acordo com quem desferiu o golpe.
- **Positiva**: Eliminação de anomalias e riscos fantasmas do rastro de plasma na retomada pós-ponto.
- **Positiva**: Demonstração de IA autônoma em duas telas funcionando de ponta a ponta sem intervenção humana manual.
- **Positiva**: Interface de configuração de bots polida, moderna e 100% internacionalizada.
