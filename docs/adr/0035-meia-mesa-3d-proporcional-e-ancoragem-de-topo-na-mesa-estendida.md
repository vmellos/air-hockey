# 35. Meia-Mesa 3D Proporcional (1:2) e Ancoragem de Topo na Mesa Estendida

**Data:** 2026-11-07  
**Status:** Aceito

---

## Contexto

Com a integração inicial do protótipo tridimensional da **Mesa Estendida** em dois aparelhos pareados via Wi-Fi ([ADR 0007](0007-mesa-estendida-proporcional-e-mesa-conectada.md)), a malha 3D (`mesa_arcade_half_p1.glb`) havia sido gerada cortando a arena padrão ao meio, resultando em dimensões métricas de `1.08m` de largura por `0.96m` de comprimento.

Como os smartphones modernos possuem aspect ratio vertical médio de 19.5:9 (~0.46), projetar um campo quase quadrado (proporção ~1:1) com a câmera centralizada no centro geométrico gerava distorção visual severa:
1. **Desproporção Vertical:** O campo ocupava menos de 40% da altura da tela, deixando mais de 60% do display em vazio preto.
2. **Descolamento da Junção Física:** A extremidade aberta da meia-mesa (arco neon de passagem) flutuava no meio da tela com uma barra preta intermediária, quebrando a ilusão de continuidade física na Disposição Topo-a-Topo.
3. **Anisotropia de Escala:** A conversão de unidades virtuais para o espaço métrico utilizava escalas divergentes nos eixos X e Z (`scaleX = 0.00108f`, `scaleZ = 0.00048f`), gerando ligeira ovalização em elementos circulares e trajetórias.
4. **Variações de Aparelhos e Zoom:** Usuários utilizam aparelhos com resoluções e proporções de aspecto distintas (16:9 a 20.5:9) e desejam utilizar os níveis de zoom/enquadramento 3D (`Próximo`, `Padrão` e `Amplo`) sem que a ampliação descole a junção entre os dois celulares.

---

## Decisão

1. **Redimensionamento Proporcional da Meia-Mesa 3D (Proporção 1:2) para Todos os 11 Temas Esportivos:**
   - As malhas 3D de Meia-Mesa foram geradas proceduralmente via Blender com `1.08m` de largura e `2.16m` de comprimento para a totalidade dos **11 Temas de Mesa** do catálogo (totalizando 22 modelos `.glb`, P1 Anfitrião e P2 Convidado), espelhando fielmente o viewport virtual de `1000 x 2000` unidades normalizadas por aparelho estabelecido na [ADR 0007](0007-mesa-estendida-proporcional-e-mesa-conectada.md).
   - O `Theme3DAssetResolver.kt` foi parametrizado para mapear cada tema ao seu respectivo par de meias-mesas P1/P2 na Mesa Estendida.
   - Unificou-se a escala espacial de conversão métrica em **isotropia estrita** (`scaleX = scaleZ = 0.00108f`), assegurando circularidade pura para o disco, rebatedores e arcos da arena.

2. **Câmera 3D Zenital Ancorada no Topo Físico do Display:**
   - Para garantir que a borda de junção encoste no topo da tela sem vazios (`offsetY = 0`), a posição longitudinal da câmera (`camZ`) passou a ser calculada analiticamente em função da altura da lente:
     $$\text{camZ} = Z_{\text{top}} + \text{camY} \cdot \tan\left(\frac{\text{fovY}}{2}\right)$$
     onde $Z_{\text{top}} = -1.08\text{m}$ corresponde à linha central da meia-mesa.
   - Isso garante que a extremidade aberta da mesa permaneça cravada no limite superior do vidro do dispositivo independentemente da altura da câmera ou da resolução da tela.

3. **Adaptação Dinâmica por Proporção de Tela (`screenAspect`):**
   - A altura da câmera (`calibratedY`) passou a ser computada em tempo de execução com base no aspect ratio local do aparelho:
     $$\text{calibratedY} = \max\left(\frac{W_{\text{target}}}{2 \cdot \tan(\text{fovY}/2) \cdot \text{screenAspect}}, \frac{L_{\text{target}}}{2 \cdot \tan(\text{fovY}/2)}\right) \cdot \text{zoomFactor}$$
   - Aparelhos mais compridos (19.5:9 a 20.5:9) absorvem naturalmente o comprimento extra na base do campo com empunhadura ergonômica, enquanto aparelhos mais curtos/largos (16:9 ou tablets) evitam cortes na meta ou nos controles.

4. **Preservação do Controle de Zoom HUD com Pivô Superior:**
   - O botão de enquadramento 3D (`Próximo`, `Padrão`, `Amplo`) foi preservado no HUD da Mesa Estendida.
   - Como a câmera tem pivô no topo, a transição entre os níveis de zoom aproxima ou afasta a visão em direção à base do jogador, preservando 100% da integridade da junção física entre os dois aparelhos pareados.

5. **Atualização do Glossário de Domínio:**
   - O [`GLOSSARY.md`](../../GLOSSARY.md) foi atualizado registrando os termos canônicos **Meia-Mesa 3D** e **Ancoragem de Topo na Mesa Estendida 3D**, além de adequar **Dimensão de Renderização** e **Níveis de Enquadramento 3D**.

---

## Consequências

- **Imersão Espacial em 2 Telas:** A Mesa Estendida 3D preenche a totalidade das duas telas físicas, formando uma contínua e imersiva arena de `1000 x 4000` unidades com luzes dinâmicas, materiais PBR e reflexos neon.
- **Continuidade Visual Topo-a-Topo:** A linha central da mesa fica perfeitamente alinhada à moldura física de cada aparelho, eliminando o efeito de mesa flutuante.
- **Robustez Multidispositivo:** Aparelhos de diferentes tamanhos físicos e resoluções mantêm sincronismo exato de física via UDP e projeção sem perdas nas extremidades.
- **Consistência Matemática:** Desprojeção de toque (`screenToTable3D`) e projeção de partículas (`tableToScreen3D`) preservam reversibilidade e precisão milimétrica.
