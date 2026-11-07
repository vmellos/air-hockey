package dev.vmellos.airhockey.core.playgames

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

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

    private var currentActivityRef: WeakReference<Activity>? = null

    companion object {
        private const val TAG = "PlayGamesManager"
        const val RC_ACHIEVEMENTS = 9002
        const val RC_LEADERBOARDS = 9003
    }

    /**
     * Anexa a Activity em primeiro plano para abertura de intents e submissão de pontuação.
     */
    fun attachActivity(activity: Activity) {
        currentActivityRef = WeakReference(activity)
    }

    /**
     * Desanexa a Activity quando destruída para prevenir vazamento de memória.
     */
    fun detachActivity(activity: Activity) {
        if (currentActivityRef?.get() === activity) {
            currentActivityRef = null
        }
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
        attachActivity(activity)
        try {
            val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
            gamesSignInClient.isAuthenticated.addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    val authResult = authTask.result
                    val isAuthenticated = authResult.isAuthenticated
                    Log.i(TAG, "authenticateSilently: isSuccessful=true, isAuthenticated=$isAuthenticated")
                    if (isAuthenticated) {
                        fetchPlayerProfile(activity)
                    } else {
                        Log.i(TAG, "authenticateSilently: Jogador não autenticado silenciosamente no Play Games. Modo offline ativo.")
                        _authState.value = PlayGamesAuthState.Unauthenticated
                    }
                } else {
                    val ex = authTask.exception
                    val statusCode = (ex as? ApiException)?.statusCode
                    val statusMessage = statusCode?.let { CommonStatusCodes.getStatusCodeString(it) } ?: "UNKNOWN"
                    Log.w(TAG, "authenticateSilently falhou: statusCode=$statusCode ($statusMessage), msg=${ex?.message}", ex)
                    _authState.value = PlayGamesAuthState.Unauthenticated
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exceção ao autenticar no Play Games: ${e.message}", e)
            _authState.value = PlayGamesAuthState.Unauthenticated
        }
    }

    /**
     * Realiza login interativo solicitado expressamente pelo usuário (ex: ao tocar em Ver Rankings Globais).
     */
    fun signInExplicitly(
        activity: Activity,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((String?) -> Unit)? = null
    ) {
        attachActivity(activity)
        try {
            val gamesSignInClient = PlayGames.getGamesSignInClient(activity)
            Log.i(TAG, "Iniciando signInExplicitly interativo...")
            gamesSignInClient.signIn().addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    val authResult = signInTask.result
                    val isAuthenticated = authResult.isAuthenticated
                    Log.i(TAG, "signInExplicitly concluído: isSuccessful=true, isAuthenticated=$isAuthenticated")
                    if (isAuthenticated) {
                        fetchPlayerProfile(activity) {
                            onSuccess?.invoke()
                        }
                    } else {
                        Log.w(TAG, "signInExplicitly: fluxo completou mas isAuthenticated=false (usuário cancelou ou rejeitou)")
                        _authState.value = PlayGamesAuthState.Unauthenticated
                        onFailure?.invoke("Login não concluído (isAuthenticated=false)")
                    }
                } else {
                    val ex = signInTask.exception
                    val statusCode = (ex as? ApiException)?.statusCode
                    val statusMessage = statusCode?.let { CommonStatusCodes.getStatusCodeString(it) } ?: "UNKNOWN"
                    val errorMsg = "Código $statusCode: $statusMessage - ${ex?.message}"
                    Log.e(TAG, "signInExplicitly falhou com exceção: $errorMsg", ex)
                    _authState.value = PlayGamesAuthState.Unauthenticated
                    onFailure?.invoke(errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exceção no login interativo: ${e.message}", e)
            _authState.value = PlayGamesAuthState.Unauthenticated
            onFailure?.invoke(e.message)
        }
    }

    private fun fetchPlayerProfile(activity: Activity, onComplete: (() -> Unit)? = null) {
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
                onComplete?.invoke()
            }.addOnFailureListener { e ->
                val statusCode = (e as? ApiException)?.statusCode
                val statusMessage = statusCode?.let { CommonStatusCodes.getStatusCodeString(it) } ?: "UNKNOWN"
                Log.w(TAG, "Não foi possível obter perfil do jogador: statusCode=$statusCode ($statusMessage), msg=${e.message}", e)
                _authState.value = PlayGamesAuthState.Authenticated(
                    PlayGamesPlayer("local", "Jogador")
                )
                saveManager.syncProgressAsync(activity)
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao buscar dados do jogador: ${e.message}", e)
            _authState.value = PlayGamesAuthState.Unauthenticated
            onComplete?.invoke()
        }
    }

    /**
     * Exibe a tela nativa de Conquistas (Achievements) da Google Play.
     * Se o usuário não estiver autenticado, dispara login interativo.
     */
    fun showAchievements(
        activity: Activity? = null,
        onFeedback: ((String) -> Unit)? = null
    ) {
        val targetActivity = activity ?: currentActivityRef?.get()
        if (targetActivity == null) {
            Log.w(TAG, "Nenhuma Activity disponível para abrir conquistas.")
            onFeedback?.invoke("Activity não disponível.")
            return
        }
        val auth = _authState.value
        if (auth !is PlayGamesAuthState.Authenticated) {
            signInExplicitly(
                activity = targetActivity,
                onSuccess = {
                    launchAchievementsIntent(targetActivity, onFeedback)
                },
                onFailure = { errorReason ->
                    Log.w(TAG, "showAchievements: falha no login interativo: $errorReason")
                    val feedbackMsg = if (errorReason?.contains("10") == true ||
                        errorReason?.contains("DEVELOPER_ERROR") == true ||
                        errorReason?.contains("isAuthenticated=false") == true
                    ) {
                        targetActivity.getString(dev.vmellos.airhockey.R.string.pgs_error_developer_config)
                    } else {
                        targetActivity.getString(dev.vmellos.airhockey.R.string.pgs_error_login_required)
                    }
                    onFeedback?.invoke(feedbackMsg)
                }
            )
            return
        }

        launchAchievementsIntent(targetActivity, onFeedback)
    }

    private fun launchAchievementsIntent(
        activity: Activity,
        onFeedback: ((String) -> Unit)? = null
    ) {
        try {
            PlayGames.getAchievementsClient(activity).achievementsIntent
                .addOnSuccessListener { intent ->
                    activity.startActivityForResult(intent, RC_ACHIEVEMENTS)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Erro ao abrir tela de conquistas: ${e.message}")
                    onFeedback?.invoke(activity.getString(dev.vmellos.airhockey.R.string.pgs_error_unavailable))
                }
        } catch (e: Exception) {
            Log.w(TAG, "Play Games não disponível para conquistas: ${e.message}")
            onFeedback?.invoke(activity.getString(dev.vmellos.airhockey.R.string.pgs_error_unavailable))
        }
    }

    /**
     * Exibe a tela nativa com todos os Placares Oficiais (Leaderboards) da Google Play.
     * Se o usuário não estiver autenticado, dispara login interativo.
     */
    fun showLeaderboards(
        activity: Activity? = null,
        onFeedback: ((String) -> Unit)? = null
    ) {
        val targetActivity = activity ?: currentActivityRef?.get()
        if (targetActivity == null) {
            Log.w(TAG, "Nenhuma Activity disponível para abrir rankings da Google Play.")
            onFeedback?.invoke("Activity não disponível.")
            return
        }
        val auth = _authState.value
        if (auth !is PlayGamesAuthState.Authenticated) {
            signInExplicitly(
                activity = targetActivity,
                onSuccess = {
                    launchLeaderboardsIntent(targetActivity, onFeedback)
                },
                onFailure = { errorReason ->
                    Log.w(TAG, "showLeaderboards: falha no login interativo: $errorReason")
                    val feedbackMsg = if (errorReason?.contains("10") == true ||
                        errorReason?.contains("DEVELOPER_ERROR") == true ||
                        errorReason?.contains("isAuthenticated=false") == true
                    ) {
                        targetActivity.getString(dev.vmellos.airhockey.R.string.pgs_error_developer_config)
                    } else {
                        targetActivity.getString(dev.vmellos.airhockey.R.string.pgs_error_login_required)
                    }
                    onFeedback?.invoke(feedbackMsg)
                }
            )
            return
        }

        launchLeaderboardsIntent(targetActivity, onFeedback)
    }

    private fun launchLeaderboardsIntent(
        activity: Activity,
        onFeedback: ((String) -> Unit)? = null
    ) {
        try {
            PlayGames.getLeaderboardsClient(activity).allLeaderboardsIntent
                .addOnSuccessListener { intent ->
                    activity.startActivityForResult(intent, RC_LEADERBOARDS)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Erro ao abrir tela de rankings: ${e.message}")
                    onFeedback?.invoke(activity.getString(dev.vmellos.airhockey.R.string.pgs_error_unavailable))
                }
        } catch (e: Exception) {
            Log.w(TAG, "Play Games não disponível para rankings: ${e.message}")
            onFeedback?.invoke(activity.getString(dev.vmellos.airhockey.R.string.pgs_error_unavailable))
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
     * Suporta passagem explícita de Activity ou resolução automática via Activity anexada.
     */
    fun submitScore(leaderboardId: String, score: Long, activity: Activity? = null) {
        if (leaderboardId.isBlank()) {
            Log.d(TAG, "ID do placar vazio/não configurado. Submissão ignorada ($score pts).")
            return
        }
        val targetActivity = activity ?: currentActivityRef?.get()
        if (targetActivity == null) {
            Log.d(TAG, "Nenhuma Activity ativa no momento para submeter pontuação $score ao placar $leaderboardId.")
            return
        }
        try {
            PlayGames.getLeaderboardsClient(targetActivity).submitScore(leaderboardId, score)
            Log.i(TAG, "Pontuação $score submetida com sucesso no placar $leaderboardId.")
        } catch (e: Exception) {
            Log.w(TAG, "Erro ao submeter pontuação $score em $leaderboardId: ${e.message}")
        }
    }

    fun submitScore(activity: Activity, leaderboardId: String, score: Long) {
        submitScore(leaderboardId, score, activity)
    }
}
