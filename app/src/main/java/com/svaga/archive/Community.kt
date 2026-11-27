package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

private fun daysLeft(iso: String?): Long? = runCatching {
    ChronoUnit.DAYS.between(LocalDate.now(), OffsetDateTime.parse(iso).toLocalDate()).coerceAtLeast(0)
}.getOrNull()

// =====================================================================
// Seasonal challenge card (home screen)
// =====================================================================

@Composable
fun SeasonCards(vm: AppViewModel, nav: Nav) {
    val list = vm.extras.seasons
    if (list.isEmpty()) return
    SectionHeader(stringResource(R.string.season_title), Modifier.padding(top = 8.dp), onMore = { nav.go(Routes.ACHIEVEMENTS) })
    list.forEach { c ->
        val a = Color(c.colors.getOrElse(0) { 0xFF3EE0FF })
        val b = Color(c.colors.getOrElse(1) { 0xFF4F7CFF })
        val shape = RoundedCornerShape(22.dp)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clip(shape)
                .background(Brush.linearGradient(listOf(a.copy(alpha = 0.35f), b.copy(alpha = 0.2f), Panel)))
                .border(1.dp, a.copy(alpha = 0.6f), shape).clickable { nav.go(Routes.ACHIEVEMENTS) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(c.emoji, fontSize = 38.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Body(c.title, weight = FontWeight.Bold, maxLines = 1)
                c.description?.let { Caption(runCatching { String.format(it, c.goal) }.getOrDefault(it), maxLines = 2) }
                Spacer(Modifier.height(8.dp))
                ProgressLine(c.progress.toFloat() / c.goal.coerceAtLeast(1), if (c.progress >= c.goal) Green else a)
                Spacer(Modifier.height(4.dp))
                Row {
                    Caption("${c.progress} / ${c.goal}", Modifier.weight(1f), color = if (c.progress >= c.goal) Green else Muted)
                    daysLeft(c.endsAt)?.let { Caption(stringResource(R.string.season_days_left, it), color = Muted) }
                }
            }
            Spacer(Modifier.width(10.dp))
            Pill("+${c.sp} SP", Amber, filled = c.progress >= c.goal)
        }
    }
}

// =====================================================================
// Leaderboards
// =====================================================================

private val BOARD_METRICS = listOf("sp" to R.string.board_sp, "completed" to R.string.board_completed, "hours" to R.string.board_hours, "achievements" to R.string.board_achievements)

@Composable
fun LeaderboardScreen(vm: AppViewModel, nav: Nav) {
    val ex = vm.extras
    var metric by remember { mutableIntStateOf(0) }
    var friends by remember { mutableStateOf(false) }
    LaunchedEffect(metric, friends) { ex.loadBoard(BOARD_METRICS[metric].first, friends) }
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(stringResource(R.string.board_title), nav::back, subtitle = stringResource(R.string.board_subtitle))
        Segmented(listOf(stringResource(R.string.board_all), stringResource(R.string.people_friends)), if (friends) 1 else 0, { friends = it == 1 }, Modifier.padding(horizontal = 16.dp))
        ChipRow(Modifier.padding(vertical = 8.dp)) {
            BOARD_METRICS.forEachIndexed { i, (_, label) -> SvChip(stringResource(label), metric == i, { metric = i }) }
        }
        val b = ex.board
        when {
            b == null && ex.boardError != null -> ErrorState(stringResource(ex.boardError!!), { ex.loadBoard(BOARD_METRICS[metric].first, friends) })
            b == null -> LoadingBox()
            b.items.isEmpty() -> EmptyState(stringResource(R.string.board_empty))
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(b.items, key = { it.userId }) { r -> BoardRow(r, BOARD_METRICS[metric].first) { nav.user(r.userId) } }
                val me = b.me
                if (me != null && b.items.none { it.me }) {
                    item { Divider1(Modifier.padding(vertical = 6.dp)) }
                    item { BoardRow(me, BOARD_METRICS[metric].first) {} }
                }
            }
        }
    }
}

