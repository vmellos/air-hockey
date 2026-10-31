# 0004 - Pareamento Híbrido Triplo (NSD, Código Numérico e QR Code Offline)

## Contexto e Decisão
Para conectar dois dispositivos físicos na Mesa Estendida sem depender de internet externa e sem impor atrito de permissões, decidimos adotar um sistema de pareamento triplo: Descoberta Automática de Rede (NSD/mDNS), Código Numérico de 4 dígitos e Leitura de Código QR offline com permissão de câmera sob demanda.

## Justificativa e Alternativas
- **Zero Permissões Obrigatórias**: Nem a descoberta automática via NSD nem o código numérico exigem permissões de tempo de execução no Android, permitindo entrada imediata na partida.
- **Conveniência do QR Code**: O anfitrião desenha o Código QR na tela e o convidado pode escaneá-lo. A permissão de câmera é solicitada estritamente quando o usuário clica no botão "Escanear", nunca na inicialização do app.
- **Funcionamento Offline**: Todo o processamento de geração e leitura do Código QR opera localmente sem chamadas a serviços de nuvem, viabilizando o jogo via Hotspot Wi-Fi em ambientes sem sinal de internet.
