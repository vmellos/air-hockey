package dev.vmellos.airhockey.feature.game.ui

import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.TableTheme

/**
 * Resolvedor canônico de Kits de Ativos 3D (.glb).
 * Mapeia os temas e aparências selecionados para os modelos tridimensionais
 * de alta fidelidade compilados em assets/models, com fallback automático
 * para os modelos clássicos da mesa arcade durante a transição do catálogo.
 */
object Theme3DAssetResolver {

    /**
     * Retorna o caminho do arquivo .glb para a Mesa 3D do tema selecionado.
     * Na Mesa Estendida (isExtended == true), entrega o modelo dedicado de Meia-Mesa (half table),
     * diferenciando Anfitrião (P1) e Convidado (P2).
     */
    fun getTable3DAsset(
        theme: TableTheme,
        isExtended: Boolean = false,
        isPlayer1: Boolean = true
    ): String = when {
        isExtended -> {
            if (isPlayer1) "models/mesa_arcade_half_p1.glb" else "models/mesa_arcade_half_p2.glb"
        }
        else -> when (theme) {
            TableTheme.CYBER_SOCCER -> "models/mesa_cyber_soccer.glb"
            TableTheme.NEO_BASKETBALL -> "models/mesa_neo_basketball.glb"
            TableTheme.GRAVITY_VOLLEY -> "models/mesa_gravity_volley.glb"
            TableTheme.GRIDIRON_FOOTBALL -> "models/mesa_gridiron_football.glb"
            TableTheme.CYBER_BASEBALL -> "models/mesa_cyber_baseball.glb"
            TableTheme.WASTELAND_APOCALYPSE -> "models/mesa_wasteland_apocalypse.glb"
            TableTheme.RETRO_SYNTHWAVE -> "models/mesa_retro_synthwave.glb"
            TableTheme.TOKAMAK_REACTOR -> "models/mesa_tokamak_reactor.glb"
            TableTheme.CYBER_PONG -> "models/mesa_cyber_pong.glb"
            TableTheme.CYBER_COLOSSEUM -> "models/mesa_cyber_colosseum.glb"
            TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D -> "models/mesa_arcade.glb"
        }
    }

    /**
     * Retorna o modelo 3D correspondente à aparência do disco equipada ou ao tema ativo.
     */
    fun getPuck3DAsset(skin: PuckSkin, theme: TableTheme): String = when {
        skin == PuckSkin.CYBER_SOCCER_BALL || theme == TableTheme.CYBER_SOCCER -> "models/disco_cyber_soccer.glb"
        skin == PuckSkin.NEO_BASKETBALL || theme == TableTheme.NEO_BASKETBALL -> "models/disco_neo_basketball.glb"
        skin == PuckSkin.GRAVITY_VOLLEY || theme == TableTheme.GRAVITY_VOLLEY -> "models/disco_gravity_volley.glb"
        skin == PuckSkin.GRIDIRON_FOOTBALL || theme == TableTheme.GRIDIRON_FOOTBALL -> "models/disco_gridiron_football.glb"
        skin == PuckSkin.CYBER_BASEBALL || theme == TableTheme.CYBER_BASEBALL -> "models/disco_cyber_baseball.glb"
        skin == PuckSkin.SAWBLADE || theme == TableTheme.WASTELAND_APOCALYPSE -> "models/disco_wasteland_apocalypse.glb"
        skin == PuckSkin.SYNTH_DISC || theme == TableTheme.RETRO_SYNTHWAVE -> "models/disco_retro_synthwave.glb"
        skin == PuckSkin.ATOMIC_CORE || theme == TableTheme.TOKAMAK_REACTOR -> "models/disco_tokamak_reactor.glb"
        skin == PuckSkin.PONG_SQUARE || theme == TableTheme.CYBER_PONG -> "models/disco_cyber_pong.glb"
        skin == PuckSkin.GLADIATOR_SHIELD || theme == TableTheme.CYBER_COLOSSEUM -> "models/disco_cyber_colosseum.glb"
        else -> "models/disco_cyber.glb"
    }

    /**
     * Retorna o modelo 3D do rebatedor do Jogador 1 ou Jogador 2.
     */
    fun getMallet3DAsset(skin: MalletSkin, isPlayer1: Boolean, theme: TableTheme): String = when {
        theme == TableTheme.CYBER_SOCCER || skin == MalletSkin.MAGNETIC_STRIKER -> {
            if (isPlayer1) "models/rebatedor_cyber_soccer_p1.glb" else "models/rebatedor_cyber_soccer_p2.glb"
        }
        theme == TableTheme.NEO_BASKETBALL || skin == MalletSkin.DUNK_STRIKER -> {
            if (isPlayer1) "models/rebatedor_neo_basketball_p1.glb" else "models/rebatedor_neo_basketball_p2.glb"
        }
        theme == TableTheme.GRAVITY_VOLLEY || skin == MalletSkin.SPIKE_EMITTER -> {
            if (isPlayer1) "models/rebatedor_gravity_volley_p1.glb" else "models/rebatedor_gravity_volley_p2.glb"
        }
        theme == TableTheme.GRIDIRON_FOOTBALL || skin == MalletSkin.LINEBACKER_STRIKER -> {
            if (isPlayer1) "models/rebatedor_gridiron_football_p1.glb" else "models/rebatedor_gridiron_football_p2.glb"
        }
        theme == TableTheme.CYBER_BASEBALL || skin == MalletSkin.SLUGGER_STRIKER -> {
            if (isPlayer1) "models/rebatedor_cyber_baseball_p1.glb" else "models/rebatedor_cyber_baseball_p2.glb"
        }
        theme == TableTheme.WASTELAND_APOCALYPSE || skin == MalletSkin.RIPPER_STRIKER -> {
            if (isPlayer1) "models/rebatedor_wasteland_apocalypse_p1.glb" else "models/rebatedor_wasteland_apocalypse_p2.glb"
        }
        theme == TableTheme.RETRO_SYNTHWAVE || skin == MalletSkin.OUTRUN_STRIKER -> {
            if (isPlayer1) "models/rebatedor_retro_synthwave_p1.glb" else "models/rebatedor_retro_synthwave_p2.glb"
        }
        theme == TableTheme.TOKAMAK_REACTOR || skin == MalletSkin.MAGNETIC_COIL -> {
            if (isPlayer1) "models/rebatedor_tokamak_reactor_p1.glb" else "models/rebatedor_tokamak_reactor_p2.glb"
        }
        theme == TableTheme.CYBER_PONG || skin == MalletSkin.PONG_PADDLE -> {
            if (isPlayer1) "models/rebatedor_cyber_pong_p1.glb" else "models/rebatedor_cyber_pong_p2.glb"
        }
        theme == TableTheme.CYBER_COLOSSEUM || skin == MalletSkin.SPARTAN_AEGIS -> {
            if (isPlayer1) "models/rebatedor_cyber_colosseum_p1.glb" else "models/rebatedor_cyber_colosseum_p2.glb"
        }
        else -> {
            if (isPlayer1) "models/rebatedor_p1.glb" else "models/rebatedor_p2.glb"
        }
    }

    /**
     * Indica se o tema possui um Kit de Ativos 3D nativo modelado e homologado no app.
     */
    fun hasNative3DSupport(theme: TableTheme): Boolean = true
}
