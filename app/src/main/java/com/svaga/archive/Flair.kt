package com.svaga.archive

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Retro bingo and profile frames/backgrounds. */
class Flair(private val vm: AppViewModel) {
    var bingo by mutableStateOf<Bingo?>(null)
        private set
    var bingoError by mutableStateOf<Int?>(null)
        private set
    var cos by mutableStateOf<Cosmetics?>(null)
        private set

    private fun run(onError: (Int) -> Unit = {}, block: suspend () -> Unit) {
        vm.viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                vm.handle(e)
                onError(vm.errorRes(e))
            }
        }
    }

    fun loadBingo() = run({ bingoError = it }) {
        bingoError = null
        val before = bingo?.lines?.size ?: -1
        val b = vm.api.bingo()
        bingo = b
        if (before >= 0 && b.lines.size > before) vm.refreshAfterAchievements()
    }

    fun loadCosmetics() = run { cos = vm.api.cosmetics() }

    var season by mutableStateOf<SeasonPass?>(null)
        private set
    var seasonError by mutableStateOf<Int?>(null)
        private set
    var seasonTop by mutableStateOf<List<SeasonTopRow>?>(null)
        private set

    fun loadSeason(fresh: Boolean = false) = run({ seasonError = it }) {
        seasonError = null
        val before = season?.level ?: -1
        val s = vm.api.season(fresh)
        season = s
        if (before >= 0 && s.level > before) {
            vm.refreshAfterAchievements()
            cos = vm.api.cosmetics()
        }
    }

    fun loadSeasonTop(friends: Boolean) = run {
        seasonTop = null
        seasonTop = vm.api.seasonTop(friends)
    }

    fun equip(frame: String? = null, background: String? = null, badge: String? = null) = run({ vm.showNotice(it) }) {
        val (f, b, n) = vm.api.setCosmetics(frame, background, badge)
        cos = cos?.copy(frame = f, background = b, badge = n)
        vm.showNotice(R.string.flair_saved)
    }

    fun setComeback(value: Boolean) {
        val before = vm.notifyComeback
        vm.notifyComeback = value
        run({ vm.notifyComeback = before }) { vm.notifyComeback = vm.api.setNotifyComeback(value) }
    }
}

// =====================================================================
// Frames and backgrounds
// =====================================================================

private val Gold = listOf(Color(0xFFFFE08A), Color(0xFFE8A93A), Color(0xFFFFF3C4), Color(0xFFB9781E), Color(0xFFFFE08A))
private val Rainbow = listOf(Color(0xFFFF5A6A), Color(0xFFFFB547), Color(0xFFFFF06A), Color(0xFF3DDC84), Color(0xFF3EE0FF), Color(0xFFB98BFF), Color(0xFFFF5A6A))
private val Fire = listOf(Color(0xFFFF3D2E), Color(0xFFFFB547), Color(0xFFFFF06A), Color(0xFFFF6A00), Color(0xFFFF3D2E))
private val Ice = listOf(Color(0xFFE6FBFF), Color(0xFF8FE3FF), Color(0xFFFFFFFF), Color(0xFF5BC0EB), Color(0xFFE6FBFF))
private val Legend = listOf(Color(0xFFB98BFF), Color(0xFFFFD36A), Color(0xFF7A4DFF), Color(0xFFFFD36A), Color(0xFFB98BFF))

val FRAME_NAMES = mapOf(
    "none" to R.string.frame_none, "neon" to R.string.frame_neon, "retro" to R.string.frame_retro, "pixel" to R.string.frame_pixel,
    "gold" to R.string.frame_gold, "ice" to R.string.frame_ice, "fire" to R.string.frame_fire, "rainbow" to R.string.frame_rainbow,
    "legend" to R.string.frame_legend, "season" to R.string.frame_season, "champion" to R.string.frame_champion, "creator" to R.string.frame_creator,
)
val BG_NAMES = mapOf(
    "none" to R.string.bg_none, "night" to R.string.bg_night, "sunset" to R.string.bg_sunset, "cart" to R.string.bg_cart,
    "grid" to R.string.bg_grid, "bingo" to R.string.bg_bingo, "stars" to R.string.bg_stars, "aurora" to R.string.bg_aurora,
    "season" to R.string.bg_season, "creator" to R.string.bg_creator,
)

