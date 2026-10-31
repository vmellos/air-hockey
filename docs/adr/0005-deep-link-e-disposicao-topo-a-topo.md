# 0005 - Pareamento Direto via Deep Link e Consolidação da Disposição Topo-a-Topo

## Contexto e Decisão
Para simplificar a conexão de dois aparelhos na Mesa Estendida e garantir a melhor experiência física sem atritos, decidimos integrar Deep Linking do sistema operacional (`airhockey://connect`) e consolidar exclusivamente a Disposição Topo-a-Topo na vertical.

## Justificativa e Alternativas
- **Pareamento com Câmera Nativa do Celular**: Ao registrar um intent-filter para o esquema `airhockey`, o usuário pode apontar a própria câmera padrão do Android (ou Google Lens) para o Código QR do Dispositivo Anfitrião e entrar na partida instantaneamente, sem precisar navegar manualmente pelo menu ou pelo fluxo de papéis do app.
- **Consolidação Topo-a-Topo**: Descartamos a orientação horizontal (lado a lado) porque colocar dois celulares compridos na horizontal estreita excessivamente a largura da mesa e distorce a proporção das metas. A disposição vertical Topo-a-Topo oferece a melhor ergonomia frente a frente e permite que cada jogador defenda sua meta segurando confortavelmente a base do seu aparelho.
- **Privacidade e Conectividade Sem Fio**: A detecção de rede exibe o status de Wi-Fi e endereço IP do anfitrião sem exigir permissões invasivas de GPS/Localização, disponibilizando atalho nativo para criação de Ponto de Acesso Móvel.
