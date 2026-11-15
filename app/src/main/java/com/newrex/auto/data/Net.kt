// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object Net {
    const val USER_AGENT = "OmniDrive/1.0"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun get(url: String): String {
        val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return resp.body?.string() ?: ""
        }
    }
}

/** M3U / M3U8 IPTV listelerini indirir, önbelleğe alır ve kanallara çevirir. */
object Playlists {
    private const val CACHE_HOURS = 6

    /** A Haber, A Spor, ATV, A2 gibi kanallar listenin sonuna konur. */
    private val LAST = Regex("^(a |a2|atv)", RegexOption.IGNORE_CASE)

    data class Result(val channels: List<MediaEntry>, val errors: List<String>)

    suspend fun loadAll(context: Context, forceRefresh: Boolean = false, lists: List<Playlist> = Prefs.playlists): Result =
        withContext(Dispatchers.IO) {
            val channels = ArrayList<MediaEntry>()
            val errors = ArrayList<String>()
            for (pl in lists) {
                try {
                    channels += parse(fetch(context, pl, forceRefresh), pl.name, pl.url.takeIf { it.startsWith("http://") || it.startsWith("https://") })
                } catch (e: Exception) {
                    errors += "${pl.name}: ${e.message ?: "açılamadı"}"
                }
            }
            Result(channels.distinctBy { it.url }.sortedBy { LAST.containsMatchIn(it.title) }, errors)
        }

    private fun fetch(context: Context, pl: Playlist, forceRefresh: Boolean): String {
        if (pl.url.startsWith("content://")) {
            return context.contentResolver.openInputStream(Uri.parse(pl.url))
                ?.bufferedReader()?.use { it.readText() }
                ?: throw IOException("dosya okunamadı")
        }
        val cacheKey = MessageDigest.getInstance("SHA-256")
            .digest(pl.url.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val cache = File(context.cacheDir, "pl_$cacheKey.m3u")
        val fresh = cache.exists() &&
            System.currentTimeMillis() - cache.lastModified() < CACHE_HOURS * 3_600_000L
        if (fresh && !forceRefresh) return cache.readText()
        return try {
            Net.get(pl.url).also { cache.writeText(it) }
        } catch (e: IOException) {
            // İnternet yoksa eski liste ile devam et
            if (cache.exists()) cache.readText() else throw e
        }
    }

    fun parse(text: String, fallbackGroup: String, sourceUrl: String? = null): List<MediaEntry> =
        PlaylistParser.parse(text, fallbackGroup, sourceUrl)

}

/** Ücretsiz ve açık Radio Browser servisi (radio-browser.info). */
object RadioApi {
    private val SERVERS = listOf("de1", "de2")
    private const val PREFS = "omnidrive_radio_cache"
    private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L
    private lateinit var preferences: android.content.SharedPreferences

    /** Son başarılı katalog, uygulama yeniden başlatıldığında da kullanılabilir. */
    fun init(context: Context) {
        if (!::preferences.isInitialized) {
            synchronized(this) {
                if (!::preferences.isInitialized) {
                    preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                }
            }
        }
    }

    suspend fun popular(countryCode: String = "TR", limit: Int = 300) =
        query(
            "/search?countrycode=$countryCode&hidebroken=true&order=clickcount&reverse=true&limit=$limit",
            "popular_$countryCode"
        )

    suspend fun search(name: String, country: String? = null, limit: Int = 150) =
        query(
            "/search?name=${URLEncoder.encode(name.trim(), "UTF-8")}" +
                (country?.let { "&countrycode=$it" } ?: "") +
                "&hidebroken=true&order=clickcount&reverse=true&limit=$limit",
            "search_${sha256((country ?: "all") + "|" + name.trim().lowercase())}"
        )