/** Badge next to the nickname: emoji + name. */
val BADGES = mapOf(
    "og" to ("🕹️" to R.string.badge_og), "fox" to ("🦊" to R.string.badge_fox), "collector" to ("📦" to R.string.badge_collector),
    "finisher" to ("🏁" to R.string.badge_finisher), "brain" to ("🧠" to R.string.badge_brain), "flame" to ("🔥" to R.string.badge_flame),
    "curator" to ("📝" to R.string.badge_curator), "star" to ("⭐" to R.string.badge_star), "bingo" to ("🎱" to R.string.badge_bingo),
    "spark" to ("⚡" to R.string.badge_spark), "comet" to ("☄️" to R.string.badge_comet), "crown" to ("👑" to R.string.badge_crown),
    "trophy" to ("🏆" to R.string.badge_trophy), "creator" to ("🛠️" to R.string.badge_creator),
)

@Composable
fun NickBadge(code: String?, size: androidx.compose.ui.unit.TextUnit = 15.sp) {
    val b = BADGES[code ?: return] ?: return
    Spacer(Modifier.width(5.dp))
    Text(b.first, fontSize = size, maxLines = 1)
}

/** Avatar in the chosen frame. "none" is the usual accent ring. */
@Composable
fun FramedAvatar(userId: String, version: Int, name: String, size: Dp, frame: String, modifier: Modifier = Modifier) {
    if (frame == "none" || frame !in FRAME_NAMES) {
        Avatar(userId, version, name, size, modifier, ring = true)
        return
    }
    val spin by rememberInfiniteTransition(label = "frame").animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "spin")
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(0.55f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse")
    Box(modifier.size(size).drawBehind { drawFrame(frame, spin, pulse) }, contentAlignment = Alignment.Center) {
        Avatar(userId, version, name, size - 14.dp)
    }
}

private fun DrawScope.ring(brush: Brush, width: Float, inset: Float = 0f) {
    val r = size.minDimension / 2 - width / 2 - inset
    drawCircle(brush, radius = r, style = Stroke(width))
}

