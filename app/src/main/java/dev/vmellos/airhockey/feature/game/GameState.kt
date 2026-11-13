package dev.vmellos.airhockey.feature.game

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import dev.vmellos.airhockey.R
import dev.vmellos.airhockey.core.preferences.ArenaDimension
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.feature.physics.TableDimensions

enum class TablePerspective {
    LOCAL_FULL,
    SPLIT_DUAL,
    EXTENDED_HOST,
    EXTENDED_GUEST,
    CONNECTED_HOST,
    CONNECTED_GUEST;

    val isExtended: Boolean
        get() = this == EXTENDED_HOST || this == EXTENDED_GUEST
}

enum class StatsPolicy {
    RECORD_LOCAL_VS_AI,
    RECORD_HUMAN_MULTIPLAYER,
    NONE
}

enum class LocalPlayerSlot {
    PLAYER_1,
    PLAYER_2,
    BOTH_SPLIT,
    NONE
}

enum class ModeDisplayKind(@StringRes val labelResId: Int) {
    SOLO(R.string.menu_mode_solo),
    SPLIT_SCREEN(R.string.menu_mode_split),
    EXTENDED_TABLE(R.string.menu_mode_extended),
    CONNECTED_TABLE(R.string.menu_mode_connected),
    AI_VS_AI(R.string.menu_mode_ai_vs_ai);

    companion object {
        fun fromStorageKey(key: String): ModeDisplayKind? = when {
            key == "SOLO" -> SOLO
            key == "SPLIT_SCREEN" -> SPLIT_SCREEN
            key == "EXTENDED_TABLE" || key == "EXTENDED_HOST" || key == "EXTENDED_GUEST" -> EXTENDED_TABLE
            key == "CONNECTED_TABLE" || key == "CONNECTED_HOST" || key == "CONNECTED_GUEST" -> CONNECTED_TABLE
            key.startsWith("AI_VS_AI") -> AI_VS_AI
            else -> null
        }
    }
}

@Immutable
data class ModeCapabilities(
    val perspective: TablePerspective,
    val statsPolicy: StatsPolicy,
    val localPlayer: LocalPlayerSlot,
    val isHost: Boolean,
    val isNetworked: Boolean,
    val hasLocalAiP1: Boolean,
    val hasLocalAiP2: Boolean,
    val displayKind: ModeDisplayKind
) {
    val isGuest: Boolean get() = !isHost
    val isExtended: Boolean get() = perspective.isExtended
    val isExtendedHost: Boolean get() = perspective == TablePerspective.EXTENDED_HOST
    val isExtendedGuest: Boolean get() = perspective == TablePerspective.EXTENDED_GUEST
    val isConnected: Boolean get() = perspective == TablePerspective.CONNECTED_HOST || perspective == TablePerspective.CONNECTED_GUEST
    val isConnectedHost: Boolean get() = perspective == TablePerspective.CONNECTED_HOST
    val isConnectedGuest: Boolean get() = perspective == TablePerspective.CONNECTED_GUEST
    val isSplitScreen: Boolean get() = perspective == TablePerspective.SPLIT_DUAL
    val isSpectator: Boolean get() = localPlayer == LocalPlayerSlot.NONE
    val recordsStats: Boolean get() = statsPolicy != StatsPolicy.NONE
}

