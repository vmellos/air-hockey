# 21. Ícone de Mesa Estendida Lado a Lado e Identidade Visual do Menu

## Status
Aceito (Complementa a ADR 0005 e a ADR 0019)

## Contexto
1. O diferencial competitivo central do jogo é o modo para dois dispositivos móveis físicos (**Mesa Estendida**), executado na física e na projeção de coordenadas em **Disposição Topo-a-Topo** (ADR 0005, `TableProjection.kt`), onde dois jogadores ficam frente a frente com as bordas superiores dos celulares em contato mútuo.
2. Contudo, os ícones de lançador do Android e os ativos de vitrine da Google Play Store operam estritamente em proporção quadrada 1:1 (512x512). Reproduzir dois smartphones dispostos topo-a-topo na vertical resulta em uma proporção física extrema de aproximadamente 40:9 (4,4 vezes mais alta que larga). Em um enquadramento 1:1, isso força uma escala reduzida onde os aparelhos ocupam uma faixa minúscula no centro, com grandes vazios laterais, destruindo a legibilidade do ícone em grades de 48dp na tela inicial do celular.
3. Ao dispor os dois aparelhos **Lado a Lado**, a proporção combinada atinge aproximadamente 20:18 (~1:1), preenchendo harmonicamente o quadrado, permitindo carcaças amplas, visualização nítida dos rebatedores em oposição cromática (ciano vs. carmesim) e o disco cruzando o Vão Físico com arcos elétricos.
4. Além disso, o plano de fundo do menu principal (`bg_fiery_puck.jpg`) continha textos, botões e cartões de modos de jogo desenhados diretamente na imagem de mapa de bits, gerando duplicidade visual e poluição com os cartões interativos do Jetpack Compose. O cabeçalho também utilizava um componente `Text` simples sem a identidade cromada arcade do projeto.

## Decisão
1. **Licença Artística Comercial no Ícone de Mesa Estendida**:
   - Assumir formalmente a disposição **Lado a Lado** como licença visual de marketing e iconografia na Google Play Store (`store_assets/icon_512x512.png`), no lançador Android (`res/mipmap-*`) e no portal web.
   - Manter a regra de autoridade física e a projeção de coordenadas do jogo estritamente em **Disposição Topo-a-Topo** (`TableProjection.kt`), preservando a ergonomia presencial dos participantes frente a frente.
   - Aplicar recuo perimetral de segurança (Safe Zone de 15%) com gradiente escuro contínuo, assegurando que máscaras circulares e esquilos (squircles) de diferentes fabricantes Android não decepem as bordas ou botões dos smartphones.

2. **Plano de Fundo Despoluído do Disco Flamejante**:
   - Substituir `bg_fiery_puck.jpg` por arte cinematográfica 16:9 limpa, contendo exclusivamente o disco metálico em chamas de plasma sobre a mesa neon e holofotes de estádio, com 0% de textos ou botões embutidos na imagem.
   - Otimizar o arquivo para ~333KB, garantindo carregamento instantâneo e contenção do tamanho do APK.

3. **Título Arcade Volumétrico no Menu Principal**:
   - Adicionar o banner transparente com canal alfa `title_banner_championship.png` estampado com **"AIR HOCKEY ULTIMATE CHAMPIONSHIP"** (chanfro metálico escovado, chamas no primeiro bloco e relâmpagos ciano no segundo).
   - Renderizar o componente no topo de `MainMenuScreen.kt` mantendo o slogan localizado da aplicação logo abaixo.

## Consequências
- **Positiva**: Máximo destaque visual para a feature de dois celulares físicos na vitrine da Play Store, convertendo visitantes em instalações com leitura instantânea do conceito.
- **Positiva**: Fim dos conflitos de máscara circular no Android: o ícone mantém integridade em qualquer launcher (Pixel, One UI, MIUI).
- **Positiva**: Menu principal com separação de responsabilidades cristalina: arte de segundo plano sem interferência e componentes Compose totalmente legíveis e interativos.
- **Positiva**: Documentação explícita da licença artística, evitando que futuros desenvolvedores questionem a divergência entre a arte lateral do ícone e a física topo-a-topo do motor.