private fun DrawScope.drawFrame(frame: String, spin: Float, pulse: Float) {
    val w = size.minDimension * 0.055f
    when (frame) {
        "neon" -> {
            drawCircle(Cyan.copy(alpha = 0.25f * pulse), radius = size.minDimension / 2, style = Stroke(w * 2.2f))
            ring(SolidColorBrush(Cyan), w)
        }
        "retro" -> {
            ring(SolidColorBrush(Color(0xFF8A5A2B)), w * 1.6f)
            val n = 24
            for (i in 0 until n) {
                val a = Math.toRadians(i * 360.0 / n)
                val r = size.minDimension / 2 - w * 0.8f
                drawCircle(if (i % 2 == 0) Color(0xFFFFB547) else Color(0xFFFF5A6A), w * 0.35f, Offset(center.x + (r * cos(a)).toFloat(), center.y + (r * sin(a)).toFloat()))
            }
        }
        "pixel" -> {
            val cells = 28
            val r = size.minDimension / 2 - w
            val px = w * 1.4f
            for (i in 0 until cells) {
                val a = Math.toRadians(i * 360.0 / cells)
                val x = center.x + (r * cos(a)).toFloat() - px / 2
                val y = center.y + (r * sin(a)).toFloat() - px / 2
                drawRect(if (i % 2 == 0) Color(0xFF3DDC84) else Color(0xFF0F380F), Offset(x, y), Size(px, px))
            }
        }
        "gold" -> rotate(spin / 3) { ring(Brush.sweepGradient(Gold), w * 1.3f) }
        "ice" -> {
            rotate(-spin / 2) { ring(Brush.sweepGradient(Ice), w * 1.2f) }
            drawCircle(Color.White.copy(alpha = 0.18f * pulse), radius = size.minDimension / 2, style = Stroke(w * 2))
        }
        "fire" -> {
            drawCircle(Color(0xFFFF6A00).copy(alpha = 0.3f * pulse), radius = size.minDimension / 2, style = Stroke(w * 2.4f))
            rotate(spin) { ring(Brush.sweepGradient(Fire), w * 1.4f) }
        }
        "rainbow" -> rotate(spin) { ring(Brush.sweepGradient(Rainbow), w * 1.4f) }
        "legend" -> {
            rotate(spin) { ring(Brush.sweepGradient(Legend), w * 1.2f) }
            rotate(-spin) { ring(Brush.sweepGradient(Legend.reversed()), w * 0.6f, inset = w * 1.5f) }
            drawCircle(Color(0xFFFFD36A).copy(alpha = 0.22f * pulse), radius = size.minDimension / 2, style = Stroke(w * 2.4f))
        }
        "season" -> {
            rotate(spin) { ring(Brush.sweepGradient(listOf(Color(0xFF3EE0FF), Color(0xFF4F7CFF), Color(0xFFB98BFF), Color(0xFF3EE0FF))), w * 1.3f) }
            val n = 8
            for (i in 0 until n) {
                val a = Math.toRadians(i * 360.0 / n + spin)
                val r = size.minDimension / 2 - w * 0.65f
                drawCircle(Color.White.copy(alpha = 0.5f + 0.5f * pulse), w * 0.32f, Offset(center.x + (r * cos(a)).toFloat(), center.y + (r * sin(a)).toFloat()))
            }
        }
        "champion" -> {
            drawCircle(Color(0xFFFFD36A).copy(alpha = 0.35f * pulse), radius = size.minDimension / 2, style = Stroke(w * 2.6f))
            rotate(spin / 2) { ring(Brush.sweepGradient(Gold), w * 1.5f) }
            rotate(-spin) { ring(Brush.sweepGradient(listOf(Color(0xFFFF5A6A), Color(0xFFFFD36A), Color(0xFFFF5A6A))), w * 0.5f, inset = w * 1.7f) }
        }
        "creator" -> {
            rotate(spin / 2) { ring(Brush.sweepGradient(listOf(Amber, Cyan, Amber, Cyan, Amber)), w * 1.3f) }
            drawCircle(Amber.copy(alpha = 0.25f * pulse), radius = size.minDimension / 2, style = Stroke(w * 2.2f))
        }
    }
}

@Suppress("FunctionName")
private fun SolidColorBrush(c: Color) = androidx.compose.ui.graphics.SolidColor(c)

/** Profile background behind the header; "none" draws nothing. */
fun Modifier.profileBackground(code: String): Modifier = if (code == "none" || code !in BG_NAMES) this else drawBehind { drawBg(code) }

