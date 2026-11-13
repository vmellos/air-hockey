package dev.vmellos.airhockey.core.playgames

import android.app.Activity
import android.util.Log
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import kotlinx.coroutines.tasks.await

/**
 * Interface de porta para leitura e gravação atômica de snapshots na nuvem,
 * isolando a lógica de mesclagem de progresso dos detalhes do Google Play Games Services.
 */
interface CloudSnapshotPort {
    suspend fun openAndResolveSnapshot(
        snapshotName: String,
        resolveConflict: (serverBytes: ByteArray, localConflictBytes: ByteArray) -> ByteArray
    ): ByteArray

    suspend fun commitSnapshot(
        snapshotName: String,
        bytes: ByteArray,
        description: String,
        progressScore: Long
    )

    suspend fun submitLeaderboards(
        eloRating: Long?,
        totalStars: Long
    )
}

/**
 * Porta de leitura e escrita atômica das preferências locais sincronizadas com a nuvem.
 */
interface CloudPreferencesPort {
    suspend fun readSyncSnapshot(): Pair<String, PlayGamesPreferencesPayload?>
    suspend fun applyCloudSyncedPreferences(
        playerName: String?,
        syncedPrefs: PlayGamesPreferencesPayload?
    )
}

/**
 * Adaptador fino que conecta [CloudSnapshotPort] ao SDK do Google Play Games Services v2.
 */
class GmsCloudSnapshotAdapter(
    private val activity: Activity
) : CloudSnapshotPort {

    private var activeSnapshot: Snapshot? = null

    override suspend fun openAndResolveSnapshot(
        snapshotName: String,
        resolveConflict: (serverBytes: ByteArray, localConflictBytes: ByteArray) -> ByteArray
    ): ByteArray {
        val snapshotsClient = PlayGames.getSnapshotsClient(activity)
        val openResult = snapshotsClient.open(
            snapshotName,
            true,
            SnapshotsClient.RESOLUTION_POLICY_HIGHEST_PROGRESS
        ).await()

        val snapshot: Snapshot = if (openResult.isConflict) {
            Log.d(TAG, "Conflito detectado no snapshot. Aplicando resolução automática...")
            val conflict = openResult.conflict ?: throw IllegalStateException("Conflito nulo")
            val serverBytes = conflict.snapshot.snapshotContents.readFully()
            val localBytes = conflict.conflictingSnapshot.snapshotContents.readFully()

            val resolvedBytes = resolveConflict(serverBytes, localBytes)
            val resolvedSnapshot = conflict.snapshot
            if (resolvedBytes.isNotEmpty()) {
                resolvedSnapshot.snapshotContents.writeBytes(resolvedBytes)
            }

            val resolveTask = snapshotsClient.resolveConflict(
                conflict.conflictId,
                resolvedSnapshot
            ).await()
            resolveTask.data ?: conflict.snapshot
        } else {
            openResult.data ?: throw IllegalStateException("Snapshot retornado nulo")
        }

        activeSnapshot = snapshot
        return snapshot.snapshotContents.readFully()
    }

    override suspend fun commitSnapshot(
        snapshotName: String,
        bytes: ByteArray,
        description: String,
        progressScore: Long
    ) {
        val snapshot = activeSnapshot
            ?: throw IllegalStateException("Snapshot ativo ausente para commit: $snapshotName")
        val snapshotsClient = PlayGames.getSnapshotsClient(activity)
        snapshot.snapshotContents.writeBytes(bytes)

        val metadataChange = SnapshotMetadataChange.Builder()
            .setDescription(description)
            .setProgressValue(progressScore)
            .build()

        snapshotsClient.commitAndClose(snapshot, metadataChange).await()
        activeSnapshot = null
    }

    override suspend fun submitLeaderboards(
        eloRating: Long?,
        totalStars: Long
    ) {
        try {
            val eloId = activity.getString(dev.vmellos.airhockey.R.string.leaderboard_elo_id)
            if (eloId.isNotBlank() && eloRating != null) {
                PlayGames.getLeaderboardsClient(activity).submitScore(eloId, eloRating)
            }
            val stageId = activity.getString(dev.vmellos.airhockey.R.string.leaderboard_stages_id)
            if (stageId.isNotBlank()) {
                PlayGames.getLeaderboardsClient(activity).submitScore(stageId, totalStars)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Submissão de placares na sincronização ignorada: ${e.message}")
        }
    }

    private companion object {
        const val TAG = "GmsCloudSnapshotAdapter"
    }
}
