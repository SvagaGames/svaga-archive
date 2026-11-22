package com.svaga.archive

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** A library change made while the server was unreachable; it is sent when the connection is back. */
data class PendingEdit(val gameId: Int, val fields: JSONObject, val at: Long)

/** Small on-device cache: library, games seen recently and the queue of changes waiting to be synchronised. */
class OfflineStore(context: Context) {
    private val prefs = context.getSharedPreferences("svaga_offline", Context.MODE_PRIVATE)

    fun saveLibrary(items: Collection<LibraryItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(libraryItemToJson(it)) }
        prefs.edit().putString("library", arr.toString()).apply()
    }

    fun loadLibrary(): List<LibraryItem> = try {
        val arr = JSONArray(prefs.getString("library", "[]") ?: "[]")
        (0 until arr.length()).map { parseLibraryItem(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveGames(games: Collection<Game>) {
        val arr = JSONArray()
        games.take(600).forEach { arr.put(gameToJson(it)) }
        prefs.edit().putString("games", arr.toString()).apply()
    }

    fun loadGames(): List<Game> = try {
        val arr = JSONArray(prefs.getString("games", "[]") ?: "[]")
        (0 until arr.length()).map { parseGame(arr.getJSONObject(it)) }
    } catch (e: Exception) {
        emptyList()
    }

    fun pending(): List<PendingEdit> = try {
        val arr = JSONArray(prefs.getString("pending", "[]") ?: "[]")
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            PendingEdit(o.getInt("gameId"), o.getJSONObject("fields"), o.optLong("at", System.currentTimeMillis()))
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun setPending(list: List<PendingEdit>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("gameId", it.gameId).put("fields", it.fields).put("at", it.at)) }
        prefs.edit().putString("pending", arr.toString()).apply()
    }

    /** Adds a change to the queue; several changes of one game are folded into one. */
    fun enqueue(gameId: Int, fields: JSONObject, at: Long): List<PendingEdit> {
        val list = pending().toMutableList()
        val i = list.indexOfFirst { it.gameId == gameId }
        if (i >= 0) {
            val merged = JSONObject(list[i].fields.toString())
            fields.keys().forEach { merged.put(it, fields.get(it)) }
            list[i] = PendingEdit(gameId, merged, at)
        } else {
            list.add(PendingEdit(gameId, JSONObject(fields.toString()), at))
        }
        setPending(list)
        return list
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

/** Applies a queued change on top of a library entry. */
fun LibraryItem.withEdit(fields: JSONObject): LibraryItem = copy(
    status = if (fields.has("status")) (if (fields.isNull("status")) null else fields.optString("status")) else status,
    rating = if (fields.has("rating")) (if (fields.isNull("rating")) null else fields.optInt("rating")) else rating,
    note = if (fields.has("note")) (if (fields.isNull("note")) null else fields.optString("note")) else note,
    favorite = if (fields.has("favorite")) fields.optBoolean("favorite") else favorite,
    updatedAt = java.time.Instant.now().toString(),
)
