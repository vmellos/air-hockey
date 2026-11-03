# 24. Descoberta Unificada de Mesas e Centralização de Configuração IA x IA Multijogador

## Status
Aceito (Evolui a ADR 0009 e a ADR 0016)

## Contexto
1. **Fricção Assimétrica no Modo IA x IA em Mesa Estendida**:
   - Para assistir a um confronto autônomo entre IAs em dois dispositivos colocados em Disposição Topo-a-Topo (**Mesa Estendida**), o usuário precisava abrir o diálogo de IA x IA no Menu Principal, escolher "Mesa Estendida", selecionar a dificuldade do Bot 1 e Bot 2 e só então avançar para o Lobby.
   - O segundo aparelho (**Dispositivo Convidado**) era forçado a passar pelo exato mesmo formulário redundante de seleção de robôs, mesmo não tendo qualquer influência na simulação física (que é computada integralmente pelo **Dispositivo Anfitrião**).
2. **Invisibilidade entre Telas de Descoberta no Lobby**:
   - Na ADR 0009, com receio de entradas acidentais, a descoberta de rede foi isolada por `targetMode` estrito (`EXTENDED`, `CONNECTED`, `AI_EXTENDED`).
   - Isso gerava forte frustração de usabilidade: se o Anfitrião criava uma mesa como `AI_EXTENDED` e o Convidado abria a busca pelo botão comum de "Mesa Estendida" (`EXTENDED`), a lista permanecia vazia, levando o usuário a crer que a rede local ou o Wi-Fi haviam falhado.
3. **Divergência de HUD e Metadados**:
   - Como os nomes dos bots eram inferidos de variáveis estáticas locais da Activity e não transmitidos no handshake, escolhas divergentes no formulário inicial do Convidado causavam discrepâncias na identificação dos robôs no placar.

## Decisão

1. **Autoridade Exclusiva do Dispositivo Anfitrião**:
   - O **Dispositivo Anfitrião** detém a responsabilidade total pela configuração das IAs (dificuldade e nomes do Bot 1 e Bot 2) e das regras da partida.
   - O **Dispositivo Convidado** nunca é solicitado a selecionar robôs ou dificuldades em partidas de rede.

2. **Descoberta Unificada de Mesas em Rede**:
   - A lista de salas descobertas via mDNS/NSD e sondagem UDP no Lobby exibe **todas** as mesas anunciadas na rede local para os modos multijogador (Mesa Estendida PvP, Mesa Estendida IA x IA e Mesa Conectada).
   - Cada mesa é apresentada com seu respectivo selo visual neon (`[Mesa Estendida]`, `[Mesa Conectada]` ou `[IA x IA]`).
   - Ao selecionar uma mesa, o Convidado adota automaticamente o modo de jogo do Anfitrião, direcionando para a calibração de vão físico (quando Mesa Estendida) ou diretamente para a arena (quando Mesa Conectada).

3. **Centralização da Modalidade na Criação da Sala**:
   - No Lobby, ao acionar *"Criar Mesa (Anfitrião)"*, o Anfitrião escolhe a modalidade da sessão:
     1. Mesa Estendida (Humano x Humano)
     2. Mesa Estendida (IA x IA) — com seletores de dificuldade do Bot 1 e Bot 2
     3. Mesa Conectada (Humano x Humano)
   - O Anfitrião publica essa modalidade no broadcast de rede.

4. **Transição Automática para Modo Espectador**:
   - Ao conectar-se a uma sessão identificada como IA x IA em Mesa Estendida, o Convidado entra automaticamente em **Modo Espectador**, com bloqueio de toque no canvas da arena.
   - As informações de dificuldade e nomes dos robôs são sincronizadas pelo Anfitrião durante o handshake/pacote de início.

5. **Simplificação do Menu Principal**:
   - O cartão **Modo IA x IA** no Menu Principal mantém foco ágil na partida em aparelho único (Local), trazendo um atalho explícito *"Criar em 2 Aparelhos (Mesa Estendida)"* que já abre o Lobby como Anfitrião no modo IA x IA.

## Consequências
- **Positiva**: Fim dos erros de conexão onde os jogadores não encontravam a mesa por terem navegado por botões diferentes no menu.
- **Positiva**: Experiência limpa e sem atritos para o Convidado em partidas de IA x IA: basta abrir a busca, tocar na mesa encontrada e posicionar o aparelho.
- **Positiva**: Garantia de integridade física e visual: os controladores de IA rodam exclusivamente no Anfitrião e os metadados são 100% sincronizados via rede.
- **Negativa / Atenção**: Exige atualização do protocolo de handshake para incluir os metadados de bots quando o modo for `AI_VS_AI_EXTENDED`.
