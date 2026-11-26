package com.svaga.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay

// ======================= root =======================

@Composable
fun App(vm: AppViewModel = viewModel()) {
    val context = LocalContext.current
    var crash by remember { mutableStateOf(CrashLog.read(context)) }
    val crashText = crash
    Box(Modifier.fillMaxSize().background(Bg)) {
        Box(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            when {
                crashText != null -> CrashScreen(crashText) {
                    CrashLog.clear(context)
                    crash = null
                }
                !vm.loggedIn -> AuthScreen(vm)
                else -> MainNav(vm)
            }
            Column(Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
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
        fontSize = 14.sp,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .background(Panel2, RoundedCornerShape(16.dp))
            .border(1.dp, Cyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    )
}

@Composable
fun AchievementToast(t: UiToast, onClick: () -> Unit) {
    val title = if (t.kind == "level") stringResource(R.string.level_up_title) else stringResource(R.string.ach_unlocked)
    val line = if (t.kind == "level") {
        stringResource(R.string.level_n, t.level) + " · " + levelName(t.level)
    } else {
        achTitle(t.code) + " · " + tierName(t.tier) + "  +" + t.sp + " SP"
    }
    val color = if (t.kind == "level") Cyan else tierColor(t.tier)
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .background(Panel, RoundedCornerShape(20.dp))
            .border(1.dp, color, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(color.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Star, null, tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(line, color = Ink, fontSize = 15.sp, maxLines = 2)
        }
    }
}

@Composable
fun CrashScreen(text: String, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg).padding(16.dp)) {
        H2(stringResource(R.string.crash_title), color = Red)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(text, color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(12.dp))
        SvButton(stringResource(R.string.crash_close), onClose, Modifier.fillMaxWidth())
    }
}

// ======================= navigation =======================

object Routes {
    const val MAIN = "main"
    const val CATALOG = "catalog"
    const val ACHIEVEMENTS = "achievements"
    const val NOTIFICATIONS = "notifications"
    const val STATS = "stats"
    const val PLATFORMS = "platforms"
    const val ADD_GAME = "addgame"
    const val ADMIN = "admin"
    const val SETTINGS = "settings"
    const val PEOPLE = "people"
    const val NEWS_NEW = "news_new"
    const val MY_COMMENTS = "my_comments"
    const val COLLECTION = "collection"
    const val STEAM = "steam"
    const val RA = "retroachievements"
    fun game(id: Int) = "game/$id"
    fun user(id: String) = "user/$id"
}

/** Everything screens need to move around. */
class Nav(private val nav: NavHostController) {
    fun back() { nav.popBackStack() }
    fun go(route: String) = nav.navigate(route) { launchSingleTop = true }
    fun game(id: Int) = nav.navigate(Routes.game(id))
    fun user(id: String) = nav.navigate(Routes.user(id))
}

@Composable
fun MainNav(vm: AppViewModel) {
    val controller = rememberNavController()
    val nav = remember(controller) { Nav(controller) }
    NavHost(navController = controller, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) { MainScreen(vm, nav) }
        composable("game/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { e ->
            GameScreen(vm, e.arguments?.getInt("id") ?: 0, nav)
        }
        composable(Routes.user("{id}"), arguments = listOf(navArgument("id") { type = NavType.StringType })) { e ->
            val id = e.arguments?.getString("id").orEmpty()
            if (id == vm.userId) ProfileScreen(vm, nav, standalone = true) else UserProfileScreen(vm, id, nav)
        }
        composable(Routes.CATALOG) { CatalogScreen(vm, nav) }
        composable(Routes.ACHIEVEMENTS) { AchievementsScreen(vm, nav) }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(vm, nav) }
        composable(Routes.STATS) { StatsScreen(vm, nav) }
        composable(Routes.PLATFORMS) { PlatformsScreen(vm, nav) }
        composable(Routes.ADD_GAME) { AddGameScreen(vm, nav) }
        composable(Routes.ADMIN) { AdminScreen(vm, nav) }
        composable(Routes.SETTINGS) { SettingsScreen(vm, nav) }
        composable(Routes.PEOPLE) { PeopleScreen(vm, nav) }
        composable(Routes.NEWS_NEW) { NewsEditorScreen(vm, nav) }
        composable(Routes.MY_COMMENTS) { MyCommentsScreen(vm, nav) }
        composable(Routes.COLLECTION) { CollectionScreen(vm, nav) }
        composable(Routes.STEAM) { SteamImportScreen(vm, nav) }
        composable(Routes.RA) { RaScreen(vm, nav) }
    }
}

@Composable
fun MainScreen(vm: AppViewModel, nav: Nav) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var libraryStatus by rememberSaveable { mutableStateOf("all") }
    BackHandler(enabled = tab != 0) { tab = 0 }
    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize()) {
            if (vm.offline || vm.pendingCount > 0) {
                Text(
                    if (vm.pendingCount > 0) stringResource(R.string.offline_pending, vm.pendingCount) else stringResource(R.string.offline_banner),
                    color = Color(0xFF1A1200),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().background(Amber).padding(vertical = 4.dp, horizontal = 12.dp),
                )
            }
            UpdateBanner()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> HomeScreen(vm, nav, openLibrary = { s -> libraryStatus = s; tab = 1 }, openCalendar = { tab = 2 })
                    1 -> LibraryScreen(vm, nav, libraryStatus) { libraryStatus = it }
                    2 -> CalendarScreen(vm, nav)
                    3 -> FeedScreen(vm, nav)
                    else -> ProfileScreen(vm, nav, standalone = false, openLibrary = { s -> libraryStatus = s; tab = 1 })
                }
            }
        }
        BottomBar(vm, tab, onSelect = { tab = it }, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

/** Space the floating bottom bar takes; scrolling content adds it at the end. */
val BottomBarSpace = 104.dp

@Composable
fun BottomBar(vm: AppViewModel, tab: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val items: List<Pair<ImageVector?, Int>> = listOf(
        Icons.Filled.Home to R.string.tab_home,
        Icons.Filled.Star to R.string.tab_library,
        Icons.Filled.DateRange to R.string.tab_calendar,
        Icons.Filled.ThumbUp to R.string.tab_feed,
        null to R.string.tab_profile,
    )
    val shape = RoundedCornerShape(34.dp)
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth()
            .shadow(16.dp, shape)
            .clip(shape)
            .background(Panel.copy(alpha = 0.97f), shape)
            .border(1.dp, Stroke, shape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val sel = i == tab
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (sel) Panel2 else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    if (icon != null) {
                        Icon(icon, null, tint = if (sel) Cyan else Muted, modifier = Modifier.size(24.dp))
                    } else {
                        Avatar(
                            vm.userId, vm.avatarVersion, vm.displayName.ifBlank { vm.email }, 26.dp,
                            Modifier.border(2.dp, if (sel) Cyan else Stroke, CircleShape),
                        )
                    }
                    if (i == 3 && vm.friends.incoming.isNotEmpty()) {
                        CountBadge(vm.friends.incoming.size, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-6).dp))
                    }
                    if (i == 4 && vm.unread > 0) {
                        CountBadge(vm.unread, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-6).dp))
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(stringResource(label), color = if (sel) Ink else Muted, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}
