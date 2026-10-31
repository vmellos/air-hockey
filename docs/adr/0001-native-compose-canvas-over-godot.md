# 0001 - Utilização de Jetpack Compose Canvas Nativo em vez de Godot Engine

## Contexto e Decisão
Para implementar o Air Hockey com suporte a partidas em dois dispositivos físicos pareados localmente, precisávamos escolher a base tecnológica entre a engine Godot 4.3 (instalada no ambiente) e uma solução nativa em Android com Jetpack Compose Canvas. Decidimos construir o jogo inteiramente em Kotlin nativo com Jetpack Compose Canvas desacoplado, sem utilizar a Godot Engine.

## Justificativa e Alternativas
- **Reuso de I18n e Arquitetura do `iptv_app`**: O projeto de referência utiliza `LocaleHelper`, `ProvideAppLocale` e catálogos `strings.xml` em Jetpack Compose. No Compose nativo, esse subsistema é reutilizado integralmente, enquanto na Godot exigiria uma ponte JNI complexa e dois sistemas de UI distintos.
- **Rede P2P e Descoberta Local**: A integração com Android NSD (Network Service Discovery / mDNS), `MulticastLock` e criação de sockets locais é nativa e direta em Kotlin Coroutines, sem atritos de permissões ou empacotamento externo.
- **Simplicidade da Física**: A mecânica 2D de Air Hockey (colisão elástica círculo-círculo e círculo-segmento) requer menos de 250 linhas de código determinístico, dispensando o peso de uma engine inteira (~40MB adicionais de APK).
- **Desempenho**: O Canvas acelerado por hardware com corrotinas de frame (`withFrameNanos`) sustenta taxas estáveis de 60 a 120 FPS com consumo mínimo de bateria.
