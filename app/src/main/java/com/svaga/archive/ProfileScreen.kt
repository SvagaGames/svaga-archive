package com.svaga.archive

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale

// ======================= shared profile parts =======================

/** Big profile header: ringed avatar, name, level, "since" line. */
@Composable
private fun ProfileHeader(
    userId: String,
    avatarVersion: Int,
    name: String,
    createdAt: String,
    level: Int,
    creator: Boolean,
    onAvatar: (() -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            Avatar(userId, avatarVersion, name, 112.dp, Modifier.then(if (onAvatar != null) Modifier.clickable(onClick = onAvatar) else Modifier), ring = true)
            if (onAvatar != null) {
                Box(
                    Modifier.align(Alignment.BottomEnd).size(34.dp).background(AccentBrush, CircleShape).border(3.dp, Bg, CircleShape).clickable(onClick = onAvatar),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Edit, null, tint = Color(0xFF071018), modifier = Modifier.size(16.dp)) }
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 30.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(stringResource(R.string.level_n, level), Cyan)
                if (creator) {
                    Spacer(Modifier.width(6.dp))
                    Pill(stringResource(R.string.creator_badge), Amber)
                }
            }
            Spacer(Modifier.height(6.dp))
            Caption(levelName(level), maxLines = 1)
            val since = parseInstant(createdAt)?.year
            if (since != null) Caption(stringResource(R.string.profile_since_year, since), maxLines = 1)
        }
    }
}

@Composable
private fun NumbersRow(items: List<Pair<String, String>>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp)) {
        items.forEach { (v, l) -> StatNumber(v, l, Modifier.weight(1f)) }
    }
}

@Composable
private fun LevelBar(level: Int, sp: Int, from: Int, to: Int) {
    SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Body(levelName(level), Modifier.weight(1f), weight = FontWeight.SemiBold, maxLines = 1)
            Text(stringResource(R.string.level_progress, sp, to), color = Muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        ProgressLine(if (to > from) (sp - from).toFloat() / (to - from) else 1f, Cyan)
    }
}

/** Achievement badge for the horizontal row on profiles. */
@Composable
fun AchBadge(code: String, tier: Int, modifier: Modifier = Modifier, secret: Boolean = false) {
    Column(modifier.width(100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AchievementArt(code, if (secret) 0 else tier, unlocked = true, size = 84.dp)
        Spacer(Modifier.height(6.dp))
        Text(achTitle(code), color = Ink, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 15.sp)
    }
}

@Composable
private fun StatusGrid(library: Collection<LibraryItem>, onOpen: ((String) -> Unit)?) {
    val keys = listOf("backlog", "playing", "completed", "dropped")
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        keys.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { k ->
                    StatTile(
                        library.count { it.status == k }.toString(),
                        statusLabel(k),
                        Modifier.weight(1f),
                        accent = statusColor(k),
                        onClick = onOpen?.let { f -> { f(k) } },
                    )
                }
            }
        }
    }
}

/** "16 нояб  [cover] Game · status" rows of the latest library changes. */
@Composable
private fun EventsList(items: List<LibraryItem>, vm: AppViewModel, nav: Nav) {
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM", Locale(Lang.code)) }
    items.forEach { item ->
        val g = vm.gamesCache[item.gameId]
        Row(
            Modifier.fillMaxWidth().clickable { nav.game(item.gameId) }.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(parseInstant(item.updatedAt)?.format(fmt) ?: "", color = Muted, fontSize = 14.sp, modifier = Modifier.width(64.dp))
            Cover(g?.cover, Modifier.width(48.dp).aspectRatio(0.75f), RoundedCornerShape(10.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Body(g?.name ?: "#${item.gameId}", size = 15.sp, weight = FontWeight.SemiBold, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.status?.let { Caption(statusLabel(it), color = statusColor(it), maxLines = 1) }
                    if (item.favorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Favorite, null, tint = Pink, modifier = Modifier.size(14.dp))
                    }
                }
            }
            item.rating?.let { StarBadge(it.toString(), color = Cyan) }
        }
    }
}

// ======================= my profile =======================

