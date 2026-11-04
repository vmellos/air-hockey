# ADR 0028: Centralização de Áudio, Vibração e Temas no Menu Principal e Ancoragem Fixa de Banners

## Status
Aceito (Complementa a ADR 0023, ADR 0026 e ADR 0027)

## Contexto
1. Anteriormente, a aplicação mantinha controles redundantes de preferências sensoriais (Efeitos Sonoros, Música de Fundo e Vibração Tátil) tanto na Tela de Configurações (`SettingsScreen`) quanto no diálogo modal de Acesso Rápido de Áudio (`QuickAudioDialog`) acionado diretamente pelo botão de volume no cabeçalho do Menu Principal.
2. Similarmente, o acesso à tela de Customização e Temas Visuais (`ArenaCustomizationScreen`) estava duplicado: existia um botão dedicado com ícone de Paleta no cabeçalho do Menu Principal e, simultaneamente, uma seção com cartão navegável no meio da Tela de Configurações.
3. Essa redundância gerava atrito de navegação e sobrecarregava a Tela de Configurações com opções cosméticas e sensoriais que pertencem ao fluxo de entrada imediata do jogador.
4. Por fim, o Anúncio em Banner (`AdBanner`) na Tela de Configurações encontrava-se acoplado dentro da coluna com rolagem vertical (`Modifier.verticalScroll`), fazendo com que o anúncio rolasse junto com os cartões e desaparecesse da visualização do usuário. Nas demais telas de navegação (Placar de Líderes, Customização da Arena, Seleção de Fases e Lobby Multijogador), o banner não era sequer renderizado, desaproveitando inventário publicitário regulado e em conformidade com as diretrizes do AdMob.

## Decisão
1. **Desacoplamento e Remoção na Tela de Configurações**:
   - Remover integralmente da `SettingsScreen` a seção *"Áudio e Vibração"* (switches e controles deslizantes de volume) e a seção *"Temas & Estilo da Arena"* (cartão de atalho para customização).
   - Eliminar o parâmetro `onOpenCustomization` da assinatura de `SettingsScreen` e da rota de navegação em `MainActivity`.
   - Limitar o escopo da Tela de Configurações estritamente aos ajustes técnicos e globais: Remoção Permanente de Anúncios, Perfil do Jogador, Idioma, Regras de Jogo / IA, Ergonomia de Toque (Deslocamento de Toque) e Perfil de Efeitos Visuais.
2. **Centralização Exclusiva no Cabeçalho do Menu Principal**:
   - Manter com exclusividade o acesso a **Customização e Temas Visuais** através do botão de Paleta (`Icons.Default.Palette`) no cabeçalho do `MainMenuScreen`.
   - Manter com exclusividade o **Acesso Rápido de Áudio** (`QuickAudioDialog`) através do botão de Volume (`Icons.AutoMirrored.Filled.VolumeUp`/`VolumeOff`) no cabeçalho do `MainMenuScreen`.
   - Preservar os botões pontuais de silenciamento rápido (mute/unmute) de som e música no **Menu de Pausa da Arena** (`PauseMenu`) durante as partidas ativas para conveniência dos jogadores em confronto.
3. **Ancoragem Fixa do Anúncio em Banner no Rodapé das Telas de Navegação**:
   - Desacoplar o `AdBanner` da rolagem vertical na `SettingsScreen`, isolando o conteúdo em uma coluna com `Modifier.weight(1f).verticalScroll(...)` e fixando o banner estático na base (footer).
   - Introduzir a ancoragem fixa de rodapé do `AdBanner` nas seguintes telas de navegação adicionais:
     - **Placar de Líderes** (`LeaderboardScreen`): posicionado no rodapé com as abas e listas de jogadores rolando acima dele com `weight(1f)`.
     - **Customização e Temas Visuais** (`ArenaCustomizationScreen`): posicionado no rodapé com o carrossel de abas e a lista de skins/conjuntos rolando acima dele com `weight(1f)`.
     - **Seleção de Fases** (`StageSelectionScreen` - Modo Fases): posicionado no rodapé com a grade dos 100 níveis rolando acima dele com `weight(1f)`.
     - **Lobby Multijogador** (`LobbyScreen` - Mesa Estendida / Mesa Conectada): posicionado no rodapé nos estados de seleção de papel (Criar Mesa e Entrar na Mesa), tela de espera do anfitrião e lista de mesas descobertas do convidado.
   - Suprimir o banner estritamente durante o escaneamento ativo de Código QR com a câmera em tela cheia (`isScanningQr`), preservando a área de captura do visor.
4. **Sincronização do Modelo de Domínio**:
   - Atualizar no `GLOSSARY.md` as definições canônicas de **Anúncio em Banner**, **Acesso Rápido de Áudio** e formalizar **Tela de Configurações**.

## Consequências
- **Positiva**: Eliminação de redundâncias de navegação e telas de configuração sobrecarregadas; todas as opções cosméticas e sensoriais ficam a 1 toque de distância na tela inicial.
- **Positiva**: Monetização estável e consistente com banners visíveis e fixos no rodapé, sem rolagem indesejada nem sobreposição de botões interativos.
- **Positiva**: Preservação estrita da usabilidade na câmera QR e no menu de pausa em jogo.
