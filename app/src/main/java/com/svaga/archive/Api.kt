package com.svaga.archive

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.TimeZone
import java.util.concurrent.TimeUnit

const val BASE_URL = "https://45-151-106-142.sslip.io:8443"

val STATUSES = listOf("backlog", "playing", "completed", "dropped", "wont_play")

data class Game(
    val id: Int,
    val name: String,
    val summary: String?,
    val releaseDate: String?,
    val rating: Int?,
    val cover: String?,
    val genres: List<String>,
    val platforms: List<String>,
    val screenshots: List<String>,
    val developers: List<String>,
    val languages: GameLanguages? = null,
    val platformIds: List<Int> = emptyList(),
    val custom: Boolean = false,
    val variantIds: List<Int> = emptyList(),
    val addedBy: String? = null,
    val stores: List<StoreLink> = emptyList(),
) {
    val year: Int? get() = releaseDate?.take(4)?.toIntOrNull()
}

data class StoreLink(val store: String, val url: String, val search: Boolean)

data class GameLanguages(val audio: List<String>, val subtitles: List<String>, val ui: List<String>)

data class LibraryItem(
    val gameId: Int,
    val status: String?,
    val rating: Int?,
    val note: String?,
    val favorite: Boolean,
    val updatedAt: String,
)

data class Profile(
    val id: String,
    val email: String,
    val displayName: String,
    val createdAt: String,
    val avatarVersion: Int,
    val role: String = "user",
    val libraryPublic: Boolean = true,
    val sp: Int = 0,
    val level: Int = 1,
    val featured: List<String> = emptyList(),
    val notifyComeback: Boolean = true,
)

data class AchTier(val tier: Int, val goal: Int, val sp: Int, val unlockedAt: String?)

data class AchItem(
    val code: String,
    val category: String,
    val secret: Boolean,
    val hidden: Boolean,
    val tier: Int,
    val tiers: List<AchTier>,
    val progress: Int,
    val goal: Int,
    val maxed: Boolean,
    val earnedSp: Int,
    val unlockedAt: String?,
    val rarity: Double,
    val featured: Boolean,
    /** Themed and seasonal achievements come with their own title and look from the server. */
    val title: String? = null,
    val description: String? = null,
    val emoji: String? = null,
    val colors: List<Long> = emptyList(),
    val endsAt: String? = null,
)

/** Titles and looks of server-defined achievements (themes, seasons), by code. */
object DynAch {
    data class Look(val title: String, val description: String?, val emoji: String, val a: Long, val b: Long)
    val map = java.util.concurrent.ConcurrentHashMap<String, Look>()
    fun put(code: String, title: String?, description: String?, emoji: String?, colors: List<Long>) {
        if (title.isNullOrBlank()) return
        map[code] = Look(title, description, emoji ?: "🎮", colors.getOrElse(0) { 0xFF3EE0FF }, colors.getOrElse(1) { 0xFF4F7CFF })
    }
}

fun parseHexColor(s: String?): Long? = s?.removePrefix("#")?.takeIf { it.length == 6 }?.toLongOrNull(16)?.let { 0xFF000000 or it }

data class LeaderRow(val rank: Int, val userId: String, val name: String, val avatarVersion: Int, val level: Int, val value: Long, val me: Boolean, val badge: String? = null)
data class Leaderboard(val items: List<LeaderRow>, val me: LeaderRow?)

data class SeasonChallenge(
    val id: Int, val code: String, val title: String, val description: String?, val emoji: String, val colors: List<Long>,
    val progress: Int, val goal: Int, val endsAt: String?, val sp: Int,
)

data class AdminChallenge(
    val id: Int, val kind: String, val title: String, val description: String?, val emoji: String, val metric: String,
    val pattern: String?, val gameIds: List<Int>, val goals: List<Int>, val sp: Int, val endsAt: String?, val active: Boolean, val builtin: Boolean,
)

data class AchievementsResult(
    val sp: Int,
    val level: Int,
    val levelFrom: Int,
    val levelTo: Int,
    val stats: Map<String, Int>,
    val items: List<AchItem>,
    val featured: List<String>,
)

data class NotificationItem(val id: Long, val kind: String, val payload: JSONObject, val read: Boolean, val createdAt: String)

data class NotificationsResult(val unread: Int, val items: List<NotificationItem>)

data class NotifCount(val unread: Int, val lastId: Long, val lastAchievementId: Long)

data class StatsResult(
    val period: String,
    val window: Map<String, Int>,
    val series: List<Pair<String, Int>>,
    val byStatus: Map<String, Int>,
    val total: Map<String, Int>,
    val avgRating: Double?,
    val sp: Int,
    val level: Int,
)

data class Comment(
    val id: Long,
    val userId: String,
    val name: String,
    val avatarVersion: Int,
    val body: String,
    val createdAt: String,
    val rating: Int?,
    val likes: Int,
    val liked: Boolean,
    val gameId: Int = 0,
    val gameName: String? = null,
    val badge: String? = null,
)

data class GameStats(val avgRating: Double?, val ratingCount: Int, val inLibrary: Int, val completed: Int, val playing: Int = 0, val backlog: Int = 0, val dropped: Int = 0)

data class Platform(
    val id: Int,
    val name: String,
    val short: String,
    val family: String? = null,
    val generation: Int? = null,
    val logo: String? = null,
    val year: Int? = null,
    val games: Int? = null,
    val custom: Boolean = false,
)

data class PlatformsResult(val pinned: List<Int>, val items: List<Platform>)

data class Person(
    val id: String,
    val name: String,
    val avatarVersion: Int,
    val createdAt: String,
    val relation: String,
    val library: Int,
    val completed: Int,
    val level: Int = 1,
    val creator: Boolean = false,
    val badge: String? = null,
)

data class FriendsResult(val friends: List<Person>, val incoming: List<Person>, val outgoing: List<Person>)

data class FeaturedAch(val code: String, val tier: Int)

data class PublicProfile(
    val person: Person,
    val achievements: Int,
    val library: List<LibraryItem>?,
    val libraryPublic: Boolean = true,
    val sp: Int = 0,
    val level: Int = 1,
    val levelFrom: Int = 0,
    val levelTo: Int = 100,
    val featured: List<FeaturedAch> = emptyList(),
    val frame: String = "none",
    val background: String = "none",
    val badge: String? = null,
)

// ---- 1.1: retro bingo, profile frames and backgrounds ----
data class BingoCell(val id: String, val title: String, val emoji: String, val goal: Int, val progress: Int, val done: Boolean)
data class Bingo(val started: Boolean, val startsAt: String, val season: String, val endsAt: String, val cells: List<BingoCell>, val lines: List<Int>, val linesTotal: Int, val full: Boolean)
data class Ttb(val main: Double?, val extra: Double?, val full: Double?)
data class Forecast(val games: Int, val known: Int, val hours: Int)
data class Compat(val hidden: Boolean, val percent: Int, val shared: Int, val lovedBoth: List<Int>, val theyHave: List<Int>, val iHave: List<Int>)
data class CosItem(val code: String, val unlocked: Boolean, val kind: String?, val progress: Int, val goal: Int)
data class Cosmetics(val frame: String, val background: String, val frames: List<CosItem>, val backgrounds: List<CosItem>, val badge: String = "none", val badges: List<CosItem> = emptyList())
data class SeasonQuest(val id: String, val title: String, val emoji: String, val goal: Int, val progress: Int, val done: Boolean, val xp: Int)
data class SeasonSource(val kind: String, val count: Int, val xp: Int)
data class SeasonReward(val level: Int, val kind: String, val code: String, val unlocked: Boolean)
data class SeasonPass(val number: Int, val started: Boolean, val startsAt: String, val season: String, val endsAt: String, val xp: Int, val level: Int, val levelXp: Int, val maxLevel: Int, val sources: List<SeasonSource>, val quests: List<SeasonQuest>, val weekEndsAt: String, val rewards: List<SeasonReward>)
data class SeasonTopRow(val rank: Int, val userId: String, val name: String, val avatarVersion: Int, val badge: String?, val xp: Int, val level: Int, val me: Boolean)

