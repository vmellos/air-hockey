# Desacoplamento da Dimensão de Renderização (2D vs. 3D) dos Temas de Mesa

Anteriormente, a experiência tridimensional em Sceneview/Filament foi implementada como um tema individual no catálogo (`TableTheme.ARCADE_3D`), isolando a renderização 3D em uma única aparência e forçando todos os demais temas esportivos a rodarem exclusivamente no renderizador 2D Canvas.

Decidimos desacoplar formalmente a **Dimensão de Renderização** (`2D` vs. `3D`) da seleção do **Tema de Mesa**, transformando a visualização espacial em uma capacidade universal da arena aplicável a qualquer Conjunto Temático Esportivo. O suporte 3D é expandido através de **Kits de Ativos 3D** dedicados (`mesa_*.glb`, `disco_*.glb`, `rebatedor_*_p1.glb`, `rebatedor_*_p2.glb`), iniciando com Cyber Futebol (`CYBER_SOCCER`) e Neo-Tokyo Basquete (`NEO_BASKETBALL`), acompanhado de fallback automático para o conjunto clássico nos temas ainda em transição. O Modo Tela Dividida (Split Screen) permanece obrigatoriamente restrito a 2D Canvas por razões ergonômicas de perspectiva para dois jogadores locais no mesmo aparelho.

Esta decisão elimina a proliferação artificial de entradas redundantes no enum de temas (`CYBER_SOCCER_3D`, etc.), simplifica a interface de customização e estabelece uma arquitetura uniforme e escalável para validar e publicar a versão 3D de todas as arenas do jogo.
