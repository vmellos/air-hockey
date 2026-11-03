# 22. Redesign do Menu Inicial: Desobstrução do Disco Flamejante e Cartões Chanfrados Neon

## Status
Aceito (Complementa a ADR 0021 e refina o domínio do Menu Principal)

## Contexto
1. Na ADR 0021, o aplicativo adotou a arte do **Disco Flamejante** (`bg_fiery_puck.jpg`) como plano de fundo oficial e o **Título Arcade Volumétrico** metálico cromado (`title_banner_championship.png`).
2. Contudo, a disposição prática dos cartões na tela inicial mantinha componentes densos e opacos com **Miniaturas da Mesa** em Canvas (`TableModeThumbnail`) e crachás de texto redundantes (*"100 FASES"*, *"WI-FI"*, *"2 TELAS"*, etc.). Esses elementos inflavam a altura de cada opção, cobrindo quase 100% da área vertical e ocultando por completo o Disco Flamejante no centro da arena.
3. Além disso, uma malha quadriculada gerada via Canvas por cima do plano de fundo gerava poluição visual e conflito estético com a nova identidade de alta voltagem demonstrada em `selected_style_side_by_side.png`.

## Decisão
1. **Preservação Exclusiva de Nome Principal e Descrição**:
   - Descontinuar a **Miniatura da Mesa** e as etiquetas de crachá nos cartões do menu principal.
   - Preservar estritamente o **Nome Principal** (em tipografia preta sólida arcade com espaçamento estendido) e a **Descrição** concisa das 5 opções de jogo:
     - **Modo Fases**: Jornada sequencial de 100 níveis (Verde Esmeralda `#10B981`).
     - **Mesa Conectada**: 2 aparelhos via Wi-Fi com visão completa (Ciano Elétrico `#00E5FF`).
     - **Mesa Estendida**: 2 aparelhos topo a topo formando mesa contínua (Índigo `#818CF8`).
     - **Mesa Dividida**: 2 jogadores no mesmo aparelho em tela dividida (Laranja Neon `#FF5722`).
     - **Modo IA x IA**: Duelo autônomo entre robôs com seletor de dificuldade (Púrpura Neon `#A855F7`).

2. **Estética Glassmorphism com Bordas Chanfradas Neon (`CutCornerShape`)**:
   - Estruturar os **Cartões de Modo de Jogo** com chanfros angulares de 45 graus (estilo tech/cyberpunk), fundo escuro translúcido (`Color(0xD9080E1A)`) e borda luminosa com gradiente horizontal da cor neon de cada modalidade.
   - Adicionar uma barra vertical neon de acento e ícone indicador de avanço discreto.

3. **Desobstrução do Disco Flamejante e Compactação Ergonômica**:
   - Rebaixar a altura dos cartões para ~48dp com espaçamento de 8dp entre eles, liberando ampla área central desobstruída para os holofotes e o Disco Flamejante em alta velocidade.
   - Suprimir o desenho da grade azul arbitrária em Canvas em favor de um gradiente vertical limpo para legibilidade perfeita do texto.

4. **HUD Superior Harmonizado**:
   - Redesenhar os botões de ação do topo (Placar de Líderes, Remover Anúncios, Recompensas e Configurações) no estilo HUD arcade translúcido com anéis neon discretos.

## Consequências
- **Positiva**: Máxima valorização da arte cinematográfica do Disco Flamejante no plano de fundo sem qualquer obstrução.
- **Positiva**: Redução de mais de 250 linhas de código Canvas dispensáveis em `MainMenuScreen.kt`, acelerando a renderização e reduzindo o consumo de GPU/CPU no menu.
- **Positiva**: Perfeita coerência visual entre o ícone da Play Store (`selected_style_side_by_side.png`), o plano de fundo da arena e a navegação do usuário.
