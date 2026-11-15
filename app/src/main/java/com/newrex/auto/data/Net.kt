// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
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

    /** RSS gibi dış kaynak yanıtlarını belleğe almadan önce kesin boyut sınırı uygular. */
    fun getLimited(url: String, maxBytes: Int): String {
        require(maxBytes > 0) { "Yanıt boyutu sınırı pozitif olmalı" }
        var current = okhttp3.HttpUrl.get(url)
        val redirectClient = client.newBuilder().followRedirects(false).followSslRedirects(false).build()
        repeat(6) { hop ->
            if (!isSafeHttpUrl(current.toString())) throw IOException("Güvensiz yönlendirme adresi")
            val req = Request.Builder().url(current).header("User-Agent", USER_AGENT).build()
            redirectClient.newCall(req).execute().use { resp ->
                if (resp.code in setOf(301, 302, 303, 307, 308)) {
                    if (hop == 5) throw IOException("Çok fazla RSS yönlendirmesi")
                    val location = resp.header("Location") ?: throw IOException("Yönlendirme adresi eksik")
                    val next = current.resolve(location) ?: throw IOException("Yönlendirme adresi geçersiz")
                    if (current.isHttps && !next.isHttps) throw IOException("HTTPS bağlantısı HTTP'ye düşürülemez")
                    if (!isSafeHttpUrl(next.toString())) throw IOException("Güvensiz RSS yönlendirmesi")
                    current = next
                    return@use
                }
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                val body = resp.body ?: return ""
                val mime = body.contentType()?.let { "${it.type}/${it.subtype}".lowercase() }.orEmpty()
                if (mime.isNotBlank() && mime !in setOf("application/rss+xml", "application/atom+xml", "application/xml", "text/xml", "text/plain", "application/octet-stream")) {
                    throw IOException("RSS için desteklenmeyen içerik türü")
                }
                return readBounded(body.byteStream(), maxBytes, body.contentLength())
            }
        }
        throw IOException("RSS yönlendirmesi tamamlanamadı")
    }

    internal fun isSafeHttpUrl(value: String): Boolean = runCatching {
        val uri = java.net.URI(value)
        uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null && value.length <= 4096 &&
            value.none { it.isWhitespace() || it.isISOControl() }
    }.getOrDefault(false)

    internal fun readBounded(input: InputStream, maxBytes: Int, declaredLength: Long = -1L): String {
        require(maxBytes > 0) { "Yanıt boyutu sınırı pozitif olmalı" }
        if (declaredLength > maxBytes) throw IOException("Yanıt boyut sınırını aşıyor")
        val output = ByteArrayOutputStream(minOf(maxBytes, 8192))
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer, 0, minOf(buffer.size, maxBytes - total + 1))
            if (count < 0) break
            if (count == 0) continue
            total += count
            if (total > maxBytes) throw IOException("Yanıt boyut sınırını aşıyor")
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
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
    private const val MAX_STALE_CACHE_MS = 30L * 24 * 60 * 60 * 1000
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

    suspend fun popular(countryCode: String = "TR", limit: Int = 300): List<MediaEntry> {
        val country = normalizeCountryCode(countryCode) ?: "TR"
        val safeLimit = limit.coerceIn(1, 300)
        return query(
            "/search?countrycode=$country&hidebroken=true&order=clickcount&reverse=true&limit=$safeLimit",
            "popular_$country"
        )
    }

    suspend fun search(name: String, country: String? = null, limit: Int = 150): List<MediaEntry> {
        val term = name.trim().take(100)
        if (term.isBlank()) return emptyList()
        val safeCountry = country?.let(::normalizeCountryCode)
        val safeLimit = limit.coerceIn(1, 300)
        return query(
            "/search?name=${URLEncoder.encode(term, "UTF-8")}" +
                (safeCountry?.let { "&countrycode=$it" } ?: "") +
                "&hidebroken=true&order=clickcount&reverse=true&limit=$safeLimit",
            "search_${sha256((safeCountry ?: "all") + "|" + term.lowercase(java.util.Locale.ROOT))}"
        )
    }

    internal fun normalizeCountryCode(value: String): String? =
        value.trim().takeIf { it.matches(Regex("[A-Za-z]{2}")) }?.uppercase(java.util.Locale.ROOT)

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
                codec = o.optString("codec").trim().takeIf { it.isNotBlank() && it.length <= 24 },
                bitrateKbps = o.optInt("bitrate").takeIf { it in 1..10000 },
                countryCode = o.optString("countrycode").trim().takeIf { it.matches(Regex("[A-Za-z]{2}")) }?.uppercase(),
                votes = o.optInt("votes").takeIf { it >= 0 },
                clickCount = o.optInt("clickcount").takeIf { it >= 0 },
                directoryReportsHealthy = if (o.has("lastcheckok") && !o.isNull("lastcheckok"))
                    o.optBoolean("lastcheckok") else null,
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
            val age = System.currentTimeMillis() - savedAt
            if (savedAt <= 0 || age < -5 * 60 * 1000L || age > MAX_STALE_CACHE_MS || items.isEmpty())
                null else savedAt to items
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
