# ADR 0026: Centralização do Menu Principal e Lobby Multijogador

## Status
Aceito

## Contexto
Anteriormente, o menu principal exibia 5 cartões de modalidades:
1. `Modo Fases` (Solo)
2. `Mesa Conectada` (Rede)
3. `Mesa Estendida` (Rede)
4. `Mesa Dividida` (1 tela compartilhada)
5. `Modo IA x IA` (com opção Local vs Estendida)

Essa segmentação forçava o segundo jogador (Convidado) a adivinhar qual botão o primeiro jogador (Anfitrião) havia pressionado, além de gerar poluição visual na tela inicial e obrigar a repetição da configuração de bots em confrontos de IA em Mesa Estendida.

O objetivo desta decisão é consolidar o menu principal em apenas 3 opções fundamentais, centralizando todos os fluxos que dependem de dois aparelhos.

## Decisão
1. **Menu Principal em 3 Cartões**:
   - `Modo Fases`: Campanha individual de 100 fases contra a IA (1 aparelho).
   - `Mesa Dividida`: Confronto local de 2 jogadores na mesma tela compartilhada (1 aparelho).
   - `Jogar em 2 Celulares`: Ponto de entrada unificado para todas as partidas que utilizam dois dispositivos móveis via rede sem fio.
2. **Lobby Multijogador Centralizado**:
   - **Convidado (`Entrar na Mesa`)**: Lista automaticamente todas as mesas ativas na rede local com selos de modalidade (`[Mesa Estendida]`, `[Mesa Conectada]`, `[IA x IA]`), ou permite entrada por código de 4 dígitos e QR Code. O convidado entra e sincroniza sem necessidade de configurações prévias.
   - **Anfitrião (`Criar Mesa`)**: Exibe seletor de modalidade para escolher entre `Mesa Estendida`, `Mesa Conectada` ou `Duelo IA x IA`. Caso opte por IA x IA, o painel permite ajustar os níveis dos bots e escolher entre hospedar em 2 celulares ou iniciar imediatamente neste próprio aparelho (Duelo Local).
   - **Alteração Dinâmica de Modalidade**: No lobby do Anfitrião, o selo da modalidade permite troca rápida sem necessidade de reiniciar a aplicação.

## Consequências
- A tela inicial ganha clareza imediata e amplo espaço visual, sem necessidade de rolagem vertical.
- O Convidado não necessita adivinhar a modalidade nem duplicar configurações de inteligência artificial.
- O Anfitrião mantém controle total sobre o tipo de partida e regras de bots.
