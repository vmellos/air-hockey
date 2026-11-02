# 13. Monetização com AdMob, Portal no Cloudflare Pages e Placar de Líderes (Ranking)

## Status
Aceito

## Contexto
1. Para viabilizar a sustentabilidade financeira do jogo na Google Play Store sem degradar a experiência do usuário, é necessária uma estratégia de monetização ética e não intrusiva. Jogos de ritmo rápido não toleram anúncios em momentos de ação, sob pena de destruição da física em tempo real e interrupção do tráfego P2P da Mesa Estendida.
2. O Google AdMob exige a presença de um arquivo `app-ads.txt` hospedado na raiz do domínio público associado à ficha do app para validação de inventário e prevenção de fraudes. Além disso, a Google Play exige uma URL pública e acessível da Política de Privacidade.
3. Para engajamento de longo prazo e retenção de jogadores locais, o jogo necessita de um sistema competitivo de progressão ([Placar de Líderes](file:///home/vinicius/AndroidStudioProjects/AirHockey/GLOSSARY.md#L143-L146)) baseado nas melhores práticas da indústria (pontuação Elo, taxas de vitória, sequências invictas e saldo de gols), persistido localmente via Room Database.

## Decisão
1. **Régua de Monetização Ética com AdMob**:
   - **Arena 100% Livre de Anúncios:** Nenhuma publicidade é exibida durante as partidas ou na tela de calibração física.
   - **Anúncio em Banner:** Posicionado exclusivamente no rodapé do Menu Principal e das Configurações.
   - **Anúncio Intersticial de Pós-Partida:** Exibido unicamente após o término da partida (Game Over), regulado por um `AdCooldownTracker` que impõe intervalo mínimo de 3 minutos e pelo menos 2 partidas concluídas entre exibições.
   - **Anúncio Recompensado (Rewarded Ad):** Acesso voluntário que premia o usuário com 2 horas de supressão total de anúncios (Modo Sem Anúncios Temporário).
   - **Conformidade Regulatória (UMP):** Integração do Google User Messaging Platform para consentimento em conformidade com GDPR/LGPD.
2. **Implantação do Portal Oficial no Cloudflare Pages**:
   - Criação do projeto estático em `web/` com landing page responsiva em estética arcade neon, página oficial de privacidade (`web/privacy.html`) e o arquivo `web/app-ads.txt` contendo o ID do anunciante (`pub-6935322996335458`).
   - Implantação automatizada na infraestrutura Cloudflare Pages sob o domínio `https://airhockey-web.pages.dev` via Wrangler CLI, com script utilitário [`scripts/deploy-website.sh`](file:///home/vinicius/AndroidStudioProjects/AirHockey/scripts/deploy-website.sh).
3. **Placar de Líderes com Persistência em Room SQLite**:
   - Criação do banco de dados Room [`AppDatabase.kt`](file:///home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/java/dev/vmellos/airhockey/core/database/AppDatabase.kt) com entidades `MatchRecordEntity` e `PlayerProfileStatsEntity`.
   - Implementação da fórmula de Pontuação Elo ($K=32$, base 1.000 pontos) no [`LeaderboardRepository.kt`](file:///home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/java/dev/vmellos/airhockey/feature/leaderboard/LeaderboardRepository.kt).
   - Criação da tela [`LeaderboardScreen.kt`](file:///home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/java/dev/vmellos/airhockey/feature/leaderboard/ui/LeaderboardScreen.kt) com abas dedicadas para Classificação Geral (Elo), Mesa Estendida (2P), Solo vs IA e Histórico de Partidas.

## Consequências
- **Positiva**: Monetização integrada sem impacto na latência ou na jogabilidade de tempo real.
- **Positiva**: Inventário de anúncios 100% protegido contra desmonetização com `app-ads.txt` online no Cloudflare.
- **Positiva**: Progressão competitiva offline-first de nível de console para jogadores locais.
