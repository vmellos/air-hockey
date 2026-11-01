# 0011 - Retomada com Contagem Regressiva, Efeitos Arcade e Ajustes de Regras e UI

## Contexto e Decisão
Durante sessões com dois jogadores reais e partidas estendidas, foram observadas oportunidades de refinamento na jogabilidade, áudio, apresentação gráfica e regras:
1. **Áudio e Ícone de Volume no Multijogador**: O ícone de som na partida utilizava a variante `Icons.AutoMirrored.Filled.VolumeUp` que sofria com ofuscação pelo R8 em compilações Release, deixando apenas o círculo de fundo cinza visível. Além disso, a inicialização assíncrona do `SoundPool` não utilizava listener de carregamento (`OnLoadCompleteListener`), podendo descartar sons nas primeiras jogadas.
2. **Retomada Abrupta após Pausa**: Ao despausar, o jogo mudava instantaneamente para o estado `PLAYING`, movimentando o disco antes que os participantes tivessem tempo de repousar os dedos na tela.
3. **Imobilidade do Jogador na Contagem Regressiva**: Durante o estado `COUNTDOWN`, a reconciliação da Previsão no Cliente forçava o rebatedor do convidado de volta para as coordenadas padrão do anfitrião a cada 8ms, impedindo o posicionamento tático preparatório antes da largada.
4. **Ausência de Dinâmica Visual (VFX)**: Os rebatedores e o disco eram renderizados de forma plana, sem sensação de velocidade ou impacto típica dos jogos arcade.
5. **Declaração Errada de Vitória em Empates**: Ao término do tempo regulamentar com placares iguais (e.g. 0 a 0), a tela de fim de jogo declarava erroneamente a vitória do Jogador 2 por falta de tratamento explícito do caso de empate.
6. **Medição de Desempenho (FPS)**: Não havia opção para verificar a taxa de quadros por segundo real suportada pelos aparelhos modernos (como displays a 120Hz).

Decidimos:
1. **Blindagem do Ícone e Áudio**:
   - Adicionar regras explícitas no Proguard/R8 para preservar classes e membros de `androidx.compose.material.icons.**` e `androidx.compose.material.icons.automirrored.**`.
   - Aplicar dimensionamento explícito de 24dp e paleta de alto contraste (Ciano neon quando ativo, Vermelho coral quando silenciado).
   - Adicionar `OnLoadCompleteListener` no `GameAudioEngine` e checagem de integridade dos arquivos de som gerados no cache.
2. **Retomada com Contagem Regressiva (Padrão da Indústria)**:
   - Ao despausar a partida (`resumeMatch`), o sistema entra em `COUNTDOWN` (3, 2, 1, JÁ!) com áudio harmônico arcade, congelando o disco no ponto onde foi pausado e concedendo 3 segundos de preparação aos jogadores.
3. **Posicionamento Preparatório Livre**:
   - Durante `COUNTDOWN`, suspender a reconciliação forçada do rebatedor do convidado e permitir movimentação tática imediata de ambos os jogadores em seus respectivos meios-campos, mantendo apenas o disco bloqueado.
4. **Efeitos Visuais Arcade (VFX)**:
   - **Rastro Cinético do Disco**: Buffer de posições históricas renderizando estelas luminosas com decaimento suave de opacidade.
   - **Ondas de Choque de Impacto**: Anéis expansivos translúcidos emitidos nos pontos exatos de impacto do disco contra rebatedores e bordas da mesa.
   - **Aura Neon nos Rebatedores**: Gradiente radial pulsante em volta dos rebatedores (Ciano para o Jogador 1 e Laranja para o Jogador 2).
5. **Tratamento Canônico de Empate**:
   - Quando `winner == 0` no fim do jogo por tempo esgotado, exibir o título `"Empate!"` com visual neutro iluminado e placar final idêntico.
6. **Contador de FPS em Tempo Real**:
   - Nova preferência persistida no DataStore (`SHOW_FPS`) ativável no menu de Configurações, exibindo badge no canto da tela com taxa de quadros real medida a cada 500ms via VSync.