data class BrowseResult(val items: List<Game>, val rawCount: Int)

data class Related(val prequels: List<Game>, val sequels: List<Game>)

data class RuTranslation(
    val id: Long,
    val studio: String,
    val platform: String?,
    val version: String?,
    val url: String?,
    val svaga: Boolean,
    val addedBy: String?,
    val mine: Boolean,
)

data class IdentityItem(val provider: String, val name: String?, val email: String?)

data class IdentitiesResult(val hasPassword: Boolean, val available: List<String>, val items: List<IdentityItem>, val paused: List<String> = emptyList())

data class AdminUser(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val banned: Boolean,
    val banReason: String?,
    val avatarVersion: Int,
)

data class NewGame(
    val name: String,
    val year: Int?,
    val summary: String,
    val coverUrl: String,
    val developers: List<String>,
    val genres: List<String>,
    val platformIds: List<Int>,
)

data class ActivityDay(val day: String, val count: Int)

data class FriendEvent(
    val userId: String,
    val name: String,
    val avatarVersion: Int,
    val gameId: Int,
    val status: String?,
    val rating: Int?,
    val favorite: Boolean,
    val at: String,
    val badge: String? = null,
)

data class NewsItem(
    val id: Long,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val gameId: Int?,
    val author: String?,
    val createdAt: String,
)

data class PlaytimeItem(val gameId: Int, val seconds: Long, val sessions: Int, val lastPlayed: String)

data class PlaytimeResult(val totalSeconds: Long, val items: List<PlaytimeItem>)

data class CollectionCopy(
    val id: Long,
    val gameId: Int,
    val platform: String?,
    val region: String?,
    val hasCart: Boolean,
    val hasBox: Boolean,
    val hasManual: Boolean,
    val sealed: Boolean,
    val condition: String?,
    val price: Double?,
    val currency: String?,
    val acquiredOn: String?,
    val note: String?,
) {
    val complete: Boolean get() = hasCart && hasBox && hasManual
}

data class MoneyTotal(val currency: String, val sum: Double, val count: Int)

data class CollectionResult(val items: List<CollectionCopy>, val copies: Int, val games: Int, val complete: Int, val totals: List<MoneyTotal>)

data class SteamStatus(val configured: Boolean, val steamId: String?, val importedAt: String?)

data class SteamImportResult(val owned: Int, val matched: Int, val added: Int, val already: Int, val hours: Int, val unmatched: List<String>)

data class RaGame(
    val raId: Int, val title: String, val console: String, val icon: String?,
    val total: Int, val earned: Int, val earnedHardcore: Int, val award: String?,
) {
    val beaten: Boolean get() = award != null
    val mastered: Boolean get() = award == "mastered" || award == "completed"
}

data class RaAch(val id: Int, val title: String, val description: String, val points: Int, val hardcore: Boolean, val badge: String?, val game: String, val console: String, val at: String)

data class RaSummary(
    val user: String, val avatar: String?, val memberSince: String?, val points: Int, val softPoints: Int, val truePoints: Int,
    val presence: String?, val gamesCount: Int, val beaten: Int, val mastered: Int, val games: List<RaGame>, val recent: List<RaAch>,
    val stale: Boolean,
)

data class RaState(val configured: Boolean, val linked: Boolean, val summary: RaSummary?)

data class EmulatorInfo(val enabled: Boolean, val romUrl: String?, val romName: String?)

data class RaImportResult(val added: Int, val updated: Int, val unmatched: List<String>)

fun avatarUrl(userId: String, version: Int) = "$BASE_URL/v1/users/$userId/avatar?v=$version"

fun formatDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return try {
        java.time.OffsetDateTime.parse(iso).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    } catch (e: Exception) {
        iso.take(10)
    }
}

class ApiException(val code: String, val http: Int, val body: JSONObject = JSONObject()) : Exception(code)

class TokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("svaga_session", Context.MODE_PRIVATE)

    var access: String?
        get() = prefs.getString("access", null)
        set(value) { prefs.edit().putString("access", value).apply() }

    var refresh: String?
        get() = prefs.getString("refresh", null)
        set(value) { prefs.edit().putString("refresh", value).apply() }

    var email: String
        get() = prefs.getString("email", "") ?: ""
        set(value) { prefs.edit().putString("email", value).apply() }

    var displayName: String
        get() = prefs.getString("displayName", "") ?: ""
        set(value) { prefs.edit().putString("displayName", value).apply() }

    var userId: String
        get() = prefs.getString("userId", "") ?: ""
        set(value) { prefs.edit().putString("userId", value).apply() }

    var createdAt: String
        get() = prefs.getString("createdAt", "") ?: ""
        set(value) { prefs.edit().putString("createdAt", value).apply() }

    var avatarVersion: Int
        get() = prefs.getInt("avatarVersion", 0)
        set(value) { prefs.edit().putInt("avatarVersion", value).apply() }

    var role: String
        get() = prefs.getString("role", "user") ?: "user"
        set(value) { prefs.edit().putString("role", value).apply() }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

private fun JSONObject.str(name: String): String? = if (isNull(name)) null else optString(name)

private fun JSONObject.intOrNull(name: String): Int? = if (isNull(name)) null else optInt(name)

private fun JSONArray.strings(): List<String> =
    (0 until length()).mapNotNull { i -> if (isNull(i)) null else optString(i) }

private fun JSONArray.ints(): List<Int> = (0 until length()).map { optInt(it) }

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

private fun JSONObject.intMap(): Map<String, Int> = keys().asSequence().associateWith { optInt(it) }

fun parseGame(o: JSONObject): Game = Game(
    id = o.getInt("id"),
    name = o.optString("name"),
    summary = o.str("summary"),
    releaseDate = o.str("releaseDate"),
    rating = o.intOrNull("rating"),
    cover = o.str("cover"),
    genres = o.optJSONArray("genres")?.strings() ?: emptyList(),
    platforms = o.optJSONArray("platforms")?.strings() ?: emptyList(),
    screenshots = o.optJSONArray("screenshots")?.strings() ?: emptyList(),
    developers = o.optJSONArray("developers")?.strings() ?: emptyList(),
    languages = o.optJSONObject("languages")?.let { l ->
        GameLanguages(
            audio = l.optJSONArray("audio")?.strings() ?: emptyList(),
            subtitles = l.optJSONArray("subtitles")?.strings() ?: emptyList(),
            ui = l.optJSONArray("interface")?.strings() ?: emptyList(),
        )
    },
    platformIds = o.optJSONArray("platformIds")?.ints() ?: emptyList(),
    custom = o.optBoolean("custom", false),
    variantIds = o.optJSONArray("variantIds")?.ints() ?: emptyList(),
    addedBy = o.str("addedBy"),
    stores = (o.optJSONArray("stores") ?: JSONArray()).objects().mapNotNull {
        val u = it.optString("url")
        if (u.startsWith("http")) StoreLink(it.optString("store"), u, it.optBoolean("search")) else null
    },
)

fun gameToJson(g: Game): JSONObject = JSONObject()
    .put("id", g.id)
    .put("name", g.name)
    .put("summary", g.summary ?: JSONObject.NULL)
    .put("releaseDate", g.releaseDate ?: JSONObject.NULL)
    .put("rating", g.rating ?: JSONObject.NULL)
    .put("cover", g.cover ?: JSONObject.NULL)
    .put("genres", JSONArray(g.genres))
    .put("platforms", JSONArray(g.platforms))
    .put("platformIds", JSONArray(g.platformIds))
    .put("developers", JSONArray(g.developers))
    .put("custom", g.custom)
    .put("variantIds", JSONArray(g.variantIds))

