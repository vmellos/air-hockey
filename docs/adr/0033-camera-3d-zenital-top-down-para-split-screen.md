# 33. Câmera 3D Zenital (Top-Down) para Modo Tela Dividida (Split Screen)

**Data:** 2026-11-06  
**Status:** Aceito

---

## Contexto

Na [ADR 0029](0029-desacoplamento-da-dimensao-de-renderizacao-2d-3d-dos-temas-de-mesa.md), o modo de jogo Tela Dividida no mesmo aparelho físico (`GameMode.SPLIT_SCREEN`) havia sido restrito obrigatoriamente à renderização 2D Canvas. A justificativa residia na perspectiva inclinada da câmera 3D padrão (~46° de inclinação a partir da base), que criava uma assimetria visual injusta: excelente profundidade para o Jogador 1 (que segura a base), porém uma visualização distorcida, invertida e de ponta-cabeça para o Jogador 2 (que joga do lado oposto do celular).

No entanto, jogadores de disputas locais em um único dispositivo manifestaram o desejo de usufruir da fidelidade gráfica tridimensional das novas arenas esportivas (modelos 3D em glTF/GLB com relevo, materiais PBR com iluminação dinâmica Filament, reflexos especulares e efeitos visuais acelerados por GPU).

---

## Decisão

1. **Introdução da Câmera 3D Zenital (Top-Down 3D):**
   - Para o `GameMode.SPLIT_SCREEN` em 3D, calibrou-se um preset dedicado de câmera:
     - Posição: `(X = 0.0f, Y = 3.40f, Z = 0.0f)` (centralizada geometricamente no plano métrico da mesa).
     - Rotação: `(Pitch = -90.0°, Yaw = 0.0°, Roll = 0.0°)`.
   - Essa angulação ortogonal de 90° garante **simetria geométrica absoluta** entre os campos do Jogador 1 e do Jogador 2, espelhando com precisão milimétrica a visão clássica do 2D Canvas, sem qualquer desvantagem competitiva para qualquer dos participantes.

2. **Desbloqueio do Seletor de Dimensão no Pré-Jogo (`PreMatchArenaSelectionScreen`):**
   - Removeu-se a restrição que ocultava os seletores 2D / 3D no fluxo de Split-Screen.
   - O jogador local agora tem autonomia total para escolher disputar a partida em 2D ou 3D.
   - A preferência escolhida é salva no `UserPreferencesRepository`.

3. **Adequações de Interface e Fluxo de Partida:**
   - **Início Imediato sem Atraso:** No Split Screen 3D, a apresentação cinematográfica de sobrevoo (drone flyover) é desativada por padrão, permitindo que a contagem regressiva da partida (3, 2, 1) inicie imediatamente para ambos os competidores.
   - **Ocultação do Botão de Zoom HUD:** O botão de alternância de zoom 3D é suprimido no HUD do Split Screen para evitar toques acidentais do Jogador 2 no topo da tela e preservar a visão calibrada de toda a extensão da arena.

---

## Consequências

- **Equilíbrio Competitivo:** Ambos os jogadores têm a mesma linha de visão em relação ao disco e aos seus respectivos gols.
- **Riqueza Gráfica no Mesmo Aparelho:** O modo Split Screen passa a exibir os modelos 3D volumétricos (disco com giroscópio, rebatedores e arena temática) aliados a reflexos de luz dinâmica.
- **Continuidade Matemática:** O algoritmo de desprojeção de toque (`screenToTable3D`) e de projeção de partículas (`tableToScreen3D`) preserva reversibilidade analítica contínua para coordenadas simétricas a partir do centro da tela.