@Composable
private fun BoardRow(r: LeaderRow, metric: String, onClick: () -> Unit) {
    val medal = when (r.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> null }
    Row(
        Modifier.fillMaxWidth().background(if (r.me) Cyan.copy(alpha = 0.08f) else Color.Transparent).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
            if (medal != null) Text(medal, fontSize = 24.sp) else Text("#${r.rank}", color = Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Avatar(r.userId, r.avatarVersion, r.name, 42.dp, ring = r.rank <= 3)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Body(r.name, weight = FontWeight.SemiBold, maxLines = 1, color = if (r.me) Cyan else Ink)
            Caption(stringResource(R.string.level_n, r.level) + " · " + levelName(r.level), maxLines = 1)
        }
        Text(
            compact(r.value) + when (metric) { "sp" -> " SP"; "hours" -> " ч"; else -> "" },
            color = if (r.rank <= 3) Amber else Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold,
        )
    }
}

// =====================================================================
// Creator: themed and seasonal achievements
// =====================================================================

@Composable
fun ChallengesAdminScreen(vm: AppViewModel, nav: Nav) {
    val ex = vm.extras
    LaunchedEffect(Unit) { ex.loadChallenges() }
    var editing by remember { mutableStateOf<AdminChallenge?>(null) }
    var creating by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(stringResource(R.string.ch_admin_title), nav::back, subtitle = stringResource(R.string.ch_admin_subtitle)) {
            IconBtn(Icons.Filled.Add, { creating = true }, tint = Amber)
        }
        ex.challengeError?.let { Body(stringResource(it), Modifier.padding(16.dp), color = Red) }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Caption(stringResource(R.string.ch_admin_hint), Modifier.padding(horizontal = 16.dp, vertical = 4.dp), maxLines = 6) }
            items(ex.challenges, key = { it.id }) { c ->
                Row(
                    Modifier.fillMaxWidth().clickable { editing = c }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(c.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Body(c.title, weight = FontWeight.SemiBold, maxLines = 1, color = if (c.active) Ink else Faint)
                        Caption(
                            (if (c.kind == "season") stringResource(R.string.ach_cat_season) else stringResource(R.string.ach_cat_theme)) +
                                " · " + (c.pattern ?: stringResource(R.string.ch_games_n, c.gameIds.size)) + " · " + c.goals.joinToString("/"),
                            maxLines = 1,
                        )
                    }
                    if (!c.active) Pill(stringResource(R.string.ch_off), Faint)
                }
            }
        }
    }
    val target = editing
    if (creating || target != null) ChallengeEditor(vm, target) { creating = false; editing = null }
}

