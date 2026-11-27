package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/** Feed: news from Svaga, what friends do, everyone's comments, people search. */
@Composable
fun FeedScreen(vm: AppViewModel, nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(tab) {
        when (tab) {
            0 -> if (!vm.newsLoaded) vm.loadNews()
            1 -> {
                vm.loadFriends()
                vm.loadFriendFeed()
            }
            2 -> if (vm.feed.isEmpty()) vm.loadFeed()
        }
    }
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(10.dp))
        UnderlineTabs(
            listOf(
                stringResource(R.string.feed_news),
                stringResource(R.string.people_friends),
                stringResource(R.string.comments_title),
                stringResource(R.string.tab_people),
            ),
            tab,
            { tab = it },
            badges = listOf(0, vm.friends.incoming.size, 0, 0),
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(Stroke))
        when (tab) {
            0 -> NewsTab(vm, nav)
            1 -> FriendsTab(vm, nav) { tab = 3 }
            2 -> CommentsTab(vm, nav)
            else -> PeopleList(vm, nav, Modifier.weight(1f))
        }
    }
}

@Composable
private fun NewsTab(vm: AppViewModel, nav: Nav) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = BottomBarSpace)) {
        if (vm.role == "creator") {
            item {
                SvButton(stringResource(R.string.news_new), { nav.go(Routes.NEWS_NEW) }, Modifier.fillMaxWidth().padding(16.dp), icon = Icons.Filled.Add)
            }
        }
        if (vm.news.isEmpty()) {
            item {
                if (vm.newsLoading || !vm.newsLoaded) LoadingBox() else EmptyState(stringResource(R.string.news_empty), hint = stringResource(R.string.news_empty_hint))
            }
        }
        items(vm.news, key = { it.id }) { n -> NewsCard(n, vm, nav) }
        if (vm.newsMore) {
            item {
                SvButton(stringResource(R.string.load_more), { vm.loadNews(append = true) }, Modifier.fillMaxWidth().padding(16.dp), kind = BtnKind.Secondary, busy = vm.newsLoading)
            }
        }
    }
}

@Composable
private fun NewsCard(n: NewsItem, vm: AppViewModel, nav: Nav) {
    var expanded by rememberSaveable(n.id) { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 12.dp)) {
        H2(n.title)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Caption(relativeTime(n.createdAt), maxLines = 1)
            if (!n.author.isNullOrBlank()) {
                Spacer(Modifier.width(12.dp))
                Text(n.author, color = Cyan, fontSize = 13.sp, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            if (vm.role == "creator") Icon(Icons.Filled.Delete, null, tint = Faint, modifier = Modifier.size(20.dp).clickable { confirm = true })
        }
        if (!n.imageUrl.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = n.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(CardShape).background(Panel2),
            )
        }
        if (n.body.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Body(n.body, size = 15.sp, maxLines = if (expanded) Int.MAX_VALUE else 5)
        }
        val gid = n.gameId
        if (gid != null) {
            Spacer(Modifier.height(8.dp))
            SvButton(stringResource(R.string.open_game), { nav.game(gid) }, kind = BtnKind.Secondary)
        }
    }
    Divider1()
    if (confirm) ConfirmDialog(stringResource(R.string.news_delete), n.title, { confirm = false; vm.deleteNews(n.id) }, { confirm = false })
}

@Composable
private fun FriendsTab(vm: AppViewModel, nav: Nav, openPeople: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = BottomBarSpace)) {
        if (vm.friends.incoming.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.people_requests), count = vm.friends.incoming.size) }
            items(vm.friends.incoming, key = { "req_" + it.id }) { p ->
                PersonRow(p, { nav.user(p.id) }, { vm.friendAction(p.id, it) })
            }
            item { Divider1(Modifier.padding(vertical = 8.dp)) }
        }
        if (vm.friendFeed.isEmpty()) {
            item {
                when {
                    vm.friendFeedLoading || !vm.friendFeedLoaded -> LoadingBox()
                    vm.friends.friends.isEmpty() -> EmptyState(
                        stringResource(R.string.friends_feed_no_friends),
                        hint = stringResource(R.string.friends_feed_hint),
                        action = stringResource(R.string.find_friends),
                        onAction = openPeople,
                    )
                    else -> EmptyState(stringResource(R.string.feed_empty))
                }
            }
        }
        items(vm.friendFeed, key = { "${it.userId}_${it.gameId}_${it.at}" }) { e -> FriendEventRow(e, vm, nav) }
        if (vm.friendFeedMore) {
            item {
                SvButton(stringResource(R.string.load_more), { vm.loadFriendFeed(append = true) }, Modifier.fillMaxWidth().padding(16.dp), kind = BtnKind.Secondary, busy = vm.friendFeedLoading)
            }
        }
    }
}

