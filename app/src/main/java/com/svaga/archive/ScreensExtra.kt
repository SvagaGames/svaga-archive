package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    Column {
        PixelButton("< " + stringResource(R.string.back), onBack)
        Text(title, color = Cyan, fontFamily = Pixel, fontSize = 14.sp, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
    }
}

// ======================= achievements =======================

@Composable
fun AchievementsScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadAchievements() }
    var category by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableIntStateOf(0) } // 0 all, 1 received, 2 unfinished, 3 secret
    var sort by rememberSaveable { mutableIntStateOf(0) } // 0 default, 1 rarity, 2 progress
    val ach = vm.ach

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(12.dp)) {
        ScreenHeader(stringResource(R.string.ach_title), onBack)
        if (ach == null) {
            Text(stringResource(R.string.loading), color = Muted, fontFamily = Vt, fontSize = 19.sp)
            return@Column
        }
        LevelCard(ach.level, ach.sp, ach.levelFrom, ach.levelTo)
        val visible = ach.items.filter { !it.hidden || it.secret }
        val got = ach.items.count { it.tier > 0 }
        Text(
            stringResource(R.string.ach_counts, got, ach.items.size),
            color = Muted, fontFamily = Vt, fontSize = 16.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(stringResource(R.string.ach_featured_hint), color = Muted, fontFamily = Vt, fontSize = 15.sp)

        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(R.string.ach_f_all, R.string.ach_f_received, R.string.ach_f_unfinished, R.string.ach_f_secret).forEachIndexed { i, r ->
                PixelChip(stringResource(r), kind == i, Cyan, onClick = { kind = i })
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PixelChip(stringResource(R.string.ach_cat_all), category == "", Amber, onClick = { category = "" })
            ACH_CATEGORIES.forEach { c ->
                PixelChip(if (c == "secret") stringResource(R.string.ach_f_secret) else achTitle(c), category == c, Amber, onClick = { category = c })
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(R.string.ach_sort_default, R.string.ach_sort_rarity, R.string.ach_sort_progress).forEachIndexed { i, r ->
                PixelChip(stringResource(r), sort == i, Pink, onClick = { sort = i })
            }
        }
        Spacer(Modifier.height(12.dp))

        var list = visible.filter { category == "" || it.category == category }
        list = when (kind) {
            1 -> list.filter { it.tier > 0 }
            2 -> list.filter { !it.maxed && !it.secret }
            3 -> list.filter { it.secret }
            else -> list
        }
        list = when (sort) {
            1 -> list.sortedBy { if (it.rarity <= 0.0) 101.0 else it.rarity }
            2 -> list.sortedByDescending { if (it.maxed) 1.0 else it.progress.toDouble() / it.goal.coerceAtLeast(1) }
            else -> list
        }
        if (list.isEmpty()) {
            Text(stringResource(R.string.ach_none), color = Muted, fontFamily = Vt, fontSize = 17.sp)
        }
        list.forEach { AchCard(it, onToggleFeatured = { vm.toggleFeatured(it.code) }) }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun AchCard(a: AchItem, onToggleFeatured: () -> Unit) {
    val color = if (a.tier > 0) tierColor(a.tier) else Muted
    val hiddenSecret = a.secret && a.hidden
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(Panel)
            .border(2.dp, if (a.tier > 0) color else Line)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (a.tier > 0) "★" else "☆",
                color = color,
                fontSize = 26.sp,
                modifier = Modifier.clickable(enabled = a.tier > 0, onClick = onToggleFeatured).padding(end = 10.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (hiddenSecret) stringResource(R.string.ach_secret_title) else achTitle(a.code),
                    color = if (a.tier > 0) Ink else Muted,
                    fontFamily = Vt,
                    fontSize = 20.sp,
                )
                if (a.tier > 0) {
                    Text(
                        (if (a.secret) stringResource(R.string.ach_f_secret) else tierName(a.tier) + " " + ROMAN[a.tier]),
                        color = color, fontFamily = Pixel, fontSize = 8.sp,
                    )
                }
            }
            if (a.featured) Text(stringResource(R.string.ach_on_profile), color = Pink, fontFamily = Vt, fontSize = 14.sp)
        }
        if (!hiddenSecret) {
            val descTier = if (a.maxed) a.tiers.size.coerceAtLeast(1) else (a.tier + 1)
            Text(achDescription(a.code, descTier, a.goal), color = Muted, fontFamily = Vt, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
            if (!a.secret) {
                Spacer(Modifier.height(6.dp))
                ProgressBar(if (a.maxed) 1f else a.progress.toFloat() / a.goal.coerceAtLeast(1), color)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${a.progress}/${a.goal}", color = Muted, fontFamily = Vt, fontSize = 15.sp)
                    val nextSp = a.tiers.getOrNull(a.tier)?.sp
                    if (!a.maxed && nextSp != null) Text("+$nextSp SP", color = Green, fontFamily = Vt, fontSize = 15.sp)
                }
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (t in 1..5) {
                        Box(Modifier.size(width = 22.dp, height = 8.dp).background(if (t <= a.tier) tierColor(t) else Panel2).border(1.dp, Line))
                    }
                }
            }
            if (a.tier > 0) {
                Text(
                    stringResource(R.string.ach_earned, a.earnedSp) + if (a.unlockedAt != null) " · " + formatDate(a.unlockedAt) else "",
                    color = Green, fontFamily = Vt, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (a.rarity > 0) {
                Text(stringResource(R.string.ach_rarity, a.rarity.toString()), color = Muted, fontFamily = Vt, fontSize = 15.sp)
            }
        } else {
            Text(stringResource(R.string.ach_secret_hint), color = Muted, fontFamily = Vt, fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

// ======================= notifications =======================

@Composable
fun notificationText(n: NotificationItem): String {
    val name = n.payload.optString("name")
    return when (n.kind) {
        "achievement" -> stringResource(
            R.string.n_achievement,
            achTitle(n.payload.optString("code")) + " " + ROMAN.getOrElse(n.payload.optInt("tier", 1)) { "" },
            n.payload.optInt("sp", 0),
        )
        "level_up" -> stringResource(R.string.n_level_up, levelName(n.payload.optInt("level", 1)))
        "friend_request" -> stringResource(R.string.n_friend_request, name)
        "friend_accepted" -> stringResource(R.string.n_friend_accepted, name)
        "friend_declined" -> stringResource(R.string.n_friend_declined, name)
        "comment_like" -> stringResource(R.string.n_comment_like, name)
        else -> n.kind
    }
}

@Composable
fun NotificationsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenGame: (Int) -> Unit,
    onOpenAchievements: () -> Unit,
) {
    LaunchedEffect(Unit) { vm.loadNotifications() }
    DisposableEffect(Unit) { onDispose { vm.markAllRead() } }
    Column(Modifier.fillMaxSize().background(Bg).padding(12.dp)) {
        ScreenHeader(stringResource(R.string.notifications_title), onBack)
        if (vm.notifications.isEmpty()) {
            CenterText(stringResource(R.string.notifications_empty))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(vm.notifications, key = { it.id }) { n ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Panel)
                            .border(2.dp, if (n.read) Line else Amber)
                            .clickable {
                                when (n.kind) {
                                    "achievement", "level_up" -> onOpenAchievements()
                                    "friend_request", "friend_accepted", "friend_declined" -> {
                                        val uid = n.payload.optString("userId")
                                        if (uid.isNotEmpty()) onOpenUser(uid)
                                    }
                                    "comment_like" -> {
                                        val gid = n.payload.optInt("gameId", 0)
                                        if (gid > 0) onOpenGame(gid)
                                    }
                                }
                            }
                            .padding(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(notificationText(n), color = Ink, fontFamily = Vt, fontSize = 18.sp)
                            Text(formatDate(n.createdAt), color = Muted, fontFamily = Vt, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ======================= statistics =======================

@Composable
fun StatsScreen(vm: AppViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { vm.loadMyStats(vm.statsPeriod) }
    val s = vm.stats
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(12.dp)) {
        ScreenHeader(stringResource(R.string.stats_title), onBack)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "day" to R.string.stats_day, "week" to R.string.stats_week,
                "month" to R.string.stats_month, "year" to R.string.stats_year,
            ).forEach { (p, r) ->
                PixelChip(stringResource(r), vm.statsPeriod == p, Cyan, onClick = { vm.loadMyStats(p) })
            }
        }
        Spacer(Modifier.height(12.dp))
        if (s == null || s.period != vm.statsPeriod) {
            Text(stringResource(R.string.loading), color = Muted, fontFamily = Vt, fontSize = 19.sp)
            return@Column
        }
        val rows = listOf(
            R.string.stat_added to "added", R.string.stat_completed to "completed", R.string.stat_rated to "rated",
            R.string.stat_comments to "comments", R.string.stat_likes to "likes", R.string.stat_friends to "friends",
            R.string.stat_sp to "sp", R.string.stat_active_days to "activeDays",
        )
        rows.forEach { (label, key) ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp).background(Panel).border(2.dp, Line).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(label), color = Ink, fontFamily = Vt, fontSize = 18.sp)
                Text((s.window[key] ?: 0).toString(), color = Amber, fontFamily = Vt, fontSize = 20.sp)
            }
        }

        if (s.series.isNotEmpty()) {
            SectionTitle(stringResource(R.string.stats_activity))
            val max = s.series.maxOf { it.second }.coerceAtLeast(1)
            Row(
                Modifier.horizontalScroll(rememberScrollState()).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                s.series.forEach { (at, count) ->
                    val label = when (s.period) {
                        "day" -> at.drop(11).take(2)
                        "year" -> at.drop(5).take(2)
                        else -> at.drop(8).take(2)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(count.toString(), color = Muted, fontFamily = Vt, fontSize = 13.sp)
                        Box(Modifier.width(22.dp).height((8 + 72 * count / max).dp).background(Cyan))
                        Text(label, color = Muted, fontFamily = Vt, fontSize = 13.sp)
                    }
                }
            }
        }

        SectionTitle(stringResource(R.string.stats_total))
        listOf(
            R.string.stat_in_library to "library", R.string.stat_completed to "completed", R.string.stat_rated to "rated",
            R.string.stat_comments to "comments", R.string.stat_friends to "friends", R.string.stat_streak to "streak",
        ).forEach { (label, key) ->
            MetaLine(stringResource(label), (s.total[key] ?: 0).toString())
        }
        s.avgRating?.let { MetaLine(stringResource(R.string.stat_avg_rating), it.toString()) }
        MetaLine(stringResource(R.string.stat_level), levelNameText(s.level) + " · " + s.sp + " SP")

        if (s.byStatus.isNotEmpty()) {
            SectionTitle(stringResource(R.string.stats_by_status))
            STATUSES.forEach { st ->
                val n = s.byStatus[st] ?: 0
                if (n > 0) MetaLine(statusLabel(st), n.toString())
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun levelNameText(level: Int): String = levelName(level)

// ======================= platforms =======================

@Composable
fun PlatformsScreen(vm: AppViewModel, onBack: () -> Unit, onPick: (Int) -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableIntStateOf(0) } // 0 name, 1 year, 2 generation, 3 games
    val needle = q.trim()
    val base = vm.platforms.filter {
        needle.isEmpty() || it.name.contains(needle, ignoreCase = true) || it.short.contains(needle, ignoreCase = true)
    }
    val list = when (sort) {
        1 -> base.sortedWith(compareBy({ it.year ?: 9999 }, { it.name }))
        2 -> base.sortedWith(compareBy({ it.generation ?: 99 }, { it.year ?: 9999 }, { it.name }))
        3 -> base.sortedWith(compareByDescending<Platform> { it.games ?: -1 }.thenBy { it.name })
        else -> base.sortedBy { it.name }
    }
    Column(Modifier.fillMaxSize().background(Bg).padding(12.dp)) {
        ScreenHeader(stringResource(R.string.platforms_screen), onBack)
        PixelField(q, { q = it }, stringResource(R.string.platform_search))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(R.string.sort_name, R.string.sort_year, R.string.sort_generation, R.string.sort_games).forEachIndexed { i, r ->
                PixelChip(stringResource(r), sort == i, Pink, onClick = { sort = i })
            }
        }
        Spacer(Modifier.height(8.dp))
        if (list.isEmpty()) {
            CenterText(stringResource(R.string.loading))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                items(list, key = { it.id }) { p ->
                    Row(
                        Modifier.fillMaxWidth().background(Panel).border(2.dp, Line).clickable { onPick(p.id) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(64.dp).background(Panel2), contentAlignment = Alignment.Center) {
                            if (p.logo != null) {
                                AsyncImage(model = p.logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(4.dp))
                            } else {
                                Text(p.short.take(3), color = Muted, fontFamily = Pixel, fontSize = 9.sp)
                            }
                        }
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(p.name, color = Ink, fontFamily = Vt, fontSize = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            val parts = listOfNotNull(
                                p.year?.let { stringResource(R.string.platform_year, it) },
                                p.generation?.let { stringResource(R.string.platform_generation, it) },
                                p.games?.let { stringResource(R.string.platform_games, it) },
                            )
                            if (parts.isNotEmpty()) Text(parts.joinToString(" · "), color = Muted, fontFamily = Vt, fontSize = 15.sp)
                            p.family?.let { Text(it, color = Muted, fontFamily = Vt, fontSize = 14.sp) }
                        }
                    }
                }
            }
        }
    }
}

// ======================= add a game =======================

@Composable
fun PlatformMultiPicker(
    platforms: List<Platform>,
    selected: List<Int>,
    onChange: (List<Int>) -> Unit,
    onDismiss: () -> Unit,
) {
    var q by rememberSaveable { mutableStateOf("") }
    val needle = q.trim()
    val shown = platforms.filter {
        needle.isEmpty() || it.name.contains(needle, ignoreCase = true) || it.short.contains(needle, ignoreCase = true)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth().fillMaxHeight(0.85f).background(Bg).border(2.dp, Cyan).padding(12.dp),
        ) {
            Text(stringResource(R.string.add_game_platforms), color = Cyan, fontFamily = Pixel, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            PixelField(q, { q = it }, stringResource(R.string.platform_search))
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.id }) { p ->
                    val on = p.id in selected
                    Text(
                        (if (on) "[x] " else "[ ] ") + p.name,
                        color = if (on) Cyan else Ink,
                        fontFamily = Vt,
                        fontSize = 19.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChange(if (on) selected - p.id else selected + p.id) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            PixelButton(stringResource(R.string.done), onDismiss, filled = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AddGameScreen(vm: AppViewModel, onBack: () -> Unit, onOpenGame: (Int) -> Unit, onCreated: (Int) -> Unit) {
    LaunchedEffect(Unit) { vm.clearAddState() }
    var name by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var devs by rememberSaveable { mutableStateOf("") }
    var genres by rememberSaveable { mutableStateOf("") }
    var summary by rememberSaveable { mutableStateOf("") }
    var cover by rememberSaveable { mutableStateOf("") }
    var chosen by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var picking by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(12.dp)) {
        ScreenHeader(stringResource(R.string.add_game_title), onBack)
        Text(stringResource(R.string.add_game_hint), color = Muted, fontFamily = Vt, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        PixelField(name, { name = it.take(120) }, stringResource(R.string.add_game_name))
        Spacer(Modifier.height(8.dp))
        PixelField(year, { year = it.filter { c -> c.isDigit() }.take(4) }, stringResource(R.string.add_game_year), keyboard = KeyboardType.Number)
        Spacer(Modifier.height(8.dp))
        PixelField(devs, { devs = it }, stringResource(R.string.add_game_devs))
        Spacer(Modifier.height(8.dp))
        PixelField(genres, { genres = it }, stringResource(R.string.add_game_genres))
        Spacer(Modifier.height(8.dp))
        PixelField(summary, { summary = it.take(4000) }, stringResource(R.string.add_game_summary), singleLine = false)
        Spacer(Modifier.height(8.dp))
        PixelField(cover, { cover = it.trim() }, stringResource(R.string.add_game_cover))
        Spacer(Modifier.height(10.dp))
        val names = vm.platforms.filter { it.id in chosen }.joinToString(", ") { it.short }
        PixelButton(
            stringResource(R.string.add_game_platforms) + if (names.isNotBlank()) ": $names" else "",
            { picking = true },
            accent = Blue,
            modifier = Modifier.fillMaxWidth(),
        )
        if (picking) {
            PlatformMultiPicker(vm.platforms, chosen, { chosen = it }, { picking = false })
        }
        vm.addError?.let { res ->
            Spacer(Modifier.height(10.dp))
            Text(stringResource(res), color = Red, fontFamily = Vt, fontSize = 17.sp)
        }
        if (vm.addDuplicates.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.add_game_duplicates), color = Amber, fontFamily = Vt, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            vm.addDuplicates.forEach { g ->
                Box(Modifier.padding(bottom = 8.dp)) { GameListRow(g, vm.library[g.id]?.status) { onOpenGame(g.id) } }
            }
        }
        Spacer(Modifier.height(14.dp))
        PixelButton(
            stringResource(R.string.add_game_submit),
            {
                vm.createGame(
                    NewGame(
                        name = name.trim(),
                        year = year.toIntOrNull(),
                        summary = summary.trim(),
                        coverUrl = cover,
                        developers = devs.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        genres = genres.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        platformIds = chosen,
                    ),
                    onCreated,
                )
            },
            filled = true,
            enabled = !vm.addBusy && name.trim().length >= 2 && chosen.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

// ======================= Creator panel =======================

@Composable
fun AdminScreen(vm: AppViewModel, onBack: () -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    var banTarget by remember { mutableStateOf<AdminUser?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminUser?>(null) }
    var reason by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.adminSearch("") }

    Column(Modifier.fillMaxSize().background(Bg).padding(12.dp)) {
        ScreenHeader(stringResource(R.string.admin_title), onBack)
        PixelField(q, { q = it; vm.adminSearch(it) }, stringResource(R.string.admin_search))
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
            items(vm.adminUsers, key = { it.id }) { u ->
                Column(Modifier.fillMaxWidth().background(Panel).border(2.dp, if (u.banned) Red else Line).padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.id, u.avatarVersion, u.name, 40.dp)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(u.name + if (u.role == "creator") "  *" else "", color = Ink, fontFamily = Vt, fontSize = 19.sp)
                            Text(u.email, color = Muted, fontFamily = Vt, fontSize = 15.sp)
                            if (u.banned) Text(stringResource(R.string.admin_banned) + (u.banReason?.let { ": $it" } ?: ""), color = Red, fontFamily = Vt, fontSize = 15.sp)
                        }
                    }
                    if (u.role != "creator") {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (u.banned) {
                                PixelChip(stringResource(R.string.admin_unban), false, Green, onClick = { vm.adminUnban(u.id, q.trim()) })
                            } else {
                                PixelChip(stringResource(R.string.admin_ban), false, Red, onClick = { reason = ""; banTarget = u })
                            }
                            PixelChip(stringResource(R.string.admin_delete), false, Red, onClick = { deleteTarget = u })
                            PixelChip(stringResource(R.string.admin_reset_code), false, Amber, onClick = { vm.adminResetCode(u) })
                            PixelChip(stringResource(R.string.admin_grant), false, Pink, onClick = { vm.adminGrant(u.id) })
                        }
                    }
                }
            }
        }
    }

    banTarget?.let { u ->
        SimpleDialog(
            title = stringResource(R.string.admin_ban) + ": " + u.name,
            confirm = stringResource(R.string.admin_ban),
            onConfirm = {
                vm.adminBan(u.id, reason, q.trim())
                banTarget = null
            },
            onDismiss = { banTarget = null },
        ) {
            PixelField(reason, { reason = it.take(300) }, stringResource(R.string.admin_ban_reason))
        }
    }
    deleteTarget?.let { u ->
        SimpleDialog(
            title = stringResource(R.string.admin_delete_confirm, u.name),
            confirm = stringResource(R.string.admin_delete),
            onConfirm = {
                vm.adminDelete(u.id, q.trim())
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        ) {}
    }
    vm.adminCode?.let { (mail, code) ->
        SimpleDialog(
            title = stringResource(R.string.admin_code_title, mail),
            confirm = stringResource(R.string.close),
            onConfirm = { vm.closeAdminCode() },
            onDismiss = { vm.closeAdminCode() },
        ) {
            Text(code, color = Cyan, fontFamily = Pixel, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Text(stringResource(R.string.admin_code_hint), color = Muted, fontFamily = Vt, fontSize = 15.sp)
        }
    }
}

@Composable
fun SimpleDialog(
    title: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().background(Bg).border(2.dp, Cyan).padding(14.dp)) {
            Text(title, color = Ink, fontFamily = Vt, fontSize = 19.sp)
            Spacer(Modifier.height(10.dp))
            content()
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelButton(confirm, onConfirm, filled = true)
                PixelButton(stringResource(R.string.cancel), onDismiss, accent = Red)
            }
        }
    }
}

// ======================= Svaga ID helpers =======================

fun openInBrowser(context: android.content.Context, url: String) {
    try {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: Exception) {
        // no browser installed: nothing sensible to do
    }
}

fun providerTitle(provider: String): String = when (provider) {
    "google" -> "Google"
    "yandex" -> "Яндекс ID"
    "vk" -> "VK ID"
    else -> provider
}

@Composable
fun SocialLoginButtons(vm: AppViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { vm.loadProviders() }
    if (vm.providers.isEmpty()) return
    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.or_sign_in_with), color = Muted, fontFamily = Vt, fontSize = 17.sp)
    Spacer(Modifier.height(8.dp))
    vm.providers.forEach { p ->
        PixelButton(
            providerTitle(p),
            { vm.startOauth(ctx, p) },
            accent = Blue,
            enabled = !vm.authBusy,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun SvagaIdSection(vm: AppViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { vm.loadIdentities() }
    val ids = vm.identities ?: return
    if (ids.available.isEmpty() && ids.items.isEmpty()) return
    SectionTitle(stringResource(R.string.svaga_id_title))
    Text(stringResource(R.string.svaga_id_hint), color = Muted, fontFamily = Vt, fontSize = 15.sp)
    Spacer(Modifier.height(8.dp))
    val all = (ids.available + ids.items.map { it.provider }).distinct()
    all.forEach { p ->
        val linked = ids.items.firstOrNull { it.provider == p }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp).background(Panel).border(2.dp, if (linked != null) Green else Line).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(providerTitle(p), color = Ink, fontFamily = Vt, fontSize = 19.sp)
                Text(
                    linked?.let { it.name ?: it.email ?: stringResource(R.string.svaga_id_linked) } ?: stringResource(R.string.svaga_id_not_linked),
                    color = Muted, fontFamily = Vt, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (linked != null) {
                PixelChip(stringResource(R.string.svaga_id_unlink), false, Red, onClick = { vm.unlinkIdentity(p) })
            } else {
                PixelChip(stringResource(R.string.svaga_id_link), false, Cyan, onClick = { vm.linkIdentity(ctx, p) })
            }
        }
    }
}
