# 23. Unificação Visual Arcade Glassmorphism em Todas as Telas de Navegação

## Status
Aceito (Complementa a ADR 0021 e a ADR 0022)

## Contexto
1. Após a aprovação do redesenho do menu inicial (ADR 0022), o aplicativo passou a apresentar um alto padrão estético baseado na arena cinematográfica (`bg_fiery_puck.jpg`), componentes translúcidos (*glassmorphism* escuro) e bordas chanfradas neon (`CutCornerShape`).
2. Entretanto, as telas secundárias de navegação (Configurações, Seleção do Modo Fases e Lobbies de Mesa Conectada/Estendida) ainda mantinham fundos pretos foscos (`#070B14` e `#0A0E1A`), cartões retangulares opacos (`#1E293B`) e blocos brancos sólidos, gerando descontinuidade e quebra de imersão ao navegar pelo jogo.
3. Além disso, a tela de Seleção de Fases possuía um botão circular ciano de "Info" sem qualquer funcionalidade vinculada, e o topo do menu inicial mantinha um ícone de Presente/Recompensa (`Icons.Default.Redeem`) sem loja de cosméticos ou customizações correspondente.

## Decisão
1. **Unificação do Fundo Cinematográfico**:
   - Aplicar `R.drawable.bg_fiery_puck` com gradiente escuro vertical (*scrim*) em todas as telas de navegação ([LeaderboardScreen.kt](file:///home/vinicius/AndroidStudioProjects/AirHockey-feat-unify-arcade-screens-visuals/app/src/main/java/dev/vmellos/airhockey/feature/leaderboard/ui/LeaderboardScreen.kt), [SettingsScreen.kt](file:///home/vinicius/AndroidStudioProjects/AirHockey-feat-unify-arcade-screens-visuals/app/src/main/java/dev/vmellos/airhockey/feature/settings/ui/SettingsScreen.kt), [StageSelectionScreen.kt](file:///home/vinicius/AndroidStudioProjects/AirHockey-feat-unify-arcade-screens-visuals/app/src/main/java/dev/vmellos/airhockey/feature/stage/ui/StageSelectionScreen.kt) e [LobbyScreen.kt](file:///home/vinicius/AndroidStudioProjects/AirHockey-feat-unify-arcade-screens-visuals/app/src/main/java/dev/vmellos/airhockey/feature/lobby/ui/LobbyScreen.kt)).
   - Ajustar o contraste do scrim para garantir legibilidade estrita de textos, controles e formulários.

2. **Remoção de Elementos Inoperantes**:
   - Remover o botão Info do cabeçalho da Seleção do Modo Fases.
   - Remover o botão de Recompensa/Presente do menu inicial, eliminando itens visuais sem contrapartida funcional.

3. **Modernização dos Cartões de Fases**:
   - Cartões com formato chanfrado `CutCornerShape(12.dp)`.
   - Fases desbloqueadas com fundo translúcido escuro (`Color(0xD9080E1A)`), borda neon ciano (ou dourada para fases concluídas com 3 estrelas), número em tipografia branca sólida e cápsula de estrelas translúcida.
   - Fases bloqueadas em silhueta escura discreta com ícone de cadeado integrado.

4. **Padronização dos Lobbies e Configurações**:
   - Cartões de seleção de papel (Criar Mesa / Entrar na Mesa) alinhados aos Cartões de Modo do menu inicial (chanfros, barras verticais de acento neon e setas de avanço).
   - Agrupamentos de Configurações e diálogo do Modo IA x IA convertidos para superfícies translúcidas com bordas chanfradas neon.
   - Botões de retorno (Voltar) padronizados como atalhos HUD circulares de 44dp com anel neon ciano.

## Consequências
- **Positiva**: Identidade visual 100% coesa e imersiva em todas as rotas do aplicativo.
- **Positiva**: Eliminação de botões sem ação que causavam frustração aos jogadores.
- **Positiva**: Experiência visual premium de jogo arcade moderno em todas as telas.
