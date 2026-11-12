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
        isExtended -> when (theme) {
            TableTheme.CYBER_SOCCER -> if (isPlayer1) "models/mesa_cyber_soccer_half_p1.glb" else "models/mesa_cyber_soccer_half_p2.glb"
            TableTheme.NEO_BASKETBALL -> if (isPlayer1) "models/mesa_neo_basketball_half_p1.glb" else "models/mesa_neo_basketball_half_p2.glb"
            TableTheme.GRAVITY_VOLLEY -> if (isPlayer1) "models/mesa_gravity_volley_half_p1.glb" else "models/mesa_gravity_volley_half_p2.glb"
            TableTheme.GRIDIRON_FOOTBALL -> if (isPlayer1) "models/mesa_gridiron_football_half_p1.glb" else "models/mesa_gridiron_football_half_p2.glb"
            TableTheme.CYBER_BASEBALL -> if (isPlayer1) "models/mesa_cyber_baseball_half_p1.glb" else "models/mesa_cyber_baseball_half_p2.glb"
            TableTheme.WASTELAND_APOCALYPSE -> if (isPlayer1) "models/mesa_wasteland_apocalypse_half_p1.glb" else "models/mesa_wasteland_apocalypse_half_p2.glb"
            TableTheme.RETRO_SYNTHWAVE -> if (isPlayer1) "models/mesa_retro_synthwave_half_p1.glb" else "models/mesa_retro_synthwave_half_p2.glb"
            TableTheme.TOKAMAK_REACTOR -> if (isPlayer1) "models/mesa_tokamak_reactor_half_p1.glb" else "models/mesa_tokamak_reactor_half_p2.glb"
            TableTheme.CYBER_PONG -> if (isPlayer1) "models/mesa_cyber_pong_half_p1.glb" else "models/mesa_cyber_pong_half_p2.glb"
            TableTheme.CYBER_COLOSSEUM -> if (isPlayer1) "models/mesa_cyber_colosseum_half_p1.glb" else "models/mesa_cyber_colosseum_half_p2.glb"
            TableTheme.KIARA_CAT -> if (isPlayer1) "models/mesa_kiara_cat_half_p1.glb" else "models/mesa_kiara_cat_half_p2.glb"
            TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D -> if (isPlayer1) "models/mesa_arcade_half_p1.glb" else "models/mesa_arcade_half_p2.glb"
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
            TableTheme.KIARA_CAT -> "models/mesa_kiara_cat.glb"
            TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D -> "models/mesa_arcade.glb"
        }
    }

    /**
     * Retorna o modelo 3D correspondente à aparência do disco equipada ou ao tema ativo.
     * Se o jogador equipou uma skin personalizada (não padrão), ela é preservada rigorosamente.
     * Caso contrário, adota o disco temático correspondente à arena ativa.
     */
    fun getPuck3DAsset(skin: PuckSkin, theme: TableTheme = TableTheme.CLASSIC_NEON): String = when {
        skin != PuckSkin.CLASSIC_PLASMA -> when (skin) {
            PuckSkin.CYBER_SOCCER_BALL -> "models/disco_cyber_soccer.glb"
            PuckSkin.NEO_BASKETBALL -> "models/disco_neo_basketball.glb"
            PuckSkin.GRAVITY_VOLLEY -> "models/disco_gravity_volley.glb"
            PuckSkin.GRIDIRON_FOOTBALL -> "models/disco_gridiron_football.glb"
            PuckSkin.CYBER_BASEBALL -> "models/disco_cyber_baseball.glb"
            PuckSkin.SAWBLADE -> "models/disco_wasteland_apocalypse.glb"
            PuckSkin.SYNTH_DISC -> "models/disco_retro_synthwave.glb"
            PuckSkin.ATOMIC_CORE -> "models/disco_tokamak_reactor.glb"
            PuckSkin.PONG_SQUARE -> "models/disco_cyber_pong.glb"
            PuckSkin.GLADIATOR_SHIELD -> "models/disco_cyber_colosseum.glb"
            PuckSkin.KIARA_PAW -> "models/disco_kiara_paw.glb"
            PuckSkin.CLASSIC_PLASMA -> "models/disco_cyber.glb"
        }
        else -> when (theme) {
            TableTheme.CYBER_SOCCER -> "models/disco_cyber_soccer.glb"
            TableTheme.NEO_BASKETBALL -> "models/disco_neo_basketball.glb"
            TableTheme.GRAVITY_VOLLEY -> "models/disco_gravity_volley.glb"
            TableTheme.GRIDIRON_FOOTBALL -> "models/disco_gridiron_football.glb"
            TableTheme.CYBER_BASEBALL -> "models/disco_cyber_baseball.glb"
            TableTheme.WASTELAND_APOCALYPSE -> "models/disco_wasteland_apocalypse.glb"
            TableTheme.RETRO_SYNTHWAVE -> "models/disco_retro_synthwave.glb"
            TableTheme.TOKAMAK_REACTOR -> "models/disco_tokamak_reactor.glb"
            TableTheme.CYBER_PONG -> "models/disco_cyber_pong.glb"
            TableTheme.CYBER_COLOSSEUM -> "models/disco_cyber_colosseum.glb"
            TableTheme.KIARA_CAT -> "models/disco_kiara_paw.glb"
            TableTheme.CLASSIC_NEON, TableTheme.ARCADE_3D -> "models/disco_cyber.glb"
        }
    }

    /**
     * Retorna o modelo 3D do rebatedor do Jogador 1 ou Jogador 2.
     * Desacoplado do tema da mesa: o jogador preserva estritamente a skin equipada,
     * impedindo sobreposições involuntárias antes do desbloqueio da arena (ADR 0040).
     */
    fun getMallet3DAsset(skin: MalletSkin, isPlayer1: Boolean, theme: TableTheme = TableTheme.CLASSIC_NEON): String = when (skin) {
        MalletSkin.MAGNETIC_STRIKER -> if (isPlayer1) "models/rebatedor_cyber_soccer_p1.glb" else "models/rebatedor_cyber_soccer_p2.glb"
        MalletSkin.DUNK_STRIKER -> if (isPlayer1) "models/rebatedor_neo_basketball_p1.glb" else "models/rebatedor_neo_basketball_p2.glb"
        MalletSkin.SPIKE_EMITTER -> if (isPlayer1) "models/rebatedor_gravity_volley_p1.glb" else "models/rebatedor_gravity_volley_p2.glb"
        MalletSkin.LINEBACKER_STRIKER -> if (isPlayer1) "models/rebatedor_gridiron_football_p1.glb" else "models/rebatedor_gridiron_football_p2.glb"
        MalletSkin.SLUGGER_STRIKER -> if (isPlayer1) "models/rebatedor_cyber_baseball_p1.glb" else "models/rebatedor_cyber_baseball_p2.glb"
        MalletSkin.RIPPER_STRIKER -> if (isPlayer1) "models/rebatedor_wasteland_apocalypse_p1.glb" else "models/rebatedor_wasteland_apocalypse_p2.glb"
        MalletSkin.OUTRUN_STRIKER -> if (isPlayer1) "models/rebatedor_retro_synthwave_p1.glb" else "models/rebatedor_retro_synthwave_p2.glb"
        MalletSkin.MAGNETIC_COIL -> if (isPlayer1) "models/rebatedor_tokamak_reactor_p1.glb" else "models/rebatedor_tokamak_reactor_p2.glb"
        MalletSkin.PONG_PADDLE -> if (isPlayer1) "models/rebatedor_cyber_pong_p1.glb" else "models/rebatedor_cyber_pong_p2.glb"
        MalletSkin.SPARTAN_AEGIS -> if (isPlayer1) "models/rebatedor_cyber_colosseum_p1.glb" else "models/rebatedor_cyber_colosseum_p2.glb"
        MalletSkin.NEKO_STRIKER -> if (isPlayer1) "models/rebatedor_kiara_p1.glb" else "models/rebatedor_kiara_p2.glb"
        MalletSkin.CLASSIC_NEON -> if (isPlayer1) "models/rebatedor_p1.glb" else "models/rebatedor_p2.glb"
    }

    /**
     * Retorna o ativo da Mascote 3D de mesa caso o tema possua uma mascote acompanhante.
     */
    fun getMascot3DAsset(theme: TableTheme): String? = when (theme) {
        TableTheme.KIARA_CAT -> "models/kiara_3d.glb"
        else -> null
    }

    /**
     * Indica se o tema possui um Kit de Ativos 3D nativo modelado e homologado no app.
     */
    fun hasNative3DSupport(theme: TableTheme): Boolean = true
}
