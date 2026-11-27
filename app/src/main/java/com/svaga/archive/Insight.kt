package com.svaga.archive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import java.util.Locale

private suspend fun <T> quiet(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

private fun h(v: Double): String = if (v >= 10) Math.round(v).toString() else String.format(Locale.US, "%.1f", v).removeSuffix(".0")

/** "How long to beat" on the game page: main story, with extras, 100%. Nothing if IGDB has no data. */
@Composable
fun TtbRow(vm: AppViewModel, gameId: Int) {
    var t by remember(gameId) { mutableStateOf<Ttb?>(null) }
    LaunchedEffect(gameId) { t = quiet { vm.api.ttb(gameId) } }
    val v = t ?: return
    Spacer(Modifier.height(10.dp))
    SvCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⏱", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Body(stringResource(R.string.ttb_title), weight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.ttb_main to v.main, R.string.ttb_extra to v.extra, R.string.ttb_full to v.full).forEach { (label, hours) ->
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Panel2).padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(hours?.let { stringResource(R.string.ttb_hours, h(it)) } ?: "—", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Caption(stringResource(label), maxLines = 1)
                }
            }
        }
    }
}

/** One line under the library counter: how many hours the backlog holds. */
@Composable
fun BacklogForecastLine(vm: AppViewModel) {
    var f by remember { mutableStateOf<Forecast?>(null) }
    LaunchedEffect(vm.library.size) { f = quiet { vm.api.backlogForecast() } }
    val v = f ?: return
    if (v.games == 0 || v.known == 0) return
    val days = (v.hours + 1) / 2 // two hours a day
    val months = days / 30
    Text(
        stringResource(R.string.forecast_line, v.hours, if (months >= 1) stringResource(R.string.forecast_months, months) else stringResource(R.string.forecast_days, days)),
        color = Amber, fontSize = 13.sp,
    )
}

/** Taste compatibility on someone else's profile. */
@Composable
fun CompatCard(vm: AppViewModel, nav: Nav, userId: String) {
    var c by remember(userId) { mutableStateOf<Compat?>(null) }
    LaunchedEffect(userId) {
        c = quiet { vm.api.compat(userId) }
        c?.let { x -> vm.play.ensureGames(x.lovedBoth + x.theyHave) }
    }
    val v = c ?: return
    if (v.hidden) return
    val color = when {
        v.percent >= 80 -> Pink
        v.percent >= 60 -> Green
        v.percent >= 40 -> Cyan
        else -> Muted
    }
    SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = 9.dp.toPx()
                    drawArc(Color.White.copy(alpha = 0.08f), 0f, 360f, false, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
                    drawArc(
                        Brush.sweepGradient(listOf(color, color.copy(alpha = 0.6f), color)), -90f, 360f * v.percent / 100f, false,
                        Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Round),
                    )
                }
                Text("${v.percent}%", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Body(stringResource(R.string.compat_title), weight = FontWeight.Bold)
                Text(
                    stringResource(
                        when {
                            v.percent >= 80 -> R.string.compat_80
                            v.percent >= 60 -> R.string.compat_60
                            v.percent >= 40 -> R.string.compat_40
                            else -> R.string.compat_0
                        },
                    ),
                    color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                )
                Caption(stringResource(R.string.compat_shared, v.shared))
            }
        }
        if (v.lovedBoth.isNotEmpty()) CompatShelf(vm, nav, stringResource(R.string.compat_loved_both), v.lovedBoth)
        if (v.theyHave.isNotEmpty()) CompatShelf(vm, nav, stringResource(R.string.compat_they_have), v.theyHave)
    }
}

@Composable
private fun CompatShelf(vm: AppViewModel, nav: Nav, title: String, ids: List<Int>) {
    Spacer(Modifier.height(12.dp))
    Caption(title)
    Spacer(Modifier.height(6.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ids, key = { it }) { id ->
            val g = vm.gamesCache[id]
            Cover(
                g?.cover, Modifier.width(64.dp).aspectRatio(0.75f).clickable { nav.game(id) }, RoundedCornerShape(8.dp),
            )
        }
    }
}

/** Share picture of the bingo card. */
@Composable
fun BingoShareCard(b: Bingo, name: String) {
    Column(
        Modifier.width(320.dp).clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF0F2A24), Color(0xFF0A0C11))))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("SVAGA ARCHIVE", color = Cyan, fontSize = 12.sp, fontFamily = Pixel)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.bingo_share_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(name, color = Color(0xFF9AA3B5), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().aspectRatio(1f)) {
            for (r in 0 until 5) Row(Modifier.weight(1f)) {
                for (c in 0 until 5) {
                    val cell = b.cells.getOrNull(r * 5 + c)
                    Box(
                        Modifier.weight(1f).fillMaxHeight().padding(2.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (cell?.done == true) Color(0xFF3DDC84).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.07f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(cell?.emoji ?: "", fontSize = 16.sp) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.bingo_share_stats, b.lines.size, b.linesTotal, b.cells.count { it.done } - 1), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text(seasonLabel(b.season), color = Color(0xFF9AA3B5), fontSize = 12.sp)
    }
}
