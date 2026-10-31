# 0003 - Coordenadas Virtuais Normalizadas e Compensação de Vão Físico

## Contexto e Decisão
Diferentes aparelhos móveis possuem densidades de pixels (DPI), larguras de tela e bordas plásticas (bezels) variadas. Para garantir que o comportamento físico, o raio de colisão e a velocidade do disco sejam idênticos em qualquer modelo de dispositivo (e que o disco atravesse perfeitamente entre os dois aparelhos em Mesa Estendida), decidimos utilizar um espaço de coordenadas virtuais fixo com ajuste visual de compensação de vão físico.

## Justificativa e Alternativas
- **Sistema de Coordenadas Fixo**: A mesa possui dimensões virtuais fixas de 1000 x 2000 unidades. Cada aparelho no modo Topo-a-Topo projeta exatamente 1000 x 1000 unidades virtuais em sua tela, renderizadas via escala proporcional (`ContentScale.Fit`). Isso elimina distorções visuais e impede que telas maiores confiram vantagens desleais.
- **Compensação de Vão Físico**: Quando o disco cruza a borda de uma tela para a outra, ele percorre uma distância virtual oculta proporcional ao vão físico real das molduras dos celulares. O jogador pode ajustar essa distância em milímetros através de um slider simples na calibração, assegurando uma ilusão de continuidade perfeita.
