package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage

@Composable
private fun ScreenScaffold(title: String, nav: Nav, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(title, nav::back, subtitle = subtitle)
        Column(Modifier.weight(1f).fillMaxWidth(), content = content)
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp, modifier = Modifier.padding(start = 20.dp, top = 22.dp, bottom = 8.dp))
}

// ======================= settings =======================

@Composable
fun SettingsScreen(vm: AppViewModel, nav: Nav) {
    val ctx = LocalContext.current
    var langDialog by remember { mutableStateOf(false) }
    var nameDialog by remember { mutableStateOf(false) }
    var logoutDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.uploadAvatar(uri) }
    ScreenScaffold(stringResource(R.string.settings_title), nav, subtitle = stringResource(R.string.settings_subtitle)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Label(stringResource(R.string.profile_account))
            SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                SettingRow(Icons.Filled.Person, stringResource(R.string.profile_nick), subtitle = vm.displayName.ifBlank { vm.email }, onClick = { nameDialog = true })
                Divider1()
                SettingRow(Icons.Filled.AccountCircle, stringResource(R.string.avatar_change), subtitle = vm.email, onClick = { picker.launch("image/*") })
                if (vm.avatarVersion > 0) {
                    Divider1()
                    SettingRow(Icons.Filled.Delete, stringResource(R.string.avatar_remove), tint = Red, onClick = { vm.removeAvatar() })
                }
                Divider1()
                SettingRow(
                    Icons.Filled.Lock,
                    stringResource(R.string.privacy_title),
                    subtitle = stringResource(if (vm.libraryPublic) R.string.library_open_hint else R.string.library_closed_hint),
                    trailing = {
                        Switch(
                            checked = vm.libraryPublic,
                            onCheckedChange = { vm.updateLibraryPublic(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = Cyan, checkedThumbColor = Color.White),
                        )
                    },
                )
            }
            SvagaIdSection(vm)

            Label(stringResource(R.string.appearance))
            SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                Column(Modifier.padding(16.dp)) {
                    Body(stringResource(R.string.theme), weight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    val modes = listOf("system" to R.string.theme_system, "dark" to R.string.theme_dark, "light" to R.string.theme_light)
                    Segmented(modes.map { stringResource(it.second) }, modes.indexOfFirst { it.first == ThemeMode.code }.coerceAtLeast(0), { ThemeMode.save(ctx, modes[it].first) })
                }
                Divider1()
                SettingRow(
                    Icons.Filled.Place,
                    stringResource(R.string.language),
                    value = LANGUAGES.firstOrNull { it.first == Lang.code }?.second ?: Lang.code,
                    onClick = { langDialog = true },
                )
            }

            Label(stringResource(R.string.settings_more))
            SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                SettingRow(Icons.Filled.Notifications, stringResource(R.string.notifications_title), value = if (vm.unread > 0) vm.unread.toString() else null, onClick = { nav.go(Routes.NOTIFICATIONS) })
                Divider1()
                var pushOn by remember { mutableStateOf(Push.enabled(ctx)) }
                SettingRow(
                    Icons.Filled.Notifications, stringResource(R.string.push_title), subtitle = stringResource(R.string.push_hint),
                    trailing = {
                        Switch(pushOn, { pushOn = it; Push.setEnabled(ctx, it) }, colors = SwitchDefaults.colors(checkedTrackColor = Cyan, checkedThumbColor = Color.White))
                    },
                )
                Divider1()
                SettingRow(
                    Icons.Filled.Notifications, stringResource(R.string.comeback_title), subtitle = stringResource(R.string.comeback_hint),
                    trailing = {
                        Switch(vm.notifyComeback, { vm.flair.setComeback(it) }, colors = SwitchDefaults.colors(checkedTrackColor = Cyan, checkedThumbColor = Color.White))
                    },
                )
                Divider1()
                SettingRow(Icons.Filled.Star, stringResource(R.string.ach_title), onClick = { nav.go(Routes.ACHIEVEMENTS) })
                Divider1()
                SettingRow(Icons.Filled.ThumbUp, stringResource(R.string.stats_title), onClick = { nav.go(Routes.STATS) })
                Divider1()
                SettingRow(Icons.Filled.Settings, stringResource(R.string.platforms_screen), onClick = { nav.go(Routes.PLATFORMS) })
                Divider1()
                SettingRow(Icons.Filled.Create, stringResource(R.string.add_game_title), onClick = { nav.go(Routes.ADD_GAME) })
            }
            Label(stringResource(R.string.integrations))
            SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                SettingRow(Icons.Filled.Home, stringResource(R.string.collection_mine), subtitle = stringResource(R.string.collection_subtitle), onClick = { nav.go(Routes.COLLECTION) })
                Divider1()
                SettingRow(Icons.Filled.ShoppingCart, stringResource(R.string.steam_title), subtitle = stringResource(R.string.steam_subtitle), onClick = { nav.go(Routes.STEAM) })
                Divider1()
                SettingRow(Icons.Filled.Star, "RetroAchievements", subtitle = stringResource(R.string.ra_subtitle), onClick = { nav.go(Routes.RA) })
            }
            if (vm.role == "creator") {
                Label(stringResource(R.string.creator_badge))
                SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                    SettingRow(Icons.Filled.Build, stringResource(R.string.admin_title), tint = Amber, onClick = { nav.go(Routes.ADMIN) })
                    Divider1()
                    SettingRow(Icons.Filled.Edit, stringResource(R.string.news_new), tint = Amber, onClick = { nav.go(Routes.NEWS_NEW) })
                    Divider1()
                    SettingRow(Icons.Filled.Star, stringResource(R.string.ch_admin_title), tint = Amber, onClick = { nav.go(Routes.CHALLENGES) })
                }
            }

            Label(stringResource(R.string.about_title))
            SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Logo(size = 13.sp)
                Spacer(Modifier.height(10.dp))
                Body(stringResource(R.string.about_text), color = Muted, size = 14.sp)
                Spacer(Modifier.height(10.dp))
                Caption(stringResource(R.string.data_credit))
                Caption(stringResource(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
            }
            Spacer(Modifier.height(10.dp))
            SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                UpdatesRow()
                Divider1()
                SettingRow(Icons.Filled.Info, stringResource(R.string.privacy_policy), onClick = { openInBrowser(ctx, PRIVACY_PAGE) })
            }
            Spacer(Modifier.height(16.dp))
            SvButton(stringResource(R.string.logout), { logoutDialog = true }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), kind = BtnKind.Danger, icon = Icons.AutoMirrored.Filled.ExitToApp)
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.delete_account),
                color = Red.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { deleteDialog = true }.padding(12.dp),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
    if (langDialog) {
        Dialog(onDismissRequest = { langDialog = false }) {
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Panel).padding(vertical = 12.dp)) {
                H2(stringResource(R.string.language), Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                LazyColumn(Modifier.heightIn(max = 460.dp)) {
                    items(LANGUAGES) { (code, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { Lang.save(ctx, code); langDialog = false }.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Body(label, Modifier.weight(1f), color = if (code == Lang.code) Cyan else Ink, size = 16.sp)
                            if (code == Lang.code) Icon(Icons.Filled.Check, null, tint = Cyan)
                        }
                    }
                }
            }
        }
    }
    if (nameDialog) {
        var nick by remember { mutableStateOf(vm.displayName) }
        Dialog(onDismissRequest = { nameDialog = false }) {
            Column(Modifier.fillMaxWidth().clip(CardShape).background(Panel).padding(20.dp)) {
                H2(stringResource(R.string.profile_nick))
                Spacer(Modifier.height(12.dp))
                SvField(nick, { nick = it.take(40) }, stringResource(R.string.nick_hint))
                vm.profileError?.let { Body(stringResource(it), color = Red, size = 14.sp) }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SvButton(stringResource(R.string.cancel), { nameDialog = false }, Modifier.weight(1f), kind = BtnKind.Secondary)
                    SvButton(stringResource(R.string.save), { vm.saveName(nick); nameDialog = false }, Modifier.weight(1f), enabled = nick.trim() != vm.displayName && nick.isNotBlank())
                }
            }
        }
    }
    if (deleteDialog) DeleteAccountDialog(vm) { deleteDialog = false }
    if (logoutDialog) {
        ConfirmDialog(stringResource(R.string.logout), vm.email, { logoutDialog = false; vm.logout() }, { logoutDialog = false }, confirmText = stringResource(R.string.logout))
    }
}

