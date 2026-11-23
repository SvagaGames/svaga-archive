package com.svaga.archive

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

// ======================= shared pieces =======================

@Composable
fun Avatar(userId: String, version: Int, name: String, size: Dp, modifier: Modifier = Modifier) {
    val frame = modifier.size(size).background(Panel2).border(2.dp, Line)
    if (version > 0 && userId.isNotEmpty()) {
        AsyncImage(
            model = avatarUrl(userId, version),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = frame,
        )
    } else {
        Box(frame, contentAlignment = Alignment.Center) {
            Text(name.take(1).uppercase(), color = Cyan, fontFamily = Pixel, fontSize = (size.value / 3f).sp)
        }
    }
}

@Composable
fun ProgressBar(fraction: Float, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).background(Panel2)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

@Composable
fun CommentRow(
    c: Comment,
    canDelete: Boolean,
    onLike: () -> Unit,
    onDelete: () -> Unit,
    onOpenGame: (() -> Unit)? = null,
    onOpenUser: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(Panel)
            .border(2.dp, Line)
            .padding(10.dp),
    ) {
        Avatar(c.userId, c.avatarVersion, c.name, 40.dp, if (onOpenUser != null) Modifier.clickable(onClick = onOpenUser) else Modifier)
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(c.name, color = Cyan, fontFamily = Vt, fontSize = 17.sp)
                c.rating?.let { Text("$it/10", color = Amber, fontFamily = Vt, fontSize = 17.sp) }
            }
            Text(formatDate(c.createdAt), color = Muted, fontFamily = Vt, fontSize = 15.sp)
            if (onOpenGame != null && !c.gameName.isNullOrBlank()) {
                Text(
                    "▶ " + c.gameName,
                    color = Cyan,
                    fontFamily = Vt,
                    fontSize = 17.sp,
                    modifier = Modifier.clickable(onClick = onOpenGame).padding(vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(c.body, color = Ink, fontSize = 14.sp, lineHeight = 20.sp)
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    (if (c.liked) "♥ " else "♡ ") + c.likes,
                    color = if (c.liked) Pink else Muted,
                    fontFamily = Vt,
                    fontSize = 19.sp,
                    modifier = Modifier.clickable(onClick = onLike).padding(vertical = 4.dp),
                )
                if (canDelete) {
                    Text(
                        stringResource(R.string.comment_delete),
                        color = Red,
                        fontFamily = Vt,
                        fontSize = 17.sp,
                        modifier = Modifier.clickable(onClick = onDelete).padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun LevelCard(level: Int, sp: Int, from: Int, to: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).border(2.dp, Cyan, RoundedCornerShape(12.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.level_n, level), color = Cyan, fontFamily = Pixel, fontSize = 11.sp)
            Text(levelName(level), color = Ink, fontFamily = Vt, fontSize = 22.sp, modifier = Modifier.padding(start = 10.dp))
        }
        Spacer(Modifier.height(8.dp))
        ProgressBar(if (to > from) (sp - from).toFloat() / (to - from) else 1f, Cyan)
        Text(
            stringResource(R.string.level_progress, sp, to),
            color = Muted,
            fontFamily = Vt,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun FeaturedRow(items: List<FeaturedAch>) {
    if (items.isEmpty()) return
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { a ->
            Row(
                Modifier.background(Panel).border(2.dp, tierColor(a.tier)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("★", color = tierColor(a.tier), fontSize = 16.sp)
                Text(" " + achTitle(a.code) + " " + ROMAN.getOrElse(a.tier) { "" }, color = Ink, fontFamily = Vt, fontSize = 17.sp)
            }
        }
    }
}

// ======================= profile =======================

@Composable
fun ProfileScreen(vm: AppViewModel, onOpenGame: (Int) -> Unit, onGo: (String) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.loadProfile()
        vm.loadAchievements()
        vm.loadMyComments()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.uploadAvatar(uri)
    }
    var nick by remember(vm.displayName) { mutableStateOf(vm.displayName) }
    val shownName = vm.displayName.ifBlank { vm.email }
    val ach = vm.ach

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        Text(
            stringResource(R.string.profile_account),
            color = Cyan,
            fontFamily = Pixel,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(vm.userId, vm.avatarVersion, shownName, 96.dp, Modifier.clickable { picker.launch("image/*") })
            Column(Modifier.padding(start = 14.dp)) {
                Text(shownName, color = Ink, fontFamily = Vt, fontSize = 22.sp)
                if (vm.role == "creator") Text(stringResource(R.string.creator_badge), color = Amber, fontFamily = Pixel, fontSize = 8.sp)
                Text(vm.email, color = Muted, fontFamily = Vt, fontSize = 17.sp)
                if (vm.createdAt.isNotBlank()) {
                    Text(
                        stringResource(R.string.profile_since, formatDate(vm.createdAt)),
                        color = Muted,
                        fontFamily = Vt,
                        fontSize = 17.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelButton(
                text = stringResource(R.string.avatar_change),
                onClick = { picker.launch("image/*") },
                enabled = !vm.profileBusy,
            )
            if (vm.avatarVersion > 0) {
                PixelButton(
                    text = stringResource(R.string.avatar_remove),
                    onClick = { vm.removeAvatar() },
                    accent = Red,
                    enabled = !vm.profileBusy,
                )
            }
        }
        vm.profileError?.let { res ->
            Spacer(Modifier.height(8.dp))
            Text(stringResource(res), color = Red, fontFamily = Vt, fontSize = 17.sp)
        }

        Spacer(Modifier.height(16.dp))
        LevelCard(
            level = ach?.level ?: vm.level,
            sp = ach?.sp ?: vm.sp,
            from = ach?.levelFrom ?: 0,
            to = ach?.levelTo ?: 100,
        )
        Spacer(Modifier.height(10.dp))
        StatusGrid(
            ach?.stats?.get("library") ?: 0,
            ach?.stats?.get("completed") ?: 0,
            ach?.stats?.get("rated") ?: 0,
            vm.myComments.size,
        )
        val featured = ach?.items.orEmpty().filter { it.featured }
        if (featured.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FeaturedRow(featured.map { FeaturedAch(it.code, it.tier) })
        }
        Text(
            stringResource(
                R.string.profile_stats,
                ach?.stats?.get("library") ?: 0,
                ach?.stats?.get("completed") ?: 0,
                ach?.stats?.get("rated") ?: 0,
            ),
            color = Muted,
            fontFamily = Vt,
            fontSize = 17.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelButton(stringResource(R.string.ach_title), { onGo("achievements") }, accent = Amber)
            PixelButton(stringResource(R.string.stats_title), { onGo("stats") })
            PixelButton(
                stringResource(R.string.notifications_title) + if (vm.unread > 0) " (${vm.unread})" else "",
                { onGo("notifications") },
                accent = Pink,
            )
        }
        if (vm.role == "creator") {
            Spacer(Modifier.height(8.dp))
            PixelButton(stringResource(R.string.admin_title), { onGo("admin") }, accent = Red)
        }

        SectionTitle(stringResource(R.string.privacy_title))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PixelChip(stringResource(R.string.library_open), vm.libraryPublic, Green, onClick = { vm.updateLibraryPublic(true) })
            PixelChip(stringResource(R.string.library_closed), !vm.libraryPublic, Red, onClick = { vm.updateLibraryPublic(false) })
        }
        Text(
            stringResource(if (vm.libraryPublic) R.string.library_open_hint else R.string.library_closed_hint),
            color = Muted,
            fontFamily = Vt,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 6.dp),
        )

        SvagaIdSection(vm)

        SectionTitle(stringResource(R.string.profile_nick))
        PixelField(nick, { nick = it.take(40) }, stringResource(R.string.nick_hint))
        Spacer(Modifier.height(8.dp))
        PixelButton(
            text = stringResource(R.string.save),
            onClick = { vm.saveName(nick) },
            enabled = nick.trim() != vm.displayName,
        )

        SectionTitle(stringResource(R.string.language))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LANGUAGES.forEach { (code, label) ->
                PixelChip(label, Lang.code == code, Cyan, onClick = { Lang.save(context, code) })
            }
        }

        SectionTitle(stringResource(R.string.theme))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val modes = listOf(
                "system" to R.string.theme_system,
                "dark" to R.string.theme_dark,
                "light" to R.string.theme_light,
            )
            modes.forEach { (code, res) ->
                PixelChip(stringResource(res), ThemeMode.code == code, Cyan, onClick = { ThemeMode.save(context, code) })
            }
        }

        SectionTitle(stringResource(R.string.my_comments_title))
        if (vm.myComments.isEmpty()) {
            Text(stringResource(R.string.my_comments_empty), color = Muted, fontFamily = Vt, fontSize = 17.sp)
        } else {
            vm.myComments.forEach { c ->
                CommentRow(
                    c = c,
                    canDelete = true,
                    onLike = { vm.toggleLike(c) },
                    onDelete = { vm.deleteComment(c) },
                    onOpenGame = { onOpenGame(c.gameId) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        PixelButton(
            text = stringResource(R.string.logout),
            onClick = { vm.logout() },
            accent = Red,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionTitle(stringResource(R.string.about_title))
        Text(stringResource(R.string.about_text), color = Muted, fontSize = 13.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.data_credit), color = Muted, fontFamily = Vt, fontSize = 17.sp)
        Text(stringResource(R.string.version), color = Muted, fontFamily = Vt, fontSize = 17.sp)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun PlatformPicker(
    platforms: List<Platform>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit,
    showAll: Boolean = true,
) {
    var q by rememberSaveable { mutableStateOf("") }
    val needle = q.trim()
    val shown = platforms.filter {
        needle.isEmpty() || it.name.contains(needle, ignoreCase = true) || it.short.contains(needle, ignoreCase = true)
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .background(Bg)
                .border(2.dp, Cyan)
                .padding(12.dp),
        ) {
            Text(stringResource(R.string.all_platforms), color = Cyan, fontFamily = Pixel, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            PixelField(q, { q = it }, stringResource(R.string.platform_search))
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                if (showAll) {
                    item {
                        Text(
                            stringResource(R.string.filter_all),
                            color = if (selected == null) Cyan else Ink,
                            fontFamily = Vt,
                            fontSize = 19.sp,
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(null) }.padding(vertical = 10.dp),
                        )
                    }
                }
                items(shown, key = { it.id }) { p ->
                    Text(
                        p.name,
                        color = if (p.id == selected) Cyan else Ink,
                        fontFamily = Vt,
                        fontSize = 19.sp,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(p.id) }.padding(vertical = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            PixelButton(stringResource(R.string.close), onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ======================= comments feed =======================

@Composable
fun FeedScreen(vm: AppViewModel, onOpenGame: (Int) -> Unit, onOpenUser: (String) -> Unit) {
    LaunchedEffect(Unit) { vm.loadFeed() }
    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.feed_title),
            color = Cyan,
            fontFamily = Pixel,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 60.dp, bottom = 8.dp),
        )
        if (vm.feed.isEmpty()) {
            CenterText(stringResource(if (vm.feedLoading) R.string.loading else R.string.feed_empty))
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxSize()) {
                items(vm.feed, key = { it.id }) { c ->
                    CommentRow(
                        c = c,
                        canDelete = c.userId == vm.userId || vm.role == "creator",
                        onLike = { vm.toggleLike(c) },
                        onDelete = { vm.deleteComment(c) },
                        onOpenGame = { onOpenGame(c.gameId) },
                        onOpenUser = { onOpenUser(c.userId) },
                    )
                }
                item {
                    if (vm.feedMore) {
                        PixelButton(
                            stringResource(if (vm.feedLoading) R.string.loading else R.string.load_more),
                            { vm.loadFeed(append = true) },
                            enabled = !vm.feedLoading,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

// ======================= people =======================

@Composable
fun PersonRow(p: Person, onClick: () -> Unit, onAction: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .border(2.dp, Line)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(p.id, p.avatarVersion, p.name, 48.dp)
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(p.name, color = Ink, fontFamily = Vt, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.level_n, p.level) + " · " + stringResource(R.string.people_games, p.library, p.completed),
                color = Muted,
                fontFamily = Vt,
                fontSize = 15.sp,
            )
        }
        when (p.relation) {
            "none" -> PixelChip(stringResource(R.string.friend_add), false, Cyan, onClick = { onAction("add") })
            "incoming" -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PixelChip("✓", true, Green, onClick = { onAction("accept") })
                PixelChip("✕", false, Red, onClick = { onAction("remove") })
            }
            "outgoing" -> Text(stringResource(R.string.friend_sent), color = Muted, fontFamily = Vt, fontSize = 15.sp)
            "friends" -> Text("♥", color = Pink, fontFamily = Vt, fontSize = 22.sp)
            else -> Spacer(Modifier.width(1.dp))
        }
    }
}

@Composable
fun PeopleScreen(vm: AppViewModel, onOpenUser: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) {
        vm.loadFriends()
        vm.searchPeople(query)
    }
    val requests = vm.friends.incoming + vm.friends.outgoing
    val list = when (tab) {
        0 -> vm.friends.friends
        1 -> requests
        else -> vm.people
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.tab_people),
            color = Cyan,
            fontFamily = Pixel,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 60.dp),
        )
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PixelChip(
                stringResource(R.string.people_friends) + " " + vm.friends.friends.size,
                tab == 0,
                Cyan,
                onClick = { tab = 0 },
            )
            PixelChip(
                stringResource(R.string.people_requests) + " " + vm.friends.incoming.size,
                tab == 1,
                Pink,
                onClick = { tab = 1 },
            )
            PixelChip(stringResource(R.string.people_all), tab == 2, Amber, onClick = { tab = 2 })
        }
        if (tab == 2) {
            PixelField(
                value = query,
                onChange = {
                    query = it
                    vm.searchPeople(it)
                },
                label = stringResource(R.string.people_search),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        if (list.isEmpty()) {
            CenterText(stringResource(R.string.people_empty))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(list, key = { it.id }) { p ->
                    PersonRow(p, onClick = { onOpenUser(p.id) }, onAction = { vm.friendAction(p.id, it) })
                }
            }
        }
    }
}

@Composable
fun UserProfileScreen(vm: AppViewModel, id: String, onBack: () -> Unit, onOpenGame: (Int) -> Unit) {
    LaunchedEffect(id) { vm.loadProfileOf(id) }
    val prof = vm.viewedProfile
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
    ) {
        PixelButton("< " + stringResource(R.string.back), onBack)
        if (prof == null || prof.person.id != id) {
            Text(
                stringResource(R.string.loading),
                color = Muted,
                fontFamily = Vt,
                fontSize = 19.sp,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            val p = prof.person
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(p.id, p.avatarVersion, p.name, 96.dp)
                Column(Modifier.padding(start = 14.dp)) {
                    Text(p.name, color = Ink, fontFamily = Vt, fontSize = 22.sp)
                    if (p.creator) Text(stringResource(R.string.creator_badge), color = Amber, fontFamily = Pixel, fontSize = 8.sp)
                    if (p.createdAt.isNotBlank()) {
                        Text(
                            stringResource(R.string.profile_since, formatDate(p.createdAt)),
                            color = Muted,
                            fontFamily = Vt,
                            fontSize = 17.sp,
                        )
                    }
                    Text(
                        stringResource(R.string.people_games, p.library, p.completed),
                        color = Muted,
                        fontFamily = Vt,
                        fontSize = 17.sp,
                    )
                    Text(
                        stringResource(R.string.user_achievements, prof.achievements),
                        color = Amber,
                        fontFamily = Vt,
                        fontSize = 17.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LevelCard(prof.level, prof.sp, prof.levelFrom, prof.levelTo)
            if (prof.featured.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FeaturedRow(prof.featured)
            }
            Spacer(Modifier.height(14.dp))
            when (p.relation) {
                "none" -> PixelButton(stringResource(R.string.friend_add), { vm.friendAction(p.id, "add") }, filled = true)
                "outgoing" -> {
                    Text(stringResource(R.string.friend_sent), color = Muted, fontFamily = Vt, fontSize = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    PixelButton(stringResource(R.string.friend_cancel), { vm.friendAction(p.id, "remove") }, accent = Red)
                }
                "incoming" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PixelButton(stringResource(R.string.friend_accept), { vm.friendAction(p.id, "accept") }, accent = Green, filled = true)
                    PixelButton(stringResource(R.string.friend_decline), { vm.friendAction(p.id, "remove") }, accent = Red)
                }
                "friends" -> {
                    Text("♥ " + stringResource(R.string.friend_in), color = Pink, fontFamily = Vt, fontSize = 19.sp)
                    Spacer(Modifier.height(8.dp))
                    PixelButton(stringResource(R.string.friend_remove), { vm.friendAction(p.id, "remove") }, accent = Red)
                }
                else -> Spacer(Modifier.height(1.dp))
            }
            SectionTitle(stringResource(R.string.tab_library))
            val lib = prof.library
            if (lib == null) {
                Text(stringResource(R.string.user_library_hidden), color = Muted, fontFamily = Vt, fontSize = 17.sp)
            } else if (lib.isEmpty()) {
                Text(stringResource(R.string.library_empty), color = Muted, fontFamily = Vt, fontSize = 17.sp)
            } else {
                lib.forEach { item ->
                    Box(Modifier.padding(bottom = 10.dp)) {
                        LibraryRow(item, vm.gamesCache[item.gameId]) { onOpenGame(item.gameId) }
                    }
                }
            }
            SectionTitle(stringResource(R.string.comments_title))
            if (vm.viewedComments.isEmpty()) {
                Text(stringResource(R.string.user_comments_empty), color = Muted, fontFamily = Vt, fontSize = 17.sp)
            } else {
                vm.viewedComments.forEach { c ->
                    CommentRow(
                        c = c,
                        canDelete = vm.role == "creator",
                        onLike = { vm.toggleLike(c) },
                        onDelete = { vm.deleteComment(c) },
                        onOpenGame = { onOpenGame(c.gameId) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}


@Composable
fun StatusGrid(lib: Int, done: Int, rated: Int, comments: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubCell(lib, R.string.hub_library, Cyan, Modifier.weight(1f))
            HubCell(done, R.string.hub_completed, Green, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubCell(rated, R.string.hub_rated, Amber, Modifier.weight(1f))
            HubCell(comments, R.string.hub_comments, Pink, Modifier.weight(1f))
        }
    }
}

@Composable
fun HubCell(value: Int, label: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(Panel, RoundedCornerShape(12.dp))
            .border(2.dp, Line, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$value", color = color, fontFamily = Pixel, fontSize = 22.sp)
        Text(stringResource(label), color = Muted, fontFamily = Vt, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    }
}
