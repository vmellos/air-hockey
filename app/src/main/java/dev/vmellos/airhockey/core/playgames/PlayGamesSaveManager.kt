package dev.vmellos.airhockey.core.playgames

import android.app.Activity
import android.util.Log
import androidx.room.withTransaction
import dev.vmellos.airhockey.core.database.AppDatabase
import dev.vmellos.airhockey.core.database.PlayerProfileStatsEntity
import dev.vmellos.airhockey.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gerenciador de Sincronização Atômica em Nuvem (Jogos Salvos / Snapshots)
 * do Google Play Games Services v2.
 */
class PlayGamesSaveManager(
    private val database: AppDatabase,
    private val preferencesRepository: UserPreferencesRepository? = null,
    private val transactionRunner: suspend (suspend () -> Unit) -> Unit = { block ->
        database.withTransaction { block() }
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    companion object {
        private const val TAG = "PlayGamesSaveManager"
        const val SNAPSHOT_NAME = "airhockey_saved_progress"

        internal fun resolveConflictBytes(
            serverBytes: ByteArray,
            localConflictBytes: ByteArray
        ): ByteArray {
            val serverPayload = PlayGamesSavePayload.fromByteArray(serverBytes)
            val conflictLocalPayload = PlayGamesSavePayload.fromByteArray(localConflictBytes)
            val merged = when {
                serverPayload != null && conflictLocalPayload != null ->
                    PlayGamesSavePayload.merge(serverPayload, conflictLocalPayload)
                else -> serverPayload ?: conflictLocalPayload
            }
            return merged?.toByteArray() ?: serverBytes
        }
    }

    /**
     * Sincroniza o progresso local (Room) com o snapshot da nuvem.
     * Realiza união automática de fases e estatísticas seguindo a regra HIGHEST_PROGRESS.
     */
    fun syncProgressAsync(activity: Activity, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            val result = syncWithPort(GmsCloudSnapshotAdapter(activity))
            withContext(mainDispatcher) {
                onComplete?.invoke(result.isSuccess)
            }
        }
    }

    suspend fun syncProgress(activity: Activity): Result<Unit> {
        return withContext(ioDispatcher) {
            syncWithPort(GmsCloudSnapshotAdapter(activity)).map { Unit }
        }
    }

    /**
     * Executa o ciclo completo de leitura, resolução de conflito, persistência atômica local e envio à nuvem
     * usando a porta [CloudSnapshotPort] e [CloudPreferencesPort] informadas.
     */
    suspend fun syncWithPort(
        port: CloudSnapshotPort,
        preferencesPort: CloudPreferencesPort? = preferencesRepository
    ): Result<PlayGamesSavePayload> {
        _syncState.value = CloudSyncState.Syncing
        return try {
            val remoteBytes = port.openAndResolveSnapshot(SNAPSHOT_NAME) { serverBytes, conflictBytes ->
                resolveConflictBytes(serverBytes, conflictBytes)
            }
            val remotePayload = PlayGamesSavePayload.fromByteArray(remoteBytes)

            val stageDao = database.stageProgressDao()
            val localStages = stageDao.getAllStagesDirect()

            val leaderboardDao = database.leaderboardDao()
            val (effectivePlayerName, localPrefPayload) = preferencesPort?.readSyncSnapshot()
                ?: ("Jogador" to null)

            val localStats = leaderboardDao.getProfileStatsDirect(effectivePlayerName)
                ?: leaderboardDao.getProfileStatsDirect("Jogador")
                ?: PlayerProfileStatsEntity(playerName = effectivePlayerName)

            val localPayload = PlayGamesSavePayload(
                stages = localStages,
                stats = localStats,
                preferences = localPrefPayload
            )

            val finalPayload = if (remotePayload != null) {
                PlayGamesSavePayload.merge(localPayload, remotePayload)
            } else {
                localPayload
            }

            // 1. Atualiza Room Database localmente em transação atômica única
            transactionRunner {
                if (finalPayload.stages.isNotEmpty()) {
                    stageDao.upsertAll(finalPayload.stages)
                }
                finalPayload.stats?.let { stats ->
                    leaderboardDao.upsertProfileStats(stats)
                }
            }

            // 2. Atualiza nome do jogador e preferências cosméticas em transação única de DataStore
            preferencesPort?.applyCloudSyncedPreferences(
                playerName = finalPayload.stats?.playerName,
                syncedPrefs = finalPayload.preferences
            )

            // 3. Prepara e envia os bytes consolidados para a nuvem
            val highestStage = finalPayload.highestUnlockedStage
            val totalStars = finalPayload.totalStars
            val progressScore = (highestStage.toLong() * 1000L) + totalStars
            val description = "Fase $highestStage • $totalStars ★"

            port.commitSnapshot(
                snapshotName = SNAPSHOT_NAME,
                bytes = finalPayload.toByteArray(),
                description = description,
                progressScore = progressScore
            )

            // 4. Submete pontuações consolidadas aos placares
            port.submitLeaderboards(
                eloRating = finalPayload.stats?.ratingElo?.toLong(),
                totalStars = totalStars.toLong()
            )

            Log.i(TAG, "Sincronização em nuvem concluída com sucesso! Fase $highestStage, $totalStars estrelas.")
            _syncState.value = CloudSyncState.Success(
                lastSyncTimestamp = clock(),
                summary = description
            )
            Result.success(finalPayload)
        } catch (e: Exception) {
            Log.w(TAG, "Falha ou cancelamento na sincronização em nuvem: ${e.message}")
            _syncState.value = CloudSyncState.Error(e.message ?: "Erro de sincronização")
            Result.failure(e)
        }
    }
}