/** Google / Yandex / VK accounts linked to the Svaga ID. */
@Composable
fun SvagaIdSection(vm: AppViewModel) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { vm.loadIdentities() }
    val ids = vm.identities ?: return
    if (ids.available.isEmpty() && ids.items.isEmpty()) return
    Label(stringResource(R.string.svaga_id_title))
    SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
        Caption(stringResource(R.string.svaga_id_hint), Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp), maxLines = 4)
        val all = (ids.available + ids.items.map { it.provider }).distinct()
        all.forEachIndexed { i, p ->
            if (i > 0) Divider1()
            val linked = ids.items.firstOrNull { it.provider == p }
            SettingRow(
                Icons.Filled.AccountCircle,
                providerTitle(p),
                subtitle = linked?.let { it.name ?: it.email ?: stringResource(R.string.svaga_id_linked) } ?: stringResource(R.string.svaga_id_not_linked),
                tint = if (linked != null) Green else Ink,
                trailing = {
                    if (linked != null) SvChip(stringResource(R.string.svaga_id_unlink), false, { vm.unlinkIdentity(p) }, accent = Red)
                    else SvChip(stringResource(R.string.svaga_id_link), false, { vm.linkIdentity(ctx, p) })
                },
            )
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
            (n.payload.optString("title").ifBlank { null } ?: achTitle(n.payload.optString("code"))) + " " + ROMAN.getOrElse(n.payload.optInt("tier", 1)) { "" },
            n.payload.optInt("sp", 0),
        )
        "level_up" -> stringResource(R.string.n_level_up, levelName(n.payload.optInt("level", 1)))
        "friend_request" -> stringResource(R.string.n_friend_request, name)
        "friend_accepted" -> stringResource(R.string.n_friend_accepted, name)
        "friend_declined" -> stringResource(R.string.n_friend_declined, name)
        "comment_like" -> stringResource(R.string.n_comment_like, name)
        "release" -> stringResource(R.string.n_release, name)
        "news" -> stringResource(R.string.n_news, n.payload.optString("title"))
        "anniversary" -> stringResource(R.string.n_anniversary, name, androidx.compose.ui.platform.LocalContext.current.resources.getQuantityString(R.plurals.years_count, n.payload.optInt("years"), n.payload.optInt("years")))
        "list_like" -> stringResource(R.string.n_list_like, name, n.payload.optString("title"))
        "comeback" -> stringResource(R.string.n_comeback, name, n.payload.optInt("days"))
        "bingo_line" -> stringResource(R.string.n_bingo_line, n.payload.optInt("lines"), n.payload.optInt("total", 12))
        "bingo_full" -> stringResource(R.string.n_bingo_full)
        "season_level" -> stringResource(R.string.n_season_level, n.payload.optInt("level"))
        else -> n.kind
    }
}

