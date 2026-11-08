package dev.vmellos.airhockey.core.playgames

import android.app.Activity
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import dev.vmellos.airhockey.core.database.AppDatabase
import dev.vmellos.airhockey.core.database.PlayerProfileStatsEntity
import dev.vmellos.airhockey.core.preferences.ArenaDimension
import dev.vmellos.airhockey.core.preferences.MalletSkin
import dev.vmellos.airhockey.core.preferences.PuckSkin
import dev.vmellos.airhockey.core.preferences.TableTheme
import dev.vmellos.airhockey.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Gerenciador de Sincronização em Nuvem (Jogos Salvos / SnapshotsClient)
 * do Google Play Games Services v2.
 */
class PlayGamesSaveManager(
    private val database: AppDatabase,
    private val preferencesRepository: UserPreferencesRepository? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    companion object {
        private const val TAG = "PlayGamesSaveManager"
        const val SNAPSHOT_NAME = "airhockey_saved_progress"
    }

    /**
     * Sincroniza o progresso local (Room) com o snapshot da nuvem.
     * Realiza união automática de fases e estatísticas seguindo a regra HIGHEST_PROGRESS.
     */
    fun syncProgressAsync(activity: Activity, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            val result = syncProgressInternal(activity)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(result.isSuccess)
            }
        }
    }

    suspend fun syncProgress(activity: Activity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            syncProgressInternal(activity)
        }
    }

    private suspend fun syncProgressInternal(activity: Activity): Result<Unit> {
        _syncState.value = CloudSyncState.Syncing
        return try {
            val snapshotsClient = PlayGames.getSnapshotsClient(activity)

            // Abre o snapshot na nuvem com política automática de maior progresso
            val openResult = snapshotsClient.open(
                SNAPSHOT_NAME,
                true, // cria caso não exista
                SnapshotsClient.RESOLUTION_POLICY_HIGHEST_PROGRESS
            ).await()

            val snapshot: Snapshot = if (openResult.isConflict) {
                Log.d(TAG, "Conflito detectado no snapshot. Aplicando resolução automática...")
                val conflict = openResult.conflict ?: throw IllegalStateException("Conflito nulo")
                val serverBytes = conflict.snapshot.snapshotContents.readFully()
                val localBytes = conflict.conflictingSnapshot.snapshotContents.readFully()

                val serverPayload = PlayGamesSavePayload.fromByteArray(serverBytes)
                val conflictLocalPayload = PlayGamesSavePayload.fromByteArray(localBytes)

                val base = serverPayload ?: conflictLocalPayload
                val merged = if (serverPayload != null && conflictLocalPayload != null) {
                    PlayGamesSavePayload.merge(serverPayload, conflictLocalPayload)
                } else base

                val resolvedSnapshot = conflict.snapshot
                if (merged != null) {
                    resolvedSnapshot.snapshotContents.writeBytes(merged.toByteArray())
                }

                val resolveTask = snapshotsClient.resolveConflict(
                    conflict.conflictId,
                    resolvedSnapshot
                ).await()
                resolveTask.data ?: conflict.snapshot
            } else {
                openResult.data ?: throw IllegalStateException("Snapshot retornado nulo")
            }

            // Lê o payload que está na nuvem
            val remoteBytes = snapshot.snapshotContents.readFully()
            val remotePayload = PlayGamesSavePayload.fromByteArray(remoteBytes)

            // Carrega dados locais do Room Database e UserPreferences
            val stageDao = database.stageProgressDao()
            val localStages = stageDao.getAllStagesDirect()

            val leaderboardDao = database.leaderboardDao()
            val currentPrefs = preferencesRepository?.userPreferencesFlow?.firstOrNull()
            val effectivePlayerName = currentPrefs?.playerName?.ifBlank { "Jogador" } ?: "Jogador"

            val localStats = leaderboardDao.getProfileStatsDirect(effectivePlayerName)
                ?: leaderboardDao.getProfileStatsDirect("Jogador")
                ?: PlayerProfileStatsEntity(playerName = effectivePlayerName)

            val localPrefPayload = currentPrefs?.let { p ->
                PlayGamesPreferencesPayload(
                    tableTheme = p.tableTheme.name,
                    puckSkin = p.puckSkin.name,
                    malletSkinP1 = p.player1MalletSkin.name,
                    malletSkinP2 = p.player2MalletSkin.name,
                    arenaDimension = p.arenaDimension.name,
                    arena3dIntroEnabled = p.arena3dIntroEnabled
                )
            }

            val localPayload = PlayGamesSavePayload(
                stages = localStages,
                stats = localStats,
                preferences = localPrefPayload
            )

            // Executa o merge de progresso
            val finalPayload = if (remotePayload != null) {
                PlayGamesSavePayload.merge(localPayload, remotePayload)
            } else {
                localPayload
            }

            // 1. Atualiza Room Database local com dados consolidados
            if (finalPayload.stages.isNotEmpty()) {
                stageDao.upsertAll(finalPayload.stages)
            }
            finalPayload.stats?.let { stats ->
                leaderboardDao.upsertProfileStats(stats)
                if (stats.playerName.isNotBlank() && stats.playerName != "Jogador") {
                    preferencesRepository?.setPlayerName(stats.playerName)
                }
            }

            // Atualiza preferências cosméticas locais se fornecidas pelo snapshot remoto
            finalPayload.preferences?.let { p ->
                preferencesRepository?.let { repo ->
                    p.tableTheme?.let { tName ->
                        try {
                            repo.setTableTheme(TableTheme.valueOf(tName))
                        } catch (_: Exception) {}
                    }
                    p.puckSkin?.let { sName ->
                        try {
                            repo.setPuckSkin(PuckSkin.valueOf(sName))
                        } catch (_: Exception) {}
                    }
                    p.malletSkinP1?.let { m1Name ->
                        try {
                            repo.setMalletSkinP1(MalletSkin.valueOf(m1Name))
                        } catch (_: Exception) {}
                    }
                    p.malletSkinP2?.let { m2Name ->
                        try {
                            repo.setMalletSkinP2(MalletSkin.valueOf(m2Name))
                        } catch (_: Exception) {}
                    }
                    p.arenaDimension?.let { dName ->
                        try {
                            repo.updateArenaDimension(ArenaDimension.valueOf(dName))
                        } catch (_: Exception) {}
                    }
                    p.arena3dIntroEnabled?.let { intro ->
                        repo.setArena3dIntroEnabled(intro)
                    }
                }
            }

            // 2. Prepara e envia os bytes atualizados para a nuvem da Google Play
            val mergedBytes = finalPayload.toByteArray()
            snapshot.snapshotContents.writeBytes(mergedBytes)

            val highestStage = finalPayload.highestUnlockedStage
            val totalStars = finalPayload.totalStars
            val progressScore = (highestStage.toLong() * 1000L) + totalStars

            val metadataChange = SnapshotMetadataChange.Builder()
                .setDescription("Fase $highestStage • $totalStars ★")
                .setProgressValue(progressScore)
                .build()

            snapshotsClient.commitAndClose(snapshot, metadataChange).await()

            // 3. Submete pontuações consolidadas aos Placares Globais da Google Play (se IDs estiverem configurados)
            try {
                val eloId = activity.getString(dev.vmellos.airhockey.R.string.leaderboard_elo_id)
                if (eloId.isNotBlank() && finalPayload.stats != null) {
                    PlayGames.getLeaderboardsClient(activity).submitScore(eloId, finalPayload.stats.ratingElo.toLong())
                }
                val stageId = activity.getString(dev.vmellos.airhockey.R.string.leaderboard_stages_id)
                if (stageId.isNotBlank()) {
                    PlayGames.getLeaderboardsClient(activity).submitScore(stageId, finalPayload.totalStars.toLong())
                }
            } catch (e: Exception) {
                Log.d(TAG, "Submissão de placares na sincronização ignorada: ${e.message}")
            }

            Log.i(TAG, "Sincronização em nuvem concluída com sucesso! Fase $highestStage, $totalStars estrelas.")
            _syncState.value = CloudSyncState.Success(
                lastSyncTimestamp = System.currentTimeMillis(),
                summary = "Fase $highestStage • $totalStars ★"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ou cancelamento na sincronização em nuvem: ${e.message}")
            _syncState.value = CloudSyncState.Error(e.message ?: "Erro de sincronização")
            Result.failure(e)
        }
    }
}
