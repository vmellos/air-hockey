# 0002 - Protocolo Híbrido de Transporte de Rede (TCP para Eventos e UDP para Telemetria)

## Contexto e Decisão
Para suportar o modo Mesa Estendida entre dois dispositivos físicos com movimentação contínua do disco e dos rebathedores a 60-120 FPS em rede local (Wi-Fi e Hotspot móvel), decidimos utilizar uma arquitetura de transporte híbrida: TCP para controle de sessão e eventos pontuais, e UDP para telemetria contínua de física.

## Justificativa e Alternativas
- **TCP Sockets**: Utilizado para handshake, negociação de resolução de mesa, início de partida, pontuação de gols, pausas e finalização de jogo. Garante entrega estrita e ordenada onde a perda de pacotes corromperia o estado da partida.
- **UDP DatagramSockets**: Utilizado para o envio contínuo das posições do disco e dos rebathedores. Em jogos físicos rápidos, a perda passageira de um pacote de posição não deve bloquear a fila de rede com retransmissões; o pacote seguinte já conterá o estado mais atual. A interpolação no cliente compensa variações normais de jitter.
