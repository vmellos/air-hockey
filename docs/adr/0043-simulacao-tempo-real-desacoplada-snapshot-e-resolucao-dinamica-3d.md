# ADR 0043: Simulação em Tempo Real Desacoplada, Isolamento por Snapshot e Resolução Dinâmica 3D

## Status
Aceito (Complementa a ADR 0036, ADR 0039 e ADR 0041)

## Contexto
1. Na versão 1.9.0, relatos de campo em tablets intermediários/básicos (como Samsung Galaxy Tab A9 com Android 16) evidenciaram que a contagem regressiva de ~3 segundos levava cerca de 10 segundos e o disco movia-se em câmera lenta durante a partida.
2. A causa raiz residia no acoplamento temporal de [GameViewModel](file:///home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/java/dev/vmellos/airhockey/feature/game/GameViewModel.kt): o loop `physicsJob` executava em `Dispatchers.Main` (`viewModelScope.launch`) e invocava `arbitro.tick(1f / 120f)` exatamente uma vez por iteração de `delay(8L)`, sem acumulador de tempo real (`System.nanoTime()`). Quando a thread principal ou o `Handler` sofria contenção de renderização (~30 ms por ciclo), os 324 passos da contagem regressiva (`0,90s × 3`) estendiam-se para ~10 segundos e toda a física operava a ~0,28× da velocidade real.
3. Adicionalmente, a camada visual apresentava pontos acoplados à taxa de quadros (`stepSmoothPose`, rotação giroscópica do disco, `VisualShockwave3D`, `ParticlePool` em 2D e invalidação contínua via `withFrameNanos` em [AirHockeyArena3DScene](file:///home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/java/dev/vmellos/airhockey/feature/game/ui/AirHockeyArena3DScene.kt)), o motor 3D ignorava `VisualEffectsProfile.OFF`, e o `SceneView.createView` mantinha `dynamicResolutionOptions.enabled = false` (herdado originalmente para compatibilidade de stream de câmera AR).

## Decisão

1. **Acumulador de Passo Fixo em Tempo Real no Árbitro Virtual**:
   - Preservar `ArbitroVirtual.tick(dt)` como passo único determinístico (mantendo intacta a suíte de testes unitários) e introduzir `advanceRealTime(elapsedSec)` executando sub-passos fixos `FIXED_DT = 1f / 120f` com teto anti-espiral (`MAX_FRAME_ELAPSED_SEC = 0.10f`, até 12 passos por ciclo).
   - Em sessões multijogador, a telemetria UDP é emitida no máximo uma vez por ciclo de despertar quando ao menos 1 passo foi executado, priorizando eventos de colisão com rebatedor sobre batidas de parede no lote.

2. **Loop de Simulação Fora da Thread Principal e Fronteira por `MatchFrameSnapshot`**:
   - Mover `physicsJob` para uma fila dedicada fora da thread de UI (`Dispatchers.Default.limitedParallelism(1)`), cronometrada por `System.nanoTime()`.
   - Tornar `MatchFrameSnapshot` (`arbitro.frames.value`) a única superfície de leitura lock-free para renderização 2D/3D e cálculo de toque (`pointerInput`), protegendo mutações em `ArbitroVirtual` com trava de simulação dedicada para manter resposta síncrona de toque e pausa sem fila assíncrona.

3. **Independência de Taxa de Quadros em Efeitos 2D/3D e Supressão de Redesenho Ocioso**:
   - Normalizar por `dt` real as animações visuais em 3D e 2D (`stepSmoothPose`, rotação giroscópica do disco, `VisualShockwave`, `VisualShockwave3D` e amortecimento exponencial de `SparkParticle`), evitando redesenho contínuo do `Canvas` sobreposto ao Filament quando não há trilha, partículas ou aura de ponto ativa.

4. **Resolução Dinâmica no Filament e Extensão do Perfil de Efeitos Visuais para 3D**:
   - Extrair configurador compartilhado de `View` Filament para a arena 3D e a vitrine de customização habilitando `dynamicResolutionOptions` (`minScale = 0.65f`, `maxScale = 1.0f`, `homogeneousScaling = true`, `quality = QualityLevel.LOW`) e passes de bloom mais leves (`levels = 4`, `resolution = 256`).
   - Fazer `VisualEffectsProfile.OFF` desativar tanto os efeitos em `Canvas` 3D quanto o passe de bloom HDR em partida (preservando `neonBloomIntensity` nas preferências e mantendo o controle ao vivo na vitrine de customização).

## Consequências
- **Positiva**: A contagem regressiva e a velocidade física do disco passam a operar em ritmo 1:1 com o relógio real independentemente do FPS ou de engasgos de GPU/UI.
- **Positiva**: A renderização em dispositivos com GPU modesta (ex.: Galaxy Tab A9) ganha alívio adaptativo de preenchimento via resolução dinâmica e respeito real ao modo leve (`VisualEffectsProfile.OFF`), sem introduzir configurações técnicas fragmentadas no menu.
- **Cuidados**: Leituras futuras de estado físico na UI devem continuar consumindo `MatchFrameSnapshot` em vez de acessar campos mutáveis de `AirHockeyPhysicsEngine` diretamente.
