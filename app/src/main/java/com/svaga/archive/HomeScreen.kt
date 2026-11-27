package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class GenreLook(val id: Int, val emoji: String, val a: Long, val b: Long)

private val HOME_GENRES = listOf(
    GenreLook(12, "⚔️", 0xFF3A2A7A, 0xFF0F1530),
    GenreLook(8, "🍄", 0xFF7A2A3A, 0xFF1A0F18),
    GenreLook(31, "🗺️", 0xFF1F5A4A, 0xFF0B1A16),
    GenreLook(4, "🥊", 0xFF7A3A1F, 0xFF1A100A),
    GenreLook(5, "🎯", 0xFF2A4A7A, 0xFF0B1220),
    GenreLook(10, "🏎️", 0xFF6A1F5A, 0xFF180A16),
    GenreLook(9, "🧩", 0xFF1F4A7A, 0xFF0A1420),
    GenreLook(15, "♟️", 0xFF4A4A1F, 0xFF16160A),
    GenreLook(33, "👾", 0xFF2A1F7A, 0xFF0D0A20),
    GenreLook(14, "⚽", 0xFF1F7A3A, 0xFF0A1A10),
)

// ======================= home =======================

@Composable
fun HomeScreen(vm: AppViewModel, nav: Nav, openLibrary: (String) -> Unit, openCalendar: () -> Unit) {
    LaunchedEffect(Unit) {
        vm.loadHomeRows()
        if (vm.upcoming.isEmpty()) vm.loadUpcoming()
        vm.extras.loadSeasons()
        if (vm.extras.recs.isEmpty()) vm.extras.loadRecs()
    }
    val playing = vm.library.values.filter { it.status == "playing" }.sortedByDescending { it.updatedAt }.mapNotNull { vm.gamesCache[it.gameId] }
    val today = LocalDate.now()
    val soon = vm.upcoming.filter { g -> releaseDateOf(g)?.let { !it.isBefore(today) } ?: false }.take(12)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = BottomBarSpace)) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Logo(Modifier.weight(1f), size = 16.sp)
                IconBtn(Icons.Filled.Notifications, { nav.go(Routes.NOTIFICATIONS) }, badge = vm.unread)
            }
        }
        item {
            SearchBar("", {}, stringResource(R.string.search_hint), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), onClick = { nav.go(Routes.CATALOG) })
        }
        item {
            ChipRow(Modifier.padding(vertical = 6.dp)) {
                SECTION_KEYS.forEach { key ->
                    SvChip(sectionLabel(key), false, {
                        vm.selectSection(key)
                        nav.go(Routes.CATALOG)
                    })
                }
                SvChip(stringResource(R.string.platforms_screen), false, { nav.go(Routes.PLATFORMS) }, accent = Amber)
            }
        }
        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(HOME_GENRES, key = { it.id }) { g ->
                    val label = GENRE_FILTERS.firstOrNull { it.id == g.id }?.label() ?: ""
                    GenreTile(label, g.emoji, listOf(Color(g.a), Color(g.b)), onClick = {
                        vm.applyFilter(GameFilter(genre = g.id))
                        nav.go(Routes.CATALOG)
                    })
                }
            }
        }
        item { SeasonCards(vm, nav) }
        item(key = "play") { HomePlayBlock(vm, nav) }
        if (playing.isNotEmpty()) {
            item {
                GameShelf(stringResource(R.string.home_playing), playing, vm, nav::game, Modifier.padding(top = 8.dp), onMore = { openLibrary("playing") })
            }
        }
        if (soon.isNotEmpty()) {
            item {
                SectionHeader(stringResource(R.string.calendar_title), Modifier.padding(top = 8.dp), onMore = openCalendar)
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(soon, key = { it.id }) { g -> ReleaseCard(g, vm.library[g.id]?.status) { nav.game(g.id) } }
                }
            }
        }
        val recs = vm.extras.recs.filter { vm.library[it.id]?.status == null }
        if (recs.isNotEmpty()) {
            item(key = "recs") {
                GameShelf(stringResource(R.string.recs_title), recs, vm, nav::game, Modifier.padding(top = 8.dp))
            }
        }
        listOf("popular", "svaga", "new", "top", "retro").forEach { key ->
            val list = vm.homeRows[key].orEmpty()
            if (list.isNotEmpty()) {
                item(key = "row_$key") {
                    GameShelf(sectionLabel(key), list.take(20), vm, nav::game, Modifier.padding(top = 8.dp), onMore = {
                        vm.selectSection(key)
                        nav.go(Routes.CATALOG)
                    })
                }
            }
        }
        if (vm.homeRows.isEmpty()) {
            item {
                if (vm.homeRowsError) ErrorState(stringResource(R.string.err_network), { vm.loadHomeRows(force = true) }) else LoadingBox()
            }
        }
        item {
            SvCard(Modifier.fillMaxWidth().padding(16.dp), onClick = { nav.go(Routes.ADD_GAME) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconDot(Icons.Filled.Add, Green)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Body(stringResource(R.string.add_game_missing), weight = FontWeight.SemiBold)
                        Caption(stringResource(R.string.add_game_hint))
                    }
                }
            }
        }
    }
}

