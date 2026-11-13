# 0044. Restrição da Regra de Disco Preso ao Alcance Não-Humano e Hierarquia entre Perfil de Efeitos Visuais e Brilho do Neon

Restringimos a intervenção de **Disco Preso** no **Árbitro Virtual** às áreas sem ação possível de jogador humano (meia-mesa da IA, Modo IA x IA e Vão Físico da Mesa Estendida) e explicitamos na interface a hierarquia entre o **Perfil de Efeitos Visuais** (`VisualEffectsProfile`) e o **Brilho do Neon** (`neonBloomIntensity`).

## Contexto e Decisão

1. **Disco Preso no meio-campo do jogador humano:** A ADR 0039 introduziu o temporizador de 3,5 segundos (`DEAD_PUCK_TIMEOUT_SEC`) globalmente sempre que `puckSpeed < 30f`, reposicionando o disco na linha central (`centerLineY`). No **Modo Fases** (`SOLO`), sempre que um disco parava na metade inferior do jogador humano e este aguardava mais de 3,5s para golpear, o disco era teletransportado à linha central — onde a IA imediatamente avançava para atacá-lo. Adicionalmente, na **Mesa Estendida** (`isExtendedMode`), `centerLineY` coincide com o interior do **Vão Físico** (`bezelGapUnits`), zona fisicamente inalcançável pelos rebatedores caso o vão supere o diâmetro do disco.
2. **Contradição aparente entre Configurações e Menu de Pausa:** A ADR 0043 passou a desligar o bloom HDR do Filament quando `VisualEffectsProfile.OFF`, mantendo `neonBloomIntensity` intacto nas preferências. Porém, a Tela de Configurações rotulava o interruptor como *"Efeitos Arcade Neon"*, enquanto o **Menu de Pausa da Arena** continuava exibindo a barra *"Brilho do Neon"* ativa e marcando **`50% • Padrão`**.

Decidimos:
1. **Disco Preso restrito a áreas de IA e ao Vão Físico:** O temporizador de **Disco Preso** no `ArbitroVirtual` só acumula tempo quando o disco está parado em área controlada por IA (metade da IA no **Modo Fases**, ou toda a mesa no **Modo IA x IA**) ou dentro da zona morta do **Vão Físico** na **Mesa Estendida**. Qualquer disco parado dentro da meia-mesa exclusiva de jogador humano é considerado posse legítima e nunca sofre reposicionamento.
2. **Destino seguro de reinício ao disparar Disco Preso:**
   - No **Modo Fases** (`SOLO`), se a IA travar o disco em sua metade, o reinício ocorre na posição de **Saque de Início** do jogador humano (`getServePosition(2)`), em vez de devolver à linha de ataque da IA.
   - Na **Mesa Estendida**, o reinício evita o **Vão Físico** usando `getServePosition(...)` para o adversário do último tocador (ou Jogador 1 por padrão).
   - No **Modo IA x IA** em tela única, preserva-se o reinício no centro neutro (`centerLineY`).
3. **Hierarquia explícita de Neon na UI:** A preferência salva `neonBloomIntensity` continua preservada ao desligar o **Perfil de Efeitos Visuais**, mas no **Menu de Pausa da Arena** a barra de **Brilho do Neon** é exibida bloqueada e em **`0% • Desativado`** com legenda indicando que os efeitos visuais estão desativados nas Configurações. Na **Vitrine de Pré-Visualização 3D** (`ArenaCustomizationScreen`), a regulagem permanece editável ao vivo com aviso discreto quando suspensa em partidas. Na **Tela de Configurações**, o título do interruptor esclarece que controla todos os **Efeitos Visuais da Arena** (incluindo brilho neon).

## Consequências

- **Positiva:** Jogadores no **Modo Fases** (e em partidas humano vs humano) mantêm controle pleno de posse sobre discos parados em seu meio-campo sem perdê-los para o centro, e discos parados no **Vão Físico** retornam a uma metade jogável.
- **Positiva:** A interface no **Menu de Pausa da Arena** e nas Configurações passa a refletir exatamente o estado ativo do Bloom 3D sem destruir a calibração salva pelo jogador.
