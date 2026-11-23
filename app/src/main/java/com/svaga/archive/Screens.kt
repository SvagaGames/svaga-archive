package com.svaga.archive

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// ======================= root =======================

@Composable
fun App(vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    var crash by remember { mutableStateOf(CrashLog.read(context)) }
    val crashText = crash
    Box(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            if (crashText != null) {
                CrashScreen(crashText) {
                    CrashLog.clear(context)
                    crash = null
                }
            } else if (!vm.loggedIn) {
                AuthScreen(vm)
            } else {
                MainNav(vm)
            }
            Column(Modifier.align(Alignment.TopCenter)) {
                val t = vm.toast
                if (t != null) {
                    LaunchedEffect(t) {
                        delay(3800)
                        vm.dismissToast()
                    }
                    AchievementToast(t) { vm.dismissToast() }
                }
                val n = vm.notice
                if (n != null) {
                    LaunchedEffect(n) {
                        delay(3500)
                        vm.dismissNotice()
                    }
                    NoticeBar(stringResource(n)) { vm.dismissNotice() }
                }
            }
        }
    }
}

@Composable
fun NoticeBar(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = Ink,
        fontFamily = Vt,
        fontSize = 18.sp,
        modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .background(Panel)
            .border(2.dp, Cyan)
            .clickable(onClick = onClick)
            .padding(12.dp),
    )
}

@Composable
fun AchievementToast(t: UiToast, onClick: () -> Unit) {
    val isLevel = t.kind == "level_up"
    val title = if (isLevel) stringResource(R.string.level_up_title) else stringResource(R.string.ach_unlocked)
    val name = if (isLevel) levelName(t.level) else achTitle(t.code) + " " + ROMAN.getOrElse(t.tier) { "" }
    Row(
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .background(Panel)
            .border(2.dp, if (isLevel) Cyan else if (t.tier > 0) tierColor(t.tier) else Amber)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("★", color = if (isLevel) Cyan else Amber, fontSize = 30.sp)
        Column(Modifier.padding(start = 12.dp)) {
            Text(title, color = Amber, fontFamily = Pixel, fontSize = 9.sp)
            Spacer(Modifier.height(4.dp))
            Text(name.trim(), color = Ink, fontFamily = Vt, fontSize = 19.sp)
            if (!isLevel && t.sp > 0) Text("+${t.sp} SP", color = Green, fontFamily = Vt, fontSize = 16.sp)
        }
    }
}

@Composable
fun CrashScreen(text: String, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text(stringResource(R.string.crash_title), color = Red, fontFamily = Pixel, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            text,
            color = Ink,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        )
        Spacer(Modifier.height(10.dp))
        PixelButton(stringResource(R.string.crash_close), onClose, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun MainNav(vm: AppViewModel) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "main") {
        composable("main") {
            MainScreen(
                vm,
                onOpen = { id -> nav.navigate("game/$id") },
                onOpenUser = { uid -> nav.navigate("user/$uid") },
                onGo = { route -> nav.navigate(route) },
            )
        }
        composable(
            route = "game/{id}",
            arguments = listOf(navArgument("id") { type = NavType.IntType }),
        ) { entry ->
            val id = entry.arguments?.getInt("id") ?: 0
            GameDetailScreen(
                vm, id,
                onBack = { nav.popBackStack() },
                onOpenGame = { gid -> nav.navigate("game/$gid") },
            )
        }
        composable(
            route = "user/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val uid = entry.arguments?.getString("id").orEmpty()
            UserProfileScreen(
                vm,
                uid,
                onBack = { nav.popBackStack() },
                onOpenGame = { gid -> nav.navigate("game/$gid") },
            )
        }
        composable("achievements") { AchievementsScreen(vm, onBack = { nav.popBackStack() }) }
        composable("notifications") {
            NotificationsScreen(
                vm,
                onBack = { nav.popBackStack() },
                onOpenUser = { uid -> nav.navigate("user/$uid") },
                onOpenGame = { gid -> nav.navigate("game/$gid") },
                onOpenAchievements = { nav.navigate("achievements") },
            )
        }
        composable("stats") { StatsScreen(vm, onBack = { nav.popBackStack() }) }
        composable("platforms") {
            PlatformsScreen(
                vm,
                onBack = { nav.popBackStack() },
                onPick = { id ->
                    vm.setPlatform(id)
                    nav.popBackStack()
                },
            )
        }
        composable("addgame") {
            AddGameScreen(
                vm,
                onBack = { nav.popBackStack() },
                onOpenGame = { gid -> nav.navigate("game/$gid") },
                onCreated = { gid ->
                    nav.popBackStack()
                    nav.navigate("game/$gid")
                },
            )
        }
        composable("admin") { AdminScreen(vm, onBack = { nav.popBackStack() }) }
    }
}

