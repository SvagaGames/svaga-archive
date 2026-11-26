package com.svaga.archive

import androidx.activity.ComponentActivity
import ru.rustore.sdk.appupdate.listener.InstallStateUpdateListener
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory
import ru.rustore.sdk.appupdate.model.AppUpdateInfo
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.model.UpdateAvailability

/** RuStore build: RuStore in-app updates SDK (deferred update: download, then install). */
fun createUpdater(activity: ComponentActivity): Updater = RuStoreUpdater(activity)

private class RuStoreUpdater(private val activity: ComponentActivity) : Updater {
    private val manager = RuStoreAppUpdateManagerFactory.create(activity)
    private var info: AppUpdateInfo? = null
    override val canCheck = true

    private val listener = InstallStateUpdateListener { st ->
        when (st.installStatus) {
            InstallStatus.DOWNLOADING -> {
                val total = st.totalBytesToDownload
                UpdateState.progress = if (total > 0) st.bytesDownloaded.toFloat() / total else 0f
            }
            InstallStatus.DOWNLOADED -> { UpdateState.progress = 1f; UpdateState.ready = true }
            InstallStatus.FAILED -> { UpdateState.progress = null; UpdateState.failed = true }
            else -> {}
        }
    }

    override fun check(manual: Boolean) {
        UpdateState.checking = true
        manager.getAppUpdateInfo()
            .addOnSuccessListener { i ->
                UpdateState.checking = false
                info = i
                if (i.updateAvailability == UpdateAvailability.UPDATE_AVAILABLE) {
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
        manager.registerListener(listener)
        manager.startUpdateFlow(i, AppUpdateOptions.Builder().appUpdateType(AppUpdateType.FLEXIBLE).build())
            .addOnFailureListener { UpdateState.progress = null; UpdateState.failed = true }
    }

    override fun install() {
        manager.completeUpdate(AppUpdateOptions.Builder().appUpdateType(AppUpdateType.FLEXIBLE).build())
            .addOnFailureListener { UpdateState.failed = true }
    }

    override fun close() { runCatching { manager.unregisterListener(listener) } }
}
