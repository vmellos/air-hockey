# ADR 0049: Evolução Realista da Mascote da Mesa 3D e Playback Exclusivo de Animações no Sceneview

- **Status:** Aceito
- **Data:** 2026-11-18
- **Relacionado:** [ADR 0042](0042-mascote-de-mesa-3d-em-calha-lateral-conjunto-kiara-cat-e-acesso-livre.md), [ADR 0048](0048-cofre-criptografado-de-ativos-3d-em-memoria.md), [THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md)

## Contexto

Na implementação original do [Conjunto Temático Kiara Cat](../../GLOSSARY.md#conjunto-tematico-kiara-cat) (ADR 0042), a **Mascote da Mesa** (`app/src/main/raw_assets/models/kiara_3d.glb`) apresentava duas limitações:

1. **Bug de reprodução no Sceneview (`autoAnimate = true`):** No `ModelNode` do `sceneview:2.3.4`, `autoAnimate = true` inicia simultaneamente **todas** as faixas de animação do arquivo `.glb` dentro de `playingAnimations`. Além disso, `AirHockeyArena3DScene` chamava apenas `playAnimation(targetAnim, loop = true)` sem interromper os demais índices ativos, fazendo com que `LickPaw` e `Swat` sobrescrevessem os quadros de `Walk` e dando a impressão visual de que apenas uma pata se movia durante a caminhada.
2. **Fidelidade visual:** A malha anterior era composta apenas por primitivas procedurais simples, com pelagem aproximada e anatomia rígida de poucas articulações.

## Decisão

1. **Reprodução exclusiva de animações no runtime:**
   - Alterar `ModelNode(..., autoAnimate = false)` tanto em `AirHockeyArena3DScene.kt` quanto no preview de `ArenaCustomizationScreen.kt`.
   - Introduzir o helper `ModelNode.playExclusiveAnimation(animationName, loop)` em `AirHockeyArena3DScene.kt`, que interrompe todas as faixas atualmente ativas em `playingAnimations` antes de iniciar a faixa solicitada.
   - Inicializar `sceneState.currentMascotAnim = ""` para garantir sincronização imediata da primeira faixa (`"LickPaw"`) no loop de quadro da partida e `"Idle"` na pré-visualização da loja/personalização.

2. **Evolução visual com base esculpida realista + rig quadrúpede completo:**
   - Adotar como escultura-base o modelo CC/Sketchfab Free Standard *"Realistic cat - Pets for your project"* (autor: Micael Martins) customizado via Blender (`scripts/build_kiara_from_real_base.py`) com textura UV `Kiara_Fur_Albedo` fiel às fotos reais da Kiara: pelagem curta branca, mancha preta assimétrica envolvendo olho direito e orelha direita sem cruzar o focinho, detalhe preto no dorso da orelha esquerda, mancha oval no flanco/garupa esquerdo, cauda inteiramente preta, focinho e almofadas das patas rosa, olhos âmbar, coleira rosa com argola dourada e medalha magenta `"KIARA"`.
   - Estruturar armadura quadrúpede completa de 24 ossos com articulação independente nas 4 patas (`UpperArm_L/R`, `Forearm_L/R`, `Paw_FL/FR`, `Thigh_L/R`, `Shin_L/R`, `Paw_BL/BR`), mantendo canonicamente os 4 nomes de ação esperados pelo `KiaraMascotController` (`Idle`, `Walk`, `LickPaw`, `Swat`) e altura mínima de malha assentada em `Z ≈ 0`.

3. **Atribuição discreta e glossário preservado:**
   - Registrar a licença em [THIRD_PARTY_NOTICES.md](../../THIRD_PARTY_NOTICES.md) e expor créditos discretos no rodapé da tela rolável de Configurações (`SettingsScreen.kt` -> diálogo *"Créditos 3D e Atribuições"*).
   - Preservar os termos canônicos do [GLOSSARY.md](../../GLOSSARY.md) (*Mascote da Mesa* e *Conjunto Temático Kiara Cat*).

## Consequências

- **Positivas:** Caminhada quadrúpede real com movimento alternado diagonal das 4 patas, fidelidade visual substancialmente superior com silhueta e marcações reais, e conformidade com os termos de atribuição sem poluir o HUD de jogo.
- **Neutras / Atenção:** O `.glb` atualizado em `app/src/main/raw_assets/models/kiara_3d.glb` é empacotado automaticamente pelo pipeline do cofre em memória (`packageProtectedAssets*`, ADR 0048). Futuras trocas de animação no `ModelNode` devem sempre passar por `playExclusiveAnimation`.
