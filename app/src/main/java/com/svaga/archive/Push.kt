package com.svaga.archive

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Push notifications without Google services (works in GitHub, RuStore and GetApps builds alike): every 30 minutes
 * WorkManager asks the server for new notifications and shows them as system notifications.
 */
object Push {
    private const val CHANNEL = "svaga_main"
    private const val WORK = "svaga_push"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("svaga_push", Context.MODE_PRIVATE)

    fun enabled(ctx: Context) = prefs(ctx).getBoolean("enabled", true)

    fun setEnabled(ctx: Context, on: Boolean) {
        prefs(ctx).edit().putBoolean("enabled", on).apply()
        schedule(ctx)
    }

    fun schedule(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        if (!enabled(ctx)) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val req = PeriodicWorkRequestBuilder<PushWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun channel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.push_channel), NotificationManager.IMPORTANCE_DEFAULT))
            }
        }
    }

    private fun text(ctx: Context, n: NotificationItem): String? {
        val p = n.payload
        val name = p.optString("name")
        return when (n.kind) {
            "achievement" -> ctx.getString(R.string.push_achievement, p.optString("title").ifBlank { DynAch.map[p.optString("code")]?.title ?: ctx.getString(R.string.push_new_achievement) }, p.optInt("sp"))
            "level_up" -> ctx.getString(R.string.push_level, p.optInt("level", 1))
            "friend_request" -> ctx.getString(R.string.n_friend_request, name)
            "friend_accepted" -> ctx.getString(R.string.n_friend_accepted, name)
            "comment_like" -> ctx.getString(R.string.n_comment_like, name)
            "release" -> ctx.getString(R.string.n_release, name)
            "news" -> ctx.getString(R.string.n_news, p.optString("title"))
            "anniversary" -> ctx.getString(R.string.n_anniversary, name, ctx.resources.getQuantityString(R.plurals.years_count, p.optInt("years"), p.optInt("years")))
            "list_like" -> ctx.getString(R.string.n_list_like, name, p.optString("title"))
            "comeback" -> ctx.getString(R.string.n_comeback, name, p.optInt("days"))
            "bingo_line" -> ctx.getString(R.string.n_bingo_line, p.optInt("lines"), p.optInt("total", 12))
            "bingo_full" -> ctx.getString(R.string.n_bingo_full)
            else -> null
        }
    }

    /** Shows notifications newer than the last one shown; the first run only remembers where we are. */
    suspend fun check(ctx: Context) {
        if (!enabled(ctx)) return
        val store = TokenStore(ctx)
        if (store.access == null) return
        val items = runCatching { ApiClient(store).notifications(0).items }.getOrNull() ?: return
        val key = "last_" + store.userId
        val last = prefs(ctx).getLong(key, -1L)
        val maxId = items.maxOfOrNull { it.id } ?: 0L
        prefs(ctx).edit().putLong(key, maxOf(maxId, last)).apply()
        if (last < 0 || !canPost(ctx)) return
        channel(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        items.filter { it.id > last && !it.read }.sortedBy { it.id }.takeLast(5).forEach { n ->
            val body = text(ctx, n) ?: return@forEach
            val notif = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle("Svaga Archive")
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(ctx).notify(n.id.toInt(), notif)
            } catch (e: SecurityException) {
                return
            }
        }
    }
}

class PushWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        Push.check(applicationContext)
        return Result.success()
    }
}
