package com.svaga.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LIB_KEYS = listOf("all", "playing", "backlog", "completed", "dropped", "wont_play", "fav")

@Composable
fun LibraryScreen(vm: AppViewModel, nav: Nav, status: String, onStatus: (String) -> Unit) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableIntStateOf(0) } // 0 recent, 1 name, 2 my rating, 3 year
    var showFilters by remember { mutableStateOf(false) }
    val f = vm.libFilter
    val all = vm.library.values.filter { it.status != null || it.favorite }
    val counts = LIB_KEYS.associateWith { key ->
        when (key) {
            "all" -> all.size
            "fav" -> all.count { it.favorite }
            else -> all.count { it.status == key }
        }
    }
    val needle = query.trim()
    val entries = all
        .filter {
            when (status) {
                "all" -> true
                "fav" -> it.favorite
                else -> it.status == status
            }
        }
        .filter { item -> !f.active || (vm.gamesCache[item.gameId]?.matches(f) ?: false) }
        .filter { item -> needle.isEmpty() || (vm.gamesCache[item.gameId]?.name?.contains(needle, true) ?: false) }
        .let { list ->
            when (sort) {
                1 -> list.sortedBy { (vm.gamesCache[it.gameId]?.name ?: "").lowercase() }
                2 -> list.sortedByDescending { it.rating ?: -1 }
                3 -> list.sortedByDescending { vm.gamesCache[it.gameId]?.year ?: 0 }
                else -> list.sortedByDescending { it.updatedAt }
            }
        }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                H1(stringResource(R.string.library_title))
                Caption(stringResource(R.string.library_subtitle))
            }
            IconBtn(Icons.Filled.Search, { searchOpen = !searchOpen; if (!searchOpen) query = "" }, tint = if (searchOpen) Cyan else Ink)
            Spacer(Modifier.width(8.dp))
            IconBtn(Icons.Filled.Menu, { showFilters = true }, tint = if (f.active) Pink else Ink)
        }
        if (searchOpen) {
            SearchBar(query, { query = it }, stringResource(R.string.library_search), Modifier.padding(horizontal = 16.dp, vertical = 8.dp), trailing = {
                if (query.isNotEmpty()) IconBtn(Icons.Filled.Close, { query = "" }, size = 32.dp, filled = false, tint = Muted)
            })
        }
        Spacer(Modifier.height(8.dp))
        ChipRow {
            LIB_KEYS.forEach { key ->
                SvChip(statusLabel(key), status == key, { onStatus(key) }, accent = statusColor(key), count = counts[key])
            }
        }
        ChipRow(Modifier.padding(top = 2.dp)) {
            listOf(R.string.sort_recent, R.string.sort_name, R.string.sort_my_rating, R.string.sort_year).forEachIndexed { i, r ->
                SvChip(stringResource(r), sort == i, { sort = i }, accent = Blue)
            }
            SvChip(stringResource(if (vm.gridMode) R.string.view_list else R.string.view_grid), false, { vm.toggleGrid() }, accent = Muted)
        }
        Spacer(Modifier.height(6.dp))
        if (entries.isEmpty()) {
            EmptyState(
                stringResource(R.string.library_empty_title),
                hint = stringResource(R.string.library_empty),
                action = stringResource(R.string.open_catalog),
                onAction = { nav.go(Routes.CATALOG) },
            )
        } else if (vm.gridMode) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(108.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomBarSpace),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Text(stringResource(R.string.games_count, entries.size), color = Muted, fontSize = 13.sp)
                        BacklogForecastLine(vm)
                    }
                }
                items(entries, key = { it.gameId }) { item ->
                    val g = vm.gamesCache[item.gameId]
                    if (g != null) {
                        PosterCard(g, { nav.game(item.gameId) }, status = if (status == "all" || status == "fav") item.status else null, myRating = item.rating)
                    } else {
                        Box(Modifier.fillMaxWidth().aspectRatio(0.75f)) { LoadingBox() }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 4.dp, bottom = BottomBarSpace)) {
                items(entries, key = { it.gameId }) { item ->
                    val g = vm.gamesCache[item.gameId]
                    val play = vm.playtimeByGame[item.gameId]
                    GameRow(
                        g, "#${item.gameId}", { nav.game(item.gameId) },
                        subtitle = listOfNotNull(
                            g?.year?.toString(),
                            g?.platforms?.firstOrNull(),
                            play?.let { stringResource(R.string.hours_short, hours(it)) },
                        ).joinToString(" · "),
                        status = item.status,
                        trailing = { item.rating?.let { StarBadge(it.toString(), color = Cyan) } },
                    )
                }
            }
        }
    }
    if (showFilters) {
        FilterSheet(vm.libFilter, vm.platforms, vm.pinnedPlatforms, { vm.applyLibFilter(it) }, { showFilters = false })
    }
}

