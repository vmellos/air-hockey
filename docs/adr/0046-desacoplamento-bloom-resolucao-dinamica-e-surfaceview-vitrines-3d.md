# 0046. Desativação da Resolução Dinâmica com Bloom Ativo e Contêiner Retangular nas Vitrines 3D

Complementa a [ADR 0043](0043-simulacao-tempo-real-desacoplada-snapshot-e-resolucao-dinamica-3d.md): desativamos `dynamicResolutionOptions` sempre que o Bloom HDR estiver ativo no Filament e removemos o recorte chanfrado (`CutCornerShape`) ao redor do `SurfaceView` nas vitrines 3D para eliminar vazamentos de neon pelas bordas da tela.

## Contexto e Decisão

1. **Vazamento de halos nas bordas em dispositivos com subescala dinâmica:** Ao ativar `dynamicResolutionOptions` (`minScale = 0.65f`, `quality = View.QualityLevel.LOW`) em conjunto com o passe de `Bloom` (`quality` padrão `LOW`) em `FilamentViewConfig.kt`, aparelhos cuja GPU ou orçamento de quadro (ex.: 120 Hz no Galaxy S26 vs. S24+) reduz a escala interna abaixo de `1.0x` exibem artefatos de downsampling/upsampling na pirâmide de bloom ao longo das bordas do viewport (conforme alertado pelo próprio `View.java` do Filament).
2. **Recorte de `SurfaceView` nas vitrines 3D:** `ArenaCustomizationScreen` e `PreMatchArenaSelectionScreen` envolviam `Arena3DLivePreview` (`Scene` / `SurfaceView`) com `.clip(CutCornerShape(12.dp))`, produzindo artefatos de composição na camada separada do `SurfaceView` (mesmo padrão diagnosticado em `LandingSimulator`).

Decidimos:
1. **Exclusão mútua reativa entre Bloom HDR e Resolução Dinâmica:** Em `FilamentViewConfig.kt`, `updateFilamentArenaBloom` passa a aplicar `dynamicResolutionOptions.enabled = !isBloomActive` (mantendo a resolução dinâmica apenas quando o bloom estiver desligado, como em `VisualEffectsProfile.OFF` ou `0%`), preservando o brilho neon em partida e na vitrine sem subescala.
2. **Filtro de Bloom em `QualityLevel.MEDIUM`:** Elevar `bloomOptions.quality` de `LOW` para `View.QualityLevel.MEDIUM`, mantendo `highlight = 0.85f` e a Curva Gama de Bloom inalteradas.
3. **Vitrines 3D com contêiner retangular sólido:** Remover `.clip(CutCornerShape(12.dp))` do contêiner de pré-visualização em `ArenaCustomizationScreen` e `PreMatchArenaSelectionScreen`, usando fundo sólido `#030712` e borda retangular uniforme para 2D e 3D.

## Consequências

- **Positiva:** Elimina o vazamento periférico de neon em partidas 3D e vitrines sem apagar o brilho neon nem quebrar o Controle Direto de Bloom Neon.
- **Trade-off:** Dispositivos com Bloom ativo renderizam sempre na resolução nativa (`1.0x`), podendo recorrer ao `VisualEffectsProfile.OFF` caso necessitem da resolução dinâmica para desempenho.