@Composable
fun NotificationsScreen(vm: AppViewModel, nav: Nav) {
    LaunchedEffect(Unit) { vm.loadNotifications() }
    DisposableEffect(Unit) { onDispose { vm.markAllRead() } }
    ScreenScaffold(stringResource(R.string.notifications_title), nav) {
        if (vm.notifications.isEmpty()) {
            EmptyState(stringResource(R.string.notifications_empty))
            return@ScreenScaffold
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(vm.notifications, key = { it.id }) { n ->
                val (icon, color) = when (n.kind) {
                    "achievement", "level_up" -> Icons.Filled.Star to Amber
                    "comment_like", "list_like" -> Icons.Filled.Favorite to Pink
                    "anniversary", "release", "comeback" -> Icons.Filled.DateRange to Amber
                    "bingo_line", "bingo_full" -> Icons.Filled.Star to Green
                    "season_level" -> Icons.Filled.Star to Purple
                    else -> Icons.Filled.Person to Cyan
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (n.read) Color.Transparent else Cyan.copy(alpha = 0.06f))
                        .clickable {
                            when (n.kind) {
                                "achievement", "level_up" -> nav.go(Routes.ACHIEVEMENTS)
                                "friend_request", "friend_accepted", "friend_declined" -> {
                                    val uid = n.payload.optString("userId")
                                    if (uid.isNotEmpty()) nav.user(uid)
                                }
                                "list_like" -> {
                                    val lid = n.payload.optLong("listId", 0L)
                                    if (lid > 0) nav.go(Routes.list(lid))
                                }
                                "bingo_line", "bingo_full" -> nav.go(Routes.BINGO)
                                "season_level" -> nav.go(Routes.SEASON)
                                "comment_like", "release", "anniversary", "comeback" -> {
                                    val gid = n.payload.optInt("gameId", 0)
                                    if (gid > 0) nav.game(gid)
                                }
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconDot(icon, color)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Body(notificationText(n), size = 15.sp, weight = if (n.read) FontWeight.Normal else FontWeight.SemiBold)
                        Caption(relativeTime(n.createdAt), maxLines = 1)
                    }
                    if (!n.read) Box(Modifier.size(8.dp).background(Cyan, CircleShape))
                }
            }
        }
    }
}