@Composable
fun BellButton(vm: AppViewModel, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.clickable(onClick = onClick).padding(6.dp)) {
        Box(Modifier.background(Panel2).border(2.dp, if (vm.unread > 0) Amber else Line).padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("🔔", fontSize = 16.sp)
        }
        if (vm.unread > 0) {
            Text(
                if (vm.unread > 99) "99+" else vm.unread.toString(),
                color = Bg,
                fontFamily = Pixel,
                fontSize = 7.sp,
                modifier = Modifier.align(Alignment.TopEnd).background(Red).padding(horizontal = 3.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun MainScreen(vm: AppViewModel, onOpen: (Int) -> Unit, onOpenUser: (String) -> Unit, onGo: (String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = tab != 0) { tab = 0 }
    Column(Modifier.fillMaxSize().background(Bg)) {
        if (vm.offline || vm.pendingCount > 0) {
            Text(
                if (vm.pendingCount > 0) stringResource(R.string.offline_pending, vm.pendingCount) else stringResource(R.string.offline_banner),
                color = Bg,
                fontFamily = Vt,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(Amber).padding(vertical = 3.dp, horizontal = 8.dp),
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> HomeScreen(vm, onOpen, onGo)
                1 -> LibraryScreen(vm, onOpen)
                2 -> FeedScreen(vm, onOpen, onOpenUser)
                3 -> PeopleScreen(vm, onOpenUser)
                else -> ProfileScreen(vm, onOpen, onGo)
            }
            BellButton(vm, onClick = { onGo("notifications") }, modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 6.dp))
        }
        Row(Modifier.fillMaxWidth().background(Panel).border(2.dp, Line)) {
            val tabs = listOf(R.string.tab_home, R.string.tab_library, R.string.tab_feed, R.string.tab_people, R.string.tab_profile)
            tabs.forEachIndexed { index, res ->
                Box(
                    Modifier
                        .weight(1f)
                        .clickable { tab = index }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(res),
                        fontFamily = Pixel,
                        fontSize = 7.sp,
                        maxLines = 1,
                        color = if (tab == index) Cyan else Muted,
                    )
                }
            }
        }
    }
}

// ======================= auth =======================

@Composable
fun AuthScreen(vm: AppViewModel) {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 log in, 1 sign up, 2 password recovery
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Text("SVAGA", color = Cyan, fontFamily = Pixel, fontSize = 30.sp)
        Spacer(Modifier.height(8.dp))
        Text("ARCHIVE", color = Amber, fontFamily = Pixel, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.tagline), color = Muted, fontFamily = Vt, fontSize = 19.sp)
        Spacer(Modifier.height(32.dp))

        PixelField(email, { email = it }, stringResource(R.string.email), keyboard = KeyboardType.Email)

        if (mode == 2) {
            when (vm.resetStage) {
                0 -> {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.reset_intro), color = Muted, fontFamily = Vt, fontSize = 17.sp)
                }
                1 -> {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(if (vm.resetMail) R.string.reset_sent else R.string.reset_sent_nomail),
                        color = Muted, fontFamily = Vt, fontSize = 17.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    PixelField(code, { code = it.filter { c -> c.isDigit() }.take(6) }, stringResource(R.string.reset_code), keyboard = KeyboardType.Number)
                    Spacer(Modifier.height(12.dp))
                    PixelField(password, { password = it }, stringResource(R.string.reset_new_password), password = true)
                }
                else -> {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.reset_done), color = Green, fontFamily = Vt, fontSize = 19.sp)
                }
            }
        } else {
            Spacer(Modifier.height(12.dp))
            PixelField(password, { password = it }, stringResource(R.string.password), password = true)
            if (mode == 0) {
                Text(
                    stringResource(R.string.forgot_password),
                    color = Cyan,
                    fontFamily = Vt,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable {
                            vm.leaveReset()
                            password = ""
                            mode = 2
                        }
                        .padding(vertical = 8.dp),
                )
            }
            if (mode == 1) {
                Spacer(Modifier.height(12.dp))
                PixelField(name, { name = it }, stringResource(R.string.display_name))
            }
        }

        vm.authError?.let { res ->
            Spacer(Modifier.height(12.dp))
            Text(stringResource(res), color = Red, fontFamily = Vt, fontSize = 17.sp, textAlign = TextAlign.Center)
            val reason = vm.bannedReason
            if (!reason.isNullOrBlank() && res == R.string.err_banned) {
                Text(reason, color = Muted, fontFamily = Vt, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(20.dp))
        if (mode == 2) {
            when (vm.resetStage) {
                0 -> PixelButton(
                    stringResource(R.string.reset_send),
                    { vm.requestReset(email.trim()) },
                    filled = true,
                    enabled = !vm.authBusy && email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
                1 -> PixelButton(
                    stringResource(R.string.reset_submit),
                    { vm.submitReset(email.trim(), code, password) },
                    filled = true,
                    enabled = !vm.authBusy && code.length == 6 && password.length >= 8,
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> PixelButton(
                    stringResource(R.string.login),
                    {
                        vm.leaveReset()
                        password = ""
                        code = ""
                        mode = 0
                    },
                    filled = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.reset_back),
                color = Cyan,
                fontFamily = Vt,
                fontSize = 19.sp,
                modifier = Modifier.clickable {
                    vm.leaveReset()
                    mode = 0
                }.padding(8.dp),
            )
        } else {
            PixelButton(
                text = stringResource(if (mode == 1) R.string.register else R.string.login),
                onClick = {
                    if (mode == 1) vm.register(email.trim(), password, name.trim())
                    else vm.login(email.trim(), password)
                },
                filled = true,
                enabled = !vm.authBusy && email.isNotBlank() && password.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(if (mode == 1) R.string.to_login else R.string.to_register),
                color = Cyan,
                fontFamily = Vt,
                fontSize = 19.sp,
                modifier = Modifier.clickable { mode = if (mode == 1) 0 else 1 }.padding(8.dp),
            )
            SocialLoginButtons(vm)
        }
    }
}

// ======================= home =======================

@Composable
fun CenterText(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Muted, fontFamily = Vt, fontSize = 19.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun FilterRow(allLabel: String, items: List<FilterItem>, selected: Int?, onSelect: (Int?) -> Unit) {
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PixelChip(allLabel, selected == null, Muted, onClick = { onSelect(null) })
        items.forEach { f ->
            PixelChip(f.label(), selected == f.id, Amber, onClick = { onSelect(if (selected == f.id) null else f.id) })
        }
    }
}

/** Genre, platform, year and rating filters (shared by the catalog and the library). */
@Composable
fun FilterPanel(
    filter: GameFilter,
    onChange: (GameFilter) -> Unit,
    platformItems: List<FilterItem>,
    onMorePlatforms: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    FilterRow(stringResource(R.string.all_genres), GENRE_FILTERS, filter.genre) { onChange(filter.copy(genre = it)) }
    FilterRow(stringResource(R.string.all_platforms), platformItems, filter.platform) { onChange(filter.copy(platform = it)) }
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PixelChip(
            stringResource(R.string.platforms_more),
            filter.platform != null && platformItems.none { it.id == filter.platform },
            Amber,
            onClick = onMorePlatforms,
        )
        PixelChip(stringResource(R.string.any_year), filter.yearFrom == null && filter.yearTo == null, Muted) {
            onChange(filter.copy(yearFrom = null, yearTo = null))
        }
        YEAR_RANGES.forEach { r ->
            PixelChip(r.label, filter.yearFrom == r.from && filter.yearTo == r.to, Amber) {
                val same = filter.yearFrom == r.from && filter.yearTo == r.to
                onChange(if (same) filter.copy(yearFrom = null, yearTo = null) else filter.copy(yearFrom = r.from, yearTo = r.to))
            }
        }
    }
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PixelChip(stringResource(R.string.any_rating), filter.minRating == null, Muted) { onChange(filter.copy(minRating = null)) }
        RATING_STEPS.forEach { r ->
            PixelChip("$r+", filter.minRating == r, Green) {
                onChange(filter.copy(minRating = if (filter.minRating == r) null else r))
            }
        }
        extra()
        if (filter.active) PixelChip(stringResource(R.string.filters_reset), false, Red) { onChange(GameFilter()) }
    }
}

