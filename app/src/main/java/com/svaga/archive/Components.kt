package com.svaga.archive

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ======================= games =======================

/** Poster card: cover 3:4 with status / rating badges, title and a caption under it. */
@Composable
fun PosterCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: String? = null,
    myRating: Int? = null,
    caption: String? = null,
    width: Dp? = null,
) {
    Column(modifier.then(if (width != null) Modifier.width(width) else Modifier).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.75f)) {
            Cover(game.cover, Modifier.fillMaxSize())
            if (status != null) {
                Pill(statusLabel(status), statusColor(status), Modifier.align(Alignment.TopStart).padding(6.dp), filled = true)
            }
            val r = myRating?.toString() ?: game.rating?.let { String.format(Locale.US, "%.1f", it / 10.0) }
            if (r != null) StarBadge(r, Modifier.align(Alignment.BottomStart).padding(6.dp), color = if (myRating != null) Cyan else Amber)
        }
        Spacer(Modifier.height(6.dp))
        Text(game.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
        val cap = caption ?: listOfNotNull(game.year?.toString(), game.platforms.firstOrNull()).joinToString(" · ")
        if (cap.isNotBlank()) Text(cap, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** List row: small cover, title, caption lines and an optional trailing slot. */
@Composable
fun GameRow(
    game: Game?,
    fallbackTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    status: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(game?.cover, Modifier.width(56.dp).aspectRatio(0.75f), RoundedCornerShape(10.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Body(game?.name ?: fallbackTitle, size = 16.sp, weight = FontWeight.SemiBold, maxLines = 2)
            val sub = subtitle ?: game?.let { g -> listOfNotNull(g.year?.toString(), g.platforms.take(2).joinToString(", ").ifBlank { null }).joinToString(" · ") }
            if (!sub.isNullOrBlank()) Caption(sub, maxLines = 2)
            if (status != null) {
                Spacer(Modifier.height(4.dp))
                Pill(statusLabel(status), statusColor(status))
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Horizontal shelf of posters with a section header. */
@Composable
fun GameShelf(
    title: String,
    games: List<Game>,
    vm: AppViewModel,
    onOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
    caption: ((Game) -> String?)? = null,
) {
    if (games.isEmpty()) return
    Column(modifier) {
        SectionHeader(title, onMore = onMore)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(games, key = { it.id }) { g ->
                val entry = vm.library[g.id]
                PosterCard(g, { onOpen(g.id) }, status = entry?.status, myRating = entry?.rating, caption = caption?.invoke(g), width = 124.dp)
            }
        }
    }
}

/** Horizontal row of chips that scrolls. */
@Composable
fun ChipRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

// ======================= filters =======================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    filter: GameFilter,
    platforms: List<Platform>,
    pinned: List<Platform>,
    onChange: (GameFilter) -> Unit,
    onDismiss: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pickPlatform by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = Bg) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                H2(stringResource(R.string.filters), Modifier.weight(1f))
                if (filter.active) {
                    TextButton(onClick = { onChange(GameFilter()) }) { Text(stringResource(R.string.filters_reset), color = Red) }
                }
            }
            FilterLabel(stringResource(R.string.genres))
            ChipRow {
                SvChip(stringResource(R.string.all_genres), filter.genre == null, { onChange(filter.copy(genre = null)) })
                GENRE_FILTERS.forEach { f ->
                    SvChip(f.label(), filter.genre == f.id, { onChange(filter.copy(genre = if (filter.genre == f.id) null else f.id)) })
                }
            }
            FilterLabel(stringResource(R.string.platforms))
            val items = if (pinned.isNotEmpty()) pinned.map { FilterItem(it.id, it.short, it.short) } else PLATFORM_FILTERS
            ChipRow {
                SvChip(stringResource(R.string.all_platforms), filter.platform == null, { onChange(filter.copy(platform = null)) })
                items.forEach { f ->
                    SvChip(f.label(), filter.platform == f.id, { onChange(filter.copy(platform = if (filter.platform == f.id) null else f.id)) })
                }
                val other = filter.platform != null && items.none { it.id == filter.platform }
                SvChip(
                    if (other) platforms.firstOrNull { it.id == filter.platform }?.short ?: stringResource(R.string.platforms_more) else stringResource(R.string.platforms_more),
                    other,
                    { pickPlatform = true },
                    accent = Amber,
                )
            }
            FilterLabel(stringResource(R.string.released))
            ChipRow {
                SvChip(stringResource(R.string.any_year), filter.yearFrom == null && filter.yearTo == null, { onChange(filter.copy(yearFrom = null, yearTo = null)) })
                YEAR_RANGES.forEach { r ->
                    val same = filter.yearFrom == r.from && filter.yearTo == r.to
                    SvChip(r.label, same, { onChange(if (same) filter.copy(yearFrom = null, yearTo = null) else filter.copy(yearFrom = r.from, yearTo = r.to)) })
                }
            }
            FilterLabel(stringResource(R.string.section_rating))
            ChipRow {
                SvChip(stringResource(R.string.any_rating), filter.minRating == null, { onChange(filter.copy(minRating = null)) })
                RATING_STEPS.forEach { r ->
                    SvChip("$r+", filter.minRating == r, { onChange(filter.copy(minRating = if (filter.minRating == r) null else r)) }, accent = Green)
                }
            }
            extra()
            Spacer(Modifier.height(16.dp))
            SvButton(stringResource(R.string.done), onDismiss, Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }
    }
    if (pickPlatform) {
        PlatformPicker(platforms, filter.platform, onSelect = {
            onChange(filter.copy(platform = it))
            pickPlatform = false
        }, onDismiss = { pickPlatform = false })
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(text, color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp))
}

/** Full-screen dialog with a searchable platform list. [multi] keeps it open and toggles selections. */
@Composable
fun PlatformPicker(
    platforms: List<Platform>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
    showAll: Boolean = true,
    multi: List<Int>? = null,
    onToggle: (Int) -> Unit = {},
    title: String? = null,
) {
    var q by rememberSaveable { mutableStateOf("") }
    val needle = q.trim()
    val shown = platforms.filter { needle.isEmpty() || it.name.contains(needle, true) || it.short.contains(needle, true) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .clip(CardShape)
                .background(Bg, CardShape)
                .border(1.dp, Stroke, CardShape)
                .padding(16.dp),
        ) {
            H2(title ?: stringResource(R.string.all_platforms))
            Spacer(Modifier.height(12.dp))
            SearchBar(q, { q = it }, stringResource(R.string.platform_search))
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                if (showAll && multi == null) {
                    item {
                        PickRow(stringResource(R.string.filter_all), selected == null) { onSelect(null) }
                    }
                }
                items(shown, key = { it.id }) { p ->
                    val on = if (multi != null) p.id in multi else p.id == selected
                    PickRow(p.name, on) { if (multi != null) onToggle(p.id) else onSelect(p.id) }
                }
            }
            Spacer(Modifier.height(8.dp))
            SvButton(stringResource(if (multi != null) R.string.done else R.string.close), onDismiss, Modifier.fillMaxWidth(), kind = if (multi != null) BtnKind.Primary else BtnKind.Secondary)
        }
    }
}

