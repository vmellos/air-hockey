# 16. In-App Updates, Raio de Agarre do Rebatedor, Modo IA x IA e Ondas de Choque Radiais

## Status
Aceito

## Contexto
1. **Layout e Concorrência de Botões no Placar**: O botão de alternância de áudio disputava o canto superior direito com o botão de pausa e o placar do Jogador 2, provocando compressão e sobreposição visual em displays estreitos ou com nomes extensos de oponentes.
2. **Entrada de Toque e Salto do Rebatedor**: O motor de entrada processava qualquer toque no canvas como nova posição alvo imediata do rebatedor. Cliques acidentais ou distantes faziam o rebatedor atravessar a mesa em alta velocidade, atingindo o disco e fazendo-o saltar anômalamente para o ponto clicado.
3. **Colisão na Contagem Regressiva**: Durante o intervalo preparatório de contagem regressiva, a cinemática física permitia movimentar o rebatedor através do disco estacionado na marca de saída, sobrepondo os corpos e disparando impulsos explosivos na largada.
4. **Demanda por Modo Autônomo IA x IA**: Havia necessidade de adicionar um modo de demonstração e teste onde inteligências artificiais se enfrentam tanto localmente (em aparelho único) quanto em Mesa Estendida (Disposição Topo-a-Topo entre dois aparelhos).
5. **Apresentação Visual na Marcação de Ponto**: Ao pontuar contra um bot no Modo Solo, era exibido um selo com a dificuldade do adversário ("IA FÁCIL"), o que comprometia a imersão. Demandava-se um efeito arcade em formato de onda com feixes luminosos irradiados em expansão, inspirado na referência visual do protótipo.
6. **Mecanismo de Atualização do Aplicativo**: Integração do fluxo automático e oficial do Google Play In-App Updates, seguindo a arquitetura comprovada do projeto de referência `iptv_app`.

## Decisão
1. **Desacoplamento de Controles do Cabeçalho da Arena**:
   - Remoção do botão de áudio/mudo do cabeçalho da partida. As preferências de som continuam totalmente acessíveis no diálogo de pausa e nas configurações gerais.
   - O botão de **Pausa** passa a ter posição e área de toque isoladas no canto superior direito, com limites garantidos para o placar dos jogadores.
2. **Raio de Agarre do Rebatedor (*Grab Gating*)**:
   - O controle do rebatedor exige que o toque inicial do dedo ocorra dentro de uma margem circular de tolerância ao redor do centro do rebatedor (`grabRadius = radius * 2.2f`).
   - Cliques avulsos em áreas remotas da mesa ou sobre o disco são sumariamente ignorados, impedindo saltos e batidas involuntárias.
3. **Barreira Rígida do Disco no Posicionamento Preparatório**:
   - Durante a contagem regressiva, o disco atua como um obstáculo cilíndrico rígido inamovível na marca de saída.
   - A cinemática do rebatedor projeta e bloqueia o rebatedor na superfície externa do disco (`clampMalletAgainstFrozenPuck`), impedindo sobreposição e penetração.
4. **Modo IA x IA (Local e Mesa Estendida)**:
   - Adicionados os modos `AI_VS_AI_LOCAL` e `AI_VS_AI_EXTENDED` ao `GameMode`.
   - Controlador de IA generalizado de forma simétrica (`AirHockeyAiController`) para operar tanto no meio-campo superior quanto no inferior.
   - Diálogo preparatório no Menu Principal permitindo selecionar o formato (Local vs Mesa Estendida) e regular as dificuldades dos Bots 1 e 2 independentemente.
5. **Onda de Choque Radial Arcada**:
   - Criação da primitiva gráfica `drawRadiatingEnergyWave`: halo volumétrico, anel interno de contenção, anel externo perimetral, retícula laser e 48 a 56 feixes de energia em neon com modulação harmônica de comprimento irradiando a partir do centro.
   - Aplicação do efeito no Vórtice de Contagem Regressiva e nas celebrações de ponto, substituindo o selo de dificuldade da IA por um badge arcade "+1 PONTO" de alto impacto.
6. **Google Play In-App Updates 100% Automático**:
   - Integração da biblioteca oficial `com.google.android.play:app-update-ktx:2.1.0`.
   - Verificação automática em segundo plano no ciclo de vida da `MainActivity` no cold start.
   - Apresentação de banner não-intrusivo com estética neon no Menu Principal quando o download flexível for finalizado.

## Consequências
- **Positiva**: Fim absoluto de cliques acidentais que lançavam o disco de forma anômala.
- **Positiva**: Cabeçalho de pontuação limpo e sem sobreposição em qualquer proporção de tela.
- **Positiva**: Estética arcade idêntica ao conceito do print com ondas de choque radiais ricas em feixes de luz.
- **Positiva**: Suporte a simulações de partidas de IA para espectadores e testes de balanceamento de física.
- **Positiva**: Conformidade com os padrões da Google Play Store para atualizações flexíveis automáticas.