@Composable
fun ProfileScreen(vm: AppViewModel, nav: Nav, standalone: Boolean, openLibrary: (String) -> Unit = {}) {
    LaunchedEffect(Unit) {
        vm.loadProfile()
        vm.loadAchievements()
        vm.loadMyComments()
        vm.loadActivity()
        vm.loadPlaytime()
        vm.extras.loadRa()
    }
    var share by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.uploadAvatar(uri) }
    val name = vm.displayName.ifBlank { vm.email }
    val ach = vm.ach
    val lib = vm.library.values.filter { it.status != null || it.favorite }
    val got = ach?.items.orEmpty().filter { it.tier > 0 }
    val shownAch = (got.filter { it.featured } + got.filter { !it.featured }.sortedByDescending { it.unlockedAt ?: "" }).take(12)
    val activeDays = vm.activity?.count { it.count > 0 }

    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (standalone) IconBtn(Icons.AutoMirrored.Filled.ArrowBack, nav::back)
            else IconBtn(Icons.Filled.Notifications, { nav.go(Routes.NOTIFICATIONS) }, badge = vm.unread)
            Spacer(Modifier.weight(1f))
            IconBtn(Icons.Filled.Share, { share = true })
            Spacer(Modifier.width(8.dp))
            IconBtn(Icons.Filled.Settings, { nav.go(Routes.SETTINGS) })
        }
        ProfileHeader(vm.userId, vm.avatarVersion, name, vm.createdAt, ach?.level ?: vm.level, vm.role == "creator") { picker.launch("image/*") }
        vm.profileError?.let { Body(stringResource(it), Modifier.padding(horizontal = 20.dp), color = Red) }
        NumbersRow(
            listOf(
                compact(lib.size) to stringResource(R.string.num_games),
                compact(lib.count { it.status == "completed" }) to stringResource(R.string.num_completed),
                (vm.playSeconds?.let { hours(it) } ?: "—") to stringResource(R.string.num_hours),
                (activeDays?.let { compact(it) } ?: compact(ach?.stats?.get("rated") ?: 0)) to
                    stringResource(if (activeDays != null) R.string.num_days else R.string.num_rated),
            ),
        )
        LevelBar(ach?.level ?: vm.level, ach?.sp ?: vm.sp, ach?.levelFrom ?: 0, ach?.levelTo ?: 100)

        Spacer(Modifier.height(12.dp))
        SectionHeader(stringResource(R.string.ach_title), count = got.size, onMore = { nav.go(Routes.ACHIEVEMENTS) })
        if (shownAch.isEmpty()) {
            Caption(stringResource(R.string.ach_none), Modifier.padding(horizontal = 16.dp))
        } else {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shownAch, key = { it.code }) { a -> AchBadge(a.code, a.tier, Modifier.clickable { nav.go(Routes.ACHIEVEMENTS) }, secret = a.secret) }
            }
        }

        val days = vm.activity
        if (days != null) {
            Spacer(Modifier.height(12.dp))
            SectionHeader(stringResource(R.string.activity_title), onMore = { nav.go(Routes.STATS) })
            ActivityHeatmap(days)
        }

        Spacer(Modifier.height(12.dp))
        SectionHeader(stringResource(R.string.section_status), trailing = {
            vm.playSeconds?.let { s ->
                Text(
                    stringResource(R.string.hours_short, hours(s)),
                    color = Cyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.border(1.dp, Cyan.copy(alpha = 0.5f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        })
        StatusGrid(lib, onOpen = openLibrary)

        val recent = lib.sortedByDescending { it.updatedAt }.take(6)
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionHeader(stringResource(R.string.my_events), onMore = { openLibrary("all") })
            EventsList(recent, vm, nav)
        }

        val raState = vm.extras.ra
        if (raState?.linked == true) {
            Spacer(Modifier.height(12.dp))
            RaProfileCard(raState, onClick = { nav.go(Routes.RA) })
        }

        Spacer(Modifier.height(12.dp))
        SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), padding = 0.dp) {
            SettingRow(Icons.Filled.Star, stringResource(R.string.board_title), onClick = { nav.go(Routes.LEADERBOARD) })
            Divider1()
            SettingRow(Icons.Filled.Home, stringResource(R.string.collection_mine), onClick = { nav.go(Routes.COLLECTION) })
            Divider1(Modifier.padding(start = 64.dp))
            SettingRow(Icons.Filled.Menu, stringResource(R.string.lists_mine), onClick = { nav.go(Routes.LISTS) })
            Divider1()
            SettingRow(Icons.Filled.Favorite, stringResource(R.string.people_friends), value = vm.friends.friends.size.toString(), onClick = { nav.go(Routes.PEOPLE) })
            Divider1()
            SettingRow(Icons.Filled.Edit, stringResource(R.string.my_comments_title), value = vm.myComments.size.toString(), onClick = { nav.go(Routes.MY_COMMENTS) })
            Divider1()
            SettingRow(Icons.Filled.Star, stringResource(R.string.stats_title), onClick = { nav.go(Routes.STATS) })
        }
        Spacer(Modifier.height(if (standalone) 24.dp else BottomBarSpace))
    }
    if (share) ShareDialog(stringResource(R.string.share_profile_text) + " " + DOWNLOAD_PAGE, { share = false }) { ProfileShareCard(vm) }
}

