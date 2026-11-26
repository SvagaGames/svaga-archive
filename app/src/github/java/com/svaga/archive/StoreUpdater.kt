package com.svaga.archive

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/** GitHub build: checks the latest release of SvagaGames/svaga-archive, downloads the APK and hands it to the installer. */
fun createUpdater(activity: ComponentActivity): Updater = GithubUpdater(activity)

private class GithubUpdater(private val activity: ComponentActivity) : Updater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    private val prefs = activity.getSharedPreferences("svaga_updates", Context.MODE_PRIVATE)
    private var apkUrl: String? = null
    private var apkFile: File? = null
    override val canCheck = true

    override fun check(manual: Boolean) {
        val now = System.currentTimeMillis()
        if (!manual && now - prefs.getLong("last_check", 0) < 6 * 3600 * 1000L) {
            restore()
            return
        }
        UpdateState.checking = true
        UpdateState.upToDate = null
        scope.launch {
            try {
                val o = withContext(Dispatchers.IO) {
                    val req = Request.Builder().url("https://api.github.com/repos/SvagaGames/svaga-archive/releases/latest")
                        .header("Accept", "application/vnd.github+json").build()
                    http.newCall(req).execute().use { r ->
                        if (!r.isSuccessful) throw java.io.IOException("http ${r.code}")
                        JSONObject(r.body?.string().orEmpty())
                    }
                }
                val version = o.optString("tag_name").removePrefix("v")
                val assets = o.optJSONArray("assets")
                var url: String? = null
                if (assets != null) for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name") == "SvagaArchive.apk") url = a.optString("browser_download_url")
                }
                prefs.edit().putLong("last_check", now).putString("version", version).putString("url", url)
                    .putString("notes", o.optString("body")).apply()
                if (url != null && version.isNotBlank() && isNewer(version, BuildConfig.VERSION_NAME)) {
                    apkUrl = url
                    UpdateState.offer = UpdateOffer(version, o.optString("body").takeIf { it.isNotBlank() })
                    UpdateState.dismissed = false
                    UpdateState.upToDate = false
                } else {
                    UpdateState.offer = null
                    UpdateState.upToDate = true
                }
            } catch (e: Exception) {
                if (manual) UpdateState.failed = true
            } finally {
                UpdateState.checking = false
            }
        }
    }

    /** Known newer version from the last check (the app was restarted in between). */
    private fun restore() {
        val v = prefs.getString("version", null) ?: return
        val url = prefs.getString("url", null) ?: return
        if (isNewer(v, BuildConfig.VERSION_NAME)) {
            apkUrl = url
            UpdateState.offer = UpdateOffer(v, prefs.getString("notes", null))
        }
    }

    override fun start() {
        val url = apkUrl ?: return
        val version = UpdateState.offer?.version ?: "new"
        UpdateState.progress = 0f
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val dir = File(activity.cacheDir, "updates").apply { mkdirs() }
                    dir.listFiles()?.forEach { it.delete() }
                    val out = File(dir, "SvagaArchive-$version.apk")
                    http.newCall(Request.Builder().url(url).build()).execute().use { r ->
                        if (!r.isSuccessful) throw java.io.IOException("http ${r.code}")
                        val body = r.body ?: throw java.io.IOException("empty")
                        val total = body.contentLength().takeIf { it > 0 } ?: 12_000_000L
                        body.byteStream().use { input ->
                            out.outputStream().use { output ->
                                val buf = ByteArray(64 * 1024)
                                var done = 0L
                                var last = 0L
                                while (true) {
                                    val n = input.read(buf)
                                    if (n < 0) break
                                    output.write(buf, 0, n)
                                    done += n
                                    if (done - last > 256 * 1024) {
                                        last = done
                                        val f = (done.toFloat() / total).coerceIn(0f, 1f)
                                        withContext(Dispatchers.Main) { UpdateState.progress = f }
                                    }
                                }
                            }
                        }
                    }
                    out
                }
                apkFile = file
                UpdateState.progress = 1f
                UpdateState.ready = true
                install()
            } catch (e: Exception) {
                UpdateState.progress = null
                UpdateState.failed = true
            }
        }
    }

    override fun install() {
        val file = apkFile ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.files", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            UpdateState.failed = true
        }
    }

    override fun close() = scope.cancel()
}
