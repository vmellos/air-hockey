# 38. Apresentação Cinemática de Drone e Piso da Arena 3D

Data: 2026-11-07

## Status

Aceito

## Contexto

Na Apresentação de Abertura da Mesa 3D, a cinemática de câmera anterior executava uma rotação orbital circular de 360 graus com o ponto focal travado estaticamente no centro da mesa `(0, 0, 0)` durante 6,5 segundos. Em conjunto com a ausência de uma âncora óptica fixa no chão e a decomposição manual em ângulos de Euler com ordem intrínseca `ZYX`, esse algoritmo gerava dois efeitos visuais indesejados:
1. **Percepção de *Turntable*:** Como o centro da mesa permanecia milimetricamente cravado no centro da tela sobre um fundo preto infinito, o usuário percebia a arena rodopiando em torno de seu próprio eixo na linha de visão (*turntable* de micro-ondas) em vez de um drone voando dinamicamente pelo espaço.
2. **Gimbal Lock e *Roll* Acidental:** A rotação por ângulos de Euler introduzia inclinação lateral acidental (*roll*) nos eixos transversais (±90°), tombando o horizonte visual.
3. **Fadiga de Espera:** 6,5 segundos revelavam-se excessivamente longos e maçantes para uma experiência esportiva arcade.
4. **Intensidade do Neon Padrão:** O efeito de Bloom neon vinha configurado por padrão em 50% (0.05f), quando a preferência ergonômica ideal de primeiro uso é de 10% (0.01f), devendo ser preservada qualquer customização futura realizada pelo jogador.

## Decisão

1. **Trajetória Cinemática de Drone com Foco Dinâmico:**
   Substituir a órbita centrada por uma curva cúbica contínua de sobrevoo estilo drone que parte de uma tomada aérea elevada na cabeceira oposta (meta do Jogador 2), desce em arco rasante ao longo da calha lateral esquerda da arena apresentando o disco e o rastro luminoso, contorna a meta do Jogador 1 e ascende suavemente até a perspectiva do jogador (`targetPreset`).
2. **Orientação Vetorial Estabilizada via `cameraNode.lookAt`:**
   Eliminar cálculos angulares manuais sujeitos a *roll*. A orientação da câmera durante todo o sobrevoo passa a ser computada dinamicamente via `cameraNode.lookAt(target = focusPoint, upDirection = Direction(y = 1.0f))`, garantindo que o vetor UP permaneça cravado no eixo vertical do mundo, sem nenhum tombamento de horizonte.
3. **Piso da Arena 3D como Âncora Óptica:**
   Incorporar o modelo tridimensional otimizado `piso_arena_cyber.glb` orientado no plano horizontal `XZ` do Filament e posicionado logo abaixo da Mesa Clássica (`y = -0.045f`), dotado de base metálica escura, malha de grade perimetral cibernética neon ciano e balizadores de canto, conferindo referência espacial e paralaxe real durante a movimentação da câmera, enquanto as 10 arenas temáticas exibem livremente seu próprio piso de estádio integrado (`ArenaStadiumFloor`) sem sobreposição.
4. **Calibração de Duração e Ritmo:**
   Reduzir a duração da apresentação para **3,8 segundos** com curva de aceleração e desaceleração `FastOutSlowInEasing`, conectando o fim do voo diretamente à contagem regressiva de saque.
5. **Brilho Neon Inicial em 10% com Preservação:**
   Calibrar o valor padrão de `neonBloomIntensity` em `UserPreferences.kt` e no fallback do DataStore para `0.01f` (10% na escala de 0 a 100%), garantindo que a preferência persistida do usuário permaneça intocada após qualquer ajuste manual.

## Consequências

### Positivas
- Eliminação completa da rotação tipo *turntable* e do tombamento lateral de horizonte (*roll*).
- Experiência visual esportiva imersiva, com paralaxe de alta velocidade e profundidade espacial real.
- Início de partida rápido e dinâmico (3,8s), alinhado ao ritmo arcade e sincronizado com a trilha sonora.
- Iluminação neon elegante e confortável no primeiro lançamento (10%), respeitando escolhas posteriores do jogador.

### Negativas
- Pequena adição de 38 KB no pacote de assets para o modelo do piso da arena, amplamente compensada pelo ganho estético e conformidade com o GLOSSARY.md.
