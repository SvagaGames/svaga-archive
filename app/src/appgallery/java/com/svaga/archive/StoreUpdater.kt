package com.svaga.archive

import androidx.activity.ComponentActivity

/** AppGallery build: Huawei AppGallery updates installed apps itself; no Google services or extra SDK are needed. */
fun createUpdater(activity: ComponentActivity): Updater = object : Updater {
    override val canCheck = false
    override fun check(manual: Boolean) {}
    override fun start() {}
    override fun install() {}
}
