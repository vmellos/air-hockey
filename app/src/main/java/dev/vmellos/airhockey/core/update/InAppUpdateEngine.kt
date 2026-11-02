package dev.vmellos.airhockey.core.update

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

sealed interface InAppUpdateStatus {
    object Idle : InAppUpdateStatus
    object Checking : InAppUpdateStatus
    object Pending : InAppUpdateStatus
    object NotAvailable : InAppUpdateStatus
    data class Available(val appUpdateInfo: AppUpdateInfo) : InAppUpdateStatus
    data class Downloading(val bytesDownloaded: Long, val totalBytesToDownload: Long) : InAppUpdateStatus
    object Downloaded : InAppUpdateStatus
    data class Failed(val message: String) : InAppUpdateStatus
}

interface InAppUpdateEngine {
    val status: StateFlow<InAppUpdateStatus>
    fun checkForUpdate(activity: Activity, forceCheck: Boolean = false)
    fun checkDownloadedStatus()
    fun startFlexibleUpdate(activity: Activity, updateInfo: AppUpdateInfo, requestCode: Int = REQUEST_CODE_FLEXIBLE_UPDATE)
    fun completeUpdate()
    fun dismissNotification()
    fun onUpdateFlowResult(resultCode: Int)
    fun unregister()

    companion object {
        const val REQUEST_CODE_FLEXIBLE_UPDATE = 5901
    }
}

class PlayInAppUpdateEngine(
    context: Context,
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
) : InAppUpdateEngine {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val _status = MutableStateFlow<InAppUpdateStatus>(InAppUpdateStatus.Idle)
    override val status: StateFlow<InAppUpdateStatus> = _status.asStateFlow()

    private val isUpdateFlowInProgress = AtomicBoolean(false)

    private val installListener = InstallStateUpdatedListener { state: InstallState ->
        when (state.installStatus()) {
            InstallStatus.PENDING -> {
                _status.value = InAppUpdateStatus.Pending
                Log.d("InAppUpdate", "Play In-App Update is pending in Google Play queue.")
            }
            InstallStatus.DOWNLOADING -> {
                _status.value = InAppUpdateStatus.Downloading(
                    bytesDownloaded = state.bytesDownloaded(),
                    totalBytesToDownload = state.totalBytesToDownload()
                )
            }
            InstallStatus.DOWNLOADED -> {
                isUpdateFlowInProgress.set(false)
                _status.value = InAppUpdateStatus.Downloaded
                Log.i("InAppUpdate", "Play In-App Update downloaded and ready to install.")
            }
            InstallStatus.FAILED -> {
                isUpdateFlowInProgress.set(false)
                _status.value = InAppUpdateStatus.Failed("Download failed with error ${state.installErrorCode()}")
            }
            InstallStatus.CANCELED -> {
                isUpdateFlowInProgress.set(false)
                _status.value = InAppUpdateStatus.Idle
            }
            else -> {}
        }
    }

    init {
        try {
            appUpdateManager.registerListener(installListener)
        } catch (e: Exception) {
            Log.w("InAppUpdate", "Could not register update listener", e)
        }
    }

    override fun checkForUpdate(activity: Activity, forceCheck: Boolean) {
        if (!isUpdateFlowInProgress.compareAndSet(false, true)) {
            Log.d("InAppUpdate", "Update check or flow already in progress, skipping duplicate call.")
            return
        }

        scope.launch {
            try {
                _status.value = InAppUpdateStatus.Checking

                appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                    if (info.installStatus() == InstallStatus.DOWNLOADED) {
                        isUpdateFlowInProgress.set(false)
                        _status.value = InAppUpdateStatus.Downloaded
                    } else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                    ) {
                        _status.value = InAppUpdateStatus.Available(info)
                        startFlexibleUpdate(activity, info)
                    } else {
                        isUpdateFlowInProgress.set(false)
                        _status.value = InAppUpdateStatus.NotAvailable
                    }
                }.addOnFailureListener { e ->
                    isUpdateFlowInProgress.set(false)
                    Log.w("InAppUpdate", "Update check failed", e)
                    _status.value = InAppUpdateStatus.Failed(e.message ?: "Failed to check for updates")
                }
            } catch (e: Exception) {
                isUpdateFlowInProgress.set(false)
                Log.w("InAppUpdate", "Error during update evaluation", e)
                _status.value = InAppUpdateStatus.Failed(e.message ?: "Update check error")
            }
        }
    }

    override fun checkDownloadedStatus() {
        if (isUpdateFlowInProgress.get()) return
        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _status.value = InAppUpdateStatus.Downloaded
                }
            }.addOnFailureListener { e ->
                Log.d("InAppUpdate", "Check downloaded status failed: ${e.message}")
            }
        } catch (e: Exception) {
            Log.w("InAppUpdate", "Error checking downloaded status", e)
        }
    }

    override fun startFlexibleUpdate(activity: Activity, updateInfo: AppUpdateInfo, requestCode: Int) {
        isUpdateFlowInProgress.set(true)
        val launchFlow = Runnable {
            try {
                val options = AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)
                appUpdateManager.startUpdateFlowForResult(updateInfo, activity, options, requestCode)
            } catch (e: Exception) {
                isUpdateFlowInProgress.set(false)
                Log.e("InAppUpdate", "Failed to start flexible update flow", e)
                _status.value = InAppUpdateStatus.Failed(e.message ?: "Failed to launch update flow")
            }
        }

        val decorView = activity.window?.decorView
        if (decorView != null) {
            decorView.post(launchFlow)
        } else {
            launchFlow.run()
        }
    }

    override fun completeUpdate() {
        try {
            appUpdateManager.completeUpdate()
        } catch (e: Exception) {
            Log.e("InAppUpdate", "Failed to complete update", e)
        }
    }

    override fun dismissNotification() {
        isUpdateFlowInProgress.set(false)
        _status.value = InAppUpdateStatus.Idle
    }

    override fun onUpdateFlowResult(resultCode: Int) {
        Log.d("InAppUpdate", "onUpdateFlowResult received with resultCode: $resultCode")
        when (resultCode) {
            Activity.RESULT_CANCELED -> {
                isUpdateFlowInProgress.set(false)
                _status.value = InAppUpdateStatus.Idle
            }
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                isUpdateFlowInProgress.set(false)
                _status.value = InAppUpdateStatus.Failed("Update flow failed with result code $resultCode")
            }
            Activity.RESULT_OK -> {
                Log.d("InAppUpdate", "Update flow accepted by user, download progressing.")
            }
            else -> {
                isUpdateFlowInProgress.set(false)
                _status.value = InAppUpdateStatus.Idle
            }
        }
    }

    override fun unregister() {
        try {
            appUpdateManager.unregisterListener(installListener)
        } catch (_: Exception) {}
    }
}
