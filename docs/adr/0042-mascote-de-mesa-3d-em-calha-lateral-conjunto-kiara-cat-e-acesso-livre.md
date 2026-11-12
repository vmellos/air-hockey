# ADR 0042: Mascote de Mesa 3D em Calha Lateral, Conjunto Temático Kiara Cat e Paridade Gráfica Livre

## Status
Aceito (Complementa a ADR 0029, ADR 0030, ADR 0031 e ADR 0038)

## Contexto
1. O ecossistema de personalização visual do jogo é composto por 11 conjuntos esportivos (Mesa Clássica e 10 arenas temáticas dos 100 níveis do Modo Fases). Todas as arenas temáticas exigem progressão sequencial para liberação (ADR 0031).
2. Deseja-se introduzir uma nova temática felina inspirada na gatinha Kiara ("Kiki") com a presença de sua mascote tridimensional ([kiara_3d.glb](file:///home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/assets/models/kiara_3d.glb)) acompanhando as partidas em tempo real.
3. Decisões arquiteturais críticas se impuseram:
   - **Isolamento da Física e Colisão**: A mascote 3D não pode interferir na geometria métrica da mesa, no cálculo de colisão do disco ou na área de movimentação dos rebatedores.
   - **Desobstrução do HUD e Câmera**: Na lateral direita da mesa fica ancorado o Placar Lateral Temático e os controles ergonômicos da arena; a inserção da mascote não pode cobrir o placar nem ser obstruída por ele.
   - **Desacoplamento do Modo Fases**: Como a campanha dos 100 níveis já está rigidamente balanceada em 10 blocos esportivos de 10 fases (Pong, Futebol, Basquete, Vôlei, Gridiron, Beisebol, Wasteland, Synthwave, Tokamak e Coliseu), a nova arena não deve desregular os marcos de Conquista de Arena existentes.
   - **Paridade Gráfica Estrita (2D e 3D)**: O jogo suporta alternância instantânea entre 2D (Compose Canvas) e 3D (Sceneview/Filament), exigindo que a identidade da Kiara se expresse com o mesmo primor em ambas as dimensões.

## Decisão

1. **Tema de Mesa e Acesso Livre por Padrão**:
   - Cadastrou-se o identificador canônico `TableTheme.KIARA_CAT` configurado como **Livre por Padrão** (`isDefaultUnlocked = true` / `unlockStage = 0`), tornando-o permanentemente acessível no Seletor Pré-Jogo e na Vitrine de Customização desde o primeiro instante, sem depender de conquistas no Modo Fases.

2. **Conjunto Temático Esportivo Completo**:
   - Expansão da identidade em kit cosmético integrado:
     - **Mesa**: `TableTheme.KIARA_CAT` («Kiara Cat Arcade»).
     - **Disco**: `PuckSkin.KIARA_PAW` («Patinha Neon»).
     - **Rebatedores**: `MalletSkin.NEKO_STRIKER` («Neko Striker»).

3. **Mascote da Mesa 3D com Patrulhamento em Ferradura, Locomoção Quadrúpede e Alerta do Disco**:
   - Eliminação do pedestal estático (`Studio_Pedestal`) e equipagem esquelética no Blender com ossos funcionais para patas dianteiras e traseiras (`Paw_L`, `Paw_R`, `Haunch_L`, `Haunch_R`), suportando 4 ações canônicas exportadas no glTF: `'Idle'`, `'Walk'` (trote quadrúpede felino realista), `'LickPaw'` (limpeza facial) e `'Swat'` (patadinha lúdica em direção ao vidro neon).
   - Introdução do [KiaraMascotController](file:///home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/java/dev/vmellos/airhockey/feature/game/ui/KiaraMascotController.kt): máquina de estados cinemática que comanda a gatinha a percorrer o **Circuito de Patrulha da Mascote** em ferradura expandida ("U") pelas calhas laterais no chão da arena ($X \approx \pm 0.74\text{m}$) e pelo topo panorâmico atrás da meta adversária ($Z \approx -1.30\text{m}$), desobstruindo a área de toque do Jogador 1.
   - Sensor de proximidade com o disco: quando o disco passa a menos de $0.35\text{m}$ da borda onde a Kiara está, ela reage com rastreamento visual e patada na barreira neon, pavimentando a futura **Interferência Felina (Mascote Ativa)**.
   - Calibração de enquadramento 3D em [Table3DCameraProjection](file:///home/vinicius/AndroidStudioProjects/AirHockey/app/src/main/java/dev/vmellos/airhockey/feature/physics/Table3DCameraProjection.kt): elevação suave da câmera no tema `KIARA_CAT` ($Y = 2.70\text{m}, Z = 2.30\text{m}$ no modo Padrão) para enquadrar a mascote com perfeição logo na abertura, sem exigir troca para o zoom Amplo.

4. **Paridade na Dimensão de Renderização 2D com Circuito Móvel e Rastro de Patinhas**:
   - Em `AirHockeyArenaCanvas.kt`: a silhueta neon da Kiara percorre continuamente o perímetro da mesa em velocidade sincronizada, soltando pegadas de patinhas neon com decaimento suave (fade out de 1,2s) no Compose Canvas.

5. **Geração de Malhas 3D Dedicadas via Blender Procedural**:
   - Criação das malhas PBR correspondentes (`mesa_kiara_cat.glb`, `disco_kiara_paw.glb`, `rebatedor_kiara_p1.glb`, `rebatedor_kiara_p2.glb`, `mesa_kiara_cat_half_p1.glb` e `half_p2.glb`) integradas ao pipeline de compilação de ativos.

6. **Síntese de Áudio Procedural Dedicada**:
   - Extensão do motor `ProceduralAudioSynthesizer.kt` para gerar trilha melódica synthwave pop chiptune a 124 BPM e efeitos de colisão/gol com modulação harmônica alusiva a miados lúdicos sem inclusão de arquivos de áudio estáticos pesados.

## Consequências
- **Positiva**: Experiência visual rica e carismática com a Kiara 3D viva ao lado da mesa sem qualquer interferência na física de colisão.
- **Positiva**: Paridade absoluta entre os modos 2D e 3D e compatibilidade com todas as orientações e modalidades (Solo, Dividida, Conectada e Estendida).
- **Positiva**: Preservação intacta da progressão e balanceamento dos 100 níveis do Modo Fases.
- **Positiva**: Zero overhead de tamanho com áudio estático graças à síntese procedural.
