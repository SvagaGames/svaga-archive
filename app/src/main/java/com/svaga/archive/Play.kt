package com.svaga.archive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

/** On this day, the daily quiz, the "what to play" wheel and players' game lists. */
class Play(private val vm: AppViewModel) {
    private val api get() = vm.api

    private fun run(onError: (Int) -> Unit = {}, block: suspend () -> Unit) {
        vm.viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                vm.handle(e)
                onError(vm.errorRes(e))
            }
        }
    }

    private fun cache(games: List<Game>) = games.forEach { vm.gamesCache[it.id] = it }

    /** Fetches covers of games the lists show. */
    fun ensureGames(ids: Collection<Int>) = run {
        val missing = ids.distinct().filter { !vm.gamesCache.containsKey(it) }
        missing.chunked(50).forEach { cache(api.gamesByIds(it)) }
    }

    // ---------- on this day ----------
    var anniv by mutableStateOf<List<AnnivGame>>(emptyList())
        private set
    private var annivDay: LocalDate? = null

    fun loadAnniv() {
        if (annivDay == LocalDate.now()) return
        run {
            val list = api.onThisDay()
            cache(list.map { it.game })
            anniv = list
            annivDay = LocalDate.now()
        }
    }

    // ---------- quiz ----------
    var quiz by mutableStateOf<QuizState?>(null)
        private set
    var quizError by mutableStateOf<Int?>(null)
        private set
    var quizBusy by mutableStateOf(false)
        private set
    var quizBoard by mutableStateOf<Leaderboard?>(null)
        private set

    fun loadQuiz() = run({ quizError = it }) {
        quizError = null
        quiz = api.quizToday()
    }

    fun guess(gameId: Int) {
        if (quizBusy) return
        quizBusy = true
        run({ quizBusy = false; vm.showNotice(it) }) {
            val q = api.quizGuess(gameId)
            quiz = q
            quizBusy = false
            if (q.finished) vm.refreshAfterAchievements()
        }
    }

    fun loadQuizBoard(week: Boolean) = run {
        quizBoard = null
        quizBoard = api.quizBoard(week)
    }

    // ---------- lists ----------
    var popular by mutableStateOf<List<GameList>?>(null)
        private set
    var mine by mutableStateOf<List<GameList>?>(null)
        private set
    var listsError by mutableStateOf<Int?>(null)
        private set
    val userLists = mutableStateMapOf<String, List<GameList>>()
    val details = mutableStateMapOf<Long, ListDetail>()
    var detailError by mutableStateOf<Int?>(null)
        private set
    /** My lists with a mark "already holds this game", for the add-to-list sheet. */
    var forGame by mutableStateOf<List<GameList>?>(null)
        private set

    fun loadPopular() = run({ listsError = it }) {
        listsError = null
        val l = api.popularLists()
        popular = l
        ensureGames(l.flatMap { it.preview })
    }

    fun loadMine() = run({ listsError = it }) {
        listsError = null
        val l = api.myLists()
        mine = l
        ensureGames(l.flatMap { it.preview })
    }

    fun loadUserLists(userId: String) = run {
        val l = api.userLists(userId)
        userLists[userId] = l
        ensureGames(l.flatMap { it.preview })
    }

    fun loadDetail(id: Long) = run({ detailError = it }) {
        detailError = null
        val d = api.gameList(id)
        details[id] = d
        ensureGames(d.items.map { it.gameId })
    }

    fun loadForGame(gameId: Int) = run {
        forGame = null
        forGame = api.myLists(gameId)
    }

    fun create(title: String, description: String?, public: Boolean, gameId: Int?, onDone: (Long) -> Unit) = run {
        val id = api.createList(title, description, public, gameId)
        mine = null
        if (gameId != null) forGame = api.myLists(gameId)
        loadMine()
        onDone(id)
        vm.refreshAfterAchievements()
    }

    fun toggleInList(list: GameList, gameId: Int) = run {
        if (list.hasGame) api.removeFromList(list.id, gameId) else api.addToList(list.id, gameId)
        forGame = forGame?.map { if (it.id == list.id) it.copy(hasGame = !it.hasGame, count = it.count + if (it.hasGame) -1 else 1) else it }
        details.remove(list.id)
        mine = null
        vm.showNotice(if (list.hasGame) R.string.list_removed else R.string.list_added)
        vm.refreshAfterAchievements()
    }

    fun add(listId: Long, gameId: Int) = run {
        api.addToList(listId, gameId)
        loadDetail(listId)
        vm.refreshAfterAchievements()
    }

    fun remove(listId: Long, gameId: Int) = run {
        details[listId]?.let { d -> details[listId] = d.copy(items = d.items.filter { it.gameId != gameId }, list = d.list.copy(count = d.list.count - 1)) }
        api.removeFromList(listId, gameId)
    }

    fun setNote(listId: Long, gameId: Int, note: String) = run {
        details[listId]?.let { d -> details[listId] = d.copy(items = d.items.map { if (it.gameId == gameId) it.copy(note = note.ifBlank { null }) else it }) }
        api.addToList(listId, gameId, note)
    }

    fun move(listId: Long, gameId: Int, by: Int) {
        val d = details[listId] ?: return
        val items = d.items.toMutableList()
        val i = items.indexOfFirst { it.gameId == gameId }
        val j = i + by
        if (i < 0 || j !in items.indices) return
        items.add(j, items.removeAt(i))
        details[listId] = d.copy(items = items)
        run { api.reorderList(listId, items.map { it.gameId }) }
    }

    fun update(listId: Long, title: String, description: String?, public: Boolean) = run {
        api.updateList(listId, title, description, public)
        details[listId]?.let { d -> details[listId] = d.copy(list = d.list.copy(title = title, description = description, public = public)) }
        mine = null
        vm.refreshAfterAchievements()
    }

    fun delete(listId: Long, onDone: () -> Unit) = run {
        api.deleteList(listId)
        details.remove(listId)
        mine = mine?.filter { it.id != listId }
        onDone()
    }

    fun like(listId: Long) = run {
        val (liked, likes) = api.likeList(listId)
        details[listId]?.let { d -> details[listId] = d.copy(list = d.list.copy(liked = liked, likes = likes)) }
        popular = popular?.map { if (it.id == listId) it.copy(liked = liked, likes = likes) else it }
    }
}

