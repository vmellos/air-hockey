# 0006 - Descoberta Ativa UDP Dual-Channel e Sondagem de Gateway em Hotspot

## Contexto e Decisão
Dispositivos móveis rodando Android frequentemente enfrentam restrições em redes locais P2P:
1. O recurso de Ponto de Acesso Móvel (Hotspot) do Android bloqueia ou descarta tráfego multicast e mDNS (NSD), impedindo que clientes conectados descubram o anfitrião via `_airhockey._tcp.`.
2. Diversos roteadores residenciais e corporativos implementam isolamento de clientes (AP Isolation) ou filtram pacotes multicast para poupar banda sem fio.
3. Interfaces de dados móveis (4G/5G com CGNAT) frequentemente recebem IPs privados (ex: 10.x.x.x), confundindo heurísticas simples de detecção de IP local.

Decidimos adotar uma arquitetura de descoberta resiliente de canal duplo:
- **Canal Duplo de Descoberta (NSD + UDP Broadcast Ativo)**: O Dispositivo Anfitrião emite periodicamente beacons UDP na porta 8887 para o broadcast global (`255.255.255.255`) e broadcasts dirigidos de cada interface ativa, além de responder a datagramas de sondagem direta.
- **Sondagem de Gateway DHCP para Hotspot**: Quando o Dispositivo Convidado conecta ao Hotspot do anfitrião, o gateway DHCP informado pela rede sem fio é exatamente o IP do aparelho anfitrião. O convidado sonda diretamente o gateway e realiza varredura paralela assíncrona na sub-rede `/24` caso conecte via código numérico.
- **Isolamento de Interfaces de Rede**: O método de detecção de IP local prioriza explicitamente interfaces sem fio (`wlan`, `swlan`, `ap`, `softap`) e ignora interfaces de dados celulares (`rmnet`, `ccmni`, `pdp`).
- **QR Code ISO Padrão (Preto sobre Branco)**: Garantia de módulos escuros sobre fundo claro com margem silenciosa, maximizando a compatibilidade com sensores de câmera nativos e algoritmos de binarização óptica híbrida.

## Justificativa e Alternativas
- **Descartar dependência exclusiva de NSD**: O NSD padrão do Android falha silenciosamente em conexões Hotspot devido a restrições do kernel do Android.
- **Sem Servidor Externo na Nuvem**: O jogo permanece 100% autônomo, local e offline, permitindo partidas em qualquer lugar (praia, viagens, parques) apenas ligando o Ponto de Acesso de um dos celulares.