@Composable
private fun PickRow(text: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Body(text, Modifier.weight(1f), color = if (on) Cyan else Ink, size = 16.sp)
        if (on) Icon(Icons.Filled.Check, null, tint = Cyan)
    }
}

// ======================= comments =======================

@Composable
fun CommentCard(
    c: Comment,
    canDelete: Boolean,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenUser: (() -> Unit)? = null,
    onOpenGame: (() -> Unit)? = null,
) {
    var confirm by remember { mutableStateOf(false) }
    SvCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.userId, c.avatarVersion, c.name, 38.dp, Modifier.then(if (onOpenUser != null) Modifier.clickable(onClick = onOpenUser) else Modifier))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Body(c.name, size = 15.sp, weight = FontWeight.SemiBold, maxLines = 1, modifier = if (onOpenUser != null) Modifier.clickable(onClick = onOpenUser) else Modifier)
                Caption(relativeTime(c.createdAt), maxLines = 1)
            }
            c.rating?.let { StarBadge(it.toString(), color = Cyan) }
        }
        if (c.gameName != null && onOpenGame != null) {
            Spacer(Modifier.height(8.dp))
            Text(c.gameName, color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onOpenGame))
        }
        Spacer(Modifier.height(8.dp))
        Body(c.body, size = 15.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onLike).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (c.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = if (c.liked) Pink else Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(c.likes.toString(), color = if (c.liked) Pink else Muted, fontSize = 14.sp)
            }
            Spacer(Modifier.weight(1f))
            if (canDelete) {
                Icon(Icons.Filled.Delete, null, tint = Faint, modifier = Modifier.size(20.dp).clickable { confirm = true })
            }
        }
    }
    if (confirm) {
        ConfirmDialog(stringResource(R.string.comment_delete), null, onConfirm = { confirm = false; onDelete() }, onDismiss = { confirm = false })
    }
}