@Composable
private fun ChallengeEditor(vm: AppViewModel, c: AdminChallenge?, onClose: () -> Unit) {
    val ex = vm.extras
    var season by remember { mutableStateOf(c?.kind == "season") }
    var title by remember { mutableStateOf(c?.title ?: "") }
    var desc by remember { mutableStateOf(c?.description ?: "") }
    var emoji by remember { mutableStateOf(c?.emoji ?: "🎮") }
    var pattern by remember { mutableStateOf(c?.pattern ?: "") }
    var games by remember { mutableStateOf(c?.gameIds ?: emptyList()) }
    var goals by remember { mutableStateOf(c?.goals?.joinToString(" ") ?: "1 3 5 10 20") }
    var sp by remember { mutableStateOf((c?.sp ?: 100).toString()) }
    var days by remember { mutableStateOf(c?.endsAt?.let { daysLeft(it)?.toString() } ?: "30") }
    var metric by remember { mutableIntStateOf(listOf("completed", "library", "rated").indexOf(c?.metric ?: "completed").coerceAtLeast(0)) }
    var active by remember { mutableStateOf(c?.active ?: true) }
    var query by remember { mutableStateOf("") }
    val found = vm.searchResults.orEmpty()
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.fillMaxWidth().padding(12.dp).clip(CardShape).background(Panel).verticalScroll(rememberScrollState()).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                H2(stringResource(if (c == null) R.string.ch_new else R.string.ch_edit), Modifier.weight(1f))
                IconBtn(Icons.Filled.Close, onClose)
            }
            Spacer(Modifier.height(10.dp))
            Segmented(listOf(stringResource(R.string.ach_cat_theme), stringResource(R.string.ach_cat_season)), if (season) 1 else 0, { season = it == 1; if (season && goals.contains(' ')) goals = "3" })
            Spacer(Modifier.height(10.dp))
            Row {
                SvField(emoji, { emoji = it.take(4) }, stringResource(R.string.ch_emoji), Modifier.width(90.dp))
                Spacer(Modifier.width(10.dp))
                SvField(title, { title = it.take(60) }, stringResource(R.string.ch_name), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            SvField(desc, { desc = it.take(200) }, stringResource(R.string.ch_desc))
            Spacer(Modifier.height(8.dp))
            Caption(stringResource(R.string.ch_metric))
            Segmented(listOf(stringResource(R.string.ch_m_completed), stringResource(R.string.ch_m_library), stringResource(R.string.ch_m_rated)), metric, { metric = it })
            Spacer(Modifier.height(8.dp))
            SvField(pattern, { pattern = it.take(300) }, stringResource(R.string.ch_pattern))
            Caption(stringResource(R.string.ch_pattern_hint), color = Faint, maxLines = 4)
            Spacer(Modifier.height(8.dp))
            Body(stringResource(R.string.ch_games_n, games.size), size = 14.sp, weight = FontWeight.SemiBold)
            games.forEach { id ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Caption(vm.gamesCache[id]?.name ?: "#$id", Modifier.weight(1f), maxLines = 1)
                    Text("✕", color = Red, modifier = Modifier.clickable { games = games - id }.padding(8.dp))
                }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                SvField(query, { query = it }, stringResource(R.string.ch_add_game), Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                SvButton("🔍", { if (query.isNotBlank()) vm.search(query.trim()) }, kind = BtnKind.Secondary)
            }
            found.take(6).forEach { g ->
                Row(
                    Modifier.fillMaxWidth().clickable { if (g.id !in games) games = games + g.id; vm.gamesCache[g.id] = g }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(g.cover, Modifier.width(32.dp).aspectRatio(0.75f), RoundedCornerShape(6.dp))
                    Spacer(Modifier.width(8.dp))
                    Caption(g.name + (g.year?.let { " ($it)" } ?: ""), Modifier.weight(1f), color = if (g.id in games) Green else Ink, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                SvField(goals, { goals = it.filter { ch -> ch.isDigit() || ch == ' ' }.take(30) }, stringResource(if (season) R.string.ch_goal else R.string.ch_goals), Modifier.weight(1f), keyboard = KeyboardType.Number)
                Spacer(Modifier.width(8.dp))
                if (season) {
                    SvField(sp, { sp = it.filter(Char::isDigit).take(4) }, "SP", Modifier.width(80.dp), keyboard = KeyboardType.Number)
                    Spacer(Modifier.width(8.dp))
                    SvField(days, { days = it.filter(Char::isDigit).take(3) }, stringResource(R.string.ch_days), Modifier.width(90.dp), keyboard = KeyboardType.Number)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body(stringResource(R.string.ch_active), Modifier.weight(1f), size = 14.sp)
                androidx.compose.material3.Switch(active, { active = it })
            }
            ex.challengeError?.let { Body(stringResource(it), color = Red, size = 14.sp) }
            Spacer(Modifier.height(12.dp))
            SvButton(stringResource(R.string.save), {
                val body = JSONObject()
                    .put("kind", if (season) "season" else "theme")
                    .put("title", title).put("description", desc).put("emoji", emoji)
                    .put("metric", listOf("completed", "library", "rated")[metric])
                    .put("pattern", pattern)
                    .put("gameIds", JSONArray(games))
                    .put("goals", JSONArray(goals.split(' ').mapNotNull { it.toIntOrNull() }))
                    .put("sp", sp.toIntOrNull() ?: 100)
                    .put("active", active)
                if (season) {
                    val start = c?.let { null } ?: OffsetDateTime.now()
                    if (start != null) body.put("startsAt", start.toString())
                    body.put("endsAt", OffsetDateTime.now().plusDays((days.toLongOrNull() ?: 30).coerceIn(1, 366)).toString())
                }
                ex.saveChallenge(c?.id, body, onClose)
            }, Modifier.fillMaxWidth(), busy = ex.challengeBusy, accent = Amber, enabled = title.isNotBlank() && (pattern.isNotBlank() || games.isNotEmpty()))
            if (c != null && c.active) {
                Spacer(Modifier.height(8.dp))
                SvButton(stringResource(R.string.ch_disable), { ex.deleteChallenge(c.id); onClose() }, Modifier.fillMaxWidth(), kind = BtnKind.Danger)
            }
        }
    }
}