@Composable
fun platformFilterItems(vm: AppViewModel): List<FilterItem> =
    if (vm.pinnedPlatforms.isNotEmpty()) vm.pinnedPlatforms.map { FilterItem(it.id, it.short, it.short) } else PLATFORM_FILTERS

@Composable
fun HomeScreen(vm: AppViewModel, onOpen: (Int) -> Unit, onGo: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val searching = query.trim().length >= 2
    val baseList: List<Game>? = if (searching) vm.searchResults?.filter { it.matches(vm.filter) } else vm.browse
    val list: List<Game>? = if (vm.hideAdded) baseList?.filter { vm.library[it.id]?.status == null } else baseList
    var showPlatformPicker by rememberSaveable { mutableStateOf(false) }
    val busy = if (searching) vm.searching else vm.browseLoading
    val empty = list.isNullOrEmpty()
    LaunchedEffect(list?.size, vm.canLoadMore, vm.hideAdded, vm.browseLoading) {
        if (!searching && vm.hideAdded && list != null && list.isEmpty() && !baseList.isNullOrEmpty() &&
            vm.canLoadMore && !vm.browseLoading
        ) {
            vm.loadBrowse(append = true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "SVAGA ARCHIVE",
            color = Cyan,
            fontFamily = Pixel,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 60.dp),
        )
        Text(
            stringResource(R.string.tagline),
            color = Muted,
            fontFamily = Vt,
            fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        PixelField(
            value = query,
            onChange = {
                query = it
                vm.search(it)
            },
            label = stringResource(R.string.search_hint),
            modifier = Modifier.padding(12.dp),
        )
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!searching) {
                SECTION_KEYS.forEach { key ->
                    PixelChip(sectionLabel(key), vm.section == key, Cyan, onClick = { vm.selectSection(key) })
                }
            }
            val active = vm.filter.active || vm.hideAdded
            PixelChip(
                stringResource(R.string.filters) + if (active) " *" else "",
                showFilters,
                Pink,
                onClick = { showFilters = !showFilters },
            )
            PixelChip(stringResource(if (vm.gridMode) R.string.view_list else R.string.view_grid), false, Blue, onClick = { vm.toggleGrid() })
            PixelChip(stringResource(R.string.platforms_screen), false, Amber, onClick = { onGo("platforms") })
            PixelChip(stringResource(R.string.add_game_chip), false, Green, onClick = { onGo("addgame") })
        }
        if (showFilters) {
            FilterPanel(
                filter = vm.filter,
                onChange = { vm.applyFilter(it) },
                platformItems = platformFilterItems(vm),
                onMorePlatforms = { showPlatformPicker = true },
                extra = {
                    PixelChip(stringResource(R.string.hide_added), vm.hideAdded, Green, onClick = { vm.updateHideAdded(!vm.hideAdded) })
                },
            )
        }
        Spacer(Modifier.height(8.dp))

        if (showPlatformPicker) {
            PlatformPicker(
                platforms = vm.platforms,
                selected = vm.filter.platform,
                onSelect = {
                    vm.setPlatform(it)
                    showPlatformPicker = false
                },
                onDismiss = { showPlatformPicker = false },
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                empty && vm.listError -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(vm.listErrorRes), color = Red, fontFamily = Vt, fontSize = 19.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    PixelButton(
                        text = stringResource(R.string.retry),
                        onClick = { if (searching) vm.search(query) else vm.loadBrowse() },
                    )
                }
                empty && (busy || (searching && list == null) || (!searching && !vm.browseLoaded)) ->
                    CenterText(stringResource(R.string.loading))
                empty -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.no_results), color = Muted, fontFamily = Vt, fontSize = 19.sp)
                    Spacer(Modifier.height(12.dp))
                    PixelButton(stringResource(R.string.add_game_missing), { onGo("addgame") })
                }
                else -> GameCollection(
                    list = list.orEmpty(),
                    grid = vm.gridMode,
                    statusOf = { vm.library[it]?.status },
                    onOpen = onOpen,
                    showMore = !searching && vm.canLoadMore,
                    moreLoading = vm.browseLoading,
                    onMore = { vm.loadBrowse(append = true) },
                    footerText = if (searching) stringResource(R.string.add_game_missing) else null,
                    onFooter = { onGo("addgame") },
                )
            }
        }
    }
}