    private suspend fun query(path: String, cacheKey: String): List<MediaEntry> = withContext(Dispatchers.IO) {
        val cached = readCache(cacheKey)
        if (cached != null && System.currentTimeMillis() - cached.first < CACHE_TTL_MS) {
            return@withContext cached.second
        }

        var last: Exception? = null
        for (server in SERVERS) {
            try {
                val body = Net.get("https://$server.api.radio-browser.info/json/stations$path")
                val list = parseStations(body)
                if (list.isNotEmpty()) {
                    writeCache(cacheKey, list)
                    return@withContext list
                }
                last = IOException("Radyo sunucusu boş istasyon listesi döndürdü")
            } catch (e: Exception) {
                last = e
            }
        }
        // Ağ/sunucu hatasında daha önce alınmış katalogu koru; boş yanıtla önbelleği ezme.
        if (cached != null && cached.second.isNotEmpty()) return@withContext cached.second
        last?.let { throw it }
        emptyList()
    }

    internal fun parseStations(json: String): List<MediaEntry> {
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { index ->
            val o = arr.optJSONObject(index) ?: return@mapNotNull null
            val stream = listOf(o.optString("url_resolved"), o.optString("url"))
                .firstOrNull(::isSafeStreamUrl) ?: return@mapNotNull null
            val title = o.optString("name").trim().ifBlank { "İsimsiz radyo istasyonu" }
            MediaEntry(
                kind = Kind.RADIO,
                title = title.take(160),
                url = stream,
                image = safeWebUrl(o.optString("favicon")),
                group = o.optString("tags").split(',').firstOrNull()?.trim()?.take(80).orEmpty(),
                home = safeWebUrl(o.optString("homepage")),
            )
        }.distinctBy { it.url }
    }

    internal fun isSafeStreamUrl(value: String): Boolean = safeWebUrl(value) != null

    private fun safeWebUrl(value: String): String? = runCatching {
        val uri = java.net.URI(value.trim())
        val scheme = uri.scheme?.lowercase()
        if (scheme !in setOf("http", "https") || uri.host.isNullOrBlank() ||
            uri.rawUserInfo != null || uri.host.any { it.isWhitespace() }) null
        else uri.toASCIIString().takeIf { it.length <= 4096 }
    }.getOrNull()

    private fun readCache(key: String): Pair<Long, List<MediaEntry>>? {
        if (!::preferences.isInitialized) return null
        val raw = preferences.getString(key, null) ?: return null
        return runCatching {
            val root = org.json.JSONObject(raw)
            val savedAt = root.optLong("savedAt")
            val rows = root.optJSONArray("items") ?: return@runCatching null
            val items = (0 until rows.length()).mapNotNull { i ->
                runCatching { MediaEntry.fromJson(rows.getJSONObject(i)) }.getOrNull()
            }.filter { it.kind == Kind.RADIO && isSafeStreamUrl(it.url) }
                .map { it.copy(image = safeWebUrl(it.image.orEmpty()), home = safeWebUrl(it.home.orEmpty())) }
            if (savedAt <= 0 || items.isEmpty()) null else savedAt to items
        }.getOrNull()
    }

    private fun writeCache(key: String, items: List<MediaEntry>) {
        if (!::preferences.isInitialized) return
        val rows = JSONArray()
        items.take(300).forEach { rows.put(it.toJson()) }
        val savedAt = System.currentTimeMillis()
        preferences.edit().putString(
            key,
            org.json.JSONObject().put("savedAt", savedAt).put("items", rows).toString()
        ).apply()
        if (key.startsWith("search_")) {
            val old = preferences.all.keys.filter { it.startsWith("search_") }
                .mapNotNull { cacheKey ->
                    val time = runCatching {
                        org.json.JSONObject(preferences.getString(cacheKey, null) ?: return@runCatching 0L)
                            .optLong("savedAt")
                    }.getOrDefault(0L)
                    cacheKey to time
                }.sortedBy { it.second }
            if (old.size > 20) {
                preferences.edit().also { edit ->
                    old.take(old.size - 20).forEach { edit.remove(it.first) }
                }.apply()
            }
        }
    }

    private fun sha256(value: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
