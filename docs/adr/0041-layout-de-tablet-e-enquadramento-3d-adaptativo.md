# ADR 0041: Otimização de Layout para Tablets e Enquadramento 3D Adaptativo em Modo Paisagem

## Status
Aceito (Complementa a ADR 0022, ADR 0026, ADR 0033 e ADR 0037)

## Contexto
1. Em telas de proporção expandida (como tablets em modo paisagem, proporção 16:10 / 16:9 com `sw >= 600dp` e largura > 1200dp), a implementação anterior do layout bicolunar introduziu assimetrias e problemas de usabilidade:
   - **Bug do Troféu Deslocado**: O cabeçalho de ações (`MainMenuHeaderActions`) estava inserido dentro da coluna direita, fazendo com que o botão de Placar de Líderes (troféu) ficasse posicionado no centro horizontal da tela do tablet, deixando a coluna esquerda vazia no topo.
   - **Esticamento de Botões**: Os 3 cartões de modos de jogo (`MenuModeCard`) ocupavam 100% da largura da coluna direita (`fillMaxWidth`), formando faixas excessivamente longas e desproporcionais ao logotipo oficial.
   - **Obstrução do Disco Flamejante pelo Banner**: O banner publicitário na base da coluna esquerda cobria diretamente o disco em chamas e as faíscas da arte oficial (`bg_fiery_puck`), contrariando o princípio de desobstrução da ADR 0022.
   - **Truncamento no Seletor de Enquadramento 3D**: No diálogo de pausa, o componente com altura de `34dp` sofria truncamento no botão central (`"Standard"` em vez de `"Standard (1.0x)"`), além de ser apertado para toque tátil no tablet.
   - **Câmera 3D Estática e Distante no Tablet Paisagem**: A câmera 3D em partidas individuais mantinha coordenadas métricas fixas calibradas para celulares verticais (9:19.5), fazendo com que a mesa parecesse uma régua estreita no centro do tablet, cercada por enormes faixas pretas ociosas nas laterais.
   - **Divergência de Domínio nas Strings**: O aplicativo em português utilizava termos restritivos como *"celulares"*, quebrando a imersão em tablets e divergindo do termo canônico de domínio **"Aparelhos"**.

## Decisão

1. **Unificação do Cabeçalho Superior no Menu Principal em Paisagem**:
   - `MainMenuHeaderActions` foi desacoplada da coluna direita e promovida a uma barra superior global com `fillMaxWidth()`, posicionando o Placar de Líderes no canto superior esquerdo e as utilidades (Loja, Áudio, Configurações) no canto superior direito de forma simétrica e harmônica em qualquer resolução.

2. **Limitação de Largura dos Cartões e Realocação do Banner**:
   - Aplicou-se um limitador ergonômico responsivo `widthIn(max = 440.dp)` centralizado na coluna direita para os cartões de modalidades de jogo.
   - O `AdBanner` e o rodapé de versão foram transferidos para a base da coluna direita (abaixo dos cartões), liberando 100% da coluna esquerda para exibição desobstruída do Título Arcade Volumétrico e da arte do Disco Flamejante.
   - O contraste do rodapé de versão foi elevado para `Color(0xFF94A3B8)` (Slate 400).

3. **Redesign do Seletor de Níveis de Enquadramento 3D no Diálogo de Pausa**:
   - Título formalizado como **"Enquadramento 3D"** (`camera_zoom_title`).
   - Os 3 botões em linha foram ampliados para `42dp` de altura, com padding interno calibrado, bordas luminosas neon e texto em duas linhas seguras para eliminar qualquer truncamento (`"Próximo (0.8x)"`, `"Padrão (1.0x)"`, `"Amplo (1.3x)"`).
   - Rejeitou-se a inserção de botões adicionais no HUD durante o jogo ativo, garantindo arena 100% limpa e sem interferências de toque na partida.

4. **Calibração Analítica Adaptativa da Câmera 3D para Telas em Paisagem**:
   - Em `Table3DCameraProjection.kt`, introduziu-se calibração adaptativa quando `screenAspect > 1.15f`:
     - **Próximo (0.8x)**: Câmera aproximada em Y = 1.70m, Z = 1.45m com pitch de -49°, preenchendo a maior parte da altura útil do tablet sem distorções ou cortes laterais.
     - **Padrão (1.0x)**: Y = 2.15m, Z = 1.85m com pitch de -47°.
     - **Amplo (1.3x)**: Y = 2.60m, Z = 2.25m com pitch de -45°.
   - A matemática vetorial reversível de desprojeção de toque (`screenToTable`) e projeção de partículas (`tableToScreen`) mantém fidelidade milimétrica contínua sob qualquer proporção de tela.

5. **Harmonização do Domínio e Strings**:
   - Substituição de "Celulares" por **"Aparelhos"** em todas as strings (`res/values-pt/strings.xml`) e consagração do termo canônico em `GLOSSARY.md`.

## Consequências
- **Positiva**: Layout do menu principal perfeitamente equilibrado e sem elementos deslocados ou esticados no tablet.
- **Positiva**: Máxima valorização da arte de fundo do Disco Flamejante.
- **Positiva**: Experiência de jogo 3D no tablet em modo paisagem significativamente mais imersiva, com a mesa ocupando a área útil de forma expressiva e jogável.
- **Positiva**: HUD de jogo mantido livre de poluição ou botões acidentais.
- **Positiva**: Vocabulário consistente para smartphones, tablets e dobráveis.
