package dev.vmellos.airhockey.feature.game

import dev.vmellos.airhockey.core.preferences.AiDifficulty
import dev.vmellos.airhockey.core.preferences.ArenaDimension
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.PuckSpeedPreset
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferences
import dev.vmellos.airhockey.feature.stage.StageConfig

/**
 * Especificação Imutável de Partida de Air Hockey (GLOSSARY.md).
 *
 * Encapsula de forma profunda todos os parâmetros de configuração e calibração de uma partida,
 * provendo fábricas canônicas especializadas para cada modalidade esportiva e eliminando
 * parâmetros soltos em controladores de ciclo de vida e navegadores.
 */
data class MatchSpec(
    val mode: GameMode,
    val bezelGapMm: Float = 8.0f,
    val screenHeightPx: Float = 2400f,
    val yDpi: Float = 420f,
    val targetScore: Int = 3,
    val matchTimeSeconds: Int = 180,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val aiDifficultyPlayer1: AiDifficulty = aiDifficulty,
    val aiDifficultyPlayer2: AiDifficulty = aiDifficulty,
    val puckMaxSpeed: Float = 6800f,
    val airFriction: Float = 0.40f,
    val player1Name: String = "Jogador 1",
    val player2Name: String = "Jogador 2",
    val stageNumber: Int = 1,
    val hasIntroPresentation: Boolean = false,
    val tableTheme: TableTheme? = null,
    val arenaDimension: ArenaDimension = ArenaDimension.THREE_DIMENSIONAL,
    val localPuckSkin: PuckSkin = PuckSkin.CLASSIC_PLASMA,
    val localMalletSkin: MalletSkin = MalletSkin.CLASSIC_NEON,
    val customAiMaxSpeed: Float? = null,
    val customAiReactionDelay: Float? = null,
    val customAiAimingErrorRange: Float? = null,
    val customAiBankShotChance: Float? = null
) {
    companion object {
        /**
         * Cria uma especificação para a Conquista da Arena (Modo Fases / Campanha Solo).
         */
        fun forStage(
            stageNumber: Int,
            userPrefs: UserPreferences,
            player1Name: String,
            player2Name: String
        ): MatchSpec {
            val is3d = userPrefs.arenaDimension == ArenaDimension.THREE_DIMENSIONAL
            val hasIntro = is3d && userPrefs.arena3dIntroEnabled
            return MatchSpec(
                mode = GameMode.SOLO,
                bezelGapMm = userPrefs.bezelGapMm,
                player1Name = player1Name,
                player2Name = player2Name,
                stageNumber = stageNumber,
                hasIntroPresentation = hasIntro,
                arenaDimension = userPrefs.arenaDimension,
                localPuckSkin = userPrefs.puckSkin,
                localMalletSkin = userPrefs.player1MalletSkin,
                customAiMaxSpeed = StageConfig.calculateAiMaxSpeed(stageNumber),
                customAiReactionDelay = StageConfig.calculateAiReactionDelay(stageNumber),
                customAiAimingErrorRange = StageConfig.calculateAiAimingError(stageNumber),
                customAiBankShotChance = StageConfig.calculateAiBankShotChance(stageNumber)
            )
        }

        /**
         * Cria uma especificação para o modo Tela Dividida (Dois jogadores em um único dispositivo).
         */
        fun forSplitScreen(
            userPrefs: UserPreferences,
            chosenTheme: TableTheme,
            chosenDimension: ArenaDimension,
            player1Name: String,
            player2Name: String
        ): MatchSpec {
            val puckPreset = PuckSpeedPreset.NORMAL
            val is3d = chosenDimension == ArenaDimension.THREE_DIMENSIONAL
            val hasIntro = is3d && userPrefs.arena3dIntroEnabled
            return MatchSpec(
                mode = GameMode.SPLIT_SCREEN,
                bezelGapMm = userPrefs.bezelGapMm,
                targetScore = userPrefs.targetScore,
                matchTimeSeconds = userPrefs.matchTimeSeconds,
                puckMaxSpeed = puckPreset.maxSpeed,
                airFriction = puckPreset.airFriction,
                player1Name = player1Name,
                player2Name = player2Name,
                tableTheme = chosenTheme,
                arenaDimension = chosenDimension,
                localPuckSkin = userPrefs.puckSkin,
                localMalletSkin = userPrefs.player1MalletSkin,
                hasIntroPresentation = hasIntro
            )
        }

        /**
         * Cria uma especificação para o modo de Demonstração IA vs IA Local.
         */
        fun forAiVsAiLocal(
            userPrefs: UserPreferences,
            diff1: AiDifficulty,
            diff2: AiDifficulty,
            chosenTheme: TableTheme,
            chosenDimension: ArenaDimension,
            player1Name: String,
            player2Name: String
        ): MatchSpec {
            val puckPreset = when (diff1) {
                AiDifficulty.EASY -> PuckSpeedPreset.SLOW
                AiDifficulty.MEDIUM -> PuckSpeedPreset.NORMAL
                AiDifficulty.HARD -> PuckSpeedPreset.FAST
            }
            val is3d = chosenDimension == ArenaDimension.THREE_DIMENSIONAL
            val hasIntro = is3d && userPrefs.arena3dIntroEnabled
            return MatchSpec(
                mode = GameMode.AI_VS_AI_LOCAL,
                bezelGapMm = userPrefs.bezelGapMm,
                targetScore = userPrefs.targetScore,
                matchTimeSeconds = userPrefs.matchTimeSeconds,
                aiDifficulty = diff1,
                aiDifficultyPlayer1 = diff1,
                aiDifficultyPlayer2 = diff2,
                puckMaxSpeed = puckPreset.maxSpeed,
                airFriction = puckPreset.airFriction,
                player1Name = player1Name,
                player2Name = player2Name,
                tableTheme = chosenTheme,
                arenaDimension = chosenDimension,
                localPuckSkin = userPrefs.puckSkin,
                localMalletSkin = userPrefs.player1MalletSkin,
                hasIntroPresentation = hasIntro
            )
        }

        /**
         * Cria uma especificação para Sessão Multijogador em Rede (Mesa Estendida ou Mesas Conectadas).
         */
        fun forMultiplayer(
            gameMode: GameMode,
            userPrefs: UserPreferences,
            bezelGapMm: Float,
            targetScore: Int,
            matchTimeSeconds: Int,
            aiDifficultyPlayer1: AiDifficulty,
            aiDifficultyPlayer2: AiDifficulty,
            puckMaxSpeed: Float,
            airFriction: Float,
            player1Name: String,
            player2Name: String,
            tableTheme: TableTheme?,
            arenaDimension: ArenaDimension,
            hasIntroPresentation: Boolean
        ): MatchSpec {
            return MatchSpec(
                mode = gameMode,
                bezelGapMm = bezelGapMm,
                targetScore = targetScore,
                matchTimeSeconds = matchTimeSeconds,
                aiDifficulty = aiDifficultyPlayer1,
                aiDifficultyPlayer1 = aiDifficultyPlayer1,
                aiDifficultyPlayer2 = aiDifficultyPlayer2,
                puckMaxSpeed = puckMaxSpeed,
                airFriction = airFriction,
                player1Name = player1Name,
                player2Name = player2Name,
                tableTheme = tableTheme,
                arenaDimension = arenaDimension,
                localPuckSkin = userPrefs.puckSkin,
                localMalletSkin = userPrefs.player1MalletSkin,
                hasIntroPresentation = hasIntroPresentation
            )
        }
    }
}