// =====================================================================
// Home screen block
// =====================================================================

@Composable
fun HomePlayBlock(vm: AppViewModel, nav: Nav) {
    val play = vm.play
    LaunchedEffect(Unit) {
        play.loadAnniv()
        if (play.quiz == null) play.loadQuiz()
        if (play.popular == null) play.loadPopular()
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val q = play.quiz
        PlayTile(
            "🧩", stringResource(R.string.quiz_title),
            when {
                q == null -> stringResource(R.string.quiz_tile_new)
                q.finished && q.solved -> stringResource(R.string.quiz_tile_solved, q.points)
                q.finished -> stringResource(R.string.quiz_tile_failed)
                q.attempts > 0 -> stringResource(R.string.quiz_tile_left, q.maxAttempts - q.attempts)
                else -> stringResource(R.string.quiz_tile_new)
            },
            listOf(Color(0xFFB98BFF), Color(0xFF4F7CFF)), Modifier.weight(1f),
        ) { nav.go(Routes.QUIZ) }
        PlayTile("🎡", stringResource(R.string.wheel_title), stringResource(R.string.wheel_tile), listOf(Color(0xFFFFB547), Color(0xFFFF5A6A)), Modifier.weight(1f)) {
            nav.go(Routes.WHEEL)
        }
    }
    BingoTile(vm, nav, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
    val an = play.anniv
    if (an.isNotEmpty()) {
        SectionHeader(stringResource(R.string.anniv_title), Modifier.padding(top = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(an, key = { it.game.id }) { a ->
                Box {
                    PosterCard(a.game, { nav.game(a.game.id) }, status = vm.library[a.game.id]?.status, caption = a.game.year?.toString(), width = 124.dp)
                    Pill(stringResource(R.string.anniv_years, androidx.compose.ui.platform.LocalContext.current.resources.getQuantityString(R.plurals.years_count, a.yearsAgo, a.yearsAgo)), if (a.yearsAgo % 5 == 0) Amber else Cyan, Modifier.padding(6.dp), filled = true)
                }
            }
        }
    }
    val lists = play.popular.orEmpty()
    if (lists.isNotEmpty()) {
        SectionHeader(stringResource(R.string.lists_popular), Modifier.padding(top = 8.dp), onMore = { nav.go(Routes.LISTS) })
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(lists.take(10), key = { it.id }) { l -> ListCard(vm, l, Modifier.width(240.dp)) { nav.go(Routes.list(l.id)) } }
        }
    }
}

@Composable
private fun PlayTile(emoji: String, title: String, subtitle: String, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.clip(shape).background(Brush.linearGradient(listOf(colors[0].copy(alpha = 0.35f), colors[1].copy(alpha = 0.18f), Panel)))
            .border(1.dp, colors[0].copy(alpha = 0.55f), shape).clickable(onClick = onClick).padding(14.dp),
    ) {
        Text(emoji, fontSize = 30.sp)
        Spacer(Modifier.height(6.dp))
        Body(title, weight = FontWeight.Bold, maxLines = 1)
        Caption(subtitle, maxLines = 2)
    }
}

