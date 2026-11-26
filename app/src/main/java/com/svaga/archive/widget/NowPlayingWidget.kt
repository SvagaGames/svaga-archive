package com.svaga.archive.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil.imageLoader
import coil.request.ImageRequest
import com.svaga.archive.Extras
import com.svaga.archive.MainActivity
import com.svaga.archive.R
import org.json.JSONArray

private data class WidgetGame(val name: String, val cover: Bitmap?, val hours: Long)

/** Home-screen widget "Svaga Archive": what is being played now and the nearest release from the library. */
class NowPlayingWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val p = context.getSharedPreferences(Extras.WIDGET_PREFS, Context.MODE_PRIVATE)
        val arr = runCatching { JSONArray(p.getString("playing", "[]")) }.getOrDefault(JSONArray())
        val games = (0 until minOf(arr.length(), 3)).map { i ->
            val o = arr.getJSONObject(i)
            WidgetGame(o.optString("name"), loadCover(context, o.optString("cover")), o.optLong("seconds") / 3600)
        }
        val nextName = p.getString("next_name", null)
        val nextDate = p.getString("next_date", null)
        provideContent { Content(context, games, nextName, nextDate) }
    }

    private suspend fun loadCover(context: Context, url: String): Bitmap? {
        if (url.isBlank()) return null
        return runCatching {
            val req = ImageRequest.Builder(context).data(url).size(120, 160).allowHardware(false).build()
            (context.imageLoader.execute(req).drawable as? BitmapDrawable)?.bitmap
        }.getOrNull()
    }

    @Composable
    private fun Content(context: Context, games: List<WidgetGame>, nextName: String?, nextDate: String?) {
        val ink = ColorProvider(Color(0xFFF3F5F9))
        val muted = ColorProvider(Color(0xFF9AA3B5))
        val cyan = ColorProvider(Color(0xFF3EE0FF))
        Column(
            GlanceModifier.fillMaxSize().background(Color(0xFF14171F)).cornerRadius(22.dp).padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SVAGA", style = TextStyle(color = cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                Spacer(GlanceModifier.width(6.dp))
                Text(context.getString(R.string.widget_now_playing), style = TextStyle(color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.height(10.dp))
            if (games.isEmpty()) {
                Text(context.getString(R.string.widget_empty), style = TextStyle(color = muted, fontSize = 13.sp))
            } else {
                Row(GlanceModifier.fillMaxWidth()) {
                    games.forEachIndexed { i, g ->
                        if (i > 0) Spacer(GlanceModifier.width(10.dp))
                        Column(GlanceModifier.width(76.dp)) {
                            Box(GlanceModifier.size(76.dp, 100.dp).background(Color(0xFF1C2029)).cornerRadius(10.dp)) {
                                if (g.cover != null) {
                                    Image(ImageProvider(g.cover), g.name, GlanceModifier.fillMaxSize().cornerRadius(10.dp), contentScale = ContentScale.Crop)
                                }
                            }
                            Spacer(GlanceModifier.height(4.dp))
                            Text(g.name, style = TextStyle(color = ink, fontSize = 11.sp), maxLines = 1)
                            if (g.hours > 0) Text(context.getString(R.string.widget_hours, g.hours), style = TextStyle(color = muted, fontSize = 10.sp))
                        }
                    }
                }
            }
            if (nextName != null && nextDate != null) {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    context.getString(R.string.widget_next, nextName, nextDate),
                    style = TextStyle(color = muted, fontSize = 11.sp), maxLines = 1,
                )
            }
        }
    }
}

class NowPlayingReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowPlayingWidget()
}
