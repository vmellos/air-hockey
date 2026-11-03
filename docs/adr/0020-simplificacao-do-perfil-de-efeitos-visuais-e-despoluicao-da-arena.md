# 20. Simplificação do Perfil de Efeitos Visuais e Despoluição da Arena

## Status
Aceito (Atualiza e simplifica a ADR 0015)

## Contexto
1. A ADR 0015 introduziu o `VisualEffectsProfile` com 3 níveis (`FULL`, `BALANCED`, `OFF`) e incluiu seletores de qualidade tanto na tela de Configurações quanto dentro da caixa de diálogo de Pausa da arena de jogo.
2. Na prática, a diferença técnica entre `FULL` e `BALANCED` (reduzir de 14 para 7 pontos no rastro e suprimir faíscas ao colidir) gerava indecisão cognitiva no jogador sem benefício mensurável de desempenho, já que o `ParticlePool` opera com zero alocação de memória.
3. Ao mesmo tempo, manter opções de ajuste gráfico dentro do menu de pausa durante a partida ocupava espaço vertical na arena e desviava o foco de uma pausa ágil (Retomar, Reiniciar, Sair).
4. Dispositivos Android de entrada com GPUs modestas ainda se beneficiam de uma renderização estritamente limpa (`OFF`), que desativa gradientes radiais, curvas Bézier e faíscas contínuas, reduzindo o aquecimento e o consumo de bateria.

## Decisão
1. **Controle Binário do Perfil de Efeitos Visuais**:
   - Simplificar o modelo de fidelidade gráfica para um alternador direto entre **Ativado** (`FULL`) e **Desativado** (`OFF`).
   - Descontinuar o nível intermediário `BALANCED`.
   - No `UserPreferencesRepository`, migrar transparentemente preferências legadas com valor `BALANCED` para `FULL` para evitar regressões de estado em contas existentes.

2. **Componente de Configuração Compacto**:
   - Substituir os 3 cards verticais com RadioButtons na tela de Configurações (`SettingsScreen.kt`) por um único card com `Switch` (Material 3 Neon), padronizando a interface com os seletores de Som e Vibração.

3. **Despoluição da Caixa de Diálogo de Pausa**:
   - Remover os seletores de efeitos visuais do diálogo de pausa (`GameOverlayComponents.kt`), mantendo o menu de pausa estritamente operacional e rápido. A personalização gráfica fica centralizada exclusivamente nas Configurações.

## Consequências
- **Positiva**: Interface limpa e enxuta, tanto nas Configurações quanto no diálogo de pausa durante as partidas.
- **Positiva**: Menor carga cognitiva para o usuário: ou ele joga com toda a estética arcade neon, ou ativa o modo de alto desempenho e economia de bateria.
- **Positiva**: Preservação total da compatibilidade com dispositivos de entrada e aparelhos com restrições térmicas.