@Composable
fun GameCollection(
    list: List<Game>,
    grid: Boolean,
    statusOf: (Int) -> String?,
    onOpen: (Int) -> Unit,
    showMore: Boolean = false,
    moreLoading: Boolean = false,
    onMore: () -> Unit = {},
    footerText: String? = null,
    onFooter: () -> Unit = {},
) {
    if (grid) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(list, key = { it.id }) { game ->
                GameCard(game, statusOf(game.id)) { onOpen(game.id) }
            }
            if (showMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PixelButton(
                        text = stringResource(if (moreLoading) R.string.loading else R.string.load_more),
                        onClick = onMore,
                        enabled = !moreLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (footerText != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PixelButton(footerText, onFooter, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(list, key = { it.id }) { game ->
                GameListRow(game, statusOf(game.id)) { onOpen(game.id) }
            }
            if (showMore) {
                item {
                    PixelButton(
                        text = stringResource(if (moreLoading) R.string.loading else R.string.load_more),
                        onClick = onMore,
                        enabled = !moreLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (footerText != null) {
                item { PixelButton(footerText, onFooter, modifier = Modifier.fillMaxWidth()) }
            }
        }
    }
}

fun ratingColor(r: Int): Color = when {
    r >= 75 -> Green
    r >= 50 -> Amber
    else -> Red
}

@Composable
fun Cover(url: String?, modifier: Modifier) {
    if (url == null) {
        Box(modifier.background(Panel2), contentAlignment = Alignment.Center) {
            Text("?", color = Muted, fontFamily = Pixel, fontSize = 20.sp)
        }
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.background(Panel2),
        )
    }
}

@Composable
fun GameCard(game: Game, status: String? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .background(Panel)
            .border(2.dp, Line)
            .clickable(onClick = onClick),
    ) {
        Box {
            Cover(game.cover, Modifier.fillMaxWidth().aspectRatio(0.75f))
            game.rating?.let { r ->
                Text(
                    r.toString(),
                    color = Bg,
                    fontFamily = Pixel,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .background(ratingColor(r))
                        .padding(horizontal = 5.dp, vertical = 4.dp),
                )
            }
            if (status != null) {
                Text(
                    statusLabel(status),
                    color = Bg,
                    fontFamily = Vt,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(statusColor(status))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            game.name,
            color = Ink,
            fontFamily = Vt,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
fun GameListRow(game: Game, status: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .border(2.dp, Line)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Cover(game.cover, Modifier.width(56.dp).aspectRatio(0.75f))
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(game.name, color = Ink, fontFamily = Vt, fontSize = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val line = listOfNotNull(game.year?.toString(), game.platforms.take(3).joinToString(", ").ifBlank { null }).joinToString(" · ")
            if (line.isNotBlank()) {
                Text(line, color = Muted, fontFamily = Vt, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                game.rating?.let { Text(it.toString(), color = ratingColor(it), fontFamily = Vt, fontSize = 17.sp) }
                if (status != null) Text(statusLabel(status), color = statusColor(status), fontFamily = Vt, fontSize = 17.sp)
            }
        }
    }
}

// ======================= library =======================

@Composable
fun LibraryScreen(vm: AppViewModel, onOpen: (Int) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("all") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showPlatformPicker by rememberSaveable { mutableStateOf(false) }
    val f = vm.libFilter
    val entries = vm.library.values
        .filter { it.status != null || it.favorite }
        .filter {
            when (filter) {
                "all" -> true
                "fav" -> it.favorite
                else -> it.status == filter
            }
        }
        .filter { item ->
            if (!f.active) true else vm.gamesCache[item.gameId]?.matches(f) ?: false
        }
        .sortedByDescending { it.updatedAt }

    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.library_title) + " (" + entries.size + ")",
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
            val keys = listOf("all") + STATUSES + "fav"
            keys.forEach { key ->
                val accent = when (key) {
                    "all" -> Cyan
                    "fav" -> Pink
                    else -> statusColor(key)
                }
                PixelChip(statusLabel(key), filter == key, accent, onClick = { filter = key })
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PixelChip(stringResource(R.string.filters) + if (f.active) " *" else "", showFilters, Pink, onClick = { showFilters = !showFilters })
            PixelChip(stringResource(if (vm.gridMode) R.string.view_list else R.string.view_grid), false, Blue, onClick = { vm.toggleGrid() })
        }
        if (showFilters) {
            FilterPanel(
                filter = f,
                onChange = { vm.applyLibFilter(it) },
                platformItems = platformFilterItems(vm),
                onMorePlatforms = { showPlatformPicker = true },
            )
        }
        if (showPlatformPicker) {
            PlatformPicker(
                platforms = vm.platforms,
                selected = f.platform,
                onSelect = {
                    vm.applyLibFilter(f.copy(platform = it))
                    showPlatformPicker = false
                },
                onDismiss = { showPlatformPicker = false },
            )
        }
        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) {
            CenterText(stringResource(R.string.library_empty))
        } else if (vm.gridMode) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(entries, key = { it.gameId }) { item ->
                    val g = vm.gamesCache[item.gameId]
                    if (g != null) {
                        GameCard(g, item.status) { onOpen(item.gameId) }
                    } else {
                        LibraryRow(item, null) { onOpen(item.gameId) }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(entries, key = { it.gameId }) { item ->
                    LibraryRow(item, vm.gamesCache[item.gameId]) { onOpen(item.gameId) }
                }
            }
        }
    }
}

@Composable
fun LibraryRow(item: LibraryItem, game: Game?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Panel)
            .border(2.dp, Line)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Cover(game?.cover, Modifier.width(64.dp).aspectRatio(0.75f))
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                game?.name ?: "#${item.gameId}",
                color = Ink,
                fontFamily = Vt,
                fontSize = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item.status?.let {
                    Text(statusLabel(it), color = statusColor(it), fontFamily = Vt, fontSize = 17.sp)
                }
                if (item.favorite) Text("FAV", color = Pink, fontFamily = Vt, fontSize = 17.sp)
                item.rating?.let { Text("$it/10", color = Amber, fontFamily = Vt, fontSize = 17.sp) }
            }
        }
    }
}
