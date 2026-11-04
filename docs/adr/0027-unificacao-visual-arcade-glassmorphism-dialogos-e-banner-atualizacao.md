# ADR 0027: Unificação Visual Arcade Glassmorphism em Diálogos da Arena e Banner de Atualização

## Status
Aceito (Complementa a ADR 0022 e a ADR 0023)

## Contexto
1. Após a padronização visual estabelecida nas ADRs 0022 e 0023, as telas de navegação principais (Menu Principal, Seleção de Fases, Configurações e Lobbies) consolidaram uma identidade coesa de estética arcade futurista, com painéis em *glassmorphism* escuro translúcido (`#080E1A` / `#0B0F1C`), bordas chanfradas neon (`CutCornerShape`) e paleta cibernética de alto contraste (Ciano `#00E5FF`, Laranja `#FF5722` e Dourado `#FFB800`).
2. Entretanto, as interfaces modais e diálogos sobrepostos na arena durante as partidas (`StageResultDialog`, `PauseDialog`, `GameOverDialog` e `PeerExitedDialog`) ainda utilizavam contêineres arredondados (`RoundedCornerShape`), cores cinza-ardósia opacas (`#1E293B`) e botões padrão Material 3 (`FilledTonalButton` e pílulas arredondadas), quebrando a imersão e o acabamento estético durante o jogo.
3. Além disso, a Tela de Desfecho da Fase no Modo Fases utilizava botões avulsos flutuantes com paleta arco-íris desordenada (verde, magenta e laranja) e strings misturadas e hardcoded ("YOU WIN" em inglês e "FASE NAO CONCLUIDA" em português sem acentuação).
4. Similarmente, o banner de reinício após atualização flexível (`InAppUpdateDownloadedBanner`) no menu principal exibia fundo azul-índigo (`#1E1B4B`) com bordas e botões lilás pastel (`#818CF8`), destoando radicalmente do Disco Flamejante e dos cartões de modo.

## Decisão
1. **Padronização dos Modais da Arena em CutCornerShape e Glassmorphism**:
   - Aplicar em todos os diálogos de partida (`StageResultDialog`, `PauseDialog`, `GameOverDialog` e `PeerExitedDialog`) a morfologia com `CutCornerShape(20.dp)`, fundo translúcido escuro `Color(0xF20B0F1C)`, elevação de sombra de 24.dp e bordas com chanfros luminosos em gradiente neon.
2. **Redesenho Integrado da Tela de Desfecho da Fase**:
   - Integrar os botões de ação (Lista de Fases, Tentar Novamente e Próxima Fase) e a insígnia de resultado dentro da própria arquitetura do cartão chanfrado, eliminando os botões arredondados flutuantes e a paleta arco-íris desordenada.
   - Adotar botões chanfrados arcade (`CutCornerShape(10.dp)`), cápsula translúcida com estrelas douradas (`#FFB800`) e destaque cromático consistente: Ciano (`#00E5FF`) na vitória e Magenta Neon (`#FF2A6D`) na derrota.
3. **Erradicação dos Botões Pílula Material 3 no Menu de Pausa e Fim de Partida**:
   - Substituir os botões genéricos M3 (`FilledTonalButton` e pílulas) por botões chanfrados arcade (`CutCornerShape(10.dp)`).
   - Ação principal (ex: "Continuar" ou "Jogar Novamente") estilizada com preenchimento neon de alto impacto e ações secundárias ("Reiniciar" e "Sair para o Menu") desenhadas com contorno luminoso chanfrado e fundo escuro translúcido.
   - Seletores de áudio e música com acabamento HUD neon integrado.
4. **Harmonização do Aviso de Reinício para Atualização**:
   - Alinhar o `InAppUpdateDownloadedBanner` à linguagem dos Cartões de Modo do menu principal: formato chanfrado assimétrico `CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)`, fundo `Color(0xD9080E1A)`, indicador vertical neon verde esmeralda (`#10B981`) e botão de reinício neon chanfrado.
5. **Internacionalização Estrita e Atualização do Modelo de Domínio**:
   - Extrair todos os textos hardcoded para `strings.xml` com tradução completa para Inglês, Português e Espanhol.
   - Formalizar no `GLOSSARY.md` os conceitos canônicos: **Tela de Desfecho da Fase**, **Menu de Pausa da Arena**, **Diálogo de Fim de Partida** e **Aviso de Reinício para Atualização**.

## Consequências
- **Positiva**: Coesão visual absoluta em 100% dos fluxos de jogo, desde a navegação na tela inicial até a celebração de vitória e pausa na arena.
- **Positiva**: Eliminação definitiva de elementos visuais genéricos do Material 3 e de cores dissonantes.
- **Positiva**: Suporte linguístico consistente sem textos em inglês ou termos sem acentuação misturados.
