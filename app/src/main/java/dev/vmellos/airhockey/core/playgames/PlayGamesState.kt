package dev.vmellos.airhockey.core.playgames

/**
 * Informações do perfil do jogador autenticado via Google Play Games.
 */
data class PlayGamesPlayer(
    val playerId: String,
    val displayName: String,
    val avatarUri: String? = null
)

/**
 * Estado de autenticação do Google Play Games Services v2.
 */
sealed interface PlayGamesAuthState {
    data object Checking : PlayGamesAuthState
    data object Unauthenticated : PlayGamesAuthState
    data class Authenticated(val player: PlayGamesPlayer) : PlayGamesAuthState
}

/**
 * Estado da sincronização do progresso em nuvem (SnapshotsClient / Jogos Salvos).
 */
sealed interface CloudSyncState {
    data object Idle : CloudSyncState
    data object Syncing : CloudSyncState
    data class Success(val lastSyncTimestamp: Long, val summary: String = "") : CloudSyncState
    data class Error(val message: String) : CloudSyncState
}
