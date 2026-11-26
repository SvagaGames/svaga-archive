package com.svaga.archive

import androidx.activity.ComponentActivity
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/** Google Play build: Play in-app updates (flexible: downloads in the background, then "Restart"). */
fun createUpdater(activity: ComponentActivity): Updater = PlayUpdater(activity)

private class PlayUpdater(private val activity: ComponentActivity) : Updater {
    private val manager = AppUpdateManagerFactory.create(activity)
    private var info: AppUpdateInfo? = null
    override val canCheck = true

    private val listener = InstallStateUpdatedListener { st ->
        when (st.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = st.totalBytesToDownload()
                UpdateState.progress = if (total > 0) st.bytesDownloaded().toFloat() / total else 0f
            }
            InstallStatus.DOWNLOADED -> { UpdateState.progress = 1f; UpdateState.ready = true }
            InstallStatus.FAILED, InstallStatus.CANCELED -> { UpdateState.progress = null; UpdateState.failed = true }
            else -> {}
        }
    }

    init { manager.registerListener(listener) }

    override fun check(manual: Boolean) {
        UpdateState.checking = true
        manager.appUpdateInfo
            .addOnSuccessListener { i ->
                UpdateState.checking = false
                info = i
                if (i.installStatus() == InstallStatus.DOWNLOADED) { UpdateState.ready = true; return@addOnSuccessListener }
                if (i.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && i.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    UpdateState.offer = UpdateOffer(null, null)
                    UpdateState.dismissed = false
                    UpdateState.upToDate = false
                } else {
                    UpdateState.upToDate = true
                }
            }
            .addOnFailureListener { UpdateState.checking = false; if (manual) UpdateState.failed = true }
    }

    override fun start() {
        val i = info ?: return
        UpdateState.progress = 0f
        try {
            manager.startUpdateFlowForResult(i, activity, AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE), 4711)
        } catch (e: Exception) {
            UpdateState.progress = null
            UpdateState.failed = true
        }
    }

    override fun install() { manager.completeUpdate() }

    override fun close() = manager.unregisterListener(listener)
}
