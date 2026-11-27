package com.svaga.archive

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

/** Pop-up about a fresh achievement or a new level. */
data class UiToast(val kind: String, val code: String, val tier: Int, val sp: Int, val level: Int)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = TokenStore(app)
    internal val api = ApiClient(store)
    val extras by lazy { Extras(this) }
    private val settings = app.getSharedPreferences("svaga_settings", Context.MODE_PRIVATE)
    private val offlineStore = OfflineStore(app)
    private val flushMutex = Mutex()

    var loggedIn by mutableStateOf(store.access != null)
        private set
    var email by mutableStateOf(store.email)
        private set
    var displayName by mutableStateOf(store.displayName)
        private set
    var userId by mutableStateOf(store.userId)
        private set
    var createdAt by mutableStateOf(store.createdAt)
        private set
    var avatarVersion by mutableStateOf(store.avatarVersion)
        private set
    var role by mutableStateOf(store.role)
        private set
    var libraryPublic by mutableStateOf(true)
        private set
    var sp by mutableStateOf(0)
        private set
    var level by mutableStateOf(1)
        private set
    var profileBusy by mutableStateOf(false)
        private set
    var profileError by mutableStateOf<Int?>(null)
        private set
    var ach by mutableStateOf<AchievementsResult?>(null)
        private set
    val comments = mutableStateMapOf<Int, List<Comment>>()
    val gameStats = mutableStateMapOf<Int, GameStats>()
    var commentError by mutableStateOf<Int?>(null)
        private set

    var authBusy by mutableStateOf(false)
        private set
    var authError by mutableStateOf<Int?>(null)
        private set
    var bannedReason by mutableStateOf<String?>(null)
        private set
    var resetStage by mutableStateOf(0)
        private set
    var resetMail by mutableStateOf(true)
        private set

    var section by mutableStateOf("popular")
        private set
    var filter by mutableStateOf(GameFilter())
        private set
    var libFilter by mutableStateOf(GameFilter())
        private set
    var gridMode by mutableStateOf(settings.getBoolean("grid_mode", true))
        private set
    var browse by mutableStateOf<List<Game>>(emptyList())
        private set
    var browseLoading by mutableStateOf(false)
        private set
    var browseLoaded by mutableStateOf(false)
        private set
    var canLoadMore by mutableStateOf(false)
        private set
    var listError by mutableStateOf(false)
        private set
    var listErrorRes by mutableStateOf(R.string.err_network)
        private set

    var searchResults by mutableStateOf<List<Game>?>(null)
        private set
    var searching by mutableStateOf(false)
        private set

    var library by mutableStateOf<Map<Int, LibraryItem>>(emptyMap())
        private set
    val gamesCache = mutableStateMapOf<Int, Game>()
    val translations = mutableStateMapOf<String, String>()
    /** Keys "id:lang" being translated right now, and the error shown under the description when it failed. */
    val translating = mutableStateMapOf<String, Boolean>()
    val translationErrors = mutableStateMapOf<String, Int>()
    val related = mutableStateMapOf<Int, Related>()
    val ruList = mutableStateMapOf<Int, List<RuTranslation>>()

    var platforms by mutableStateOf<List<Platform>>(emptyList())
        private set
    var pinnedPlatforms by mutableStateOf<List<Platform>>(emptyList())
        private set
    var hideAdded by mutableStateOf(settings.getBoolean("hide_added", false))
        private set

    var offline by mutableStateOf(false)
        private set
    var pendingCount by mutableStateOf(offlineStore.pending().size)
        private set

    var toast by mutableStateOf<UiToast?>(null)
        private set
    private val toastQueue = ArrayDeque<UiToast>()
    var notice by mutableStateOf<Int?>(null)
        private set
    var unread by mutableStateOf(0)
        private set
    var notifications by mutableStateOf<List<NotificationItem>>(emptyList())
        private set

    var people by mutableStateOf<List<Person>>(emptyList())
        private set
    var friends by mutableStateOf(FriendsResult(emptyList(), emptyList(), emptyList()))
        private set
    var viewedProfile by mutableStateOf<PublicProfile?>(null)
        private set
    var viewedComments by mutableStateOf<List<Comment>>(emptyList())
        private set
    private var peopleJob: Job? = null
    private var lastPeopleQuery = ""

    var feed by mutableStateOf<List<Comment>>(emptyList())
        private set
    var feedLoading by mutableStateOf(false)
        private set
    var feedMore by mutableStateOf(false)
        private set
    var myComments by mutableStateOf<List<Comment>>(emptyList())
        private set

    var stats by mutableStateOf<StatsResult?>(null)
        private set
    var statsPeriod by mutableStateOf("week")
        private set

    var adminUsers by mutableStateOf<List<AdminUser>>(emptyList())
        private set
    var adminCode by mutableStateOf<Pair<String, String>?>(null)
        private set

    var addBusy by mutableStateOf(false)
        private set
    var addError by mutableStateOf<Int?>(null)
        private set
    var addDuplicates by mutableStateOf<List<Game>>(emptyList())
        private set
    var ruError by mutableStateOf<Int?>(null)
        private set

    private var searchJob: Job? = null
    private var browseJob: Job? = null
    private var bgJob: Job? = null

    // ---------- redesign: home rows, release calendar, activity, play time, friends' feed, news ----------
    /** Horizontal rows on the home screen, by catalog section. */
    val homeRows = mutableStateMapOf<String, List<Game>>()
    var homeRowsError by mutableStateOf(false)
        private set

    fun loadHomeRows(force: Boolean = false) {
        listOf("popular", "svaga", "new", "top", "retro").forEach { key ->
            if (!force && homeRows.containsKey(key)) return@forEach
            viewModelScope.launch {
                try {
                    val r = api.browse(key, null, null, null, null, null, 0)
                    r.items.forEach { gamesCache[it.id] = it }
                    homeRows[key] = r.items
                    homeRowsError = false
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    homeRowsError = true
                    handle(e)
                }
            }
        }
    }

    /** Upcoming releases for the calendar (catalog section "upcoming"). */
    var upcoming by mutableStateOf<List<Game>>(emptyList())
        private set
    var upcomingLoading by mutableStateOf(false)
        private set
    var upcomingMore by mutableStateOf(false)
        private set

    fun loadUpcoming(append: Boolean = false) {
        if (upcomingLoading || (append && !upcomingMore)) return
        viewModelScope.launch {
            upcomingLoading = true
            try {
                val r = api.browse("upcoming", null, null, null, null, null, if (append) upcoming.size else 0)
                r.items.forEach { gamesCache[it.id] = it }
                upcoming = if (append) upcoming + r.items.filter { g -> upcoming.none { it.id == g.id } } else r.items
                upcomingMore = r.rawCount >= 30
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            } finally {
                upcomingLoading = false
            }
        }
    }

    /** Own activity per day (null: not loaded yet or the server is older than 0.8.0). */
    var activity by mutableStateOf<List<ActivityDay>?>(null)
        private set
    var viewedActivity by mutableStateOf<List<ActivityDay>?>(null)
        private set

    fun loadActivity() {
        viewModelScope.launch {
            try {
                activity = api.activity(null, 182)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun loadActivityOf(id: String) {
        viewedActivity = null
        viewModelScope.launch {
            try {
                viewedActivity = api.activity(id, 182)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    /** Total play time from Svaga Emulator in seconds (null: unknown). */
    var playSeconds by mutableStateOf<Long?>(null)
        private set
    val playtimeByGame = mutableStateMapOf<Int, Long>()

    fun loadPlaytime() {
        viewModelScope.launch {
            try {
                val r = api.playtime()
                playSeconds = r.totalSeconds
                playtimeByGame.clear()
                r.items.forEach { playtimeByGame[it.gameId] = it.seconds }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    var friendFeed by mutableStateOf<List<FriendEvent>>(emptyList())
        private set
    var friendFeedLoading by mutableStateOf(false)
        private set
    var friendFeedMore by mutableStateOf(false)
        private set
    var friendFeedLoaded by mutableStateOf(false)
        private set

    fun loadFriendFeed(append: Boolean = false) {
        if (friendFeedLoading) return
        viewModelScope.launch {
            friendFeedLoading = true
            try {
                val list = api.friendsFeed(if (append) friendFeed.size else 0)
                friendFeed = if (append) friendFeed + list else list
                friendFeedMore = list.size >= 30
                val missing = list.map { it.gameId }.distinct().filter { !gamesCache.containsKey(it) }
                missing.chunked(50).forEach { chunk -> api.gamesByIds(chunk).forEach { gamesCache[it.id] = it } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            } finally {
                friendFeedLoading = false
                friendFeedLoaded = true
            }
        }
    }

    var news by mutableStateOf<List<NewsItem>>(emptyList())
        private set
    var newsLoading by mutableStateOf(false)
        private set
    var newsMore by mutableStateOf(false)
        private set
    var newsLoaded by mutableStateOf(false)
        private set
    var newsError by mutableStateOf<Int?>(null)

    fun loadNews(append: Boolean = false) {
        if (newsLoading) return
        viewModelScope.launch {
            newsLoading = true
            try {
                val list = api.news(if (append) news.size else 0)
                news = if (append) news + list else list
                newsMore = list.size >= 20
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            } finally {
                newsLoading = false
                newsLoaded = true
            }
        }
    }

    fun postNews(title: String, body: String, imageUrl: String, onDone: () -> Unit) {
        newsError = null
        viewModelScope.launch {
            try {
                val item = api.postNews(title.trim(), body.trim(), imageUrl.trim().ifBlank { null })
                news = listOf(item) + news
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                newsError = errorRes(e)
                handle(e)
            }
        }
    }

    fun deleteNews(id: Long) {
        viewModelScope.launch {
            try {
                api.deleteNews(id)
                news = news.filter { it.id != id }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    init {
        if (loggedIn) {
            offlineStore.loadGames().forEach { gamesCache[it.id] = it }
            library = offlineStore.loadLibrary().associateBy { it.gameId }
            applyPendingToLibrary()
            loadAll()
        }
    }

    // ---------- auth ----------
    fun login(email: String, password: String) = auth { api.login(email, password) }

    fun register(email: String, password: String, name: String) = auth { api.register(email, password, name) }

    // ---------- Svaga ID ----------
    var providers by mutableStateOf<List<String>>(emptyList())
        private set
    var identities by mutableStateOf<IdentitiesResult?>(null)
        private set

    fun loadProviders() {
        viewModelScope.launch {
            try { providers = api.oauthProviders() } catch (e: CancellationException) { throw e } catch (e: Exception) { /* offline: buttons stay hidden */ }
        }
    }

    fun startOauth(context: Context, provider: String) {
        authError = null
        openInBrowser(context, api.oauthStartUrl(provider))
    }

    fun loadIdentities() {
        viewModelScope.launch {
            try { identities = api.identities() } catch (e: CancellationException) { throw e } catch (e: Exception) { /* keep old */ }
        }
    }

    fun linkIdentity(context: Context, provider: String) {
        viewModelScope.launch {
            try {
                openInBrowser(context, api.identityLinkUrl(provider))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showNotice(errorRes(e))
            }
        }
    }

    fun unlinkIdentity(provider: String) {
        viewModelScope.launch {
            try {
                api.unlinkIdentity(provider)
                loadIdentities()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showNotice(if (e is ApiException && e.code == "last_method") R.string.err_last_method else errorRes(e))
            }
        }
    }

    /** svagaarchive://auth?code=... (sign-in), ?linked=google (account linked) or ?error=... */
    fun handleDeepLink(uri: Uri?) {
        if (uri == null || uri.scheme != "svagaarchive" || uri.host != "auth") return
        val code = uri.getQueryParameter("code")
        val linked = uri.getQueryParameter("linked")
        val error = uri.getQueryParameter("error")
        when {
            code != null -> auth { api.oauthExchange(code) }
            linked != null -> {
                showNotice(R.string.notice_identity_linked)
                if (loggedIn) loadIdentities()
            }
            error != null -> {
                val res = when (error) {
                    "denied" -> R.string.err_oauth_denied
                    "banned" -> R.string.err_banned
                    "taken" -> R.string.err_oauth_taken
                    "expired" -> R.string.err_oauth_expired
                    else -> R.string.err_oauth_provider
                }
                if (loggedIn) showNotice(res) else authError = res
            }
        }
    }

    private fun auth(action: suspend () -> Unit) {
        viewModelScope.launch {
            authBusy = true
            authError = null
            bannedReason = null
            try {
                action()
                this@AppViewModel.email = store.email
                displayName = store.displayName
                userId = store.userId
                role = store.role
                loggedIn = true
                loadAll()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is ApiException && e.code == "banned") bannedReason = e.body.optString("reason", "")
                authError = errorRes(e)
            } finally {
                authBusy = false
            }
        }
    }

    fun requestReset(email: String) {
        viewModelScope.launch {
            authBusy = true
            authError = null
            try {
                resetMail = api.forgot(email)
                resetStage = 1
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                authError = errorRes(e)
            } finally {
                authBusy = false
            }
        }
    }

    fun submitReset(email: String, code: String, password: String) {
        viewModelScope.launch {
            authBusy = true
            authError = null
            try {
                api.resetPassword(email, code.trim(), password)
                resetStage = 2
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                authError = errorRes(e)
            } finally {
                authBusy = false
            }
        }
    }

    fun clearAuthError() {
        authError = null
    }

    fun leaveReset() {
        resetStage = 0
        authError = null
    }

    fun logout() {
        viewModelScope.launch {
            api.logout()
            clearLocal()
        }
    }

    private fun clearLocal() {
        bgJob?.cancel()
        store.clear()
        offlineStore.clear()
        loggedIn = false
        browse = emptyList()
        browseLoaded = false
        searchResults = null
        library = emptyMap()
        gamesCache.clear()
        translations.clear()
        related.clear()
        ruList.clear()
        comments.clear()
        gameStats.clear()
        people = emptyList()
        friends = FriendsResult(emptyList(), emptyList(), emptyList())
        viewedProfile = null
        viewedComments = emptyList()
        feed = emptyList()
        myComments = emptyList()
        ach = null
        stats = null
        notifications = emptyList()
        unread = 0
        adminUsers = emptyList()
        adminCode = null
        role = "user"
        sp = 0
        level = 1
        userId = ""
        createdAt = ""
        avatarVersion = 0
        pendingCount = 0
        offline = false
        toast = null
        toastQueue.clear()
        authError = null
        homeRows.clear()
        upcoming = emptyList()
        activity = null
        viewedActivity = null
        playSeconds = null
        playtimeByGame.clear()
        friendFeed = emptyList()
        friendFeedLoaded = false
        news = emptyList()
        newsLoaded = false
        extras.reset()
    }

    internal fun forgetLocal() = clearLocal()

    internal fun handle(e: Exception) {
        if (e is IOException) offline = true
        if (e is ApiException) {
            if (e.http == 401) {
                clearLocal()
            } else if (e.http == 403 && e.code == "banned") {
                bannedReason = e.body.optString("reason", "")
                clearLocal()
                authError = R.string.err_banned
            }
        }
    }

    internal fun errorRes(e: Exception): Int = when (e) {
        is ApiException -> when (e.code) {
            "invalid_credentials" -> R.string.err_invalid_credentials
            "email_taken" -> R.string.err_email_taken
            "invalid_email" -> R.string.err_invalid_email
            "invalid_password" -> R.string.err_invalid_password
            "invalid_code" -> R.string.err_reset_code
            "banned" -> R.string.err_banned
            "catalog_unavailable", "catalog_auth_failed", "catalog_not_configured" -> R.string.err_catalog
            "invalid_comment" -> R.string.err_comment
            "invalid_image" -> R.string.err_image
            "duplicate" -> R.string.err_duplicate
            "invalid_name" -> R.string.err_game_name
            "invalid_year" -> R.string.err_game_year
            "invalid_cover", "invalid_url" -> R.string.err_url
            "platform_required" -> R.string.err_platform_required
            "already_has" -> R.string.err_already_has
            "reserved_name" -> R.string.err_reserved
            "invalid_studio" -> R.string.err_studio
            "forbidden" -> R.string.err_forbidden
            "last_method" -> R.string.err_last_method
            "provider_disabled" -> R.string.err_oauth_provider
            "steam_not_configured" -> R.string.err_steam_off
            "steam_profile_not_found", "invalid_profile" -> R.string.err_steam_profile
            "steam_profile_private" -> R.string.err_steam_private
            "steam_key_invalid", "steam_unavailable" -> R.string.err_steam_down
            "ra_not_configured" -> R.string.err_ra_off
            "ra_user_not_found", "invalid_username" -> R.string.err_ra_user
            "ra_key_invalid", "ra_unavailable" -> R.string.err_ra_down
            "creator_cannot_delete" -> R.string.err_creator_delete
            "invalid_price" -> R.string.err_price
            "too_many" -> R.string.err_too_many
            "invalid_pattern" -> R.string.err_pattern
            "games_required" -> R.string.err_games_required
            "invalid_dates" -> R.string.err_dates
            else -> when {
                e.http == 429 -> R.string.err_too_many
                e.http >= 500 -> R.string.err_server
                else -> R.string.err_generic
            }
        }
        is IOException -> R.string.err_network
        else -> R.string.err_generic
    }

    fun showNotice(res: Int) {
        notice = res
    }

    fun dismissNotice() {
        notice = null
    }

    // ---------- loading and background work ----------
    private fun loadAll() {
        loadProfile()
        loadPlatforms()
        loadBrowse()
        loadLibrary()
        loadAchievements()
        loadFriends()
        loadHomeRows()
        loadActivity()
        loadPlaytime()
        startBackground()
    }

    private fun startBackground() {
        bgJob?.cancel()
        bgJob = viewModelScope.launch {
            delay(3000)
            while (true) {
                pollNotifications()
                if (offlineStore.pending().isNotEmpty()) flushPending()
                delay(45_000)
            }
        }
    }

    /** Called when the app returns to the foreground: pushes queued changes and refreshes the badge. */
    fun resume() {
        if (!loggedIn) return
        viewModelScope.launch {
            flushPending()
            pollNotifications()
        }
    }

    private suspend fun pollNotifications() {
        try {
            val c = api.notifCount()
            offline = false
            unread = c.unread
            val key = "notif_toast_" + store.userId
            val seen = settings.getLong(key, -1L)
            if (seen < 0) {
                val maxId = api.notifications(0).items.maxOfOrNull { it.id } ?: 0L
                settings.edit().putLong(key, maxId).apply()
                return
            }
            if (c.lastAchievementId > seen) {
                val items = api.notifications(0).items
                items.filter { it.id > seen && !it.read && (it.kind == "achievement" || it.kind == "level_up") }
                    .sortedBy { it.id }
                    .forEach {
                        DynAch.put(it.payload.optString("code"), it.payload.optString("title").ifBlank { null }, null, it.payload.optString("emoji").ifBlank { null }, emptyList())
                        toastQueue.addLast(
                            UiToast(
                                kind = it.kind,
                                code = it.payload.optString("code"),
                                tier = it.payload.optInt("tier", 1),
                                sp = it.payload.optInt("sp", 0),
                                level = it.payload.optInt("level", 0),
                            ),
                        )
                    }
                if (toast == null) toast = toastQueue.removeFirstOrNull()
                settings.edit().putLong(key, maxOf(seen, items.maxOfOrNull { it.id } ?: seen)).apply()
                loadAchievements()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handle(e)
        }
    }

    fun dismissToast() {
        toast = toastQueue.removeFirstOrNull()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                val r = api.notifications(0)
                notifications = r.items
                unread = r.unread
                offline = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                api.markNotificationsRead()
                unread = 0
                notifications = notifications.map { it.copy(read = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    // ---------- catalog ----------
    fun selectSection(key: String) {
        if (key == section) return
        section = key
        loadBrowse()
    }

    fun setGenre(id: Int?) {
        filter = filter.copy(genre = id)
        loadBrowse()
    }

    fun setPlatform(id: Int?) {
        filter = filter.copy(platform = id)
        loadBrowse()
    }

    fun setYears(from: Int?, to: Int?) {
        filter = filter.copy(yearFrom = from, yearTo = to)
        loadBrowse()
    }

    fun setMinRating(value: Int?) {
        filter = filter.copy(minRating = value)
        loadBrowse()
    }

    fun applyFilter(f: GameFilter) {
        filter = f
        loadBrowse()
    }

    fun clearFilters() {
        filter = GameFilter()
        loadBrowse()
    }

    fun applyLibFilter(f: GameFilter) {
        libFilter = f
    }

    fun toggleGrid() {
        gridMode = !gridMode
        settings.edit().putBoolean("grid_mode", gridMode).apply()
    }

    fun loadBrowse(append: Boolean = false) {
        if (append && (browseLoading || !canLoadMore)) return
        browseJob?.cancel()
        val offset = if (append) browse.size else 0
        browseJob = viewModelScope.launch {
            browseLoading = true
            browseLoaded = false
            listError = false
            if (!append) browse = emptyList()
            try {
                val f = filter
                val r = api.browse(section, f.genre, f.platform, f.yearFrom, f.yearTo, f.minRating, offset)
                r.items.forEach { gamesCache[it.id] = it }
                browse = if (append) browse + r.items.filter { g -> browse.none { it.id == g.id } } else r.items
                canLoadMore = r.rawCount >= 30
                browseLoaded = true
                offline = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                listError = true
                listErrorRes = errorRes(e)
                handle(e)
            } finally {
                browseLoading = false
            }
        }
    }

    fun ensureTranslation(id: Int) {
        val lang = Lang.code
        val key = "$id:$lang"
        if (lang == "en" || translations.containsKey(key) || translating[key] == true) return
        translationErrors.remove(key)
        translating[key] = true
        viewModelScope.launch {
            try {
                val text = api.translation(id, lang)
                if (text != null) translations[key] = text else translationErrors[key] = R.string.err_translation
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                translationErrors[key] = if (e is IOException) R.string.err_network else R.string.err_translation
                handle(e)
            } finally {
                translating.remove(key)
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        val q = query.trim()
        if (q.length < 2) {
            searchResults = null
            searching = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            searching = true
            listError = false
            try {
                val games = api.search(q)
                games.forEach { gamesCache[it.id] = it }
                searchResults = games
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                listError = true
                listErrorRes = errorRes(e)
                handle(e)
            } finally {
                searching = false
            }
        }
    }

    fun ensureGame(id: Int) {
        if (gamesCache.containsKey(id)) return
        viewModelScope.launch {
            try {
                api.gamesByIds(listOf(id)).forEach { gamesCache[it.id] = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun loadGameDetail(id: Int) {
        viewModelScope.launch {
            try {
                val g = api.gameDetail(id)
                gamesCache[g.id] = g
                offline = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!gamesCache.containsKey(id)) ensureGame(id)
                handle(e)
            }
        }
    }

    fun loadRelated(id: Int) {
        if (related.containsKey(id)) return
        viewModelScope.launch {
            try {
                val r = api.related(id)
                (r.prequels + r.sequels).forEach { gamesCache[it.id] = it }
                related[id] = r
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun loadPlatforms() {
        viewModelScope.launch {
            try {
                val r = api.platforms()
                platforms = r.items
                val byId = r.items.associateBy { it.id }
                pinnedPlatforms = r.pinned.mapNotNull { byId[it] }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun updateHideAdded(value: Boolean) {
        hideAdded = value
        settings.edit().putBoolean("hide_added", value).apply()
    }

    // ---------- user contributions ----------
    fun addPlatform(gameId: Int, platformId: Int) {
        viewModelScope.launch {
            try {
                api.addPlatform(gameId, platformId)
                loadGameDetail(gameId)
                notice = R.string.notice_platform_added
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = errorRes(e)
                handle(e)
            }
        }
    }

    fun clearAddState() {
        addError = null
        addDuplicates = emptyList()
    }

    fun createGame(g: NewGame, onCreated: (Int) -> Unit) {
        viewModelScope.launch {
            addBusy = true
            addError = null
            addDuplicates = emptyList()
            try {
                val created = api.createGame(g)
                gamesCache[created.id] = created
                onCreated(created.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                addError = errorRes(e)
                if (e is ApiException && e.code == "duplicate") {
                    addDuplicates = e.duplicateMatches()
                    addDuplicates.forEach { gamesCache[it.id] = it }
                }
                handle(e)
            } finally {
                addBusy = false
            }
        }
    }

    fun loadRu(gameId: Int) {
        viewModelScope.launch {
            try {
                ruList[gameId] = api.ruTranslations(gameId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun addRu(gameId: Int, studio: String, platform: String, version: String, url: String, onDone: () -> Unit) {
        viewModelScope.launch {
            ruError = null
            try {
                api.addRuTranslation(gameId, studio.trim(), platform.trim(), version.trim(), url.trim())
                ruList[gameId] = api.ruTranslations(gameId)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ruError = errorRes(e)
                handle(e)
            }
        }
    }

    fun deleteRu(gameId: Int, id: Long) {
        viewModelScope.launch {
            try {
                api.deleteRuTranslation(id)
                ruList[gameId] = ruList[gameId].orEmpty().filter { it.id != id }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun adminDeleteGame(id: Int, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                api.adminDeleteGame(id)
                gamesCache.remove(id)
                library = library - id
                offlineStore.saveLibrary(library.values)
                onDone()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = errorRes(e)
                handle(e)
            }
        }
    }

    // ---------- profile ----------
    fun loadProfile() {
        viewModelScope.launch {
            try {
                val p = api.me()
                store.userId = p.id
                store.createdAt = p.createdAt
                store.avatarVersion = p.avatarVersion
                store.displayName = p.displayName
                store.email = p.email
                store.role = p.role
                userId = p.id
                createdAt = p.createdAt
                avatarVersion = p.avatarVersion
                displayName = p.displayName
                email = p.email
                role = p.role
                libraryPublic = p.libraryPublic
                sp = p.sp
                level = p.level
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun saveName(name: String) {
        viewModelScope.launch {
            profileError = null
            try {
                displayName = api.updateName(name.trim()) ?: ""
                store.displayName = displayName
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                profileError = errorRes(e)
                handle(e)
            }
        }
    }

    fun updateLibraryPublic(value: Boolean) {
        val before = libraryPublic
        libraryPublic = value
        viewModelScope.launch {
            try {
                libraryPublic = api.setLibraryPublic(value)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                libraryPublic = before
                notice = errorRes(e)
                handle(e)
            }
        }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            profileBusy = true
            profileError = null
            try {
                val bytes = withContext(Dispatchers.Default) { processAvatar(uri) }
                if (bytes == null) {
                    profileError = R.string.err_image
                } else {
                    avatarVersion = api.uploadAvatar(bytes)
                    store.avatarVersion = avatarVersion
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                profileError = errorRes(e)
                handle(e)
            } finally {
                profileBusy = false
            }
        }
    }

    fun removeAvatar() {
        viewModelScope.launch {
            profileBusy = true
            try {
                api.deleteAvatar()
                avatarVersion = 0
                store.avatarVersion = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                profileError = errorRes(e)
                handle(e)
            } finally {
                profileBusy = false
            }
        }
    }

    private fun processAvatar(uri: Uri): ByteArray? {
        val resolver = getApplication<Application>().contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val orientation = try {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val bmp = if (degrees != 0f) {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
        } else {
            decoded
        }
        val side = minOf(bmp.width, bmp.height)
        val square = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
        val scaled = Bitmap.createScaledBitmap(square, 256, 256, true)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
        return out.toByteArray()
    }

    // ---------- achievements and statistics ----------
    fun loadAchievements() {
        viewModelScope.launch {
            try {
                val r = api.achievements()
                ach = r
                sp = r.sp
                level = r.level
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun toggleFeatured(code: String) {
        val cur = ach?.featured.orEmpty()
        val next = if (code in cur) cur - code else (cur + code).takeLast(3)
        viewModelScope.launch {
            try {
                val saved = api.setFeatured(next)
                ach = ach?.copy(
                    featured = saved,
                    items = ach?.items.orEmpty().map { it.copy(featured = it.code in saved) },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = errorRes(e)
                handle(e)
            }
        }
    }

    fun loadMyStats(period: String = statsPeriod) {
        statsPeriod = period
        viewModelScope.launch {
            try {
                val r = api.stats(period)
                if (period == statsPeriod) stats = r
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    // ---------- comments ----------
    fun loadComments(gameId: Int) {
        viewModelScope.launch {
            try {
                comments[gameId] = api.comments(gameId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun loadFeed(append: Boolean = false) {
        if (feedLoading) return
        viewModelScope.launch {
            feedLoading = true
            try {
                val list = api.allComments(if (append) feed.size else 0)
                feed = if (append) feed + list.filter { c -> feed.none { it.id == c.id } } else list
                feedMore = list.size >= 30
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            } finally {
                feedLoading = false
            }
        }
    }

    fun loadMyComments() {
        viewModelScope.launch {
            try {
                myComments = api.myComments(0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun postComment(gameId: Int, text: String) {
        viewModelScope.launch {
            commentError = null
            try {
                api.postComment(gameId, text)
                comments[gameId] = api.comments(gameId)
                pollNotifications()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                commentError = errorRes(e)
                handle(e)
            }
        }
    }

    private fun mapComments(id: Long, change: (Comment) -> Comment) {
        fun List<Comment>.apply2() = map { if (it.id == id) change(it) else it }
        comments.keys.toList().forEach { k -> comments[k] = comments[k].orEmpty().apply2() }
        feed = feed.apply2()
        myComments = myComments.apply2()
        viewedComments = viewedComments.apply2()
    }

    fun toggleLike(c: Comment) {
        val want = !c.liked
        mapComments(c.id) { it.copy(liked = want, likes = (it.likes + if (want) 1 else -1).coerceAtLeast(0)) }
        viewModelScope.launch {
            try {
                api.setLike(c.id, want)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mapComments(c.id) { c }
                handle(e)
            }
        }
    }

    fun deleteComment(c: Comment) {
        viewModelScope.launch {
            try {
                api.deleteComment(c.id, asCreator = role == "creator" && c.userId != userId)
                comments.keys.toList().forEach { k -> comments[k] = comments[k].orEmpty().filter { it.id != c.id } }
                feed = feed.filter { it.id != c.id }
                myComments = myComments.filter { it.id != c.id }
                viewedComments = viewedComments.filter { it.id != c.id }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun loadGameStats(gameId: Int) {
        viewModelScope.launch {
            try {
                gameStats[gameId] = api.gameStats(gameId, gamesCache[gameId]?.variantIds.orEmpty())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    // ---------- Creator tools ----------
    fun adminSearch(query: String) {
        viewModelScope.launch {
            try {
                adminUsers = api.adminUsers(query.trim())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = errorRes(e)
                handle(e)
            }
        }
    }

    private fun adminDo(query: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                adminUsers = api.adminUsers(query)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = errorRes(e)
                handle(e)
            }
        }
    }

    fun adminBan(id: String, reason: String, query: String) = adminDo(query) { api.adminBan(id, reason.trim()) }

    fun adminUnban(id: String, query: String) = adminDo(query) { api.adminUnban(id) }

    fun adminDelete(id: String, query: String) = adminDo(query) { api.adminDeleteUser(id) }

    fun adminGrant(id: String) {
        viewModelScope.launch {
            try {
                api.adminGrant(id, "supporter")
                notice = R.string.notice_granted
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = errorRes(e)
                handle(e)
            }
        }
    }

    fun adminResetCode(u: AdminUser) {
        viewModelScope.launch {
            try {
                adminCode = u.email to api.adminResetCode(u.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = errorRes(e)
                handle(e)
            }
        }
    }

    fun closeAdminCode() {
        adminCode = null
    }

    // ---------- players and friends ----------
    fun searchPeople(query: String) {
        peopleJob?.cancel()
        lastPeopleQuery = query
        peopleJob = viewModelScope.launch {
            delay(300)
            try {
                people = api.users(query.trim())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            try {
                friends = api.friends()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun friendAction(personId: String, action: String) {
        viewModelScope.launch {
            try {
                api.friendAction(personId, action)
                loadFriends()
                searchPeople(lastPeopleQuery)
                if (viewedProfile?.person?.id == personId) loadProfileOf(personId, reset = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    fun loadProfileOf(id: String, reset: Boolean = true) {
        if (reset) {
            viewedProfile = null
            viewedComments = emptyList()
        }
        viewModelScope.launch {
            try {
                val prof = api.publicProfile(id)
                viewedProfile = prof
                val missing = prof.library.orEmpty().map { it.gameId }.filter { !gamesCache.containsKey(it) }
                missing.chunked(50).forEach { chunk ->
                    api.gamesByIds(chunk).forEach { gamesCache[it.id] = it }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
        viewModelScope.launch {
            try {
                viewedComments = api.userComments(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    // ---------- library (works offline: changes are queued and sent later) ----------
    private fun applyPendingToLibrary() {
        val pend = offlineStore.pending()
        if (pend.isEmpty()) return
        val map = library.toMutableMap()
        pend.forEach { p ->
            val cur = map[p.gameId] ?: LibraryItem(p.gameId, null, null, null, false, "")
            map[p.gameId] = cur.withEdit(p.fields)
        }
        library = map
    }

    fun loadLibrary() {
        viewModelScope.launch {
            flushPending()
            try {
                val items = api.library()
                offlineStore.saveLibrary(items)
                library = items.associateBy { it.gameId }
                applyPendingToLibrary()
                offline = false
                val missing = library.keys.filter { !gamesCache.containsKey(it) }
                missing.chunked(50).forEach { chunk ->
                    api.gamesByIds(chunk).forEach { gamesCache[it.id] = it }
                }
                saveGamesCache()
                extras.refreshWidget()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handle(e)
            }
        }
    }

    private fun saveGamesCache() {
        val wanted = library.keys
        val keep = gamesCache.values.filter { it.id in wanted } + gamesCache.values.filter { it.id !in wanted }.take(150)
        offlineStore.saveGames(keep)
    }

    private suspend fun flushPending() {
        flushMutex.withLock {
            var list = offlineStore.pending()
            pendingCount = list.size
            var sentAny = false
            while (list.isNotEmpty()) {
                val e = list.first()
                val sent = e.fields.toString()
                try {
                    val body = JSONObject(sent).put("at", e.at)
                    val item = api.updateLibrary(e.gameId, body)
                    library = library + (e.gameId to item)
                    sentAny = true
                    offline = false
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: IOException) {
                    offline = true
                    return
                } catch (ex: ApiException) {
                    if (ex.http == 401 || (ex.http == 403 && ex.code == "banned")) {
                        handle(ex)
                        return
                    }
                    if (ex.http >= 500 || ex.http == 429) return
                    // the server refuses this change for good: drop it so the queue does not get stuck
                }
                val now = offlineStore.pending().toMutableList()
                val idx = now.indexOfFirst { it.gameId == e.gameId }
                if (idx >= 0 && now[idx].fields.toString() == sent) now.removeAt(idx)
                offlineStore.setPending(now)
                pendingCount = now.size
                list = now
            }
            if (sentAny) {
                offlineStore.saveLibrary(library.values)
                pollNotifications()
            }
        }
    }

    private fun update(gameId: Int, build: JSONObject.() -> Unit) {
        val fields = JSONObject().apply(build)
        val cur = library[gameId] ?: LibraryItem(gameId, null, null, null, false, "")
        library = library + (gameId to cur.withEdit(fields))
        offlineStore.saveLibrary(library.values)
        pendingCount = offlineStore.enqueue(gameId, fields, System.currentTimeMillis()).size
        viewModelScope.launch { flushPending() }
        if (fields.has("status")) extras.refreshWidget()
    }

    fun setStatus(gameId: Int, status: String?) = update(gameId) { put("status", status ?: JSONObject.NULL) }

    fun setRating(gameId: Int, rating: Int?) = update(gameId) { put("rating", rating ?: JSONObject.NULL) }

    fun setFavorite(gameId: Int, favorite: Boolean) = update(gameId) { put("favorite", favorite) }

    fun setNote(gameId: Int, note: String) = update(gameId) {
        put("note", if (note.isBlank()) JSONObject.NULL else note.trim())
    }
}
