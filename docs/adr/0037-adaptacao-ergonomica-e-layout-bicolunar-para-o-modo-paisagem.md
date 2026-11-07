# ADR 0037: Adaptação Ergonômica e Layout Bicolunar para o Modo Paisagem

## Status
Aceito (Complementa a ADR 0023, ADR 0026, ADR 0028 e ADR 0030)

## Contexto
1. A aplicação Air Hockey foi originalmente estruturada com interfaces verticais em coluna única (`Contêiner Responsivo de Menus`), otimizadas para smartphones em modo retrato (proporções 9:16 a 9:20).
2. Como a `MainActivity` permite rotações do dispositivo via sensor de orientação (`android:configChanges="orientation|screenSize..."`), ao segurar o telefone na horizontal (modo paisagem), os layouts sofriam compressão drástica de altura útil (caindo para 360–400 dp).
3. Essa restrição vertical provocava quebras visuais e de usabilidade severas:
   - **Menu de Pausa e Diálogos**: ausência de rolagem vertical gerando corte integral dos botões primários ("Retomar", "Reiniciar" e "Sair para o Menu"), aprisionando o jogador na tela de pausa.
   - **Menu Principal**: a imagem do Título Arcade Volumétrico consumia quase toda a altura, empurrando os 3 cartões de modos de jogo para fora da área visível.
   - **Vitrine 3D (Customização e Seletor Pré-Jogo)**: altura fixa ou estirada da cena 3D empurrando as abas, listas de itens e o botão de confirmação para baixo do Anúncio em Banner ou fora da tela.
   - **Lobby Multijogador**: corte do cartão "Entrar na Mesa" e campos de conexão sob o banner.
   - **Arena de Jogo**: a mesa física retangular (proporção 1:2) ficava centralizada verticalmente, enquanto o Placar Lateral e botão de pausa sobrepunham o piso da mesa dentro da área de colisão do disco, deixando duas faixas pretas laterais vazias e subaproveitadas.

## Decisão

1. **Aproveitamento das Calhas Laterais na Arena de Jogo**:
   - Manter a mesa de jogo na proporção geométrica canônica 1:2 (1000 x 2000 unidades) verticalmente centralizada, preservando o motor de física, detecção de colisões e a projeção de câmera 3D zenital intactos.
   - Em modo paisagem, desacoplar o **Placar Lateral** e o botão de pausa da largura restrita da mesa (`tableWidthDp`), ancorando-os na extremidade da **Calha Lateral Direita** (`Alignment.CenterEnd`). Isso desobstrui 100% da superfície de jogo e melhora a ergonomia de toque para o polegar direito.

2. **Layout Bicolunar no Menu Principal**:
   - Em modo paisagem, estruturar a tela em duas colunas simétricas:
     - **Coluna Esquerda**: Título Arcade Volumétrico com escala adaptativa, subtítulo e o Anúncio em Banner ancorado em sua base.
     - **Coluna Direita**: Barra de ações rápidas no topo (Placar, Loja/Remover Anúncios, Customização, Áudio e Configurações) acompanhada pelos 3 Cartões de Modo de Jogo em pilha vertical compacta, todos visíveis sem necessidade de rolagem.

3. **Layout Bicolunar nas Telas de Vitrine (Customização e Seletor Pré-Jogo)**:
   - **Coluna Esquerda**: Vitrine de Pré-Visualização 3D/2D interativa preenchendo a altura disponível, com controles de dimensão gráfica (2D/3D) e controle de Bloom Neon.
   - **Coluna Direita**: Cabeçalho de navegação (botão Voltar e título), seletor de abas (`Kits`, `Mesas`, `Discos`, `Tacos`), lista com rolagem independente de itens/arenas, botão primário de ação e o Anúncio em Banner fixado na base dessa coluna.

4. **Lobby Multijogador com Disposição Lado a Lado**:
   - Na seleção de papel (`RoleSelectionCards`), dispor os cartões "Criar Mesa" e "Entrar na Mesa" em linha horizontal (`Row` com `weight(1f)`), garantindo visibilidade simultânea sem rolagem.
   - Nos fluxos de Convidado (leitura de QR Code e lista de mesas descobertas), isolar o conteúdo em contêiner com rolagem independente acima do banner de rodapé.
   - No diálogo de modos do Anfitrião e configuração de bots (`CreateHostModeDialog`), aplicar `Modifier.verticalScroll` e ajustes de tipografia compacta.

5. **Diálogos Modais com Arquitetura Bicolunar e Rolagem Obrigatória**:
   - No **Menu de Pausa da Arena** (`PauseMenu`) em paisagem, separar o painel em duas colunas:
     - **Coluna Esquerda**: ajustes rápidos sensoriais (Efeitos Sonoros, Música, Brilho de Bloom e Zoom da Câmera 3D).
     - **Coluna Direita**: cabeçalho "PAUSADO" e botões empilhados de ação arcade ("RETOMAR", "REINICIAR" e "SAIR PARA O MENU").
   - Aplicar `Modifier.verticalScroll(rememberScrollState())` em todos os diálogos (`PauseMenu`, `CreateHostModeDialog`, `StageResultDialog` e `GameOverDialog`) como salvaguarda ergonômica.

## Consequências
- **Positiva**: Usabilidade e apresentação de alto nível em qualquer orientação (retrato ou paisagem) tanto em smartphones pequenos quanto em telas ultrawide (20:9 e 21:9).
- **Positiva**: Eliminação definitiva de elementos cortados, botões presos ou sobreposições indesejadas do banner publicitário.
- **Positiva**: Preservação integral do motor de física, colisões e renderização 2D/3D sem qualquer impacto nas partidas ativas.
