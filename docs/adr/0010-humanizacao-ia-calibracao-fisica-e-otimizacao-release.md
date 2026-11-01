# 0010 - Humanização da IA, Calibração da Física do Disco e Otimização do Build de Release

## Contexto e Decisão
Após a implementação do Perfil de Jogador e Partidas Privadas, foram identificadas oportunidades de melhoria na ergonomia do jogo, na inteligência artificial casual e no tamanho do pacote gerado:
1. **Edição do Perfil de Jogador**: A persistência assíncrona a cada tecla no DataStore causava lentidão ao apagar caracteres e forçava recomposições indesejadas com o nome padrão quando o campo ficava temporariamente vazio. Faltava um botão explícito de salvar e feedback visual de confirmação.
2. **Ritmo da Física no Modo Lento**: O teto anterior de velocidade para o modo Lento (`6500 px/s`) ainda era excessivamente veloz em telas modernas, cruzando o campo em menos de 0,3s, além de ter baixa desaceleração natural por atrito do ar.
3. **IA no Modo Fácil**: A IA possuía velocidade elevada (`1600 px/s`), reação instantânea sem tempo de reflexo humano a cada quadro e uma salvaguarda automática na linha de gol que bloqueava finalizações com perfeição robótica, frustrando jogadores casuais.
4. **Tamanho do APK**: O APK de depuração (`app-debug.apk`) pesava 23 MB devido a ferramentas completas de inspeção Compose, multi-dex sem remoção de código morto e símbolos de depuração, gerando dúvidas em comparação com aplicativos de lançamento otimizados com R8.

Decidimos:
1. **Desacoplamento e Feedback na Edição do Perfil**:
   - Manter estado local isolado durante a digitação na tela de Configurações e no Lobby, evitando chamadas repetitivas de I/O em disco a cada caractere.
   - Adicionar botões explícitos de **Salvar** e **Restaurar Padrão do Dispositivo** com validação de entrada não vazia.
   - Fornecer feedback visual imediato de sucesso ("Nome do jogador salvo com sucesso!").
2. **Recalibração das Predefinições de Velocidade do Disco**:
   - `SLOW`: Teto reduzido para `3600 px/s` e aumento do coeficiente de atrito do ar (`0.55/s`), proporcionando um ritmo calmo e controlável.
   - `NORMAL`: Ajustado para `6800 px/s` com atrito `0.40/s`.
   - `FAST`: Ajustado para `9500 px/s` com atrito `0.35/s`.
   - Proporcionalização do impulso transferido pelo rebatedor para evitar disparos desproporcionais com movimentos rápidos no modo lento.
3. **Humanização da IA no Modo Fácil**:
   - Reduzir velocidade máxima da IA para `900 px/s`.
   - Introduzir latência de percepção (~280ms) simulando o tempo de reação humano antes de atualizar as coordenadas estimadas do disco.
   - Adicionar margem de erro/imprecisão proposital de mira (±60 px).
   - Desativar a salvaguarda de linha de gol perfeita no modo Fácil, permitindo finalizações acessíveis ao jogador.
4. **Deploy de Release Otimizado com R8**:
   - Utilizar o artefato de **Release** (`app-release.apk`) assinado com a chave de desenvolvimento para distribuição no dispositivo físico, reduzindo o tamanho do pacote de 23 MB para **5.8 MB** através de minificação de código morto e corte de recursos não utilizados.