// =====================================================================
// Daily quiz
// =====================================================================

private val ZOOM = listOf(5f, 3.4f, 2.4f, 1.7f, 1.25f)

@Composable
fun QuizScreen(vm: AppViewModel, nav: Nav) {
    val play = vm.play
    LaunchedEffect(Unit) { play.loadQuiz() }
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Game>>(emptyList()) }
    var board by remember { mutableStateOf(false) }
    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(350)
        results = try { vm.api.search(query.trim()).take(8) } catch (e: CancellationException) { throw e } catch (e: Exception) { emptyList() }
    }
    if (board) { QuizBoardScreen(vm, nav) { board = false }; return }
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(stringResource(R.string.quiz_title), nav::back, subtitle = stringResource(R.string.quiz_subtitle)) {
            IconBtn(Icons.Filled.Star, { board = true }, tint = Amber)
        }
        val q = play.quiz
        when {
            q == null && play.quizError != null -> { ErrorState(stringResource(play.quizError!!), { play.loadQuiz() }); return@Column }
            q == null -> { LoadingBox(); return@Column }
        }
        q!!
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            val zoom by animateFloatAsState(if (q.finished) 1f else ZOOM.getOrElse(q.attempts) { 1.2f }, tween(700), label = "zoom")
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp)).background(Panel)) {
                AsyncImage(
                    model = q.image, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = zoom; scaleY = zoom
                        transformOrigin = TransformOrigin(q.fx, q.fy)
                    },
                )
                Pill(stringResource(R.string.quiz_attempts, (q.maxAttempts - q.attempts).coerceAtLeast(0)), if (q.finished) Muted else Cyan, Modifier.align(Alignment.TopEnd).padding(10.dp), filled = true)
            }
            Row(Modifier.padding(16.dp, 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(q.streak.toString(), stringResource(R.string.quiz_streak), Modifier.weight(1f), accent = Amber)
                StatTile("${q.solvedToday}/${q.playedToday}", stringResource(R.string.quiz_today_stat), Modifier.weight(1f))
            }
            // hints open one by one
            val hints = listOfNotNull(
                q.year?.let { stringResource(R.string.quiz_hint_year) to it.toString() },
                q.platform?.let { stringResource(R.string.quiz_hint_platform) to it },
                q.genre?.let { stringResource(R.string.quiz_hint_genre) to it },
                q.developer?.let { stringResource(R.string.quiz_hint_dev) to it },
            )
            if (hints.isNotEmpty()) {
                SvCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Caption(stringResource(R.string.quiz_hints))
                    hints.forEach { (k, v) ->
                        Row(Modifier.padding(top = 4.dp)) {
                            Body("$k: ", color = Muted, size = 14.sp)
                            Body(v, weight = FontWeight.SemiBold, size = 14.sp)
                        }
                    }
                }
            }
            q.guesses.forEach { g ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    val ok = q.solved && g == q.guesses.last()
                    Text(if (ok) "✓" else "✗", color = if (ok) Green else Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Body(g.name, color = if (ok) Green else Muted, maxLines = 1)
                }
            }
            if (q.finished) {
                val a = q.answer
                SvCard(Modifier.fillMaxWidth().padding(16.dp), border = if (q.solved) Green else Red) {
                    Body(stringResource(if (q.solved) R.string.quiz_win else R.string.quiz_lose), weight = FontWeight.Bold, size = 18.sp, color = if (q.solved) Green else Red)
                    if (q.solved) Caption(stringResource(R.string.quiz_points, q.points))
                    if (a != null) {
                        Spacer(Modifier.height(10.dp))
                        GameRow(a, a.name, { nav.game(a.id) }, subtitle = listOfNotNull(a.year?.toString(), a.platforms.firstOrNull()).joinToString(" · "))
                    }
                    Spacer(Modifier.height(6.dp))
                    Caption(stringResource(R.string.quiz_come_back))
                }
            } else {
                SearchBar(query, { query = it }, stringResource(R.string.quiz_search), Modifier.padding(16.dp, 8.dp))
                results.forEach { g ->
                    GameRow(g, g.name, {
                        play.guess(g.id)
                        query = ""
                        results = emptyList()
                    }, subtitle = listOfNotNull(g.year?.toString(), g.platforms.firstOrNull()).joinToString(" · "))
                }
                if (query.isBlank()) Caption(stringResource(R.string.quiz_rules), Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuizBoardScreen(vm: AppViewModel, nav: Nav, close: () -> Unit) {
    val play = vm.play
    var week by remember { mutableStateOf(true) }
    LaunchedEffect(week) { play.loadQuizBoard(week) }
    androidx.activity.compose.BackHandler(onBack = close)
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(stringResource(R.string.quiz_board), close)
        Segmented(listOf(stringResource(R.string.quiz_week), stringResource(R.string.quiz_all_time)), if (week) 0 else 1, { week = it == 0 }, Modifier.padding(horizontal = 16.dp))
        val b = play.quizBoard
        when {
            b == null -> LoadingBox()
            b.items.isEmpty() -> EmptyState(stringResource(R.string.quiz_board_empty))
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(b.items, key = { it.userId }) { r -> SimpleBoardRow(r) { nav.user(r.userId) } }
                b.me?.takeIf { m -> b.items.none { it.me } }?.let { m -> item { Divider1(); SimpleBoardRow(m) {} } }
            }
        }
    }
}