@Composable
private fun FriendEventRow(e: FriendEvent, vm: AppViewModel, nav: Nav) {
    val g = vm.gamesCache[e.gameId]
    Row(Modifier.fillMaxWidth().clickable { nav.game(e.gameId) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Avatar(e.userId, e.avatarVersion, e.name, 42.dp, Modifier.clickable { nav.user(e.userId) })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.name, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                NickBadge(e.badge)
                Spacer(Modifier.width(8.dp))
                Caption(relativeTime(e.at), maxLines = 1)
            }
            val action = when {
                e.status != null -> stringResource(R.string.ev_status, statusLabel(e.status))
                e.favorite -> stringResource(R.string.ev_favorite)
                else -> stringResource(R.string.ev_rated)
            }
            Caption(action, color = if (e.status != null) statusColor(e.status) else Muted, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(g?.cover, Modifier.width(44.dp).aspectRatio(0.75f), RoundedCornerShape(8.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Body(g?.name ?: "#${e.gameId}", size = 15.sp, weight = FontWeight.SemiBold, maxLines = 2)
                    g?.year?.let { Caption(it.toString(), maxLines = 1) }
                }
                if (e.favorite) Icon(Icons.Filled.Favorite, null, tint = Pink, modifier = Modifier.size(18.dp))
                e.rating?.let {
                    Spacer(Modifier.width(6.dp))
                    StarBadge(it.toString(), color = Cyan)
                }
            }
        }
    }
}

@Composable
private fun CommentsTab(vm: AppViewModel, nav: Nav) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (vm.feed.isEmpty()) {
            item { if (vm.feedLoading) LoadingBox() else EmptyState(stringResource(R.string.feed_empty)) }
        }
        items(vm.feed, key = { it.id }) { c ->
            CommentCard(
                c,
                canDelete = c.userId == vm.userId || vm.role == "creator",
                onLike = { vm.toggleLike(c) },
                onDelete = { vm.deleteComment(c) },
                onOpenUser = { nav.user(c.userId) },
                onOpenGame = { nav.game(c.gameId) },
            )
        }
        if (vm.feedMore) {
            item {
                SvButton(stringResource(R.string.load_more), { vm.loadFeed(append = true) }, Modifier.fillMaxWidth(), kind = BtnKind.Secondary, busy = vm.feedLoading)
            }
        }
    }
}

// ======================= people =======================

@Composable
fun PersonRow(p: Person, onClick: () -> Unit, onAction: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(p.id, p.avatarVersion, p.name, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Body(p.name, size = 16.sp, weight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                NickBadge(p.badge)
                if (p.creator) {
                    Spacer(Modifier.width(6.dp))
                    Pill(stringResource(R.string.creator_badge), Amber)
                }
            }
            Caption(stringResource(R.string.level_n, p.level) + " · " + stringResource(R.string.people_games, p.library, p.completed), maxLines = 1)
        }
        when (p.relation) {
            "none" -> SvChip(stringResource(R.string.friend_add), false, { onAction("add") })
            "incoming" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconBtn(Icons.Filled.Check, { onAction("accept") }, tint = Green, size = 38.dp)
                IconBtn(Icons.Filled.Close, { onAction("remove") }, tint = Red, size = 38.dp)
            }
            "outgoing" -> Caption(stringResource(R.string.friend_sent), maxLines = 1)
            "friends" -> Icon(Icons.Filled.Favorite, null, tint = Pink)
            else -> Spacer(Modifier.width(1.dp))
        }
    }
}

/** Friends, requests and search across all players. */
@Composable
fun PeopleList(vm: AppViewModel, nav: Nav, modifier: Modifier = Modifier) {
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
    Column(modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        ChipRow {
            SvChip(stringResource(R.string.people_friends), tab == 0, { tab = 0 }, count = vm.friends.friends.size)
            SvChip(stringResource(R.string.people_requests), tab == 1, { tab = 1 }, accent = Pink, count = vm.friends.incoming.size)
            SvChip(stringResource(R.string.people_all), tab == 2, { tab = 2 }, accent = Amber)
        }
        if (tab == 2) {
            SearchBar(query, {
                query = it
                vm.searchPeople(it)
            }, stringResource(R.string.people_search), Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        if (list.isEmpty()) {
            EmptyState(
                stringResource(R.string.people_empty),
                action = if (tab != 2) stringResource(R.string.find_friends) else null,
                onAction = { tab = 2 },
            )
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomBarSpace)) {
                items(list, key = { it.id }) { p ->
                    PersonRow(p, { nav.user(p.id) }, { vm.friendAction(p.id, it) })
                }
            }
        }
    }
}

/** Stand-alone people screen (from the profile). */
@Composable
fun PeopleScreen(vm: AppViewModel, nav: Nav) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(stringResource(R.string.tab_people), nav::back)
        PeopleList(vm, nav, Modifier.weight(1f))
    }
}