private fun parseGames(o: JSONObject): List<Game> {
    val arr = o.optJSONArray("items") ?: return emptyList()
    return (0 until arr.length()).map { parseGame(arr.getJSONObject(it)) }
}

fun parseLibraryItem(o: JSONObject): LibraryItem = LibraryItem(
    gameId = o.getInt("gameId"),
    status = o.str("status"),
    rating = o.intOrNull("rating"),
    note = o.str("note"),
    favorite = o.optBoolean("favorite", false),
    updatedAt = o.str("updatedAt") ?: "",
)

fun libraryItemToJson(i: LibraryItem): JSONObject = JSONObject()
    .put("gameId", i.gameId)
    .put("status", i.status ?: JSONObject.NULL)
    .put("rating", i.rating ?: JSONObject.NULL)
    .put("note", i.note ?: JSONObject.NULL)
    .put("favorite", i.favorite)
    .put("updatedAt", i.updatedAt)

private fun parsePerson(o: JSONObject): Person = Person(
    id = o.optString("id"),
    name = o.optString("name"),
    avatarVersion = o.optInt("avatarVersion", 0),
    createdAt = o.str("createdAt") ?: "",
    relation = o.optString("relation", "none"),
    library = o.optInt("library", 0),
    completed = o.optInt("completed", 0),
    level = o.optInt("level", 1),
    creator = o.optBoolean("creator", false),
    badge = o.str("badge"),
)

private fun parseComment(c: JSONObject): Comment = Comment(
    id = c.optLong("id"),
    userId = c.optString("userId"),
    name = c.optString("name"),
    avatarVersion = c.optInt("avatarVersion", 0),
    body = c.optString("body"),
    createdAt = c.str("createdAt") ?: "",
    rating = c.intOrNull("rating"),
    likes = c.optInt("likes", 0),
    liked = c.optBoolean("liked", false),
    gameId = c.optInt("gameId", 0),
    gameName = c.str("gameName"),
    badge = c.str("badge"),
)

private fun parseComments(o: JSONObject): List<Comment> =
    (o.optJSONArray("items") ?: JSONArray()).objects().map { parseComment(it) }

private fun parseAchItem(a: JSONObject): AchItem = AchItem(
    code = a.optString("code"),
    category = a.optString("category"),
    secret = a.optBoolean("secret", false),
    hidden = a.optBoolean("hidden", false),
    tier = a.optInt("tier", 0),
    tiers = (a.optJSONArray("tiers") ?: JSONArray()).objects().map {
        AchTier(it.optInt("tier"), it.optInt("goal", 1), it.optInt("sp"), it.str("unlockedAt"))
    },
    progress = a.optInt("progress", 0),
    goal = a.optInt("goal", 1),
    maxed = a.optBoolean("maxed", false),
    earnedSp = a.optInt("earnedSp", 0),
    unlockedAt = a.str("unlockedAt"),
    rarity = a.optDouble("rarity", 0.0).let { if (it.isNaN()) 0.0 else it },
    featured = a.optBoolean("featured", false),
    title = a.str("title"),
    description = a.str("description"),
    emoji = a.str("emoji"),
    colors = (a.optJSONArray("colors") ?: JSONArray()).strings().mapNotNull { parseHexColor(it) },
    endsAt = a.optJSONObject("season")?.str("endsAt"),
).also { DynAch.put(it.code, it.title, it.description, it.emoji, it.colors) }

