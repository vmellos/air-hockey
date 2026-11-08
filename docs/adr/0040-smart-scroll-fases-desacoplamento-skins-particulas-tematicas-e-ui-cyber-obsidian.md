# 0040. Auto-Rolagem de Fases, Desacoplamento de Skin em Fases, Partículas Temáticas e Design System Cyber-Obsidian

Implementamos a rolagem automática inteligente até a fase ativa mais recente, o desacoplamento da aparência do jogador no Modo Fases, a sincronização de partículas e rastros 3D com a paleta da arena ativa e a unificação da interface sob a estética Cyber-Obsidian.

## Contexto e Decisão

Analisando a experiência do jogador nas fases avançadas, constatou-se que a tela de fases abria fixada na Fase 1, obrigando rolagem manual repetitiva até a Fase 50. Além disso, o taco do jogador no Modo Fases era arbitrariamente sobreposto pelo tema da arena antes mesmo de sua conquista formal, alternando cores de forma desordenada. Na renderização 3D, efeitos de colisão, rastro e faíscas permaneciam fixos em ciano e laranja/vermelho em todas as mesas, ignorando as paletas exclusivas dos 11 temas. Por fim, a interface utilizava botões e modais com cores primárias contrastantes desconexas do clima arcade.

Decidimos:
1. **Auto-Rolagem de Fases (Smart Scroll)**: Salvar e restaurar o estado da grade de fases (`LazyGridState`), ancorando a visualização diretamente na fase desbloqueada mais recente com realce sutil.
2. **Desacoplamento de Skin em Fases**: Preservar rigorosamente a aparência e skin de rebatedor equipada pelo jogador no menu de customização, atribuindo o kit da arena rival apenas ao adversário autônomo (Boss da Arena).
3. **Partículas Temáticas da Arena**: Vincular ondas de choque, faíscas incandescentes, explosões de gol e rastro de energia de `AirHockeyArena3DScene` às cores primária, secundária e de parede catalogadas em `ArenaThemeCatalog`.
4. **Design System Cyber-Obsidian**: Harmonizar botões, molduras e cartões sob superfícies escuras Obsidian (`#070B14`), glassmorphism, acentos em Ciano Elétrico (`#00E5FF`) e Ouro Arcade (`#FFB800`).
