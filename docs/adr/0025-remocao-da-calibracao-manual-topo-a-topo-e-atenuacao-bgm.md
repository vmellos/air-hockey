# 25. Remoção da Calibração Manual Topo-a-Topo e Atenuação da Música de Fundo (BGM)

## Status
Aceito (Evolui a ADR 0003, ADR 0010 e ADR 0017)

## Contexto
1. **Fricção Excessiva de Transição na Mesa Estendida**:
   - Anteriormente, ao parear dois dispositivos para **Mesa Estendida** (seja Humano x Humano ou IA x IA), o Anfitrião precisava acionar "Avançar para Calibração", o Convidado precisava clicar em "Avançar para Calibração", ambos eram levados à tela `BezelCalibrationScreen`, ajustavam o vão em milímetros, clicavam em "Pronto" e o Anfitrião precisava clicar novamente em "Iniciar Partida".
   - Essa etapa intermediária gerava lentidão e desorientação para os jogadores, especialmente em partidas de demonstração IA x IA. Na prática, a grande maioria dos smartphones modernos possui bordas superiores quase idênticas (entre 3,5 mm e 4,5 mm), totalizando ~8,0 mm quando unidos em **Disposição Topo-a-Topo**.
2. **Volume Inicial Intrusivo da Música Sintetizada (BGM)**:
   - A trilha sonora sintetizada proceduralmente em `GameAudioEngine` inicializava internamente com volume de `0.60f` e o arquivo WAV gerado operava em ganho elevado (mix total próximo ao teto dinâmico de 16 bits), tornando a música excessivamente alta ao iniciar o jogo e competindo com os efeitos sonoros de colisão do disco.

## Decisão

1. **Início Direto de Partida com Vão Físico Padronizado**:
   - Descontinuada a navegação obrigatória para a tela `BezelCalibrationScreen` durante o fluxo de conexão do Lobby.
   - Ao estarem conectados no Lobby, o **Dispositivo Anfitrião** aciona diretamente o botão *"INICIAR PARTIDA"*, que propaga a Largada Sincronizada com o Vão Físico padrão de 8,0 mm (`userPrefs.bezelGapMm`).
   - O **Dispositivo Convidado** permanece aguardando a largada do Anfitrião e transiciona automaticamente direto para a arena no mesmo instante.
   - O vão físico de 8,0 mm atende universalmente a Disposição Topo-a-Topo entre dispositivos móveis contemporâneos.

2. **Atenuação da Música de Fundo (BGM) para Volume Mínimo Confortável**:
   - Ajustada a propriedade padrão de `musicVolume` em `GameAudioEngine`, `UserPreferences` e `UserPreferencesRepository` para `0.10f` (volume mínimo de fundo).
   - Aplicada atenuação de ganho master de 55% (`mixed *= 0.45`) na síntese procedural de `generateBgmArcadeLoopWav`, gravada sob `bgm_arcade_loop_v2.wav`.
   - A trilha synthwave arcade passa a operar como um som ambiente relaxante e discreto, sem mascarar impactos sonoros e sem exigir que o jogador reduza o volume manualmente logo após a instalação.

## Consequências
- **Positiva**: Redução radical do tempo até o jogo começar: pareou no lobby -> Anfitrião clica "Iniciar Partida" -> arena abre instantaneamente em ambos os aparelhos.
- **Positiva**: Eliminação de telas redundantes e de múltiplos botões de confirmação.
- **Positiva**: Volume de música suave e agradável por padrão, respeitando a experiência do usuário.