private fun DrawScope.drawBg(code: String) {
    when (code) {
        "night" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0B1033), Color(0xFF1B1446), Color(0x000A0C11))))
            stars(40, 11)
        }
        "sunset" -> drawRect(Brush.verticalGradient(listOf(Color(0xFF3A1240), Color(0xFFB3354F), Color(0xFFFF9A4A).copy(alpha = 0.55f), Color(0x000A0C11))))
        "cart" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF2B2E36), Color(0x000A0C11))))
            val step = 14.dp.toPx()
            var y = 0f
            while (y < size.height * 0.6f) {
                drawRect(Color.White.copy(alpha = 0.05f), Offset(0f, y), Size(size.width, step / 3))
                y += step
            }
        }
        "grid" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF2A0A3D), Color(0xFF12051E), Color(0x000A0C11))))
            val horizon = size.height * 0.45f
            val pink = Color(0xFFFF4FD8).copy(alpha = 0.45f)
            for (i in 0..8) {
                val y = horizon + (size.height - horizon) * (i / 8f) * (i / 8f)
                drawLine(pink, Offset(0f, y), Offset(size.width, y), 1.5f)
            }
            for (i in -10..10) {
                drawLine(pink, Offset(size.width / 2 + i * size.width / 20f, horizon), Offset(size.width / 2 + i * size.width / 4f, size.height), 1.5f)
            }
            drawCircle(Brush.verticalGradient(listOf(Color(0xFFFFE08A), Color(0xFFFF4FD8))), size.height * 0.18f, Offset(size.width * 0.82f, horizon - size.height * 0.04f))
        }
        "bingo" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1B2440), Color(0x000A0C11))))
            val s = 22.dp.toPx()
            var y = 0f
            var row = 0
            while (y < size.height * 0.7f) {
                var x = if (row % 2 == 0) 0f else s / 2
                while (x < size.width) {
                    drawCircle(listOf(Amber, Cyan, Purple, Green)[(row + (x / s).toInt()) % 4].copy(alpha = 0.18f), s * 0.18f, Offset(x, y))
                    x += s
                }
                y += s; row++
            }
        }
        "stars" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF070A1A), Color(0x000A0C11))))
            stars(90, 7)
        }
        "aurora" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF041A1F), Color(0x000A0C11))))
            drawRect(Brush.linearGradient(listOf(Color(0x003DDC84), Color(0x663DDC84), Color(0x553EE0FF), Color(0x44B98BFF), Color(0x00B98BFF)), Offset(0f, 0f), Offset(size.width, size.height * 0.6f)))
            stars(25, 3)
        }
        "season" -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0C1C3A), Color(0xFF1A1150), Color(0x000A0C11))))
            for (i in 0..5) {
                val x = size.width * (0.1f + i * 0.17f)
                drawLine(Brush.verticalGradient(listOf(Color(0x003EE0FF), Color(0x553EE0FF), Color(0x00B98BFF))), Offset(x, 0f), Offset(x - size.width * 0.25f, size.height * 0.8f), 3.dp.toPx())
            }
            stars(30, 5)
        }
        "creator" -> drawRect(Brush.verticalGradient(listOf(Color(0xFF3B2A0A), Color(0xFF241A06), Color(0x000A0C11))))
    }
}

private fun DrawScope.stars(n: Int, seed: Int) {
    val r = Random(seed)
    repeat(n) {
        drawCircle(Color.White.copy(alpha = 0.3f + r.nextFloat() * 0.6f), 0.8f + r.nextFloat() * 1.6f, Offset(r.nextFloat() * size.width, r.nextFloat() * size.height * 0.7f))
    }
}

// =====================================================================
// Cosmetics screen
// =====================================================================

@Composable
private fun unlockHint(it: CosItem): String = when (it.kind) {
    "level" -> stringResource(R.string.unlock_level, it.goal)
    "library" -> stringResource(R.string.unlock_library, it.goal)
    "completed" -> stringResource(R.string.unlock_completed, it.goal)
    "achievements" -> stringResource(R.string.unlock_achievements, it.goal)
    "streak" -> stringResource(R.string.unlock_streak, it.goal)
    "bingoFull" -> stringResource(R.string.unlock_bingo_full)
    "bingoLines" -> stringResource(R.string.unlock_bingo_line)
    "quiz" -> stringResource(R.string.unlock_quiz, it.goal)
    "shelf" -> stringResource(R.string.unlock_shelf, it.goal)
    "creator" -> stringResource(R.string.unlock_creator)
    "seasonLevel" -> stringResource(R.string.unlock_season, it.goal)
    "lists" -> stringResource(R.string.unlock_lists, it.goal)
    "early" -> stringResource(R.string.unlock_early)
    else -> ""
}