// ======================= statistics =======================

@Composable
fun StatsScreen(vm: AppViewModel, nav: Nav) {
    LaunchedEffect(Unit) {
        vm.loadMyStats(vm.statsPeriod)
        vm.loadActivity()
    }
    val s = vm.stats
    val periods = listOf("day" to R.string.stats_day, "week" to R.string.stats_week, "month" to R.string.stats_month, "year" to R.string.stats_year)
    ScreenScaffold(stringResource(R.string.stats_title), nav) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            vm.activity?.let { days ->
                SectionHeader(stringResource(R.string.activity_title))
                ActivityHeatmap(days, weeks = 52)
                Spacer(Modifier.height(12.dp))
            }
            Segmented(
                periods.map { stringResource(it.second) },
                periods.indexOfFirst { it.first == vm.statsPeriod }.coerceAtLeast(0),
                { vm.loadMyStats(periods[it].first) },
                Modifier.padding(horizontal = 16.dp),
            )
            if (s == null || s.period != vm.statsPeriod) {
                LoadingBox()
                return@Column
            }
            val rows = listOf(
                R.string.stat_added to "added", R.string.stat_completed to "completed", R.string.stat_rated to "rated",
                R.string.stat_comments to "comments", R.string.stat_likes to "likes", R.string.stat_friends to "friends",
                R.string.stat_sp to "sp", R.string.stat_active_days to "activeDays",
            )
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { (label, key) -> StatTile(compact(s.window[key] ?: 0), stringResource(label), Modifier.weight(1f), accent = Cyan) }
                    }
                }
            }
            if (s.series.isNotEmpty()) {
                SectionHeader(stringResource(R.string.stats_activity), Modifier.padding(top = 10.dp))
                val max = s.series.maxOf { it.second }.coerceAtLeast(1)
                Row(
                    Modifier.fillMaxWidth().horizontalScrollState().padding(horizontal = 16.dp).height(140.dp),
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
                            Text(count.toString(), color = Muted, fontSize = 11.sp)
                            Box(Modifier.width(20.dp).height((6 + 90 * count / max).dp).clip(RoundedCornerShape(6.dp)).background(AccentBrush))
                            Text(label, color = Muted, fontSize = 11.sp)
                        }
                    }
                }
            }
            SectionHeader(stringResource(R.string.stats_total), Modifier.padding(top = 10.dp))
            SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
                val totals = listOf(
                    R.string.stat_in_library to "library", R.string.stat_completed to "completed", R.string.stat_rated to "rated",
                    R.string.stat_comments to "comments", R.string.stat_friends to "friends", R.string.stat_streak to "streak",
                )
                totals.forEachIndexed { i, (label, key) ->
                    if (i > 0) Divider1()
                    KeyValue(stringResource(label), (s.total[key] ?: 0).toString())
                }
                s.avgRating?.let {
                    Divider1()
                    KeyValue(stringResource(R.string.stat_avg_rating), it.toString())
                }
                vm.playSeconds?.let {
                    Divider1()
                    KeyValue(stringResource(R.string.num_hours), hours(it))
                }
                Divider1()
                KeyValue(stringResource(R.string.stat_level), levelName(s.level) + " · " + s.sp + " SP")
            }
            if (s.byStatus.isNotEmpty()) {
                SectionHeader(stringResource(R.string.stats_by_status), Modifier.padding(top = 10.dp))
                val total = s.byStatus.values.sum().coerceAtLeast(1)
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    STATUSES.forEach { st ->
                        val n = s.byStatus[st] ?: 0
                        if (n > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Body(statusLabel(st), Modifier.width(120.dp), size = 14.sp, maxLines = 1)
                                ProgressLine(n.toFloat() / total, statusColor(st), Modifier.weight(1f))
                                Text(n.toString(), color = Ink, fontSize = 14.sp, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Modifier.horizontalScrollState(): Modifier = this.then(Modifier.horizontalScroll(rememberScrollState()))

@Composable
private fun KeyValue(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Body(key, Modifier.weight(1f), size = 15.sp)
        Text(value, color = Cyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ======================= platforms =======================

@Composable
fun PlatformsScreen(vm: AppViewModel, nav: Nav) {
    var q by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    val needle = q.trim()
    val base = vm.platforms.filter { needle.isEmpty() || it.name.contains(needle, true) || it.short.contains(needle, true) }
    val list = when (sort) {
        1 -> base.sortedWith(compareBy({ it.year ?: 9999 }, { it.name }))
        2 -> base.sortedWith(compareBy({ it.generation ?: 99 }, { it.year ?: 9999 }, { it.name }))
        3 -> base.sortedWith(compareByDescending<Platform> { it.games ?: -1 }.thenBy { it.name })
        else -> base.sortedBy { it.name }
    }
    ScreenScaffold(stringResource(R.string.platforms_screen), nav) {
        SearchBar(q, { q = it }, stringResource(R.string.platform_search), Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        ChipRow(Modifier.padding(vertical = 6.dp)) {
            listOf(R.string.sort_name, R.string.sort_year, R.string.sort_generation, R.string.sort_games).forEachIndexed { i, r ->
                SvChip(stringResource(r), sort == i, { sort = i }, accent = Pink)
            }
        }
        if (vm.platforms.isEmpty()) {
            LoadingBox()
            return@ScreenScaffold
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(list, key = { it.id }) { p ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        vm.applyFilter(GameFilter(platform = p.id))
                        nav.go(Routes.CATALOG)
                    }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)).background(Panel).border(1.dp, Stroke, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                        if (p.logo != null) {
                            AsyncImage(model = p.logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp))
                        } else {
                            Text(p.short.take(4), color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Body(p.name, size = 16.sp, weight = FontWeight.SemiBold, maxLines = 2)
                        val parts = listOfNotNull(
                            p.year?.let { stringResource(R.string.platform_year, it) },
                            p.generation?.let { stringResource(R.string.platform_generation, it) },
                            p.games?.let { stringResource(R.string.platform_games, it) },
                        )
                        if (parts.isNotEmpty()) Caption(parts.joinToString(" · "), maxLines = 1)
                        p.family?.let { Caption(it, maxLines = 1) }
                    }
                }
            }
        }
    }
}

// ======================= add a game =======================

@Composable
fun AddGameScreen(vm: AppViewModel, nav: Nav) {
    LaunchedEffect(Unit) { vm.clearAddState() }
    var name by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var devs by rememberSaveable { mutableStateOf("") }
    var genres by rememberSaveable { mutableStateOf("") }
    var summary by rememberSaveable { mutableStateOf("") }
    var cover by rememberSaveable { mutableStateOf("") }
    var chosen by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var picking by remember { mutableStateOf(false) }
    ScreenScaffold(stringResource(R.string.add_game_title), nav) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Caption(stringResource(R.string.add_game_hint), maxLines = 6)
            SvField(name, { name = it.take(120) }, stringResource(R.string.add_game_name))
            SvField(year, { year = it.filter { c -> c.isDigit() }.take(4) }, stringResource(R.string.add_game_year), keyboard = KeyboardType.Number)
            SvField(devs, { devs = it }, stringResource(R.string.add_game_devs))
            SvField(genres, { genres = it }, stringResource(R.string.add_game_genres))
            SvField(summary, { summary = it.take(4000) }, stringResource(R.string.add_game_summary), singleLine = false, minLines = 4)
            SvField(cover, { cover = it.trim() }, stringResource(R.string.add_game_cover))
            val names = vm.platforms.filter { it.id in chosen }.joinToString(", ") { it.short }
            SvButton(
                stringResource(R.string.add_game_platforms) + if (names.isNotBlank()) ": $names" else "",
                { picking = true },
                Modifier.fillMaxWidth(),
                kind = BtnKind.Secondary,
            )
            vm.addError?.let { Body(stringResource(it), color = Red, size = 14.sp) }
            if (vm.addDuplicates.isNotEmpty()) {
                Body(stringResource(R.string.add_game_duplicates), color = Amber, size = 14.sp)
                vm.addDuplicates.forEach { g -> GameRow(g, g.name, { nav.game(g.id) }, status = vm.library[g.id]?.status) }
            }
            SvButton(
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
                    ) { gid ->
                        nav.back()
                        nav.game(gid)
                    }
                },
                Modifier.fillMaxWidth(),
                enabled = name.trim().length >= 2 && chosen.isNotEmpty(),
                busy = vm.addBusy,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
    if (picking) {
        PlatformPicker(
            vm.platforms,
            null,
            onSelect = {},
            onDismiss = { picking = false },
            multi = chosen,
            onToggle = { id -> chosen = if (id in chosen) chosen - id else chosen + id },
            title = stringResource(R.string.add_game_platforms),
        )
    }
}

// ======================= news editor (creator) =======================

@Composable
fun NewsEditorScreen(vm: AppViewModel, nav: Nav) {
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var image by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    ScreenScaffold(stringResource(R.string.news_new), nav) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SvField(title, { title = it.take(200) }, stringResource(R.string.news_field_title))
            SvField(body, { body = it.take(8000) }, stringResource(R.string.news_field_body), singleLine = false, minLines = 8)
            SvField(image, { image = it.trim() }, stringResource(R.string.news_field_image))
            if (image.startsWith("https://")) {
                AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(CardShape).background(Panel2))
            }
            vm.newsError?.let { Body(stringResource(it), color = Red, size = 14.sp) }
            SvButton(stringResource(R.string.news_publish), {
                busy = true
                vm.postNews(title, body, image) {
                    busy = false
                    nav.back()
                }
            }, Modifier.fillMaxWidth(), enabled = title.trim().length >= 2, busy = busy && vm.newsError == null)
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ======================= creator panel =======================

@Composable
fun AdminScreen(vm: AppViewModel, nav: Nav) {
    var q by rememberSaveable { mutableStateOf("") }
    var banTarget by remember { mutableStateOf<AdminUser?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminUser?>(null) }
    var reason by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.adminSearch("") }
    ScreenScaffold(stringResource(R.string.admin_title), nav) {
        SearchBar(q, { q = it; vm.adminSearch(it) }, stringResource(R.string.admin_search), Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vm.adminUsers, key = { it.id }) { u ->
                SvCard(Modifier.fillMaxWidth(), border = if (u.banned) Red else null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.id, u.avatarVersion, u.name, 42.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Body(u.name, size = 16.sp, weight = FontWeight.SemiBold, maxLines = 1)
                            Caption(u.email, maxLines = 1)
                            if (u.banned) Caption(stringResource(R.string.admin_banned) + (u.banReason?.let { ": $it" } ?: ""), color = Red)
                        }
                        if (u.role == "creator") Pill(stringResource(R.string.creator_badge), Amber)
                    }
                    if (u.role != "creator") {
                        Spacer(Modifier.height(10.dp))
                        ChipRow(Modifier.padding(horizontal = 0.dp)) {
                            if (u.banned) SvChip(stringResource(R.string.admin_unban), false, { vm.adminUnban(u.id, q.trim()) }, accent = Green)
                            else SvChip(stringResource(R.string.admin_ban), false, { reason = ""; banTarget = u }, accent = Red)
                            SvChip(stringResource(R.string.admin_delete), false, { deleteTarget = u }, accent = Red)
                            SvChip(stringResource(R.string.admin_reset_code), false, { vm.adminResetCode(u) }, accent = Amber)
                            SvChip(stringResource(R.string.admin_grant), false, { vm.adminGrant(u.id) }, accent = Pink)
                        }
                    }
                }
            }
        }
    }
    banTarget?.let { u ->
        FormDialog(stringResource(R.string.admin_ban) + ": " + u.name, stringResource(R.string.admin_ban), {
            vm.adminBan(u.id, reason, q.trim())
            banTarget = null
        }, { banTarget = null }) {
            SvField(reason, { reason = it.take(300) }, stringResource(R.string.admin_ban_reason))
        }
    }
    deleteTarget?.let { u ->
        ConfirmDialog(stringResource(R.string.admin_delete_confirm, u.name), null, {
            vm.adminDelete(u.id, q.trim())
            deleteTarget = null
        }, { deleteTarget = null }, confirmText = stringResource(R.string.admin_delete))
    }
    vm.adminCode?.let { (mail, code) ->
        FormDialog(stringResource(R.string.admin_code_title, mail), stringResource(R.string.close), { vm.closeAdminCode() }, { vm.closeAdminCode() }) {
            Text(code, color = Cyan, fontFamily = Pixel, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Caption(stringResource(R.string.admin_code_hint), maxLines = 4)
        }
    }
}

@Composable
private fun FormDialog(title: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().clip(CardShape).background(Panel).padding(20.dp)) {
            H2(title)
            Spacer(Modifier.height(12.dp))
            content()
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SvButton(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f), kind = BtnKind.Secondary)
                SvButton(confirm, onConfirm, Modifier.weight(1f))
            }
        }
    }
}