/** Wide card of an upcoming release with a date badge (like "25 нояб · Новинка"). */
@Composable
fun ReleaseCard(g: Game, status: String?, onClick: () -> Unit) {
    val date = releaseDateOf(g)
    Column(Modifier.width(260.dp).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().height(150.dp).clip(CardShape)) {
            Cover(g.screenshots.firstOrNull() ?: g.cover, Modifier.fillMaxSize(), RoundedCornerShape(20.dp))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xAA000000)))))
            if (date != null) {
                Column(
                    Modifier.padding(10.dp).background(Color(0xCC0A0C11), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(date.dayOfMonth.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(date.format(DateTimeFormatter.ofPattern("MMM", Locale(Lang.code))).trimEnd('.'), color = Color.White, fontSize = 12.sp)
                }
            }
            if (status != null) Pill(statusLabel(status), statusColor(status), Modifier.align(Alignment.BottomEnd).padding(10.dp), filled = true)
        }
        Spacer(Modifier.height(6.dp))
        Body(g.name, size = 16.sp, weight = FontWeight.SemiBold, maxLines = 1)
        val plats = g.platforms.take(3).joinToString(", ")
        if (plats.isNotBlank()) Caption(plats, maxLines = 1)
    }
}

// ======================= catalog and search =======================

@Composable
fun CatalogScreen(vm: AppViewModel, nav: Nav) {
    var query by rememberSaveable { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (!vm.browseLoaded && !vm.browseLoading) vm.loadBrowse()
    }
    val searching = query.trim().length >= 2
    val base: List<Game>? = if (searching) vm.searchResults?.filter { it.matches(vm.filter) } else vm.browse
    val list: List<Game>? = if (vm.hideAdded) base?.filter { vm.library[it.id]?.status == null } else base
    val busy = if (searching) vm.searching else vm.browseLoading
    val active = vm.filter.active || vm.hideAdded

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.AutoMirrored.Filled.ArrowBack, nav::back)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                SearchBar(query, {
                    query = it
                    vm.search(it)
                }, stringResource(R.string.search_hint), Modifier.focusRequester(focus), trailing = {
                    if (query.isNotEmpty()) {
                        IconBtn(Icons.Filled.Close, { query = ""; vm.search("") }, size = 32.dp, filled = false, tint = Muted)
                    }
                })
            }
        }
        ChipRow {
            if (!searching) {
                SECTION_KEYS.forEach { key -> SvChip(sectionLabel(key), vm.section == key, { vm.selectSection(key) }) }
            }
            SvChip(stringResource(R.string.filters) + if (active) " •" else "", active, { showFilters = true }, accent = Pink)
            SvChip(stringResource(if (vm.gridMode) R.string.view_list else R.string.view_grid), false, { vm.toggleGrid() }, accent = Blue)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                list.isNullOrEmpty() && vm.listError -> ErrorState(stringResource(vm.listErrorRes), { if (searching) vm.search(query) else vm.loadBrowse() })
                list.isNullOrEmpty() && (busy || (searching && list == null) || (!searching && !vm.browseLoaded)) -> LoadingBox()
                list.isNullOrEmpty() -> EmptyState(
                    stringResource(R.string.no_results),
                    hint = stringResource(R.string.add_game_hint),
                    action = stringResource(R.string.add_game_missing),
                    onAction = { nav.go(Routes.ADD_GAME) },
                )
                vm.gridMode -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(108.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(list.orEmpty(), key = { it.id }) { g ->
                        val e = vm.library[g.id]
                        PosterCard(g, { nav.game(g.id) }, status = e?.status, myRating = e?.rating)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CatalogFooter(vm, searching, nav)
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(list.orEmpty(), key = { it.id }) { g ->
                        val e = vm.library[g.id]
                        GameRow(g, g.name, { nav.game(g.id) }, status = e?.status, trailing = {
                            g.rating?.let { StarBadge(String.format(Locale.US, "%.1f", it / 10.0)) }
                        })
                    }
                    item { CatalogFooter(vm, searching, nav) }
                }
            }
        }
    }
    if (showFilters) {
        FilterSheet(
            filter = vm.filter,
            platforms = vm.platforms,
            pinned = vm.pinnedPlatforms,
            onChange = { vm.applyFilter(it) },
            onDismiss = { showFilters = false },
            extra = {
                Text(stringResource(R.string.library_title), color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp))
                ChipRow { SvChip(stringResource(R.string.hide_added), vm.hideAdded, { vm.updateHideAdded(!vm.hideAdded) }, accent = Green) }
            },
        )
    }
}

@Composable
private fun CatalogFooter(vm: AppViewModel, searching: Boolean, nav: Nav) {
    Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (!searching && vm.canLoadMore) {
            SvButton(stringResource(R.string.load_more), { vm.loadBrowse(append = true) }, Modifier.fillMaxWidth(), kind = BtnKind.Secondary, busy = vm.browseLoading)
            Spacer(Modifier.height(10.dp))
        }
        Body(
            stringResource(R.string.add_game_missing),
            Modifier.clip(RoundedCornerShape(12.dp)).clickable { nav.go(Routes.ADD_GAME) }.padding(10.dp),
            color = Cyan,
            size = 14.sp,
            weight = FontWeight.SemiBold,
        )
    }
}