@Composable
private fun SimpleBoardRow(r: LeaderRow, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(if (r.me) Cyan.copy(alpha = 0.08f) else Color.Transparent).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(when (r.rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "${r.rank}" }, color = Muted, fontSize = 16.sp, modifier = Modifier.width(36.dp))
        Avatar(r.userId, r.avatarVersion, r.name, 36.dp)
        Spacer(Modifier.width(10.dp))
        Body(r.name, Modifier.weight(1f), weight = FontWeight.SemiBold, maxLines = 1)
        Body(compact(r.value), color = Amber, weight = FontWeight.Bold)
    }
}

// =====================================================================
// "What to play today?" wheel
// =====================================================================

@Composable
fun WheelScreen(vm: AppViewModel, nav: Nav) {
    val sources = listOf("backlog" to R.string.status_backlog, "playing" to R.string.status_playing, "dropped" to R.string.status_dropped, "fav" to R.string.wheel_favorites)
    var source by rememberSaveable { mutableStateOf("backlog") }
    var platform by rememberSaveable { mutableStateOf<String?>(null) }
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    val all = vm.library.values.filter { if (source == "fav") it.favorite else it.status == source }.mapNotNull { vm.gamesCache[it.gameId] }
    val pool = all.filter { g -> (platform == null || platform in g.platforms) && (genre == null || genre in g.genres) }
    var shown by remember { mutableStateOf<Game?>(null) }
    var picked by remember { mutableStateOf<Game?>(null) }
    var spinning by remember { mutableStateOf(false) }
    var spin by remember { mutableIntStateOf(0) }
    LaunchedEffect(spin) {
        if (spin == 0 || pool.isEmpty()) return@LaunchedEffect
        spinning = true
        picked = null
        val target = pool.random()
        var wait = 45L
        repeat(18) {
            shown = pool.random()
            delay(wait)
            wait = (wait * 1.17).toLong()
        }
        shown = target
        picked = target
        spinning = false
    }
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(stringResource(R.string.wheel_title), nav::back, subtitle = stringResource(R.string.wheel_subtitle))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ChipRow(Modifier.padding(vertical = 4.dp)) {
                sources.forEach { (k, label) -> SvChip(stringResource(label), source == k, { source = k; platform = null; genre = null; picked = null; shown = null }) }
            }
            val plats = all.flatMap { it.platforms }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(12).map { it.key }
            if (plats.size > 1) ChipRow(Modifier.padding(vertical = 4.dp)) {
                SvChip(stringResource(R.string.wheel_any_platform), platform == null, { platform = null }, accent = Amber)
                plats.forEach { p -> SvChip(p, platform == p, { platform = if (platform == p) null else p }, accent = Amber) }
            }
            val genres = all.flatMap { it.genres }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(12).map { it.key }
            if (genres.size > 1) ChipRow(Modifier.padding(vertical = 4.dp)) {
                SvChip(stringResource(R.string.wheel_any_genre), genre == null, { genre = null }, accent = Purple)
                genres.forEach { g -> SvChip(g, genre == g, { genre = if (genre == g) null else g }, accent = Purple) }
            }
            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                val g = shown
                if (g == null) {
                    Box(Modifier.width(200.dp).aspectRatio(0.75f).clip(CoverShape).background(Panel), contentAlignment = Alignment.Center) {
                        Text("🎡", fontSize = 64.sp)
                    }
                } else {
                    Cover(g.cover, Modifier.width(200.dp).aspectRatio(0.75f).graphicsLayer { if (spinning) { rotationZ = (-3..3).random().toFloat() } })
                }
            }
            val g = picked
            if (g != null && !spinning) {
                Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    H2(g.name)
                    Caption(listOfNotNull(g.year?.toString(), g.platforms.firstOrNull(), g.genres.firstOrNull()).joinToString(" · "))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SvButton(stringResource(R.string.wheel_play), {
                            vm.setStatus(g.id, "playing")
                            vm.showNotice(R.string.wheel_started)
                        }, Modifier.weight(1f), accent = Green)
                        SvButton(stringResource(R.string.wheel_open), { nav.game(g.id) }, Modifier.weight(1f), kind = BtnKind.Secondary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (pool.isEmpty()) {
                EmptyState(stringResource(R.string.wheel_empty), hint = stringResource(R.string.wheel_empty_hint))
            } else {
                SvButton(
                    stringResource(if (picked == null) R.string.wheel_spin else R.string.wheel_again), { spin++ },
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp), enabled = !spinning,
                )
                Caption(stringResource(R.string.wheel_pool, pool.size), Modifier.fillMaxWidth().padding(8.dp), color = Faint)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// =====================================================================
// Game lists
// =====================================================================

@Composable
fun ListCard(vm: AppViewModel, l: GameList, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SvCard(modifier, onClick = onClick, padding = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0 until 4).forEach { i ->
                val g = l.preview.getOrNull(i)?.let { vm.gamesCache[it] }
                Box(Modifier.weight(1f).aspectRatio(0.75f).clip(RoundedCornerShape(8.dp)).background(Panel2)) {
                    if (g != null) Cover(g.cover, Modifier.fillMaxSize(), RoundedCornerShape(8.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!l.public) Icon(Icons.Filled.Lock, null, tint = Muted, modifier = Modifier.size(14.dp).padding(end = 4.dp))
            Body(l.title, Modifier.weight(1f), weight = FontWeight.Bold, maxLines = 1)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Caption(stringResource(R.string.list_meta, l.ownerName, l.count), Modifier.weight(1f), maxLines = 1)
            Icon(if (l.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = Pink, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(3.dp))
            Caption(l.likes.toString())
        }
    }
}

@Composable
fun ListsScreen(vm: AppViewModel, nav: Nav, userId: String? = null) {
    val play = vm.play
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var create by remember { mutableStateOf(false) }
    LaunchedEffect(tab, userId) {
        when {
            userId != null -> play.loadUserLists(userId)
            tab == 0 -> play.loadPopular()
            else -> play.loadMine()
        }
    }
    Column(Modifier.fillMaxSize().background(Bg)) {
        TopBar(stringResource(if (userId != null) R.string.lists_of_player else R.string.lists_title), nav::back) {
            if (userId == null) IconBtn(Icons.Filled.Add, { create = true }, tint = Green)
        }
        if (userId == null) {
            Segmented(listOf(stringResource(R.string.lists_popular), stringResource(R.string.lists_mine)), tab, { tab = it }, Modifier.padding(horizontal = 16.dp))
        }
        val list = when {
            userId != null -> play.userLists[userId]
            tab == 0 -> play.popular
            else -> play.mine
        }
        when {
            list == null && play.listsError != null -> ErrorState(stringResource(play.listsError!!), { if (tab == 0) play.loadPopular() else play.loadMine() })
            list == null -> LoadingBox()
            list.isEmpty() -> EmptyState(
                stringResource(if (tab == 1 && userId == null) R.string.lists_mine_empty else R.string.lists_empty),
                hint = if (tab == 1 && userId == null) stringResource(R.string.lists_mine_hint) else null,
                action = if (userId == null) stringResource(R.string.list_new) else null, onAction = { create = true },
            )
            else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list, key = { it.id }) { l -> ListCard(vm, l, Modifier.fillMaxWidth()) { nav.go(Routes.list(l.id)) } }
            }
        }
    }
    if (create) ListEditDialog(null, { create = false }) { t, d, p ->
        play.create(t, d, p, null) { id -> nav.go(Routes.list(id)) }
        create = false
    }
}

@Composable
fun ListEditDialog(initial: GameList?, close: () -> Unit, save: (String, String?, Boolean) -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var public by remember { mutableStateOf(initial?.public ?: true) }
    Dialog(onDismissRequest = close) {
        Column(Modifier.clip(RoundedCornerShape(22.dp)).background(Panel).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            H2(stringResource(if (initial == null) R.string.list_new else R.string.list_edit))
            SvField(title, { title = it.take(80) }, stringResource(R.string.list_name))
            SvField(desc, { desc = it.take(500) }, stringResource(R.string.list_description), singleLine = false, minLines = 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Body(stringResource(R.string.list_public), weight = FontWeight.SemiBold)
                    Caption(stringResource(R.string.list_public_hint))
                }
                Switch(public, { public = it }, colors = SwitchDefaults.colors(checkedTrackColor = Cyan))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SvButton(stringResource(R.string.cancel), close, Modifier.weight(1f), kind = BtnKind.Secondary)
                SvButton(stringResource(R.string.save), { save(title.trim(), desc.trim().ifBlank { null }, public) }, Modifier.weight(1f), enabled = title.isNotBlank())
            }
        }
    }
}

@Composable
fun ListScreen(vm: AppViewModel, nav: Nav, id: Long) {
    val play = vm.play
    LaunchedEffect(id) { play.loadDetail(id) }
    var edit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var noteFor by remember { mutableStateOf<ListEntry?>(null) }
    val d = play.details[id]
    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBtn(Icons.AutoMirrored.Filled.ArrowBack, nav::back)
            Spacer(Modifier.weight(1f))
            if (d?.list?.mine == true) {
                IconBtn(Icons.Filled.Add, { adding = true }, tint = Green)
                Spacer(Modifier.width(8.dp))
                IconBtn(Icons.Filled.Edit, { edit = true })
                Spacer(Modifier.width(8.dp))
                IconBtn(Icons.Filled.Delete, { confirmDelete = true }, tint = Red)
            }
        }
        when {
            d == null && play.detailError != null -> { ErrorState(stringResource(play.detailError!!), { play.loadDetail(id) }); return@Column }
            d == null -> { LoadingBox(); return@Column }
        }
        d!!
        val l = d.list
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!l.public) Icon(Icons.Filled.Lock, null, tint = Muted, modifier = Modifier.size(18.dp).padding(end = 6.dp))
                        H1(l.title)
                    }
                    Row(Modifier.padding(top = 8.dp).clickable { nav.user(l.ownerId) }, verticalAlignment = Alignment.CenterVertically) {
                        Avatar(l.ownerId, l.ownerAvatar, l.ownerName, 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Body(l.ownerName, weight = FontWeight.SemiBold)
                        Caption("  ·  " + stringResource(R.string.list_games, l.count))
                    }
                    l.description?.let { Body(it, Modifier.padding(top = 10.dp), color = Muted) }
                    Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        SvButton(
                            if (l.liked) stringResource(R.string.list_liked, l.likes) else stringResource(R.string.list_like, l.likes), { play.like(l.id) },
                            kind = if (l.liked) BtnKind.Primary else BtnKind.Secondary, icon = if (l.liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            accent = if (l.liked) Pink else null,
                        )
                    }
                }
            }
            if (d.items.isEmpty()) item { EmptyState(stringResource(R.string.list_no_games), hint = if (l.mine) stringResource(R.string.list_no_games_hint) else null) }
            items(d.items, key = { it.gameId }) { e ->
                val g = vm.gamesCache[e.gameId]
                GameRow(
                    g, g?.name ?: "…", { nav.game(e.gameId) },
                    subtitle = e.note ?: listOfNotNull(g?.year?.toString(), g?.platforms?.firstOrNull()).joinToString(" · "),
                    status = vm.library[e.gameId]?.status,
                    trailing = if (!l.mine) null else {
                        {
                            Row {
                                Icon(Icons.Filled.KeyboardArrowUp, null, tint = Muted, modifier = Modifier.size(30.dp).clickable { play.move(l.id, e.gameId, -1) })
                                Icon(Icons.Filled.KeyboardArrowDown, null, tint = Muted, modifier = Modifier.size(30.dp).clickable { play.move(l.id, e.gameId, 1) })
                                Icon(Icons.Filled.Edit, null, tint = Muted, modifier = Modifier.size(30.dp).padding(4.dp).clickable { noteFor = e })
                                Icon(Icons.Filled.Close, null, tint = Red, modifier = Modifier.size(30.dp).padding(4.dp).clickable { play.remove(l.id, e.gameId) })
                            }
                        }
                    },
                )
            }
        }
        if (edit) ListEditDialog(l, { edit = false }) { t, desc, p -> play.update(l.id, t, desc, p); edit = false }
        if (confirmDelete) ConfirmDialogP(stringResource(R.string.list_delete_q, l.title), { confirmDelete = false }) {
            confirmDelete = false
            play.delete(l.id) { nav.back() }
        }
        if (adding) GamePickerDialog(vm, { adding = false }) { g -> vm.gamesCache[g.id] = g; play.add(l.id, g.id); adding = false }
        noteFor?.let { e ->
            var note by remember(e.gameId) { mutableStateOf(e.note ?: "") }
            Dialog(onDismissRequest = { noteFor = null }) {
                Column(Modifier.clip(RoundedCornerShape(22.dp)).background(Panel).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    H2(vm.gamesCache[e.gameId]?.name ?: "")
                    SvField(note, { note = it.take(300) }, stringResource(R.string.list_note), singleLine = false, minLines = 2)
                    SvButton(stringResource(R.string.save), { play.setNote(l.id, e.gameId, note.trim()); noteFor = null }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun GamePickerDialog(vm: AppViewModel, close: () -> Unit, pick: (Game) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Game>>(emptyList()) }
    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); return@LaunchedEffect }
        delay(350)
        results = try { vm.api.search(query.trim()).take(15) } catch (e: CancellationException) { throw e } catch (e: Exception) { emptyList() }
    }
    Dialog(onDismissRequest = close) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).clip(RoundedCornerShape(22.dp)).background(Bg).padding(vertical = 12.dp)) {
            SearchBar(query, { query = it }, stringResource(R.string.search_hint), Modifier.padding(horizontal = 12.dp))
            LazyColumn(Modifier.weight(1f, false)) {
                items(results, key = { it.id }) { g ->
                    GameRow(g, g.name, { pick(g) }, subtitle = listOfNotNull(g.year?.toString(), g.platforms.firstOrNull()).joinToString(" · "))
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialogP(title: String, close: () -> Unit, ok: () -> Unit) {
    Dialog(onDismissRequest = close) {
        Column(Modifier.clip(RoundedCornerShape(22.dp)).background(Panel).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            H2(title)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SvButton(stringResource(R.string.cancel), close, Modifier.weight(1f), kind = BtnKind.Secondary)
                SvButton(stringResource(R.string.list_delete_btn), ok, Modifier.weight(1f), kind = BtnKind.Danger)
            }
        }
    }
}

/** "Add to list" sheet on the game screen. */
@Composable
fun AddToListDialog(vm: AppViewModel, gameId: Int, close: () -> Unit) {
    val play = vm.play
    LaunchedEffect(gameId) { play.loadForGame(gameId) }
    var create by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = close) {
        Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).clip(RoundedCornerShape(22.dp)).background(Panel).padding(16.dp)) {
            H2(stringResource(R.string.list_add_to))
            Spacer(Modifier.height(8.dp))
            val lists = play.forGame
            when {
                lists == null -> LoadingBox(Modifier.height(120.dp))
                lists.isEmpty() -> Caption(stringResource(R.string.lists_mine_hint), Modifier.padding(vertical = 8.dp))
                else -> LazyColumn(Modifier.weight(1f, false)) {
                    items(lists, key = { it.id }) { l ->
                        Row(Modifier.fillMaxWidth().clickable { play.toggleInList(l, gameId) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (l.hasGame) "☑" else "☐", color = if (l.hasGame) Green else Muted, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Body(l.title, weight = FontWeight.SemiBold, maxLines = 1)
                                Caption(stringResource(R.string.list_games, l.count))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SvButton(stringResource(R.string.list_new), { create = true }, Modifier.fillMaxWidth(), kind = BtnKind.Secondary, icon = Icons.Filled.Add)
        }
    }
    if (create) ListEditDialog(null, { create = false }) { t, d, p ->
        play.create(t, d, p, gameId) { }
        create = false
    }
}
