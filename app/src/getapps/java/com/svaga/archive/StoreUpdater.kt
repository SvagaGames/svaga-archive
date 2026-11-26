package com.svaga.archive

import androidx.activity.ComponentActivity

/** GetApps build: Xiaomi GetApps updates installed apps itself and has no in-app update SDK. */
fun createUpdater(activity: ComponentActivity): Updater = object : Updater {
    override val canCheck = false
    override fun check(manual: Boolean) {}
    override fun start() {}
    override fun install() {}
}
