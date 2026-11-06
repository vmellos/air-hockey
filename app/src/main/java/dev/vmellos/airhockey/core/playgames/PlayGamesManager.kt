package dev.vmellos.airhockey.core.playgames

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gerenciador de Autenticação Silenciosa, Perfil, Conquistas e Placares
 * do Google Play Games Services v2.
 */
class PlayGamesManager(
    private val saveManager: PlayGamesSaveManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _authState = MutableStateFlow<PlayGamesAuthState>(PlayGamesAuthState.Checking)
    val authState: StateFlow<PlayGamesAuthState> = _authState.asStateFlow()

    companion object {
        private const val TAG = "PlayGamesManager"
        const val RC_ACHIEVEMENTS = 9002
        const val RC_LEADERBOARDS = 9003
    }

    /**
     * Inicializa o SDK v2 do Google Play Games.
     * Seguro contra ausência de Google Play Services em emuladores/dispositivos sem GMS.
     */
    fun initialize(context: Context) {
        try {
            PlayGamesSdk.initialize(context)
            Log.i(TAG, "PlayGamesSdk inicializado com sucesso.")
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao inicializar PlayGamesSdk: ${e.message}")
        }
    }

    /**
     * Realiza login silencioso em segundo plano sem diálogos intrusivos.
     */
    fun authenticateSilently(activity: Activity) {
        try {
            val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
            gamesSignInClient.isAuthenticated.addOnCompleteListener { authTask ->
                val isAuthenticated = authTask.isSuccessful && authTask.result.isAuthenticated
                if (isAuthenticated) {
                    fetchPlayerProfile(activity)
                } else {
                    gamesSignInClient.signIn().addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful && signInTask.result.isAuthenticated) {
                            fetchPlayerProfile(activity)
                        } else {
                            Log.d(TAG, "Jogador não autenticado no Play Games. Modo offline ativo.")
                            _authState.value = PlayGamesAuthState.Unauthenticated
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exceção ao autenticar no Play Games: ${e.message}")
            _authState.value = PlayGamesAuthState.Unauthenticated
        }
    }

    private fun fetchPlayerProfile(activity: Activity) {
        try {
            val playersClient = PlayGames.getPlayersClient(activity)
            playersClient.currentPlayer.addOnSuccessListener { player ->
                val p = PlayGamesPlayer(
                    playerId = player.playerId,
                    displayName = player.displayName,
                    avatarUri = player.hiResImageUri?.toString() ?: player.iconImageUri?.toString()
                )
                _authState.value = PlayGamesAuthState.Authenticated(p)
                Log.i(TAG, "Play Games conectado com sucesso: ${p.displayName} (${p.playerId})")

                // Sincroniza progresso na nuvem logo após login bem-sucedido
                saveManager.syncProgressAsync(activity)
            }.addOnFailureListener { e ->
                Log.w(TAG, "Não foi possível obter perfil do jogador: ${e.message}")
                _authState.value = PlayGamesAuthState.Authenticated(
                    PlayGamesPlayer("local", "Jogador")
                )
                saveManager.syncProgressAsync(activity)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao buscar dados do jogador: ${e.message}")
            _authState.value = PlayGamesAuthState.Unauthenticated
        }
    }

    /**
     * Exibe a tela nativa de Conquistas (Achievements) da Google Play.
     */
    fun showAchievements(activity: Activity) {
        try {
            PlayGames.getAchievementsClient(activity).achievementsIntent
                .addOnSuccessListener { intent ->
                    activity.startActivityForResult(intent, RC_ACHIEVEMENTS)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Erro ao abrir tela de conquistas: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Play Games não disponível para conquistas: ${e.message}")
        }
    }

    /**
     * Exibe a tela nativa com todos os Placares Oficiais (Leaderboards) da Google Play.
     */
    fun showLeaderboards(activity: Activity) {
        try {
            PlayGames.getLeaderboardsClient(activity).allLeaderboardsIntent
                .addOnSuccessListener { intent ->
                    activity.startActivityForResult(intent, RC_LEADERBOARDS)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Erro ao abrir tela de rankings: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Play Games não disponível para rankings: ${e.message}")
        }
    }

    /**
     * Desbloqueia uma conquista na Google Play.
     */
    fun unlockAchievement(activity: Activity, achievementId: String) {
        try {
            PlayGames.getAchievementsClient(activity).unlock(achievementId)
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao desbloquear conquista $achievementId: ${e.message}")
        }
    }

    /**
     * Submete pontuação a um ranking oficial da Google Play.
     */
    fun submitScore(activity: Activity, leaderboardId: String, score: Long) {
        try {
            PlayGames.getLeaderboardsClient(activity).submitScore(leaderboardId, score)
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao submeter pontuação $score em $leaderboardId: ${e.message}")
        }
    }
}
