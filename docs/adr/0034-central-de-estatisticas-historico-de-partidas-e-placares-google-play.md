# 34. Central de Estatísticas, Histórico de Partidas e Placares Globais Google Play

**Data:** 2026-11-06  
**Status:** Aceito

---

## Contexto

Com a integração do **Google Play Games Services v2** e sincronização de dados em nuvem via `SnapshotsClient` ([ADR 0013](0013-admob-monetization-cloudflare-web-portal-e-leaderboard-ranking.md) e commits subsequentes), surgiu o questionamento sobre a relevância e o propósito da tela interna de *Placar de Líderes* (`LeaderboardScreen.kt`).

Na implementação anterior:
- A tela possuía quatro abas: *Geral (Elo)*, *Mesa Estendida*, *Solo vs IA* e *Histórico*.
- O banco de dados local (Room Database / `AppDatabase`) isola perfis na sandbox do dispositivo. Como o Air Hockey é primariamente um jogo offline e multijogador em rede local ponto a ponto (Wi-Fi / Hotspot via Mesa Conectada e Mesa Estendida), a tabela de classificação interna continha apenas o perfil do próprio usuário e de eventuais convidados no mesmo celular.
- Para a maioria esmagadora dos jogadores, três das quatro abas apresentavam listas quase vazias (com 1 ou 2 registros), gerando a falsa expectativa de que o app deveria listar competidores da internet naquela lista interna.
- Ao mesmo tempo, o Google Play Games Services v2 provê sobreposição nativa oficial (`allLeaderboardsIntent`) para comparação global e entre amigos, mas **não fornece** histórico detalhado de confrontos (súmula com oponente, pontuação, duração, data e variação $\Delta$ Elo) nem funciona sem conexão ativa à internet ou conta Google autenticada.
- Adicionalmente, o método `submitScore` existia no `PlayGamesManager.kt`, mas não estava conectado ao ciclo de vida de término de partidas nem ao salvamento em nuvem.

---

## Decisão

1. **Reposicionamento Conceitual da Tela Interna ("Central de Estatísticas & Histórico"):**
   - A tela interna foi reestruturada para atuar como a central competitiva individual do jogador, funcionando com 100% de suporte offline e sem dependência de autenticação de terceiros.
   - Foram descontinuadas as três abas de ranqueamento comparativo local (*Geral Elo*, *Mesa Estendida* e *Solo vs IA*), substituindo-as por duas abas objetivas e complementares:
     - **Carreira (Visão Geral & Estatísticas):** Exibe o Hero Card do jogador com Gamertag, status de conexão (Play Games conectado vs Modo Local), Pontuação Elo em destaque, Card Promovendo Placares Globais da Google Play, Grid 2x2 com métricas de desempenho (Vitórias Solo/Multi, Taxa de Vitórias %, Sequência Invicta Atual/Recorde e Saldo de Gols Pró/Contra) e prévia rápida das últimas partidas.
     - **Histórico de Partidas:** Lista completa e rolável com as últimas 50 disputas gravadas no Room Database, contendo badge de resultado (*VITÓRIA*, *DERROTA*, *EMPATE*), modo esportivo com dificuldade da IA, oponentes, placar volumétrico, variação de Elo ($\pm \text{Elo}$), data/hora e duração em segundos.

2. **Delegação da Liderança Global ao Google Play Games Services v2:**
   - O ranqueamento competitivo mundial entre amigos e jogadores de todo o globo foi delegado exclusivamente aos **Placares Globais da Google Play Games**.
   - Foi inserido um card interativo com botão direto (*"Ver Rankings Globais"*) tanto na tela de Carreira quanto no cabeçalho da aplicação, disparando a sobreposição oficial do Google Play Games (`RC_LEADERBOARDS`).

3. **Submissão Automática de Pontuações (`submitScore`):**
   - O `PlayGamesManager.kt` foi aprimorado com retenção fraca (`WeakReference`) da Activity ativa (`attachActivity` / `detachActivity`), permitindo submeter scores a partir de ViewModels e corrotinas com resiliência a IDs não configurados ou falhas de rede.
   - Ao término de cada partida no `GameViewModel.kt`, o novo Elo do jogador é submetido automaticamente para o placar oficial de Elo (`leaderboard_elo_id`).
   - Ao concluir fases com estrelas no Modo Fases, o total acumulado de estrelas conquistadas é submetido ao placar de progressão (`leaderboard_stages_id`).
   - Durante a sincronização de jogos salvos (`PlayGamesSaveManager.kt`), o Elo e as estrelas consolidadas pós-resolução de conflito em nuvem são reenviados para garantir que partidas disputadas offline sejam refletidas nos placares mundiais assim que o dispositivo recuperar conectividade.

4. **Harmonização do Domínio e Glossário:**
   - O [`GLOSSARY.md`](../../GLOSSARY.md) foi atualizado para distinguir formalmente a **Central de Estatísticas & Histórico** (local, pessoal e offline) do **Placar Global da Google Play** (nuvem, amigos e mundo), marcando as antigas *Abas de Classificação Local* como descontinuadas.

---

## Consequências

- **Experiência Limpa e Sem Falsas Expectativas:** Elimina-se a sensação de "ranking quebrado/vazio" em aparelhos individuais, substituindo-a por uma rica central de estatísticas de carreira.
- **Continuidade Offline Total:** O jogador mantém histórico imutável e evolução do Elo mesmo sem internet ou sem login da Google Play.
- **Sincronia Global Automática:** Jogadores online têm suas pontuações de Elo e estrelas automaticamente espelhadas na liderança da Google Play Store.
- **Compatibilidade Retroativa:** Preservam-se os esquemas do Room Database (`match_records` e `player_profile_stats`), as rotas de navegação (`"leaderboard"`) e a integração de anúncios (`AdBanner`).
