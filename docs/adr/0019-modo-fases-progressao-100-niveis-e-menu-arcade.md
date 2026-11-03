# 19. Modo Fases com Progressão em 100 Níveis, Avaliação em Estrelas e Menu Principal Arcade

## Status
Aceito

## Contexto
1. **Ausência de Metas Sequenciais no Modo Solo**: O Modo Solo original operava apenas como uma partida rápida avulsa e infinita contra a IA sem objetivos de longo prazo, progressão de dificuldade ou senso de conquista.
2. **Necessidade de Progressão Arcade Estruturada**: Jogadores de arcade buscam desafios graduais. A introdução de uma trilha de 100 fases com critérios de vitória e estrelas de desempenho cria um ciclo de engajamento contínuo.
3. **Decisão sobre Economia de Jogo**: Alinhou-se com o usuário a não adoção de moedas virtuais ou compras cosméticas vinculadas a moedas, mantendo o foco do jogo estritamente na habilidade esportiva e conquista de fases e estrelas.
4. **Renovação Visual do Menu Principal**: A interface inicial demandava maior apelo arcade moderno com uma arte de destaque central de um disco metálico futurista envolto em chamas de plasma como plano de fundo sob a seleção de modos de jogo.
5. **Corte do Nome do Jogador no Lobby Multijogador**: A disposição em linha única do cabeçalho do lobby comprimia o chip de identificação do participante contra o título da tela, truncando nomes em telas menores.

## Decisão
1. **Modo Fases (100 Níveis Sequenciais)**:
   - Substituição da partida individual avulsa pela jornada do Modo Fases.
   - Criação da tela `StageSelectionScreen` inspirada no layout arcade com grade vertical de 3 colunas, exibindo cartões de fases desbloqueadas com estrelas e cartões bloqueados com ícone de cadeado.
   - Desbloqueio linear: apenas a Fase 1 inicia disponível; cada vitória com no mínimo 1 estrela desbloqueia a fase imediatamente seguinte.
2. **Curva de Dificuldade Determinística em 4 Faixas**:
   - Fases 1 a 10: Pontuação Alvo = 3, IA Iniciante, velocidade Normal.
   - Fases 11 a 20: Pontuação Alvo = 5, IA Iniciante, velocidade Normal.
   - Fases 21 a 50: Pontuação Alvo = 5, IA Intermediária, velocidade Normal.
   - Fases 51 a 80: Pontuação Alvo = 7, IA Avançada com mira tática nos cantos, velocidade Rápida.
   - Fases 81 a 100: Pontuação Alvo = 7 a 10, IA Mestre com tempos de reação mínimos e antecipação angular, velocidade Rápida.
3. **Avaliação por Estrelas de Desempenho (1 a 3 Estrelas)**:
   - 3 Estrelas: Vitória perfeita ou dominante (sofrer 0 ou no máximo 1 ponto).
   - 2 Estrelas: Boa defesa (sofrer até 2 pontos em metas 5+, ou 1 ponto em meta 3).
   - 1 Estrela: Vitória padrão ao atingir a Pontuação Alvo.
4. **Tela de Vitória e Derrota da Fase (`StageResultDialog`)**:
   - Layout fiel à referência arcade com título do nível, cartão com moldura neon (Ciano para vitória, Magenta/Laranja para derrota), badge superior, contêiner com 3 estrelas e exibição do placar final.
   - Botões de navegação direta em formato quadrado com bordas neon: Lista de Fases (Verde), Reiniciar (Magenta) e Próxima Fase (Laranja).
5. **Persistência de Fases no Room Database**:
   - Criação de `StageProgressEntity` e `StageProgressDao` com migração destrutiva permitida para versão 2 do banco `AppDatabase`.
6. **Menu Principal com Disco Flamejante em Segundo Plano**:
   - Incorporação do asset de alta definição `bg_fiery_puck` como plano de fundo sob camada de contraste gradiente vertical.
   - Preservação integral dos 5 modos de jogo do aplicativo (Modo Fases, Mesa Conectada, Mesa Estendida, Mesa Dividida e Modo IA x IA).
7. **Resolução Definitiva do Truncamento no Lobby**:
   - Top bar do lobby dividida em 2 linhas (Linha 1: Navegação e Título; Linha 2: Cartão de Perfil do Jogador em largura completa).
   - Configurações padrão ajustadas: volume da música no mínimo (0.15f), vibração desativada por padrão e idioma padrão em inglês para dispositivos cujo sistema não seja pt ou es.

## Consequências
- **Positiva**: Experiência solo substancialmente aprofundada com 100 fases e rejogabilidade orientada a coletar 3 estrelas em cada estágio.
- **Positiva**: Arquitetura desacoplada e reativa com `StageRepository` alimentando fluxos StateFlow e Flow no Jetpack Compose.
- **Positiva**: Apelo visual de topo de linha na tela de entrada através do disco flamejante e acabamento neon arcade sem interferir na ergonomia dos menus.
- **Positiva**: Fim dos cortes de texto do nome do usuário em qualquer tamanho de tela e resolução de dispositivo.
