# 0007 - Redimensionamento da Mesa Estendida e Arquitetura da Mesa Conectada

## Contexto e Decisão
Na versão inicial ([ADR 0003](./0003-coordenadas-normalizadas-e-compensacao-de-vao.md)), a Mesa Estendida dividia a mesa virtual de 1000 x 2000 em duas metades de 1000 x 1000 unidades. Como os smartphones modernos possuem aspect ratio aproximado de 1:2 (19.5:9), projetar 1000 x 1000 gerava um campo quadrado centralizado com grandes faixas pretas. Além disso, os jogadores solicitaram um quarto modo de jogo no qual dois aparelhos pareados pudessem disputar com visão integral da mesa e controle ergonômico na base de suas próprias telas.

Decidimos:
1. **Redimensionar o Viewport da Mesa Estendida**: Cada tela passa a ter viewport virtual de `1000 x 2000` unidades, totalizando `1000 x (4000 + vão físico)` de área física contínua. A metade da mesa é ancorada no topo da tela (`offsetY = 0`), eliminando faixas pretas intermediárias na Disposição Topo-a-Topo e desenhando um semicírculo neon que completa o círculo central contínuo na junção dos aparelhos.
2. **Introduzir o Modo Mesa Conectada**: Quarto modo de jogo onde dois dispositivos conectados via rede local (P2P Wi-Fi / Hotspot) jogam em uma mesa clássica de 1000 x 2000. O Dispositivo Anfitrião mantém a autoridade física e transmite a telemetria via UDP a 60Hz. O Dispositivo Convidado projeta a mesa com rotação de 180°, permitindo que ambos defendam a meta na base da tela dos seus respectivos aparelhos.
3. **Fluxo do Lobby Parametrizado**: O pareamento na Mesa Conectada pula a Calibração de Vão Físico e inicia a Largada Sincronizada diretamente após a conexão.
