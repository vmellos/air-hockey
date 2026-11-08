# 0039. Calibração de Dificuldade Gradual, Salvaguarda de Autogol, Regra do Disco Preso e Curva Gama de Bloom Neon

Adotamos a parametrização contínua da IA nas 100 fases, a salvaguarda de autogol vetorial e a intervenção de Disco Preso no Árbitro Virtual para eliminar travamentos e solavancos bruscos na progressão competitiva, combinados com a sincronização cosmética de perfil no Google Play Games e a curva gama com threshold HDR no Filament 3D.

## Contexto e Decisão

Observou-se na Conquista da Arena um salto abrupto de dificuldade após a fase 20 (devido à transição direta do enum Fácil com 900f/s e latência de 280ms para o Médio com 3200f/s e reação instantânea), ocasionando frenesi e gols contra em disputas na própria meta defensiva. Adicionalmente, discos parados em cantos profundos ficavam retidos indefinidamente porque o controlador de IA permanecia estacionado protegendo a boca do gol sem contorno ofensivo, sem que o Árbitro Virtual declarasse retenção. Na renderização 3D, o Bloom Filament com limiar desativado (`threshold = false`) e mapeamento linear saturava os brancos já a partir de 30% da barra de intensidade. Por fim, o payload de salvamento em nuvem não sincronizava a identidade visual equipada nem o nome real customizado pelo jogador.

Decidimos:
1. **Curva Gradual de Dificuldade**: Interpolar continuamente os atributos da IA de forma linear entre a Fase 1 (900f/s, 320ms de latência, ±70px de erro de mira) e a Fase 100 (5200f/s, 0ms de latência, 0px de dispersão), ativando tabelas gradualmente a partir da Fase 30.
2. **Salvaguarda de Autogol e Resgate de Canto**: Desacelerar o taco em 50% e executar arcos de contorno quando o disco estiver entre o taco e a própria meta, além de liberar o avanço seguro sobre discos parados nos cantos profundos.
3. **Regra de Disco Preso**: Fazer o Árbitro Virtual reiniciar o disco suavemente no ponto de saída neutro caso permaneça imóvel ou inerte por mais de 3,5 segundos sem intervenção dos jogadores.
4. **Sincronização Cosmética no Google Play Games**: Incluir no `PlayGamesSavePayload` o Tema de Mesa, Aparência de Disco, Aparência de Rebatedores, Dimensão da Arena (2D/3D) e sincronizar o nome real do atleta entre o DataStore e o banco Room.
5. **Curva Gama de Bloom Neon**: Habilitar `threshold = true` com `highlight = 1.0f` no Filament e adotar mapeamento quadrático no controle deslizante, garantindo brilho sutil em 10-30%, intensidade vibrante arcade em 50% e suprimindo superexposição em 100%.
