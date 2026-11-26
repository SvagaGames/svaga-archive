package com.svaga.archive

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.handleDeepLink(intent.data)
    }

    override fun onResume() {
        super.onResume()
        vm.resume()
    }

    override fun onDestroy() {
        UpdateState.updater?.close()
        UpdateState.updater = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Lang.load(applicationContext)
        ThemeMode.load(applicationContext)
        Palette.systemDark =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        enableEdgeToEdge()
        CrashLog.install(applicationContext)
        if (savedInstanceState == null) vm.handleDeepLink(intent?.data)
        val updater = createUpdater(this)
        UpdateState.updater = updater
        if (updater.canCheck) updater.check(manual = false)
        setContent {
            Localized {
                SvagaTheme {
                    App()
                }
            }
        }
    }
}
