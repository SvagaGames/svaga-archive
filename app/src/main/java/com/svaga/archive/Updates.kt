package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A newer version the store (or GitHub) offers. */
data class UpdateOffer(val version: String?, val notes: String?, val build: Int? = null)

/** Only the version is shown ("1.0"); the build number stays hidden and is used just to compare builds. */
@Composable
fun offerLabel(o: UpdateOffer): String? = o.version

/** Shared between the store-specific updater (created by MainActivity) and the UI. */
object UpdateState {
    var updater: Updater? = null
    var offer by mutableStateOf<UpdateOffer?>(null)
    var progress by mutableStateOf<Float?>(null)
    var ready by mutableStateOf(false)
    var checking by mutableStateOf(false)
    /** After a manual check: true = already the newest version. */
    var upToDate by mutableStateOf<Boolean?>(null)
    var failed by mutableStateOf(false)
    var dismissed by mutableStateOf(false)
}

/** Every store build ships its own implementation in src/<store>/java (createUpdater). */
interface Updater {
    /** Whether this build can check for updates by itself (GetApps builds rely on the store). */
    val canCheck: Boolean
    fun check(manual: Boolean)
    fun start()
    fun install()
    fun close() {}
}

/** "1.10.0" > "1.9.3" */
fun isNewer(remote: String, local: String): Boolean {
    fun parts(v: String) = v.trim().removePrefix("v").split('.', '-', ' ').map { it.toIntOrNull() ?: 0 }
    val a = parts(remote)
    val b = parts(local)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

/** Banner on top of the main screen: new version → download → install. */
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    val offer = UpdateState.offer
    if ((offer == null && !UpdateState.ready) || UpdateState.dismissed) return
    val u = UpdateState.updater ?: return
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .shadow(10.dp, shape)
            .background(Panel, shape)
            .border(1.dp, Cyan.copy(alpha = 0.6f), shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Refresh, null, tint = Cyan)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val p = UpdateState.progress
            val title = when {
                UpdateState.ready -> stringResource(R.string.update_ready)
                p != null -> stringResource(R.string.update_downloading, (p * 100).toInt())
                offer?.version != null -> stringResource(R.string.update_available_v, offerLabel(offer)!!)
                else -> stringResource(R.string.update_available)
            }
            Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (p != null && !UpdateState.ready) {
                Spacer(Modifier.height(6.dp))
                ProgressLine(p, Cyan)
            } else if (UpdateState.failed) {
                Caption(stringResource(R.string.update_failed), color = Red)
            }
        }
        Spacer(Modifier.width(10.dp))
        when {
            UpdateState.ready -> SvButton(stringResource(R.string.update_install), { u.install() })
            UpdateState.progress == null -> {
                Text(
                    stringResource(R.string.later), color = Muted, fontSize = 14.sp,
                    modifier = Modifier.padding(end = 10.dp).clickable { UpdateState.dismissed = true },
                )
                SvButton(stringResource(R.string.update_get), { UpdateState.failed = false; u.start() })
            }
        }
    }
}
