# 15. Redesign Arcade Neon, Partículas e Perfis de Efeitos Visuais

## Status
Aceito

## Contexto
1. A identidade visual do jogo demandava uma elevação substancial de fidelidade para se equiparar aos padrões arcade da indústria e aos protótipos de vídeo de alta fidelidade: mesa com cantos chanfrados em 45°, rebatedores em relevo pseudo-3D, disco em formato de anel de alta energia com rastro dinâmico de plasma (fogo azul para o Jogador 1 e fogo vermelho/laranja para o Jogador 2), além de vórtice luminoso na contagem regressiva e explosão de faíscas incandescentes ao marcar ponto.
2. Ao mesmo tempo, o ecossistema Android possui uma vasta gama de especificações de hardware, variando de aparelhos com GPUs de alta taxa (120 Hz) a celulares de entrada ou com restrições térmicas e de bateria.
3. A implementação gráfica não podia introduzir dependências pesadas de vídeo/sprites prontas ou bibliotecas externas que inflassem o tamanho do APK ou provocassem travamentos por coletas de lixo (*Garbage Collection*) na thread de renderização.

## Decisão
1. **Renderização Vetorial Procedural em Jetpack Compose Canvas**:
   - Manter a renderização 100% nativa em Compose Canvas via primitivas matemáticas de caminhos, gradientes radiais/lineares e `drawPath`/`drawCircle`, sem assets rasterizados pesados.
   - Cantos da mesa remodelados para chanfros angulados em 45° (*Bordas Chanfradas Neon*), com metas poligonais e círculos táticos com demarcação central.
   - Rebatedores renderizados com relevo pseudo-3D (manopla central graduada com highlight especular e sombra direcional projetada).
   - Disco renderizado como anel de energia (torus) com núcleo escuro de alto contraste e bordas incandescentes.

2. **Rastro de Energia do Disco Dinâmico e Reativo**:
   - Implementação de rastro de plasma baseado em curvas Bézier suaves e gradientes que acompanham o trajeto do disco acima do limiar de velocidade (300 px/s).
   - O rastro memoriza o último rebatedor a golpear o disco via eventos de impacto da física (`PADDLE_HIT_P1` vs `PADDLE_HIT_P2`), transitando a tonalidade para Ciano Elétrico (`#00E5FF`) ou Laranja/Vermelho Neon (`#FF5722`), iniciando com tom neutro translúcido.

3. **Arquitetura de Partículas de Zero Alocação**:
   - Criação de um sistema leve de partículas mutáveis pré-alocadas (`ParticlePool`) reutilizáveis tanto para as faíscas incandescentes da *Celebração de Ponto* quanto para as micro-partículas do rastro.
   - Atualização puramente matemática a cada frame, eliminando alocações de memória e garantindo 60 a 120 FPS sem pausas de GC.

4. **Cinemáticas de Início e Marcação de Ponto**:
   - *Vórtice de Contagem Regressiva*: Renderizado no centro da mesa com anéis de energia concêntricos e lâminas radiantes sincronizadas com os números 3, 2, 1 e GO.
   - *Celebração de Ponto*: Intervalo cinemático de 1.1s com congelamento do disco na meta, clarão luminoso e ejeção de faíscas douradas antes do reinício.

5. **Perfis de Efeitos Visuais (Modo de Desempenho)**:
   - Introdução do enum `VisualEffectsProfile` com 3 níveis:
     - `FULL` (Completo): Rastro de plasma completo, faíscas incandescentes contínuas e vórtice radiante (Padrão).
     - `BALANCED` (Econômico): Rastro cinético simplificado e ondas de choque leves, sem partículas extras de faíscas.
     - `OFF` (Desativado): Renderização pura e direta da mesa e entidades para aparelhos modestos ou economia máxima de bateria.
   - Ajustável tanto na tela de Configurações quanto no diálogo de Pausa durante a partida.

6. **Redesign do Menu Inicial**:
   - Fundo escuro com sutil grade cibernética (*cyber grid*) e iluminação ambiente neon.
   - Cartões dos 4 modos com acabamento em vidro escuro translúcido (*dark glassmorphism*), cantos suaves, bordas neon e miniaturas vetoriais com a nova geometria chanfrada.
   - Respeito às diretrizes de monetização (espaço de banner e remoção de anúncios) e micro-vibração háptica tátil ao toque.

## Consequências
- **Positiva**: Fidelidade visual extraordinária alinhada com os materiais de referência, com impacto estético premium em telas OLED e taxas de atualização variáveis.
- **Positiva**: Zero alocação de memória no loop gráfico, mantendo 120 FPS consistentes e zero sobrecarga no tamanho final do APK.
- **Positiva**: Acessibilidade e autonomia energética garantidas pelo Perfil de Efeitos Visuais.
