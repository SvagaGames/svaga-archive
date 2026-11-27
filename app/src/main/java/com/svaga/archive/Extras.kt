package com.svaga.archive

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.viewModelScope
import com.svaga.archive.widget.NowPlayingWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Collection, Steam import, RetroAchievements, account deletion and the home-screen widget. */
class Extras(private val vm: AppViewModel) {
    private val api get() = vm.api
    private val ctx: Context get() = vm.getApplication<android.app.Application>()

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

    // ---------- collection ----------
    var collection by mutableStateOf<CollectionResult?>(null)
        private set
    var collectionError by mutableStateOf<Int?>(null)
        private set
    val copies = mutableStateMapOf<Int, List<CollectionCopy>>()
    var copyBusy by mutableStateOf(false)
        private set
    var copyError by mutableStateOf<Int?>(null)
        private set

    fun loadCollection() = run({ collectionError = it }) {
        val r = api.collection()
        collection = r
        collectionError = null
        val missing = r.items.map { it.gameId }.distinct().filter { !vm.gamesCache.containsKey(it) }
        missing.chunked(50).forEach { chunk -> api.gamesByIds(chunk).forEach { vm.gamesCache[it.id] = it } }
        copies.clear()
        r.items.groupBy { it.gameId }.forEach { (id, list) -> copies[id] = list }
    }

    fun loadCopies(gameId: Int) = run {
        copies[gameId] = api.gameCopies(gameId)
    }

    fun saveCopy(c: CollectionCopy, onDone: () -> Unit) {
        copyBusy = true
        copyError = null
        run({ copyError = it; copyBusy = false }) {
            val saved = api.saveCopy(c)
            val list = copies[c.gameId].orEmpty().filter { it.id != saved.id } + saved
            copies[c.gameId] = list
            copyBusy = false
            collection = null
            onDone()
        }
    }

    fun deleteCopy(c: CollectionCopy) = run {
        api.deleteCopy(c.id)
        copies[c.gameId] = copies[c.gameId].orEmpty().filter { it.id != c.id }
        collection = collection?.let { r ->
            val items = r.items.filter { it.id != c.id }
            r.copy(items = items, copies = items.size, games = items.map { it.gameId }.distinct().size)
        }
    }

    // ---------- Svaga Emulator per game ----------
    val emulator = mutableStateMapOf<Int, EmulatorInfo>()
    var emuBusy by mutableStateOf(false)
        private set
    var emuError by mutableStateOf<Int?>(null)
        private set

    fun loadEmulator(gameId: Int) = run { emulator[gameId] = api.emulatorInfo(gameId) }

    fun saveEmulator(gameId: Int, enabled: Boolean, romUrl: String, romName: String, onDone: () -> Unit) {
        emuBusy = true
        emuError = null
        run({ emuError = it; emuBusy = false }) {
            emulator[gameId] = api.setEmulator(gameId, enabled, romUrl.trim(), romName.trim())
            emuBusy = false
            onDone()
        }
    }

    // ---------- leaderboards, recommendations, seasons, challenge editor ----------
    var board by mutableStateOf<Leaderboard?>(null)
        private set
    var boardError by mutableStateOf<Int?>(null)
        private set
    var recs by mutableStateOf<List<Game>>(emptyList())
        private set
    var seasons by mutableStateOf<List<SeasonChallenge>>(emptyList())
        private set
    var challenges by mutableStateOf<List<AdminChallenge>>(emptyList())
        private set
    var challengeBusy by mutableStateOf(false)
        private set
    var challengeError by mutableStateOf<Int?>(null)
        private set

    fun loadBoard(metric: String, friends: Boolean) = run({ boardError = it }) {
        board = null
        board = api.leaderboard(metric, friends)
        boardError = null
    }

    fun loadRecs() = run {
        val list = api.recommendations()
        list.forEach { vm.gamesCache[it.id] = it }
        recs = list
    }

    fun loadSeasons() = run { seasons = api.seasonChallenges() }

    fun loadChallenges() = run({ challengeError = it }) { challenges = api.adminChallenges() }

    fun saveChallenge(id: Int?, body: org.json.JSONObject, onDone: () -> Unit) {
        challengeBusy = true
        challengeError = null
        run({ challengeError = it; challengeBusy = false }) {
            api.saveChallenge(id, body)
            challenges = api.adminChallenges()
            challengeBusy = false
            onDone()
        }
    }

    fun deleteChallenge(id: Int) = run {
        api.deleteChallenge(id)
        challenges = api.adminChallenges()
    }