enum class GameMode(val capabilities: ModeCapabilities) {
    SOLO(
        ModeCapabilities(
            perspective = TablePerspective.LOCAL_FULL,
            statsPolicy = StatsPolicy.RECORD_LOCAL_VS_AI,
            localPlayer = LocalPlayerSlot.PLAYER_1,
            isHost = true,
            isNetworked = false,
            hasLocalAiP1 = false,
            hasLocalAiP2 = true,
            displayKind = ModeDisplayKind.SOLO
        )
    ),
    SPLIT_SCREEN(
        ModeCapabilities(
            perspective = TablePerspective.SPLIT_DUAL,
            statsPolicy = StatsPolicy.RECORD_HUMAN_MULTIPLAYER,
            localPlayer = LocalPlayerSlot.BOTH_SPLIT,
            isHost = true,
            isNetworked = false,
            hasLocalAiP1 = false,
            hasLocalAiP2 = false,
            displayKind = ModeDisplayKind.SPLIT_SCREEN
        )
    ),
    EXTENDED_HOST(
        ModeCapabilities(
            perspective = TablePerspective.EXTENDED_HOST,
            statsPolicy = StatsPolicy.RECORD_HUMAN_MULTIPLAYER,
            localPlayer = LocalPlayerSlot.PLAYER_1,
            isHost = true,
            isNetworked = true,
            hasLocalAiP1 = false,
            hasLocalAiP2 = false,
            displayKind = ModeDisplayKind.EXTENDED_TABLE
        )
    ),
    EXTENDED_GUEST(
        ModeCapabilities(
            perspective = TablePerspective.EXTENDED_GUEST,
            statsPolicy = StatsPolicy.RECORD_HUMAN_MULTIPLAYER,
            localPlayer = LocalPlayerSlot.PLAYER_2,
            isHost = false,
            isNetworked = true,
            hasLocalAiP1 = false,
            hasLocalAiP2 = false,
            displayKind = ModeDisplayKind.EXTENDED_TABLE
        )
    ),
    CONNECTED_HOST(
        ModeCapabilities(
            perspective = TablePerspective.CONNECTED_HOST,
            statsPolicy = StatsPolicy.RECORD_HUMAN_MULTIPLAYER,
            localPlayer = LocalPlayerSlot.PLAYER_1,
            isHost = true,
            isNetworked = true,
            hasLocalAiP1 = false,
            hasLocalAiP2 = false,
            displayKind = ModeDisplayKind.CONNECTED_TABLE
        )
    ),
    CONNECTED_GUEST(
        ModeCapabilities(
            perspective = TablePerspective.CONNECTED_GUEST,
            statsPolicy = StatsPolicy.RECORD_HUMAN_MULTIPLAYER,
            localPlayer = LocalPlayerSlot.PLAYER_2,
            isHost = false,
            isNetworked = true,
            hasLocalAiP1 = false,
            hasLocalAiP2 = false,
            displayKind = ModeDisplayKind.CONNECTED_TABLE
        )
    ),
    AI_VS_AI_LOCAL(
        ModeCapabilities(
            perspective = TablePerspective.LOCAL_FULL,
            statsPolicy = StatsPolicy.NONE,
            localPlayer = LocalPlayerSlot.NONE,
            isHost = true,
            isNetworked = false,
            hasLocalAiP1 = true,
            hasLocalAiP2 = true,
            displayKind = ModeDisplayKind.AI_VS_AI
        )
    ),
    AI_VS_AI_EXTENDED_HOST(
        ModeCapabilities(
            perspective = TablePerspective.EXTENDED_HOST,
            statsPolicy = StatsPolicy.NONE,
            localPlayer = LocalPlayerSlot.NONE,
            isHost = true,
            isNetworked = true,
            hasLocalAiP1 = true,
            hasLocalAiP2 = true,
            displayKind = ModeDisplayKind.AI_VS_AI
        )
    ),
    AI_VS_AI_EXTENDED_GUEST(
        ModeCapabilities(
            perspective = TablePerspective.EXTENDED_GUEST,
            statsPolicy = StatsPolicy.NONE,
            localPlayer = LocalPlayerSlot.NONE,
            isHost = false,
            isNetworked = true,
            hasLocalAiP1 = false,
            hasLocalAiP2 = false,
            displayKind = ModeDisplayKind.AI_VS_AI
        )
    ),
    AI_VS_AI_CONNECTED_HOST(
        ModeCapabilities(
            perspective = TablePerspective.CONNECTED_HOST,
            statsPolicy = StatsPolicy.NONE,
            localPlayer = LocalPlayerSlot.NONE,
            isHost = true,
            isNetworked = true,
            hasLocalAiP1 = true,
            hasLocalAiP2 = true,
            displayKind = ModeDisplayKind.AI_VS_AI
        )
    ),
    AI_VS_AI_CONNECTED_GUEST(
        ModeCapabilities(
            perspective = TablePerspective.CONNECTED_GUEST,
            statsPolicy = StatsPolicy.NONE,
            localPlayer = LocalPlayerSlot.NONE,
            isHost = false,
            isNetworked = true,
            hasLocalAiP1 = false,
            hasLocalAiP2 = false,
            displayKind = ModeDisplayKind.AI_VS_AI
        )
    )
}

enum class MatchStatus {
    IDLE,
    PRESENTATION,
    COUNTDOWN,
    PLAYING,
    GOAL_PAUSE,
    PAUSED,
    GAME_OVER
}

@Immutable
data class MatchState(
    val mode: GameMode = GameMode.SOLO,
    val status: MatchStatus = MatchStatus.IDLE,
    val scorePlayer1: Int = 0,
    val scorePlayer2: Int = 0,
    val targetScore: Int = 3,
    val countdownNumber: Int = 3,
    val lastScorer: Int = 0,
    val winner: Int = 0,
    val remainingTimeSeconds: Int = 180,
    val isHost: Boolean = true,
    val peerExited: Boolean = false,
    val player1Name: String = "Jogador 1",
    val player2Name: String = "Jogador 2",
    val localPlayerName: String = "",
    val currentStage: Int = 1,
    val starsEarned: Int = 0,
    val tableTheme: TableTheme? = null,
    val newlyConqueredTheme: TableTheme? = null,
    val arenaDimension: ArenaDimension = ArenaDimension.THREE_DIMENSIONAL,
    val dimensions: TableDimensions = TableDimensions()
) {
    val capabilities: ModeCapabilities get() = mode.capabilities
    val isSpectator: Boolean get() = capabilities.isSpectator
    val isExtendedMode: Boolean get() = capabilities.isExtended
}
