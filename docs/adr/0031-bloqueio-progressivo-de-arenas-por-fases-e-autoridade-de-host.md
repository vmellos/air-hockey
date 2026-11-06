# ADR 0031: Bloqueio Progressivo de Arenas pelo Modo Fases e Autoridade Temática do Host

## Status
Aceito (Complementa a ADR 0019, ADR 0028 e ADR 0029)

## Contexto
1. **Disponibilidade Global de Arenas**: Anteriormente, todas as 11 arenas temáticas (2D e 3D) estavam liberadas irrestritamente desde a primeira inicialização do jogo na tela de Customização (`ArenaCustomizationScreen`), desprovendo o Modo Fases (100 níveis) de uma recompensa visual e progressiva de longo prazo.
2. **Ciclo de Recompensa por Conquista**: Para incentivar a progressão nas 100 fases do jogo solo, as arenas esportivas devem ser conquistadas progressivamente ao completar blocos de 10 níveis (organizados dos esportes mais simples aos mais épicos), mantendo apenas a Arena Clássica Neon (`CLASSIC_NEON` e `ARCADE_3D`) liberada de fábrica para todos os modos (Solo, P1 x P2 Split-Screen e IA x IA).
3. **Autoridade de Criação Temática e Inclusão de Convidados**: No multijogador em rede local (Mesa Estendida e Mesa Conectada), jogadores que não desbloquearam determinada arena não devem conseguir criar mesas com ela. No entanto, se um Anfitrião (Host) que já conquistou a arena criar a mesa, o jogador Convidado (Guest) deve poder ingressar e desfrutar da partida temática sem qualquer bloqueio de acesso.
4. **Fase do Projeto**: O aplicativo encontra-se em fase de **Teste Fechado (Closed Testing)** no Google Play Console. Portanto, não há necessidade de manter esquemas redundantes de migração legada de produção.
5. **Diretrizes de Interface**: Em respeito aos padrões visuais arcade do projeto e às especificações do usuário, telas de vitória e diálogos comemorativos não devem utilizar emojis Unicode, valendo-se estritamente de tipografia com caixas altas, ícones vetoriais Material e strings localizadas (PT, EN, ES).

## Decisão
1. **Catálogo Canônico de Conquistas (`ArenaConquestCatalog`)**:
   - Mapear os 10 blocos de 10 fases da mais simples à mais épica:
     1. Fases 1–10: `CYBER_PONG` (Conquista no nível 10)
     2. Fases 11–20: `CYBER_SOCCER` (Conquista no nível 20)
     3. Fases 21–30: `NEO_BASKETBALL` (Conquista no nível 30)
     4. Fases 31–40: `CYBER_BASEBALL` (Conquista no nível 40)
     5. Fases 41–50: `GRIDIRON_FOOTBALL` (Conquista no nível 50)
     6. Fases 51–60: `GRAVITY_VOLLEY` (Conquista no nível 60)
     7. Fases 61–70: `RETRO_SYNTHWAVE` (Conquista no nível 70)
     8. Fases 71–80: `WASTELAND_APOCALYPSE` (Conquista no nível 80)
     9. Fases 81–90: `TOKAMAK_REACTOR` (Conquista no nível 90)
     10. Fases 91–100: `CYBER_COLOSSEUM` (Conquista no nível 100)
   - A Arena Clássica (`CLASSIC_NEON` / `ARCADE_3D`) permanece padrão e desbloqueada de início.
2. **Cálculo Reativo de Desbloqueio no Repositório (`StageRepository`)**:
   - Expor `unlockedThemesFlow: Flow<Set<TableTheme>>`, `unlockedPuckSkinsFlow` e `unlockedMalletSkinsFlow` calculados dinamicamente com base nas fases concluídas com sucesso (`starsEarned > 0`).
   - Fornecer consulta síncrona/reativa `isThemeUnlocked(theme)`.
3. **Autoridade de Host e Sincronização no Multijogador**:
   - No evento de inicialização de partida (`TableControlEvent.Start`), o Host serializa e transmite o nome de seu tema ativo (`themeName`).
   - O Convidado recebe `hostThemeName` via telemetria de controle e o repassa na especificação da partida (`MatchSpec.tableTheme`), garantindo que ambos os dispositivos renderizem o mesmo tema escolhido pelo Host.
   - Jogadores que ainda não desbloquearam a arena não conseguem equipá-la em suas preferências locais e, portanto, não podem criar mesas anfitriãs com ela.
4. **Resolução Dinâmica de Arena no Modo Fases**:
   - Cada fase do Modo Fases configura automaticamente o tema correspondente ao seu bloco (`StageConfig.tableTheme`).
   - Os renderizadores 2D (`AirHockeyArenaCanvas`) e 3D (`AirHockeyArena3DScene`), bem como BGM e banners, priorizam o `effectiveTheme = matchState.tableTheme ?: prefs.tableTheme`.
5. **Vitrine Interativa com Bloqueio e Pré-Visualização**:
   - Na tela de customização (`ArenaCustomizationScreen`), temas, discos e rebatedores bloqueados exibem selo e ícone de cadeado indicando a fase de conquista (`Bloqueado • Fase X`).
   - Tocar em um item bloqueado permite pré-visualizá-lo em tempo real na vitrine 3D rotativa e abre um diálogo arcade convidativo com botão "Jogar Fases" direcionando o usuário diretamente ao seletor de fases.
6. **Celebração de Conquista Limpa (Sem Emojis)**:
   - Ao vencer a 10ª fase de qualquer bloco (ex: fase 30), caso a arena ainda não estivesse desbloqueada, o diálogo final de fase (`StageResultDialog`) exibe um card comemorativo neon estilizado com o ícone vetorial da modalidade e o nome canônico da arena conquistada, sem nenhum caractere emoji.
   - No seletor de fases (`StageSelectionScreen`), as fases 10, 20, 30... 100 contam com molduras diferenciadas e o ícone esportivo do tema, destacando seu status de fase de conquista ("boss stage").

## Consequências
- **Positiva**: Gamificação profunda e sensação de conquista real a cada 10 fases superadas.
- **Positiva**: Preservação da experiência multijogador onde convidados podem jogar e se encantar com mesas raras de anfitriões veteranos.
- **Positiva**: Coerência temática completa tanto para a dimensão 2D quanto 3D.
- **Positiva**: Fidelidade estrita aos padrões visuais e tipográficos arcade definidos para a publicação na Google Play Store.