    // ---------- hours played (entered by hand) ----------
    var hoursError by mutableStateOf<Int?>(null)
        private set

    fun setHours(gameId: Int, hours: Double, onDone: () -> Unit) {
        hoursError = null
        run({ hoursError = it }) {
            val secs = api.setPlaytimeTotal(gameId, hours)
            if (secs > 0) vm.playtimeByGame[gameId] = secs else vm.playtimeByGame.remove(gameId)
            vm.loadPlaytime()
            onDone()
        }
    }

    // ---------- Steam ----------
    var steam by mutableStateOf<SteamStatus?>(null)
        private set
    var steamBusy by mutableStateOf(false)
        private set
    var steamResult by mutableStateOf<SteamImportResult?>(null)
        private set
    var steamError by mutableStateOf<Int?>(null)
        private set

    fun loadSteam() = run { steam = api.steamStatus() }

    fun importSteam(profile: String, playtime: Boolean) {
        steamBusy = true
        steamError = null
        steamResult = null
        run({ steamError = it; steamBusy = false }) {
            steamResult = api.importSteam(profile.trim(), playtime)
            steamBusy = false
            steam = api.steamStatus()
            vm.loadLibrary()
            vm.loadPlaytime()
        }
    }

    // ---------- RetroAchievements ----------
    var ra by mutableStateOf<RaState?>(null)
        private set
    var raBusy by mutableStateOf(false)
        private set
    var raError by mutableStateOf<Int?>(null)
        private set
    var raImport by mutableStateOf<RaImportResult?>(null)
        private set
    val userRa = mutableStateMapOf<String, RaState>()

    fun loadRa(refresh: Boolean = false) = run({ raError = it }) {
        ra = api.ra(refresh)
        raError = null
    }

    fun loadUserRa(id: String) = run { userRa[id] = api.userRa(id) }

    fun linkRa(username: String) {
        raBusy = true
        raError = null
        run({ raError = it; raBusy = false }) {
            ra = api.linkRa(username.trim())
            raBusy = false
        }
    }

    fun unlinkRa() = run {
        api.unlinkRa()
        ra = RaState(ra?.configured ?: true, false, null)
        raImport = null
    }

    fun importRa() {
        raBusy = true
        raError = null
        run({ raError = it; raBusy = false }) {
            raImport = api.importRa()
            raBusy = false
            vm.loadLibrary()
        }
    }

    // ---------- account ----------
    var deleting by mutableStateOf(false)
        private set
    var deleteError by mutableStateOf<Int?>(null)
        private set

    fun deleteAccount() {
        deleting = true
        deleteError = null
        run({ deleteError = it; deleting = false }) {
            api.deleteAccount()
            deleting = false
            reset()
            vm.forgetLocal()
        }
    }

    fun reset() {
        collection = null
        copies.clear()
        steam = null
        steamResult = null
        ra = null
        raImport = null
        userRa.clear()
        emulator.clear()
        board = null
        recs = emptyList()
        seasons = emptyList()
        challenges = emptyList()
        ctx.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        vm.viewModelScope.launch { runCatching { NowPlayingWidget().updateAll(ctx) } }
    }

    // ---------- widget ----------
    /** Saves what the home-screen widget shows: games being played now and the nearest release from the library. */
    fun refreshWidget() {
        val lib = vm.library.values
        val playing = lib.filter { it.status == "playing" }.sortedByDescending { it.updatedAt }.take(4)
        val arr = JSONArray()
        playing.forEach { item ->
            val g = vm.gamesCache[item.gameId] ?: return@forEach
            arr.put(
                JSONObject().put("id", g.id).put("name", g.name).put("cover", g.cover ?: "")
                    .put("seconds", vm.playtimeByGame[g.id] ?: 0L),
            )
        }
        val today = LocalDate.now().toString()
        val next = lib.mapNotNull { vm.gamesCache[it.gameId] }
            .filter { (it.releaseDate ?: "") >= today && (it.releaseDate?.length ?: 0) >= 10 }
            .minByOrNull { it.releaseDate ?: "" }
        ctx.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit()
            .putString("playing", arr.toString())
            .putString("next_name", next?.name)
            .putString("next_date", next?.releaseDate)
            .putInt("count", lib.count { it.status != null })
            .apply()
        vm.viewModelScope.launch { runCatching { NowPlayingWidget().updateAll(ctx) } }
    }

    companion object {
        const val WIDGET_PREFS = "svaga_widget"
    }
}