@Composable
fun MyCommentsScreen(vm: AppViewModel, nav: Nav) {
    LaunchedEffect(Unit) { vm.loadMyComments() }
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(stringResource(R.string.my_comments_title), nav::back)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (vm.myComments.isEmpty()) item { EmptyState(stringResource(R.string.my_comments_empty)) }
            items(vm.myComments, key = { it.id }) { c ->
                CommentCard(c, true, { vm.toggleLike(c) }, { vm.deleteComment(c) }, onOpenGame = { nav.game(c.gameId) })
            }
        }
    }
}

// ======================= other players =======================

@Composable
fun UserProfileScreen(vm: AppViewModel, id: String, nav: Nav) {
    LaunchedEffect(id) {
        vm.loadProfileOf(id)
        vm.loadActivityOf(id)
    }
    var libTab by rememberSaveable { mutableStateOf("all") }
    val prof = vm.viewedProfile
    Column(Modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            IconBtn(Icons.AutoMirrored.Filled.ArrowBack, nav::back)
        }
        if (prof == null || prof.person.id != id) {
            LoadingBox()
            return@Column
        }
        val p = prof.person
        ProfileHeader(p.id, p.avatarVersion, p.name, p.createdAt, prof.level, p.creator)
        val lib = prof.library
        NumbersRow(
            listOf(
                compact(p.library) to stringResource(R.string.num_games),
                compact(p.completed) to stringResource(R.string.num_completed),
                compact(prof.achievements) to stringResource(R.string.num_achievements),
                compact(lib?.count { it.rating != null } ?: 0) to stringResource(R.string.num_rated),
            ),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            when (p.relation) {
                "none" -> SvButton(stringResource(R.string.friend_add), { vm.friendAction(p.id, "add") }, Modifier.weight(1f))
                "outgoing" -> SvButton(stringResource(R.string.friend_cancel), { vm.friendAction(p.id, "remove") }, Modifier.weight(1f), kind = BtnKind.Secondary)
                "incoming" -> {
                    SvButton(stringResource(R.string.friend_accept), { vm.friendAction(p.id, "accept") }, Modifier.weight(1f))
                    SvButton(stringResource(R.string.friend_decline), { vm.friendAction(p.id, "remove") }, Modifier.weight(1f), kind = BtnKind.Danger)
                }
                "friends" -> {
                    SvButton(stringResource(R.string.friend_in), {}, Modifier.weight(1f), kind = BtnKind.Ghost, accent = Pink, icon = Icons.Filled.Favorite)
                    SvButton(stringResource(R.string.friend_remove), { vm.friendAction(p.id, "remove") }, Modifier.weight(1f), kind = BtnKind.Danger)
                }
                else -> {}
            }
        }
        Spacer(Modifier.height(12.dp))
        LevelBar(prof.level, prof.sp, prof.levelFrom, prof.levelTo)
        SvButton(stringResource(R.string.lists_of_player), { nav.go(Routes.userLists(id)) }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), kind = BtnKind.Secondary, icon = Icons.Filled.Menu)
        if (prof.featured.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionHeader(stringResource(R.string.ach_title), count = prof.achievements)
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(prof.featured, key = { it.code }) { a -> AchBadge(a.code, a.tier) }
            }
        }
        val days = vm.viewedActivity
        if (!days.isNullOrEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionHeader(stringResource(R.string.activity_title))
            ActivityHeatmap(days)
        }
        Spacer(Modifier.height(12.dp))
        SectionHeader(stringResource(R.string.tab_library), count = lib?.size)
        if (lib == null) {
            EmptyState(stringResource(R.string.user_library_hidden))
        } else if (lib.isEmpty()) {
            EmptyState(stringResource(R.string.library_empty))
        } else {
            StatusGrid(lib, onOpen = { libTab = it })
            Spacer(Modifier.height(8.dp))
            ChipRow {
                listOf("all", "playing", "backlog", "completed", "dropped", "fav").forEach { k ->
                    SvChip(statusLabel(k), libTab == k, { libTab = k }, accent = statusColor(k))
                }
            }
            val shown = lib.filter {
                when (libTab) {
                    "all" -> true
                    "fav" -> it.favorite
                    else -> it.status == libTab
                }
            }.sortedByDescending { it.updatedAt }.take(60)
            Spacer(Modifier.height(8.dp))
            shown.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { item ->
                        val g = vm.gamesCache[item.gameId]
                        Box(Modifier.weight(1f)) {
                            if (g != null) PosterCard(g, { nav.game(item.gameId) }, status = item.status, myRating = item.rating)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (vm.viewedComments.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SectionHeader(stringResource(R.string.comments_title), count = vm.viewedComments.size)
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                vm.viewedComments.forEach { c ->
                    CommentCard(
                        c,
                        canDelete = vm.role == "creator",
                        onLike = { vm.toggleLike(c) },
                        onDelete = { vm.deleteComment(c) },
                        onOpenGame = { nav.game(c.gameId) },
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
