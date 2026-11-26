package com.svaga.archive

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * "Sign in with Svaga Archive" for Svaga Emulator. The emulator starts this screen for a result; it answers only an app
 * signed with the same key as this one, and only after the user confirms. The emulator gets its own session (tokens).
 */
class EmulatorAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val caller = callingPackage
        val trusted = caller == EMULATOR_PACKAGE &&
            packageManager.checkSignatures(packageName, caller) == PackageManager.SIGNATURE_MATCH
        val store = TokenStore(this)
        val api = ApiClient(store)
        Lang.load(applicationContext)
        setContent {
            Localized {
                SvagaTheme {
                    var busy by remember { mutableStateOf(false) }
                    var error by remember { mutableStateOf<Int?>(null) }
                    Box(Modifier.fillMaxSize().background(Bg).padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(Modifier.fillMaxWidth().clip(CardShape).background(Panel).padding(22.dp)) {
                            Logo(size = 13.sp)
                            Spacer(Modifier.height(14.dp))
                            H2(stringResource(R.string.emu_auth_title))
                            Spacer(Modifier.height(8.dp))
                            when {
                                !trusted -> Body(stringResource(R.string.emu_auth_untrusted), color = Red, size = 14.sp)
                                store.access == null -> Body(stringResource(R.string.emu_auth_signin_first), color = Muted, size = 14.sp)
                                else -> Body(stringResource(R.string.emu_auth_text, store.displayName.ifBlank { store.email }), color = Muted, size = 14.sp)
                            }
                            error?.let { Spacer(Modifier.height(8.dp)); Body(stringResource(it), color = Red, size = 14.sp) }
                            Spacer(Modifier.height(18.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SvButton(stringResource(R.string.cancel), { setResult(Activity.RESULT_CANCELED); finish() }, Modifier.weight(1f), kind = BtnKind.Secondary)
                                if (trusted && store.access != null) {
                                    SvButton(stringResource(R.string.emu_auth_allow), {
                                        busy = true
                                        lifecycleScope.launch {
                                            try {
                                                val o = api.handoff()
                                                val u = o.optJSONObject("user")
                                                setResult(
                                                    Activity.RESULT_OK,
                                                    Intent().putExtra("accessToken", o.optString("accessToken"))
                                                        .putExtra("refreshToken", o.optString("refreshToken"))
                                                        .putExtra("email", u?.optString("email"))
                                                        .putExtra("displayName", u?.optString("displayName"))
                                                        .putExtra("userId", u?.optString("id")),
                                                )
                                                finish()
                                            } catch (e: Exception) {
                                                error = if (e is java.io.IOException) R.string.err_network else R.string.err_generic
                                                busy = false
                                            }
                                        }
                                    }, Modifier.weight(1f), busy = busy)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EMULATOR_PACKAGE = "com.svaga.emulator"
    }
}
