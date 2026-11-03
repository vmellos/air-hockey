# ⚡ Air Hockey Arcade

> **O fliperama arcade definitivo de alta velocidade para Android, desenvolvido nativamente com Jetpack Compose Canvas e física vetorial contínua.**

[![Platform](https://img.shields.io/badge/Platform-Android%2014%2B%20(API%2024%2B)-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024-purple.svg)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVI%2FMVVM-orange.svg)](#arquitetura-e-engenharia)
[![License](https://img.shields.io/badge/License-Proprietary-lightgrey.svg)](#)

---

## 🎨 1. Estilo Visual e Direção de Arte: Cyberpunk Neon & Dark Glassmorphism

* **Estética Sci-Fi / Retro-Futurista**: Cores de altíssimo contraste meticulosamente calibradas para displays modernos (com destaque para telas AMOLED/OLED), contrastando um fundo cósmico profundo com emissão de luz neon brilhante: **Ciano Elétrico** (`#00E5FF`), **Laranja/Vermelho Incandescente** (`#FF5722`), **Magenta Plasma** (`#A855F7`) e **Dourado Estelar**.
* **Dark Glassmorphism & Geometria Chanfrada**: Painéis táteis futuristas construídos com acabamento translúcido de vidro fumê, bordas iluminadas e cantos chanfrados em 45º (`CutCornerShape`), evocando a identidade visual de terminais de combate cyberpunk.
* **Disco de Alta Energia / Plasma**: O disco desliza como um emissor toroidal de energia com rastro dinâmico reativo de plasma (labaredas de fogo azul ao ser golpeado pelo Jogador 1 e fogo vermelho pelo Jogador 2), complementado por explosões de faíscas incandescentes em colisões laterais e vórtices de sucção luminosa nas contagens regressivas.
* **Rebatedores Pseudo-3D**: Renderização procedural direta no Canvas com gradientes radiais sobrepostos, anéis concêntricos de empunhadura, sombras projetadas em tempo real e pontos de luz especular simulando manoplas físicas de arcade.
* **Placar Lateral Numérico Minimalista**: Posicionado verticalmente na borda lateral direita na altura da linha divisória central, desobstruindo integralmente o campo de visão e a zona de defesa dos participantes.

---

## 🕹️ 2. Estilo de Gameplay e Mecânica: Arcade Twitch & Física Pura

* **Ritmo Frenético e Reflexo Rápido**: Motor de física 2D customizado baseado em mecânica clássica de ricochete elástico, conservação de momento, atrito cinético ajustável e detecção contínua de colisões sem clipping de alta velocidade.
* **Vitória por Pontuação Alvo (Soberana)**: Eliminação de cronômetros arbitrários e empates sem emoção. O duelo avança até que o participante alcance com maestria a meta estabelecida (3, 5, 7 ou 10 pontos).
* **Ergonomia Competitiva no Touch**:
  * **Deslocamento de Toque (*Touch Offset*)**: O rebatedor flutua ligeiramente acima do ponto de contato do dedo, garantindo que a mão nunca bloqueie a trajetória ou a leitura de aproximação do disco.
  * **Raio de Agarre (*Grab Radius*)**: Trava de ancoragem posicional que impede saltos anômalos e "teletransportes" do rebatedor disparados por cliques distantes ou toques acidentais na mesa.
  * **Controle de Pausa Protegido**: Ausência de botões flutuantes intrusivos na arena; acionamento de pausa seguro e exclusivo via botão/gesto nativo de Voltar do sistema.

---

## 🏆 3. Modos de Jogo e Estrutura

### 📱📱 Mesa Estendida (*Dual Screen* / 2 Aparelhos)
O grande diferencial tecnológico do projeto. Dois smartphones colocados em **Disposição Topo-a-Topo** transformam-se em uma única mesa contínua e imersiva:
* Pareamento automático na rede local sem fio (Wi-Fi ou Hotspot móvel) via protocolo híbrido **NSD (mDNS) + UDP Broadcast + TCP Sockets**.
* Compensação milimétrica nativa do **Vão Físico** (*Bezel Gap*) entre as molduras dos dois dispositivos, permitindo trânsito suave do disco entre as duas telas.
* Início imediato com **Largada Sincronizada** e contagem 3-2-1 simultânea após validação de conexão.

### 🌟 Modo Fases (Campanha Solo Arcade)
Uma jornada estruturada de **100 níveis progressivos**:
* Avaliação de desempenho de **1 a 3 estrelas** por eficiência defensiva.
* Oponentes virtuais com inteligência tática autônoma: predição de trajetória, leitura de vulnerabilidades de cobertura e mira anti-oponente direcionada ao lado desprotegido da meta rival.

### 👥 Mesa Dividida (Mesmo Aparelho)
Confronto clássico e veloz para dois participantes dividindo a mesma tela de celular ou tablet em extremidades opostas.

### 🤖 Modo IA x IA (Modo Espectador)
Simulação autônoma de confronto entre inteligências artificiais com configurações independentes de dificuldade (Fácil, Médio, Difícil), disponível tanto localmente quanto projetado em Mesa Estendida com controle total de pausa e visualização.

---

## 🚀 4. Filosofia de Produto: Fliperama de Bolso sem Fricção

* **100% Offline & Sem Fricção**: Sem telas de cadastro, sem login obrigatório, sem dependência de nuvem e sem compras agressivas.
* **Zero Propagandas Invasivas**: Foco exclusivo em esportividade, performance e imersão pura de fliperama.
* **Internacionalização Nativa (I18n)**: Suporte completo e dinâmico a 3 idiomas: **Inglês (`en`)**, **Português (`pt`)** e **Espanhol (`es`)**, incluindo formatação localizada de robôs, dificuldades e menus.
* **Renderização Vetorial Ultra-Otimizada**:
  * Loop gráfico desacoplado sincronizado com o VSYNC (`withFrameNanos`).
  * **Zero alocações de memória por frame** (Zero GC pauses), mantendo taxas estáveis de **60 a 120 FPS** mesmo durante rajadas intensas de partículas.

---

## 🛠️ Stack Tecnológica & Arquitetura

* **Linguagem**: Kotlin 2.0+
* **Interface**: Jetpack Compose (Material 3 + Compose Canvas nativo)
* **Gerenciamento de Estado**: StateFlow, SharedFlow, ViewModel (AndroidX Lifecycle)
* **Arquitetura**: Clean Architecture + MVI/MVVM reativo
* **Rede & Descoberta Local**:
  * Network Service Discovery (Android `NsdManager`)
  * Beacons e consultas dirigidas via UDP Datagram Sockets (Porta `8887`)
  * Troca de telemetria e controle em tempo real via TCP Sockets (Porta `8888`)
* **Armazenamento**: Jetpack DataStore Preferences & Room Database
* **Áudio Procedural**: Motor SoundPool otimizado com baixa latência e trilhas de synthwave adaptativas

---

## 📂 Estrutura do Projeto

```
AirHockey/
├── app/src/main/
│   ├── java/dev/vmellos/airhockey/
│   │   ├── core/                  # Preferências, áudio, localização e utilitários
│   │   ├── feature/
│   │   │   ├── game/              # Loop de jogo, física, projeção e overlays do Canvas
│   │   │   ├── lobby/             # Conexão multijogador, pareamento e UI de sala
│   │   │   ├── network/           # Sockets TCP/UDP, transporte de rede e NSD
│   │   │   ├── stage/             # Progressão de 100 níveis e pontuação por estrelas
│   │   │   ├── menu/              # Menus táteis neon e navegação arcade
│   │   │   └── physics/           # Árbitro virtual, colisões e dimensões da mesa
│   │   └── ui/theme/              # Paleta Cyberpunk Neon e tipografia
│   └── res/
│       ├── values/                # Strings padrão (Inglês)
│       ├── values-pt/             # Tradução (Português)
│       └── values-es/             # Tradução (Espanhol)
├── GLOSSARY.md                    # Vocabulário e entidades canônicas do domínio
└── README.md                      # Apresentação e documentação técnica do projeto
```

---

## 🏗️ Como Compilar e Executar

1. Clone o repositório:
   ```bash
   git clone https://github.com/vmellos/AirHockey.git
   cd AirHockey
   ```
2. Abra o projeto no **Android Studio Hedgehog** (ou superior).
3. Execute o build e a suíte de testes unitários:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Instale e execute em um dispositivo físico ou emulador com **Android 7.0 (API 24)** ou superior:
   ```bash
   ./gradlew installDebug
   ```
