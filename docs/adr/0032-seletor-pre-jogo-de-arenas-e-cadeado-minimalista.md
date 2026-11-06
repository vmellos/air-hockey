# 32. Seletor Pré-Jogo de Arenas, Ícone de Arenas e Selo de Bloqueio Minimalista

**Data:** 2026-11-06  
**Status:** Aceito

---

## Contexto

Com a implementação da progressão esportiva por fases (ADR 0031), as arenas passaram a ser grandes conquistas desbloqueadas a cada 10 fases superadas pelo jogador no Modo Fases (1 a 100). Isso trouxe a necessidade de alinhar a experiência do usuário em três pontos principais:

1. **Menu Principal com Ícone Adequado:** O ícone anterior do botão de temas no cabeçalho era uma paleta de pintura (`Icons.Default.Palette`), reminiscência da época em que a personalização era meramente cosmética. Com as arenas passando a representar estádios e palcos de modalidades esportivas (Futebol, Basquete, Baseball, Vôlei, Futebol Americano, etc.), o ícone precisava representar arenas/estádios esportivos (`Icons.Default.Stadium`).
2. **Selo de Bloqueio Minimalista nos Cartões:** Na tela de Customização da Arena (`ArenaCustomizationScreen`), os cartões bloqueados exibiam uma tarja textual poluída com `Bloqueado • Fase 10/100`, que competia com as ilustrações e nomes. A proposta solicitada foi exibir estritamente um ícone de cadeado elegante e minimalista (`Icons.Default.Lock`), delegando a explicação detalhada de desbloqueio e o convite de ação ("Jogar Fases") para o diálogo de toque (`AlertDialog`).
3. **Seletor Pré-Jogo de Arenas com Pré-Visualização:** Ao selecionar confrontos multijogador local (Host de 2 Dispositivos nos modos Estendido, Conectado ou IA vs IA 2 Dispositivos) ou confrontos locais no mesmo aparelho (Split-Screen e IA vs IA Local), o jogador/anfitrião era arremessado imediatamente para o lobby ou para o jogo sem a oportunidade de escolher a arena em que a disputa ocorreria. Era mandatório introduzir uma etapa pré-jogo com inspeção interativa 3D (giro orbital turntable contínuo e manipulação por toque) ou 2D, com chaveamento de dimensão (2D/3D), antes de abrir a mesa ou rolar a partida.

---

## Decisões

1. **Atualização do Ícone do Menu Principal:**
   - Na `MainMenuScreen`, o botão de acesso rápido a arenas e personalizações no cabeçalho foi atualizado para utilizar o vetor `Icons.Default.Stadium`, consolidando o conceito de estádios/arenas da liga arcade.

2. **Adoção do Selo de Bloqueio Minimalista:**
   - Nos cartões `BundleCard` e `SingleItemCard` da `ArenaCustomizationScreen`, a tarja textual inferior foi removida.
   - Em seu lugar, introduziu-se um selo circular flutuante de 32 dp no canto superior direito, contendo o ícone de cadeado âmbar (`Icons.Default.Lock`) com borda translúcida.
   - O toque em qualquer item bloqueado abre o diálogo modal arcade contextualizado (`arena_locked_dialog_title` e `arena_locked_dialog_desc`), indicando o nível exigido e oferecendo o botão "JOGAR FASES" que navega diretamente para a `StageSelectionScreen`.

3. **Criação da `PreMatchArenaSelectionScreen`:**
   - Componente Compose reutilizável contendo:
     - Header arcade com botão Voltar e título dinâmico.
     - Vitrine interativa de pré-visualização compartilhada com `ArenaCustomizationScreen`:
       - No modo 3D: renderização orbital com turntable contínuo via corrotina, rotação por arraste (touch orbit) e zoom por pinça (pinch to zoom).
       - No modo 2D: renderização geométrica vetorial de alta precisão em Canvas Compose com pulsação neon e linhas táticas da modalidade.
     - Seletor de dimensão 2D vs 3D (desabilitado em Split-Screen para preservar a projeção zenital de topo a topo em tela dividida).
     - Carrossel horizontal com as 11 arenas temáticas, destacando a arena ativa e exibindo o selo de bloqueio minimalista nas arenas ainda não conquistadas.
     - Diálogo explicativo imediato ao tocar em arena bloqueada, prevenindo seleções inválidas e oferecendo atalho para o modo fases.
     - Botão primário arcade dinâmico: "CONFIRMAR E ABRIR MESA" para sessões de Host no Lobby e "INICIAR PARTIDA" para Split-Screen e IA vs IA Local.
     - Persistência das preferências selecionadas (`tableTheme` e `arenaDimension`) no repositório `UserPreferencesRepository`.

4. **Integração no Lobby e na Navegação:**
   - No `LobbyScreen`, a seleção de modo no diálogo `HostCreateModeDialog` agora abre o fluxo de `PreMatchArenaSelectionScreen`. Após confirmar a arena, a mesa é iniciada no papel de Anfitrião com o tema escolhido propagado para os clientes conectados.
   - No card do anfitrião (`HostLobbyView`), foi adicionado um badge visual com o nome da arena configurada ao lado da etiqueta do modo de jogo.
   - Na `MainActivity`, a rota de Split-Screen agora transita por `pre-match/SPLIT_SCREEN`, abrindo o seletor com dimensão 2D travada e iniciando a partida logo após a confirmação.

---

## Consequências

- **Experiência do Jogador (UX):** Transição fluida e intuitiva antes do início das disputas, valorizando as conquistas de arenas desbloqueadas e permitindo ao jogador exibir seus estádios conquistados ao hospedar partidas multijogador.
- **Interface Limpa e Sem Emojis:** Eliminação de ruído textual nos cartões de temas através do selo de cadeado minimalista e estrita conformidade com a política de ausência de emojis nos textos da aplicação.
- **Performance e Reúso:** A `PreMatchArenaSelectionScreen` reutiliza os renderizadores de visualização 3D e 2D pré-existentes, garantindo 60/120 FPS em dispositivos modernos com baixo consumo de memória.