@Composable
fun CosmeticsScreen(vm: AppViewModel, nav: Nav) {
    val f = vm.flair
    LaunchedEffect(Unit) { f.loadCosmetics() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(stringResource(R.string.flair_title), nav::back, subtitle = stringResource(R.string.flair_subtitle))
        val c = f.cos
        if (c == null) { LoadingBox(); return@Column }
        // live preview of the header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(22.dp)).background(Panel).profileBackground(c.background).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FramedAvatar(vm.userId, vm.avatarVersion, vm.displayName.ifBlank { vm.email }, 96.dp, c.frame)
            Spacer(Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    H2(vm.displayName.ifBlank { stringResource(R.string.app_name) })
                    NickBadge(c.badge, 20.sp)
                }
                Caption(stringResource(FRAME_NAMES[c.frame] ?: R.string.frame_none) + " · " + stringResource(BG_NAMES[c.background] ?: R.string.bg_none))
            }
        }
        Segmented(listOf(stringResource(R.string.flair_frames), stringResource(R.string.flair_backgrounds), stringResource(R.string.flair_badges)), tab, { tab = it }, Modifier.padding(16.dp))
        val list = when (tab) { 0 -> c.frames; 1 -> c.backgrounds; else -> c.badges }
        LazyVerticalGrid(GridCells.Fixed(3), Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.code }) { it ->
                val selected = when (tab) { 0 -> c.frame == it.code; 1 -> c.background == it.code; else -> c.badge == it.code }
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(Panel)
                        .border(if (selected) 2.dp else 1.dp, if (selected) Cyan else com.svaga.archive.Stroke, RoundedCornerShape(16.dp))
                        .clickable(enabled = it.unlocked && !selected) {
                            when (tab) { 0 -> f.equip(frame = it.code); 1 -> f.equip(background = it.code); else -> f.equip(badge = it.code) }
                        }
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (tab == 2) {
                            Box(Modifier.size(64.dp).clip(CircleShape).background(Panel2), contentAlignment = Alignment.Center) {
                                Text(BADGES[it.code]?.first ?: "∅", fontSize = 30.sp, color = Muted)
                            }
                        } else if (tab == 0) {
                            FramedAvatar(vm.userId, vm.avatarVersion, vm.displayName, 64.dp, it.code)
                        } else {
                            Box(Modifier.size(84.dp, 64.dp).clip(RoundedCornerShape(10.dp)).background(Bg).profileBackground(it.code))
                        }
                        if (!it.unlocked) {
                            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Lock, null, tint = Ink, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(
                            when (tab) { 0 -> FRAME_NAMES[it.code]; 1 -> BG_NAMES[it.code]; else -> BADGES[it.code]?.second } ?: R.string.frame_none,
                        ),
                        color = if (selected) Cyan else Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, textAlign = TextAlign.Center,
                    )
                    if (!it.unlocked) {
                        Text(unlockHint(it), color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
                        if (it.goal > 1) {
                            Spacer(Modifier.height(4.dp))
                            ProgressLine(it.progress.toFloat() / it.goal, Amber)
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Retro bingo
// =====================================================================

@Composable
fun BingoTile(vm: AppViewModel, nav: Nav, modifier: Modifier = Modifier) {
    val b = vm.flair.bingo
    LaunchedEffect(Unit) { if (vm.flair.bingo == null) vm.flair.loadBingo() }
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.clip(shape).background(Brush.linearGradient(listOf(Green.copy(alpha = 0.30f), Cyan.copy(alpha = 0.12f), Panel)))
            .border(1.dp, Green.copy(alpha = 0.55f), shape).clickable { nav.go(Routes.BINGO) }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🎱", fontSize = 30.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Body(stringResource(R.string.bingo_title), weight = FontWeight.Bold, maxLines = 1)
            Caption(
                if (b == null) stringResource(R.string.bingo_tile_new)
                else stringResource(R.string.bingo_tile, b.cells.count { it.done } - 1, 24, b.lines.size, b.linesTotal),
                maxLines = 2,
            )
        }
        if (b != null) {
            Box(Modifier.size(46.dp)) { MiniBingo(b) }
        }
    }
}

@Composable
private fun MiniBingo(b: Bingo) {
    Canvas(Modifier.fillMaxSize()) {
        val s = size.width / 5
        b.cells.forEachIndexed { i, c ->
            drawRect(if (c.done) Green else Color.White.copy(alpha = 0.12f), Offset((i % 5) * s + 1, (i / 5) * s + 1), Size(s - 2, s - 2))
        }
    }
}

private val LINE_CELLS: List<List<Int>> = (0..4).map { r -> (0..4).map { r * 5 + it } } + (0..4).map { c -> (0..4).map { it * 5 + c } } +
    listOf(listOf(0, 6, 12, 18, 24), listOf(4, 8, 12, 16, 20))

@Composable
fun BingoScreen(vm: AppViewModel, nav: Nav) {
    val f = vm.flair
    LaunchedEffect(Unit) { f.loadBingo() }
    var open by remember { mutableStateOf<BingoCell?>(null) }
    var share by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(stringResource(R.string.bingo_title), nav::back, subtitle = stringResource(R.string.bingo_subtitle)) {
            if (f.bingo != null) IconBtn(Icons.Filled.Share, { share = true })
        }
        f.bingo?.let { cur ->
            if (share) {
                val text = stringResource(R.string.bingo_share_text, cur.lines.size, cur.linesTotal)
                ShareDialog(text, { share = false }) { BingoShareCard(cur, vm.displayName) }
            }
        }
        val b = f.bingo
        when {
            b == null && f.bingoError != null -> { ErrorState(stringResource(f.bingoError!!), { f.loadBingo() }); return@Column }
            b == null -> { LoadingBox(); return@Column }
        }
        b!!
        val inLine = b.lines.flatMap { LINE_CELLS.getOrElse(it) { emptyList() } }.toSet()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            val days = runCatching { ChronoUnit.DAYS.between(Instant.now(), Instant.parse(b.endsAt)).coerceAtLeast(0) }.getOrNull()
            Row(Modifier.padding(16.dp, 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("${b.lines.size}/${b.linesTotal}", stringResource(R.string.bingo_lines), Modifier.weight(1f), accent = Green)
                StatTile("${b.cells.count { it.done } - 1}/24", stringResource(R.string.bingo_cells), Modifier.weight(1f))
                if (days != null) StatTile(days.toString(), stringResource(R.string.bingo_days_left), Modifier.weight(1f), accent = Amber)
            }
            Column(Modifier.padding(12.dp).fillMaxWidth().aspectRatio(1f)) {
                for (r in 0 until 5) Row(Modifier.weight(1f)) {
                    for (c in 0 until 5) {
                        val i = r * 5 + c
                        val cell = b.cells.getOrNull(i) ?: continue
                        val line = i in inLine
                        Box(
                            Modifier.weight(1f).fillMaxHeight().padding(3.dp).clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        line -> Brush.linearGradient(listOf(Green.copy(alpha = 0.55f), Cyan.copy(alpha = 0.35f)))
                                        cell.done -> Brush.linearGradient(listOf(Green.copy(alpha = 0.28f), Green.copy(alpha = 0.18f)))
                                        else -> Brush.linearGradient(listOf(Panel, Panel2))
                                    },
                                )
                                .border(1.dp, if (cell.done) Green.copy(alpha = 0.8f) else com.svaga.archive.Stroke, RoundedCornerShape(12.dp))
                                .clickable { open = cell }
                                .padding(3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(cell.emoji, fontSize = 20.sp)
                                if (cell.goal > 1 && !cell.done) Text("${cell.progress}/${cell.goal}", color = Muted, fontSize = 10.sp)
                                if (cell.done && cell.id != "free") Text("✓", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Caption(stringResource(R.string.bingo_rules), Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Spacer(Modifier.height(8.dp))
            b.cells.filter { !it.done }.forEach { c ->
                Row(Modifier.fillMaxWidth().clickable { open = c }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.emoji, fontSize = 20.sp, modifier = Modifier.width(34.dp))
                    Body(c.title, Modifier.weight(1f), maxLines = 2)
                    if (c.goal > 1) Caption("${c.progress}/${c.goal}", color = Amber)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    open?.let { c ->
        Dialog(onDismissRequest = { open = null }) {
            Column(Modifier.clip(RoundedCornerShape(22.dp)).background(Panel).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(c.emoji, fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                H2(c.title)
                Spacer(Modifier.height(8.dp))
                if (c.id == "free") Caption(stringResource(R.string.bingo_free_hint))
                else if (c.done) Body(stringResource(R.string.bingo_done), color = Green, weight = FontWeight.Bold)
                else {
                    Caption(stringResource(R.string.bingo_progress, c.progress, c.goal))
                    Spacer(Modifier.height(8.dp))
                    ProgressLine(if (c.goal > 0) c.progress.toFloat() / c.goal else 0f, Amber)
                    Spacer(Modifier.height(8.dp))
                    Caption(stringResource(R.string.bingo_season_only))
                }
                Spacer(Modifier.height(14.dp))
                SvButton(stringResource(R.string.bingo_ok), { open = null }, Modifier.fillMaxWidth())
            }
        }
    }
}
