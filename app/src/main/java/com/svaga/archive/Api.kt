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
) {
    val year: Int? get() = releaseDate?.take(4)?.toIntOrNull()
}

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
)

data class GameStats(val avgRating: Double?, val ratingCount: Int, val inLibrary: Int, val completed: Int)

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
)

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

data class IdentitiesResult(val hasPassword: Boolean, val available: List<String>, val items: List<IdentityItem>)

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
)

private fun tzMinutes(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000

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
            "DELETE" -> builder.delete()
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
    suspend fun oauthProviders(): List<String> = io {
        val arr = execute("GET", "/v1/auth/providers", null, false).optJSONArray("providers")
        List(arr?.length() ?: 0) { arr!!.getString(it) }
    }

    fun oauthStartUrl(provider: String) = "$BASE_URL/v1/auth/oauth/$provider/start"

    suspend fun oauthExchange(code: String) = io {
        saveSession(execute("POST", "/v1/auth/oauth/exchange", JSONObject().put("code", code), false))
    }

    suspend fun identities(): IdentitiesResult = io {
        val o = call("GET", "/v1/me/identities")
        val items = o.optJSONArray("items")
        val av = o.optJSONArray("available")
        IdentitiesResult(
            hasPassword = o.optBoolean("hasPassword", true),
            available = List(av?.length() ?: 0) { av!!.getString(it) },
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
        )
    }
}

/** Games the server found when it refused to add a duplicate. */
fun ApiException.duplicateMatches(): List<Game> =
    (body.optJSONArray("matches") ?: JSONArray()).let { arr -> (0 until arr.length()).map { parseGame(arr.getJSONObject(it)) } }