@Composable
fun ConfirmDialog(title: String, text: String?, onConfirm: () -> Unit, onDismiss: () -> Unit, confirmText: String? = null, danger: Boolean = true) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Panel,
        shape = CardShape,
        title = { Text(title, color = Ink, fontWeight = FontWeight.Bold) },
        text = if (text != null) ({ Text(text, color = Muted) }) else null,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText ?: stringResource(R.string.done), color = if (danger) Red else Cyan, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Muted) } },
    )
}

// ======================= activity heatmap =======================

/**
 * GitHub-style activity grid: columns are weeks (oldest on the left), rows are weekdays (Mon..Sun).
 * Month names are drawn above the first week of every month.
 */
@Composable
fun ActivityHeatmap(days: List<ActivityDay>, modifier: Modifier = Modifier, weeks: Int = 26) {
    val counts = remember(days) { days.associate { it.day to it.count } }
    val today = remember { LocalDate.now() }
    // Monday of the current week, then go back
    val lastMonday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val firstMonday = lastMonday.minusWeeks((weeks - 1).toLong())
    val max = (counts.values.maxOrNull() ?: 1).coerceAtLeast(1)
    val cell = 14.dp
    val gap = 4.dp
    val monthFmt = remember { DateTimeFormatter.ofPattern("LLL", Locale(Lang.code)) }
    val dayLabels = listOf(stringResource(R.string.weekday_mon), "", stringResource(R.string.weekday_wed), "", stringResource(R.string.weekday_fri), "", stringResource(R.string.weekday_sun))
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState(Int.MAX_VALUE)).padding(horizontal = 16.dp)) {
        Column(Modifier.padding(top = 18.dp, end = 6.dp), verticalArrangement = Arrangement.spacedBy(gap)) {
            dayLabels.forEach { Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) { Text(it, color = Muted, fontSize = 10.sp) } }
        }
        for (w in 0 until weeks) {
            val monday = firstMonday.plusWeeks(w.toLong())
            Column(Modifier.padding(end = gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                val newMonth = w == 0 || monday.month != monday.minusWeeks(1).month
                Box(Modifier.height(14.dp).width(cell), contentAlignment = Alignment.BottomStart) {
                    if (newMonth) Text(monday.format(monthFmt).trimEnd('.'), color = Muted, fontSize = 10.sp, maxLines = 1, softWrap = false)
                }
                for (d in 0 until 7) {
                    val date = monday.plusDays(d.toLong())
                    val n = counts[date.toString()] ?: 0
                    val color = when {
                        date.isAfter(today) -> Color.Transparent
                        n == 0 -> Panel2
                        else -> Green.copy(alpha = 0.3f + 0.7f * (n.toFloat() / max).coerceIn(0f, 1f))
                    }
                    Box(Modifier.size(cell).clip(RoundedCornerShape(4.dp)).background(color))
                }
            }
        }
    }
}

// ======================= misc helpers =======================

fun parseInstant(iso: String?): OffsetDateTime? = try {
    if (iso.isNullOrBlank()) null else OffsetDateTime.parse(iso)
} catch (e: Exception) {
    null
}

/** "5 мин назад", "вчера", "12 нояб." */
@Composable
fun relativeTime(iso: String?): String {
    val t = parseInstant(iso) ?: return formatDate(iso)
    val now = OffsetDateTime.now()
    val mins = ChronoUnit.MINUTES.between(t, now)
    return when {
        mins < 1 -> stringResource(R.string.time_now)
        mins < 60 -> stringResource(R.string.time_min_ago, mins.toInt())
        mins < 60 * 24 -> stringResource(R.string.time_h_ago, (mins / 60).toInt())
        mins < 60 * 48 -> stringResource(R.string.time_yesterday)
        else -> t.format(DateTimeFormatter.ofPattern("d MMM", Locale(Lang.code)))
    }
}

fun releaseDateOf(g: Game): LocalDate? = try {
    g.releaseDate?.take(10)?.let { LocalDate.parse(it) }
} catch (e: Exception) {
    null
}

fun hours(seconds: Long): String = compact(seconds / 3600)

fun openInBrowser(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        // no browser: nothing to do
    }
}

/** Gradient tile for genres on the home screen. */
@Composable
fun GenreTile(title: String, emoji: String, colors: List<Color>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width = 150.dp, height = 150.dp)
            .clip(CardShape)
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        Text(emoji, fontSize = 54.sp, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

/** Little circle with an icon, used in lists. */
@Composable
fun IconDot(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(36.dp).background(color.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
    }
}