private fun tzMinutes(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000


// ---- 1.7: on this day, daily quiz, game lists ----
data class AnnivGame(val game: Game, val yearsAgo: Int, val inLibrary: Boolean)
data class QuizGuess(val gameId: Int, val name: String)
data class QuizState(
    val day: String, val image: String, val fx: Float, val fy: Float, val maxAttempts: Int, val attempts: Int,
    val finished: Boolean, val solved: Boolean, val points: Int, val guesses: List<QuizGuess>,
    val year: Int?, val platform: String?, val genre: String?, val developer: String?,
    val answer: Game?, val streak: Int, val playedToday: Int, val solvedToday: Int,
)
data class GameList(
    val id: Long, val title: String, val description: String?, val public: Boolean, val count: Int, val likes: Int,
    val liked: Boolean, val preview: List<Int>, val mine: Boolean, val ownerId: String, val ownerName: String,
    val ownerAvatar: Int, val hasGame: Boolean = false,
)
data class ListEntry(val gameId: Int, val note: String?)
data class ListDetail(val list: GameList, val items: List<ListEntry>)

class ApiClient(private val store: TokenStore) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("X-TZ-Offset", tzMinutes().toString()).build())
        }
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val refreshLock = Any()

    private fun execute(
        method: String,
        path: String,
        body: JSONObject?,
        auth: Boolean,
        raw: ByteArray? = null,
    ): JSONObject {
        val builder = Request.Builder().url(BASE_URL + path)
        if (auth) store.access?.let { builder.header("Authorization", "Bearer $it") }
        when (method) {
            "GET" -> builder.get()
            "DELETE" -> if (body != null) builder.delete(body.toString().toRequestBody(jsonType)) else builder.delete()
            else -> builder.method(
                method,
                raw?.toRequestBody("image/jpeg".toMediaType())
                    ?: (body ?: JSONObject()).toString().toRequestBody(jsonType),
            )
        }
        http.newCall(builder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val obj = try {
                JSONObject(if (text.isBlank()) "{}" else text)
            } catch (e: Exception) {
                JSONObject()
            }
            if (!resp.isSuccessful) {
                throw ApiException(obj.optString("error", "http_${resp.code}"), resp.code, obj)
            }
            return obj
        }
    }

    private fun saveSession(o: JSONObject) {
        store.access = o.getString("accessToken")
        store.refresh = o.getString("refreshToken")
        val user = o.getJSONObject("user")
        store.email = user.optString("email")
        store.displayName = user.str("displayName") ?: ""
        store.userId = user.optString("id")
        store.role = user.optString("role", "user")
    }

    /**
     * Only one refresh runs at a time (the refresh token is single-use). Returns false only when the server
     * definitively rejected the refresh token; network errors and 5xx/429 are thrown, so the session survives them.
     */
    private fun refreshTokens(usedAccess: String?): Boolean {
        synchronized(refreshLock) {
            val current = store.access
            if (current != null && current != usedAccess) return true // someone has just refreshed
            val rt = store.refresh ?: return false
            try {
                val o = execute("POST", "/v1/auth/refresh", JSONObject().put("refreshToken", rt), false)
                store.access = o.getString("accessToken")
                store.refresh = o.getString("refreshToken")
                return true
            } catch (e: ApiException) {
                if (e.http == 401 || e.http == 403) return false else throw e
            }
        }
    }

    private fun call(method: String, path: String, body: JSONObject? = null, raw: ByteArray? = null): JSONObject {
        val used = store.access
        try {
            return execute(method, path, body, true, raw)
        } catch (e: ApiException) {
            if (e.http == 401) {
                if (refreshTokens(used)) return execute(method, path, body, true, raw)
                store.clear()
            } else if (e.http == 403 && e.code == "banned") {
                store.clear()
            }
            throw e
        }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    // ---- auth ----
    suspend fun login(email: String, password: String) = io {
        val body = JSONObject().put("email", email).put("password", password)
        saveSession(execute("POST", "/v1/auth/login", body, false))
    }

    suspend fun register(email: String, password: String, name: String) = io {
        val body = JSONObject().put("email", email).put("password", password)
        if (name.isNotBlank()) body.put("displayName", name)
        saveSession(execute("POST", "/v1/auth/register", body, false))
    }

    // ---- Svaga ID (Google / Yandex / VK through the system browser) ----
    /** Enabled providers and the ones shown with a "not enabled yet" note. */
    suspend fun oauthProviders(): Pair<List<String>, List<String>> = io {
        val o = execute("GET", "/v1/auth/providers", null, false)
        fun list(k: String) = (o.optJSONArray(k) ?: JSONArray()).let { a -> List(a.length()) { a.getString(it) } }
        list("providers") to list("paused")
    }

    fun oauthStartUrl(provider: String) = "$BASE_URL/v1/auth/oauth/$provider/start"

    suspend fun oauthExchange(code: String) = io {
        saveSession(execute("POST", "/v1/auth/oauth/exchange", JSONObject().put("code", code), false))
    }

    /**
     * Yandex through the phone: our server cannot reach Yandex, so the app asks Yandex for the profile itself.
     * Yandex answers with a JWT signed with the app's secret; the server checks the signature and finishes the sign-in.
     * Returns the same answer as the deep link: code / linked / error.
     */
    suspend fun yandexViaPhone(token: String, state: String): JSONObject = io {
        val req = Request.Builder().url("https://login.yandex.ru/info?format=jwt")
            .header("Authorization", "OAuth $token").get().build()
        val jwt = http.newCall(req).execute().use { r ->
            val t = r.body?.string().orEmpty().trim()
            if (!r.isSuccessful || t.count { it == '.' } != 2) throw ApiException("provider", r.code)
            t
        }
        execute("POST", "/v1/auth/oauth/yandex/jwt", JSONObject().put("state", state).put("jwt", jwt), false)
    }

    suspend fun identities(): IdentitiesResult = io {
        val o = call("GET", "/v1/me/identities")
        val items = o.optJSONArray("items")
        val av = o.optJSONArray("available")
        IdentitiesResult(
            hasPassword = o.optBoolean("hasPassword", true),
            available = List(av?.length() ?: 0) { av!!.getString(it) },
            paused = (o.optJSONArray("paused") ?: JSONArray()).let { a -> List(a.length()) { a.getString(it) } },
            items = List(items?.length() ?: 0) {
                val r = items!!.getJSONObject(it)
                IdentityItem(r.getString("provider"), r.str("name"), r.str("email"))
            },
        )
    }

    suspend fun identityLinkUrl(provider: String): String = io {
        call("POST", "/v1/me/identities/$provider/start", JSONObject()).getString("url")
    }

    suspend fun unlinkIdentity(provider: String) = io {
        call("DELETE", "/v1/me/identities/$provider")
        Unit
    }

    suspend fun forgot(email: String): Boolean = io {
        execute("POST", "/v1/auth/forgot", JSONObject().put("email", email), false).optBoolean("mail", false)
    }

    suspend fun resetPassword(email: String, code: String, password: String) = io {
        execute(
            "POST", "/v1/auth/reset",
            JSONObject().put("email", email).put("code", code).put("password", password), false,
        )
        Unit
    }

    suspend fun logout() = io {
        try {
            store.refresh?.let {
                execute("POST", "/v1/auth/logout", JSONObject().put("refreshToken", it), false)
            }
        } catch (e: Exception) {
            // ignore: local logout still happens
        }
        store.clear()
    }

    // ---- catalog ----
    suspend fun browse(
        section: String,
        genre: Int?,
        platform: Int?,
        yearFrom: Int?,
        yearTo: Int?,
        minRating: Int?,
        offset: Int = 0,
    ): BrowseResult = io {
        val q = StringBuilder("/v1/catalog/browse?section=$section&offset=$offset")
        if (genre != null) q.append("&genre=").append(genre)
        if (platform != null) q.append("&platform=").append(platform)
        if (yearFrom != null) q.append("&yearFrom=").append(yearFrom)
        if (yearTo != null) q.append("&yearTo=").append(yearTo)
        if (minRating != null) q.append("&minRating=").append(minRating)
        val o = call("GET", q.toString())
        val items = parseGames(o)
        BrowseResult(items, o.optInt("rawCount", items.size))
    }

    suspend fun translation(gameId: Int, lang: String): String? = io {
        call("GET", "/v1/catalog/games/$gameId/translation?lang=$lang").str("summary")
    }

    suspend fun gameDetail(id: Int): Game = io { parseGame(call("GET", "/v1/catalog/games/$id")) }

    suspend fun related(id: Int): Related = io {
        val o = call("GET", "/v1/catalog/games/$id/related")
        fun list(key: String) = (o.optJSONArray(key) ?: JSONArray()).objects().map { parseGame(it) }
        Related(list("prequels"), list("sequels"))
    }

    suspend fun platforms(): PlatformsResult = io {
        val o = call("GET", "/v1/catalog/platforms")
        val items = (o.optJSONArray("items") ?: JSONArray()).objects().map {
            Platform(
                id = it.getInt("id"),
                name = it.optString("name"),
                short = it.optString("short"),
                family = it.str("family"),
                generation = it.intOrNull("generation"),
                logo = it.str("logo"),
                year = it.intOrNull("year"),
                games = it.intOrNull("games"),
                custom = it.optBoolean("custom", false),
            )
        }
        val pinnedArr = o.optJSONArray("pinned") ?: JSONArray()
        PlatformsResult((0 until pinnedArr.length()).map { pinnedArr.getInt(it) }, items)
    }

    suspend fun search(query: String): List<Game> = io {
        parseGames(call("GET", "/v1/catalog/search?q=" + enc(query)))
    }

    suspend fun gamesByIds(ids: List<Int>): List<Game> = io {
        if (ids.isEmpty()) emptyList() else parseGames(call("GET", "/v1/catalog/games?ids=" + ids.joinToString(",")))
    }

    // ---- library ----
    suspend fun library(): List<LibraryItem> = io {
        val arr = call("GET", "/v1/library").optJSONArray("items") ?: JSONArray()
        (0 until arr.length()).map { parseLibraryItem(arr.getJSONObject(it)) }
    }

    suspend fun updateLibrary(gameId: Int, fields: JSONObject): LibraryItem = io {
        val o = call("PUT", "/v1/library/$gameId", fields)
        LibraryItem(
            gameId = gameId,
            status = o.str("status"),
            rating = o.intOrNull("rating"),
            note = o.str("note"),
            favorite = o.optBoolean("favorite", false),
            updatedAt = java.time.Instant.now().toString(),
        )
    }

    // ---- profile ----
    suspend fun me(): Profile = io {
        val o = call("GET", "/v1/me")
        Profile(
            id = o.optString("id"),
            email = o.optString("email"),
            displayName = o.str("displayName") ?: "",
            createdAt = o.str("createdAt") ?: "",
            avatarVersion = o.optInt("avatarVersion", 0),
            role = o.optString("role", "user"),
            libraryPublic = o.optBoolean("libraryPublic", true),
            sp = o.optInt("sp", 0),
            level = o.optInt("level", 1),
            featured = o.optJSONArray("featured")?.strings() ?: emptyList(),
            notifyComeback = o.optBoolean("notifyComeback", true),
        )
    }

    suspend fun updateName(name: String): String? = io {
        call("PATCH", "/v1/me", JSONObject().put("displayName", name)).str("displayName")
    }

    suspend fun setLibraryPublic(value: Boolean): Boolean = io {
        call("PATCH", "/v1/me", JSONObject().put("libraryPublic", value)).optBoolean("libraryPublic", value)
    }

    suspend fun uploadAvatar(bytes: ByteArray): Int = io {
        call("PUT", "/v1/me/avatar", raw = bytes).optInt("avatarVersion", 0)
    }

    suspend fun deleteAvatar() = io {
        call("DELETE", "/v1/me/avatar")
        Unit
    }

    // ---- achievements, notifications, statistics ----
    suspend fun achievements(): AchievementsResult = io {
        val o = call("GET", "/v1/me/achievements")
        AchievementsResult(
            sp = o.optInt("sp", 0),
            level = o.optInt("level", 1),
            levelFrom = o.optInt("levelFrom", 0),
            levelTo = o.optInt("levelTo", 100),
            stats = (o.optJSONObject("stats") ?: JSONObject()).intMap(),
            items = (o.optJSONArray("items") ?: JSONArray()).objects().map { parseAchItem(it) },
            featured = o.optJSONArray("featured")?.strings() ?: emptyList(),
        )
    }

    suspend fun setFeatured(codes: List<String>): List<String> = io {
        call("PUT", "/v1/me/featured", JSONObject().put("codes", JSONArray(codes))).optJSONArray("featured")?.strings()
            ?: emptyList()
    }

    suspend fun notifications(offset: Int = 0): NotificationsResult = io {
        val o = call("GET", "/v1/notifications?offset=$offset")
        NotificationsResult(
            unread = o.optInt("unread", 0),
            items = (o.optJSONArray("items") ?: JSONArray()).objects().map {
                NotificationItem(
                    id = it.optLong("id"),
                    kind = it.optString("kind"),
                    payload = it.optJSONObject("payload") ?: JSONObject(),
                    read = it.optBoolean("read", false),
                    createdAt = it.str("createdAt") ?: "",
                )
            },
        )
    }

    suspend fun notifCount(): NotifCount = io {
        val o = call("GET", "/v1/notifications/count")
        NotifCount(o.optInt("unread", 0), o.optLong("lastId", 0), o.optLong("lastAchievementId", 0))
    }

    suspend fun markNotificationsRead() = io {
        call("POST", "/v1/notifications/read", JSONObject())
        Unit
    }

    suspend fun stats(period: String): StatsResult = io {
        val o = call("GET", "/v1/me/stats?period=$period")
        val series = (o.optJSONArray("series") ?: JSONArray()).objects().map { it.optString("at") to it.optInt("count") }
        StatsResult(
            period = o.optString("period", period),
            window = (o.optJSONObject("window") ?: JSONObject()).intMap(),
            series = series,
            byStatus = (o.optJSONObject("byStatus") ?: JSONObject()).intMap(),
            total = (o.optJSONObject("total") ?: JSONObject()).intMap(),
            avgRating = if (o.isNull("avgRating")) null else o.optDouble("avgRating"),
            sp = o.optInt("sp", 0),
            level = o.optInt("level", 1),
        )
    }

    // ---- comments ----
    suspend fun comments(gameId: Int): List<Comment> = io { parseComments(call("GET", "/v1/games/$gameId/comments")) }

    suspend fun allComments(offset: Int = 0): List<Comment> = io { parseComments(call("GET", "/v1/comments?offset=$offset")) }

    suspend fun myComments(offset: Int = 0): List<Comment> = io { parseComments(call("GET", "/v1/me/comments?offset=$offset")) }

    suspend fun userComments(id: String): List<Comment> = io { parseComments(call("GET", "/v1/users/$id/comments")) }

    suspend fun postComment(gameId: Int, text: String) = io {
        call("POST", "/v1/games/$gameId/comments", JSONObject().put("body", text))
        Unit
    }

    suspend fun setLike(commentId: Long, like: Boolean) = io {
        call(if (like) "POST" else "DELETE", "/v1/comments/$commentId/like")
        Unit
    }

    suspend fun deleteComment(id: Long, asCreator: Boolean) = io {
        call("DELETE", if (asCreator) "/v1/admin/comments/$id" else "/v1/comments/$id")
        Unit
    }

    suspend fun gameStats(gameId: Int, variants: List<Int>): GameStats = io {
        val extra = if (variants.isEmpty()) "" else "?ids=" + variants.joinToString(",")
        val o = call("GET", "/v1/games/$gameId/stats$extra")
        GameStats(
            avgRating = if (o.isNull("avgRating")) null else o.optDouble("avgRating"),
            ratingCount = o.optInt("ratingCount", 0),
            inLibrary = o.optInt("inLibrary", 0),
            completed = o.optInt("completed", 0),
            playing = o.optInt("playing", 0),
            backlog = o.optInt("backlog", 0),
            dropped = o.optInt("dropped", 0),
        )
    }

    // ---- user contributions ----
    suspend fun addPlatform(gameId: Int, platformId: Int) = io {
        call("POST", "/v1/games/$gameId/platforms", JSONObject().put("platformId", platformId))
        Unit
    }

    suspend fun createGame(g: NewGame): Game = io {
        val body = JSONObject()
            .put("name", g.name)
            .put("platformIds", JSONArray(g.platformIds))
            .put("developers", JSONArray(g.developers))
            .put("genres", JSONArray(g.genres))
        if (g.year != null) body.put("year", g.year)
        if (g.summary.isNotBlank()) body.put("summary", g.summary)
        if (g.coverUrl.isNotBlank()) body.put("coverUrl", g.coverUrl)
        parseGame(call("POST", "/v1/games", body))
    }

    suspend fun ruTranslations(gameId: Int): List<RuTranslation> = io {
        val o = call("GET", "/v1/games/$gameId/ru-translations")
        (o.optJSONArray("items") ?: JSONArray()).objects().map {
            RuTranslation(
                id = it.optLong("id"),
                studio = it.optString("studio"),
                platform = it.str("platform"),
                version = it.str("version"),
                url = it.str("url"),
                svaga = it.optBoolean("svaga", false),
                addedBy = it.str("addedBy"),
                mine = it.optBoolean("mine", false),
            )
        }
    }

    suspend fun addRuTranslation(gameId: Int, studio: String, platform: String, version: String, url: String) = io {
        val body = JSONObject().put("studio", studio)
        if (platform.isNotBlank()) body.put("platform", platform)
        if (version.isNotBlank()) body.put("version", version)
        if (url.isNotBlank()) body.put("url", url)
        call("POST", "/v1/games/$gameId/ru-translations", body)
        Unit
    }

    suspend fun deleteRuTranslation(id: Long) = io {
        call("DELETE", "/v1/ru-translations/$id")
        Unit
    }

    suspend fun adminDeleteGame(id: Int) = io {
        call("DELETE", "/v1/admin/games/$id")
        Unit
    }

    // ---- Creator tools ----
    suspend fun adminUsers(query: String): List<AdminUser> = io {
        val o = call("GET", "/v1/admin/users?q=" + enc(query))
        (o.optJSONArray("items") ?: JSONArray()).objects().map {
            AdminUser(
                id = it.optString("id"),
                email = it.optString("email"),
                name = it.optString("name"),
                role = it.optString("role", "user"),
                banned = it.optBoolean("banned", false),
                banReason = it.str("banReason"),
                avatarVersion = it.optInt("avatarVersion", 0),
            )
        }
    }

    suspend fun adminBan(id: String, reason: String) = io {
        call("POST", "/v1/admin/users/$id/ban", JSONObject().put("reason", reason))
        Unit
    }

    suspend fun adminUnban(id: String) = io {
        call("POST", "/v1/admin/users/$id/unban", JSONObject())
        Unit
    }

    suspend fun adminDeleteUser(id: String) = io {
        call("DELETE", "/v1/admin/users/$id")
        Unit
    }

    suspend fun adminResetCode(id: String): String = io {
        call("POST", "/v1/admin/users/$id/reset-code", JSONObject()).optString("code")
    }

    suspend fun adminGrant(id: String, code: String) = io {
        call("POST", "/v1/admin/users/$id/grant", JSONObject().put("code", code))
        Unit
    }

    // ---- players and friends ----
    suspend fun users(query: String, offset: Int = 0): List<Person> = io {
        val o = call("GET", "/v1/users?q=" + enc(query) + "&offset=$offset")
        (o.optJSONArray("items") ?: JSONArray()).objects().map { parsePerson(it) }
    }

    suspend fun friends(): FriendsResult = io {
        val o = call("GET", "/v1/friends")
        fun list(key: String) = (o.optJSONArray(key) ?: JSONArray()).objects().map { parsePerson(it) }
        FriendsResult(list("friends"), list("incoming"), list("outgoing"))
    }

    suspend fun friendAction(id: String, action: String) = io {
        when (action) {
            "add" -> call("POST", "/v1/friends/$id")
            "accept" -> call("POST", "/v1/friends/$id/accept")
            else -> call("DELETE", "/v1/friends/$id")
        }
        Unit
    }

    suspend fun publicProfile(id: String): PublicProfile = io {
        val o = call("GET", "/v1/users/$id/profile")
        val lib = if (o.isNull("library")) null else o.getJSONArray("library").objects().map { parseLibraryItem(it) }
        PublicProfile(
            person = parsePerson(o.getJSONObject("person")),
            achievements = o.optInt("achievements", 0),
            library = lib,
            libraryPublic = o.optBoolean("libraryPublic", true),
            sp = o.optInt("sp", 0),
            level = o.optInt("level", 1),
            levelFrom = o.optInt("levelFrom", 0),
            levelTo = o.optInt("levelTo", 100),
            featured = (o.optJSONArray("featured") ?: JSONArray()).objects().map { FeaturedAch(it.optString("code"), it.optInt("tier", 1)) },
            frame = o.optString("frame", "none").ifBlank { "none" },
            background = o.optString("background", "none").ifBlank { "none" },
            badge = o.str("badge"),
        )
    }

    suspend fun bingo(): Bingo = io {
        val o = call("GET", "/v1/bingo")
        val ru = java.util.Locale.getDefault().language == "ru"
        Bingo(
            o.optBoolean("started", true), o.optString("startsAt"), o.optString("season"), o.optString("endsAt"),
            (o.optJSONArray("cells") ?: JSONArray()).objects().map {
                BingoCell(it.optString("id"), it.optString(if (ru) "ru" else "en"), it.optString("emoji"), it.optInt("goal"), it.optInt("progress"), it.optBoolean("done"))
            },
            (o.optJSONArray("lines") ?: JSONArray()).ints(), o.optInt("linesTotal", 12), o.optBoolean("full"),
        )
    }

    private fun parseCos(o: JSONObject): Cosmetics {
        fun items(a: JSONArray?) = (a ?: JSONArray()).objects().map {
            CosItem(it.optString("code"), it.optBoolean("unlocked"), it.str("kind"), it.optInt("progress"), it.optInt("goal"))
        }
        return Cosmetics(
            o.optString("frame", "none"), o.optString("background", "none"), items(o.optJSONArray("frames")), items(o.optJSONArray("backgrounds")),
            o.optString("badge", "none").ifBlank { "none" }, items(o.optJSONArray("badges")),
        )
    }

    suspend fun cosmetics(): Cosmetics = io { parseCos(call("GET", "/v1/me/cosmetics")) }
    suspend fun setCosmetics(frame: String?, background: String?, badge: String? = null): Triple<String, String, String> = io {
        val b = JSONObject()
        frame?.let { b.put("frame", it) }
        background?.let { b.put("background", it) }
        badge?.let { b.put("badge", it) }
        val o = call("PUT", "/v1/me/cosmetics", b)
        Triple(o.optString("frame", "none"), o.optString("background", "none"), o.optString("badge", "none").ifBlank { "none" })
    }

    suspend fun season(fresh: Boolean = false): SeasonPass = io {
        val o = call("GET", "/v1/season" + if (fresh) "?fresh=1" else "")
        val ru = java.util.Locale.getDefault().language == "ru"
        SeasonPass(
            o.optInt("number", 1), o.optBoolean("started", true), o.optString("startsAt"), o.optString("season"), o.optString("endsAt"), o.optInt("xp"), o.optInt("level"), o.optInt("levelXp", 250), o.optInt("maxLevel", 30),
            (o.optJSONArray("sources") ?: JSONArray()).objects().map { SeasonSource(it.optString("kind"), it.optInt("count"), it.optInt("xp")) },
            (o.optJSONArray("quests") ?: JSONArray()).objects().map {
                SeasonQuest(it.optString("id"), it.optString(if (ru) "ru" else "en"), it.optString("emoji"), it.optInt("goal"), it.optInt("progress"), it.optBoolean("done"), it.optInt("xp"))
            },
            o.optString("weekEndsAt"),
            (o.optJSONArray("rewards") ?: JSONArray()).objects().map { SeasonReward(it.optInt("level"), it.optString("kind"), it.optString("code"), it.optBoolean("unlocked")) },
        )
    }

    suspend fun seasonTop(friends: Boolean): List<SeasonTopRow> = io {
        (call("GET", "/v1/season/top?scope=" + if (friends) "friends" else "all").optJSONArray("items") ?: JSONArray()).objects().map {
            SeasonTopRow(it.optInt("rank"), it.optString("userId"), it.optString("name"), it.optInt("avatarVersion"), it.str("badge"), it.optInt("xp"), it.optInt("level"), it.optBoolean("me"))
        }
    }
    suspend fun ttb(gameId: Int): Ttb? = io {
        val t = call("GET", "/v1/games/$gameId/ttb").optJSONObject("ttb") ?: return@io null
        fun d(k: String) = if (t.isNull(k)) null else t.optDouble(k).takeIf { it > 0 }
        Ttb(d("main"), d("extra"), d("full"))
    }
    suspend fun backlogForecast(): Forecast = io {
        val o = call("GET", "/v1/me/backlog-forecast")
        Forecast(o.optInt("games"), o.optInt("known"), o.optInt("hours"))
    }
    suspend fun compat(userId: String): Compat = io {
        val o = call("GET", "/v1/users/$userId/compat")
        Compat(
            o.optBoolean("hidden"), o.optInt("percent"), o.optInt("shared"),
            (o.optJSONArray("lovedBoth") ?: JSONArray()).ints(), (o.optJSONArray("theyHave") ?: JSONArray()).ints(),
            (o.optJSONArray("iHave") ?: JSONArray()).ints(),
        )
    }

    suspend fun setNotifyComeback(value: Boolean): Boolean = io {
        call("PATCH", "/v1/me", JSONObject().put("notifyComeback", value)).optBoolean("notifyComeback", value)
    }

    // ---- activity calendar, friends' feed, news, play time (server 0.8.0; older servers answer 404) ----
    suspend fun activity(userId: String?, days: Int = 182): List<ActivityDay> = io {
        val path = if (userId == null) "/v1/me/activity?days=$days" else "/v1/users/$userId/activity?days=$days"
        (call("GET", path).optJSONArray("days") ?: JSONArray()).objects().map { ActivityDay(it.optString("day"), it.optInt("count")) }
    }

    suspend fun friendsFeed(offset: Int = 0): List<FriendEvent> = io {
        (call("GET", "/v1/feed/friends?offset=$offset").optJSONArray("items") ?: JSONArray()).objects().map {
            FriendEvent(
                userId = it.optString("userId"),
                name = it.optString("name"),
                avatarVersion = it.optInt("avatarVersion"),
                gameId = it.optInt("gameId"),
                status = it.str("status"),
                rating = it.intOrNull("rating"),
                favorite = it.optBoolean("favorite"),
                at = it.optString("at"),
                badge = it.str("badge"),
            )
        }
    }

    private fun parseNews(it: JSONObject) = NewsItem(
        id = it.optLong("id"),
        title = it.optString("title"),
        body = it.optString("body"),
        imageUrl = it.str("imageUrl"),
        gameId = it.intOrNull("gameId"),
        author = it.str("author"),
        createdAt = it.optString("createdAt"),
    )

    suspend fun news(offset: Int = 0): List<NewsItem> = io {
        (call("GET", "/v1/news?offset=$offset").optJSONArray("items") ?: JSONArray()).objects().map { parseNews(it) }
    }

    suspend fun postNews(title: String, body: String, imageUrl: String?): NewsItem = io {
        val o = JSONObject().put("title", title).put("body", body)
        if (!imageUrl.isNullOrBlank()) o.put("imageUrl", imageUrl)
        parseNews(call("POST", "/v1/news", o))
    }

    suspend fun deleteNews(id: Long) = io {
        call("DELETE", "/v1/news/$id")
        Unit
    }

    /** Sets the total hours played for a game (anywhere, not only in Svaga Emulator). Returns seconds. */
    suspend fun setPlaytimeTotal(gameId: Int, hours: Double): Long = io {
        call("PUT", "/v1/me/playtime/$gameId/total", JSONObject().put("hours", hours)).optLong("seconds")
    }

    suspend fun playtime(): PlaytimeResult = io {
        val o = call("GET", "/v1/me/playtime")
        PlaytimeResult(
            totalSeconds = o.optLong("totalSeconds"),
            items = (o.optJSONArray("items") ?: JSONArray()).objects().map {
                PlaytimeItem(it.optInt("gameId"), it.optLong("seconds"), it.optInt("sessions"), it.optString("lastPlayed"))
            },
        )
    }

    // ---- account ----
    suspend fun deleteAccount() = io {
        call("DELETE", "/v1/me", JSONObject().put("confirm", "DELETE"))
        Unit
    }

    // ---- collection ----
    private fun parseCopy(o: JSONObject) = CollectionCopy(
        id = o.optLong("id"), gameId = o.optInt("gameId"), platform = o.str("platform"), region = o.str("region"),
        hasCart = o.optBoolean("hasCart", true), hasBox = o.optBoolean("hasBox"), hasManual = o.optBoolean("hasManual"),
        sealed = o.optBoolean("sealed"), condition = o.str("condition"),
        price = if (o.isNull("price")) null else o.optDouble("price"), currency = o.str("currency"),
        acquiredOn = o.str("acquiredOn"), note = o.str("note"),
    )

    fun copyToJson(c: CollectionCopy): JSONObject = JSONObject()
        .put("gameId", c.gameId)
        .put("platform", c.platform ?: JSONObject.NULL)
        .put("region", c.region ?: JSONObject.NULL)
        .put("hasCart", c.hasCart).put("hasBox", c.hasBox).put("hasManual", c.hasManual).put("sealed", c.sealed)
        .put("condition", c.condition ?: JSONObject.NULL)
        .put("price", c.price ?: JSONObject.NULL)
        .put("currency", c.currency ?: JSONObject.NULL)
        .put("acquiredOn", c.acquiredOn ?: JSONObject.NULL)
        .put("note", c.note ?: JSONObject.NULL)

    suspend fun collection(): CollectionResult = io {
        val o = call("GET", "/v1/me/collection")
        CollectionResult(
            items = (o.optJSONArray("items") ?: JSONArray()).objects().map { parseCopy(it) },
            copies = o.optInt("copies"), games = o.optInt("games"), complete = o.optInt("complete"),
            totals = (o.optJSONArray("totals") ?: JSONArray()).objects().map {
                MoneyTotal(it.optString("currency"), it.optDouble("sum", 0.0), it.optInt("count"))
            },
        )
    }

    suspend fun gameCopies(gameId: Int): List<CollectionCopy> = io {
        (call("GET", "/v1/games/$gameId/collection").optJSONArray("items") ?: JSONArray()).objects().map { parseCopy(it) }
    }

    suspend fun saveCopy(c: CollectionCopy): CollectionCopy = io {
        parseCopy(if (c.id > 0) call("PUT", "/v1/me/collection/${c.id}", copyToJson(c)) else call("POST", "/v1/me/collection", copyToJson(c)))
    }

    suspend fun deleteCopy(id: Long) = io {
        call("DELETE", "/v1/me/collection/$id")
        Unit
    }

    // ---- Steam ----
    suspend fun steamStatus(): SteamStatus = io {
        val o = call("GET", "/v1/import/steam")
        SteamStatus(o.optBoolean("configured"), o.str("steamId"), o.str("importedAt"))
    }

    suspend fun importSteam(profile: String, playtime: Boolean): SteamImportResult = io {
        val o = call("POST", "/v1/import/steam", JSONObject().put("profile", profile).put("playtime", playtime))
        SteamImportResult(
            o.optInt("owned"), o.optInt("matched"), o.optInt("added"), o.optInt("already"), o.optInt("hours"),
            (o.optJSONArray("unmatched") ?: JSONArray()).strings(),
        )
    }

    // ---- RetroAchievements ----
    private fun parseRa(o: JSONObject): RaState {
        val s = o.optJSONObject("summary")
        return RaState(
            configured = o.optBoolean("configured"),
            linked = o.optBoolean("linked"),
            summary = s?.let { x ->
                RaSummary(
                    user = x.optString("user"), avatar = x.str("avatar"), memberSince = x.str("memberSince"),
                    points = x.optInt("points"), softPoints = x.optInt("softPoints"), truePoints = x.optInt("truePoints"),
                    presence = x.str("presence"), gamesCount = x.optInt("gamesCount"), beaten = x.optInt("beaten"),
                    mastered = x.optInt("mastered"),
                    games = (x.optJSONArray("games") ?: JSONArray()).objects().map {
                        RaGame(
                            it.optInt("raId"), it.optString("title"), it.optString("console"), it.str("icon"),
                            it.optInt("total"), it.optInt("earned"), it.optInt("earnedHardcore"), it.str("award"),
                        )
                    },
                    recent = (x.optJSONArray("recent") ?: JSONArray()).objects().map {
                        RaAch(
                            it.optInt("id"), it.optString("title"), it.optString("description"), it.optInt("points"),
                            it.optBoolean("hardcore"), it.str("badge"), it.optString("game"), it.optString("console"), it.optString("at"),
                        )
                    },
                    stale = x.optBoolean("stale"),
                )
            },
        )
    }

    suspend fun ra(refresh: Boolean = false): RaState = io { parseRa(call("GET", "/v1/me/ra" + if (refresh) "?refresh=true" else "")) }

    suspend fun userRa(id: String): RaState = io { parseRa(call("GET", "/v1/users/$id/ra")) }

    suspend fun linkRa(username: String): RaState = io { parseRa(call("PUT", "/v1/me/ra", JSONObject().put("username", username))) }

    suspend fun unlinkRa() = io {
        call("DELETE", "/v1/me/ra")
        Unit
    }

    suspend fun importRa(): RaImportResult = io {
        val o = call("POST", "/v1/me/ra/import")
        RaImportResult(o.optInt("added"), o.optInt("updated"), (o.optJSONArray("unmatched") ?: JSONArray()).strings())
    }

    // ---- Svaga Emulator per game ----
    private fun parseEmu(o: JSONObject): EmulatorInfo {
        val d = o.optJSONObject("download")
        return EmulatorInfo(o.optBoolean("enabled"), d?.str("url"), d?.str("name"))
    }

    suspend fun emulatorInfo(gameId: Int): EmulatorInfo = io { parseEmu(call("GET", "/v1/games/$gameId/emulator")) }

    suspend fun setEmulator(gameId: Int, enabled: Boolean, romUrl: String, romName: String): EmulatorInfo = io {
        parseEmu(call("PUT", "/v1/admin/games/$gameId/emulator", JSONObject().put("enabled", enabled).put("romUrl", romUrl).put("romName", romName)))
    }

    /** A separate session for Svaga Emulator (it signs in through this app). */
    suspend fun handoff(): JSONObject = io { call("POST", "/v1/auth/handoff") }

    // ---- leaderboards, recommendations, seasonal challenges ----
    suspend fun leaderboard(metric: String, friends: Boolean): Leaderboard = io {
        val o = call("GET", "/v1/leaderboard?metric=$metric&scope=" + if (friends) "friends" else "all")
        fun row(x: JSONObject) = LeaderRow(x.optInt("rank"), x.optString("userId"), x.optString("name"), x.optInt("avatarVersion"), x.optInt("level", 1), x.optLong("value"), x.optBoolean("me"), x.str("badge"))
        Leaderboard((o.optJSONArray("items") ?: JSONArray()).objects().map { row(it) }, o.optJSONObject("me")?.let { row(it) })
    }

    suspend fun recommendations(): List<Game> = io { parseGames(call("GET", "/v1/me/recommendations")) }

    suspend fun seasonChallenges(): List<SeasonChallenge> = io {
        (call("GET", "/v1/challenges/season").optJSONArray("items") ?: JSONArray()).objects().map {
            val colors = (it.optJSONArray("colors") ?: JSONArray()).strings().mapNotNull { c -> parseHexColor(c) }
            DynAch.put(it.optString("code"), it.optString("title"), it.str("description"), it.str("emoji"), colors)
            SeasonChallenge(it.optInt("id"), it.optString("code"), it.optString("title"), it.str("description"), it.optString("emoji", "🎮"),
                colors, it.optInt("progress"), it.optInt("goal", 1), it.str("endsAt"), it.optInt("sp"))
        }
    }

    private fun parseAdminChallenge(o: JSONObject) = AdminChallenge(
        o.optInt("id"), o.optString("kind"), o.optString("title"), o.str("description"), o.optString("emoji", "🎮"), o.optString("metric"),
        o.str("pattern"), (o.optJSONArray("gameIds") ?: JSONArray()).ints(), (o.optJSONArray("goals") ?: JSONArray()).ints(),
        o.optInt("sp", 100), o.str("endsAt"), o.optBoolean("active", true), o.optBoolean("builtin"),
    )

    suspend fun adminChallenges(): List<AdminChallenge> = io {
        (call("GET", "/v1/admin/challenges").optJSONArray("items") ?: JSONArray()).objects().map { parseAdminChallenge(it) }
    }

    suspend fun saveChallenge(id: Int?, body: JSONObject): AdminChallenge = io {
        parseAdminChallenge(if (id == null) call("POST", "/v1/admin/challenges", body) else call("PUT", "/v1/admin/challenges/$id", body))
    }

    suspend fun deleteChallenge(id: Int) = io {
        call("DELETE", "/v1/admin/challenges/$id")
        Unit
    }

    // ---- 1.7: on this day, daily quiz, game lists ----
    suspend fun onThisDay(): List<AnnivGame> = io {
        (call("GET", "/v1/onthisday").optJSONArray("items") ?: JSONArray()).objects().map {
            AnnivGame(parseGame(it), it.optInt("yearsAgo"), it.optBoolean("inLibrary"))
        }
    }

    private fun parseQuiz(o: JSONObject): QuizState {
        val h = o.optJSONObject("hints") ?: JSONObject()
        val f = o.optJSONObject("focus") ?: JSONObject()
        return QuizState(
            o.optString("day"), o.optString("image"), f.optDouble("x", 0.5).toFloat(), f.optDouble("y", 0.5).toFloat(),
            o.optInt("maxAttempts", 5), o.optInt("attempts"), o.optBoolean("finished"), o.optBoolean("solved"), o.optInt("points"),
            (o.optJSONArray("guesses") ?: JSONArray()).objects().map { QuizGuess(it.optInt("gameId"), it.optString("name")) },
            if (h.has("year") && !h.isNull("year")) h.optInt("year") else null, h.str("platform"), h.str("genre"), h.str("developer"),
            o.optJSONObject("answer")?.let { parseGame(it) }, o.optInt("streak"), o.optInt("playedToday"), o.optInt("solvedToday"),
        )
    }

    suspend fun quizToday(): QuizState = io { parseQuiz(call("GET", "/v1/quiz/today")) }
    suspend fun quizGuess(gameId: Int): QuizState = io { parseQuiz(call("POST", "/v1/quiz/today/guess", JSONObject().put("gameId", gameId))) }
    suspend fun quizBoard(week: Boolean): Leaderboard = io {
        val o = call("GET", "/v1/quiz/leaderboard?period=" + if (week) "week" else "all")
        fun row(x: JSONObject) = LeaderRow(x.optInt("rank"), x.optString("userId"), x.optString("name"), x.optInt("avatarVersion"), x.optInt("level", 1), x.optLong("value"), x.optBoolean("me"), x.str("badge"))
        Leaderboard((o.optJSONArray("items") ?: JSONArray()).objects().map { row(it) }, o.optJSONObject("me")?.let { row(it) })
    }

    private fun parseList(o: JSONObject): GameList {
        val owner = o.optJSONObject("owner") ?: JSONObject()
        return GameList(
            o.optLong("id"), o.optString("title"), o.str("description"), o.optBoolean("public", true), o.optInt("count"), o.optInt("likes"),
            o.optBoolean("liked"), (o.optJSONArray("preview") ?: JSONArray()).ints(), o.optBoolean("mine"),
            owner.optString("id"), owner.optString("name"), owner.optInt("avatarVersion"), o.optBoolean("hasGame"),
        )
    }
    private fun lists(o: JSONObject) = (o.optJSONArray("items") ?: JSONArray()).objects().map { parseList(it) }

    suspend fun popularLists(offset: Int = 0): List<GameList> = io { lists(call("GET", "/v1/lists/popular?offset=$offset")) }
    suspend fun myLists(gameId: Int? = null): List<GameList> = io { lists(call("GET", "/v1/me/lists" + (gameId?.let { "?gameId=$it" } ?: ""))) }
    suspend fun userLists(userId: String): List<GameList> = io { lists(call("GET", "/v1/users/$userId/lists")) }
    suspend fun gameList(id: Long): ListDetail = io {
        val o = call("GET", "/v1/lists/$id")
        ListDetail(parseList(o), (o.optJSONArray("items") ?: JSONArray()).objects().map { ListEntry(it.optInt("gameId"), it.str("note")) })
    }
    suspend fun createList(title: String, description: String?, public: Boolean, gameId: Int?): Long = io {
        val b = JSONObject().put("title", title).put("public", public)
        description?.let { b.put("description", it) }
        gameId?.let { b.put("gameId", it) }
        call("POST", "/v1/lists", b).optLong("id")
    }
    suspend fun updateList(id: Long, title: String, description: String?, public: Boolean) = io {
        call("PUT", "/v1/lists/$id", JSONObject().put("title", title).put("description", description ?: "").put("public", public)); Unit
    }
    suspend fun deleteList(id: Long) = io { call("DELETE", "/v1/lists/$id"); Unit }
    suspend fun addToList(id: Long, gameId: Int, note: String? = null) = io {
        call("PUT", "/v1/lists/$id/items/$gameId", if (note == null) JSONObject() else JSONObject().put("note", note)); Unit
    }
    suspend fun removeFromList(id: Long, gameId: Int) = io { call("DELETE", "/v1/lists/$id/items/$gameId"); Unit }
    suspend fun reorderList(id: Long, gameIds: List<Int>) = io { call("PUT", "/v1/lists/$id/order", JSONObject().put("gameIds", JSONArray(gameIds))); Unit }
    suspend fun likeList(id: Long): Pair<Boolean, Int> = io {
        val o = call("POST", "/v1/lists/$id/like")
        o.optBoolean("liked") to o.optInt("likes")
    }
}

/** Games the server found when it refused to add a duplicate. */
fun ApiException.duplicateMatches(): List<Game> =
    (body.optJSONArray("matches") ?: JSONArray()).let { arr -> (0 until arr.length()).map { parseGame(arr.getJSONObject(it)) } }
