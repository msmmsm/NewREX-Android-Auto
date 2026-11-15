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
    // Sunuculardan biri arada boş yanıt verebiliyor; sırayla denenir
    private val SERVERS = listOf("de1", "de2")

    suspend fun popular(countryCode: String = "TR", limit: Int = 300) =
        query("/search?countrycode=$countryCode&hidebroken=true&order=clickcount&reverse=true&limit=$limit")

    suspend fun search(name: String, country: String? = null, limit: Int = 150) =
        query("/search?name=${URLEncoder.encode(name, "UTF-8")}" + (country?.let { "&countrycode=$it" } ?: "") +
            "&hidebroken=true&order=clickcount&reverse=true&limit=$limit")

    private suspend fun query(path: String): List<MediaEntry> = withContext(Dispatchers.IO) {
        var last: Exception? = null
        for (server in SERVERS) {
            try {
                val list = parse(Net.get("https://$server.api.radio-browser.info/json/stations$path"))
                if (list.isNotEmpty()) return@withContext list
            } catch (e: Exception) {
                last = e
            }
        }
        last?.let { throw it }
        emptyList()
    }

    private fun parse(json: String): List<MediaEntry> {
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull {
            val o = arr.getJSONObject(it)
            val stream = o.optString("url_resolved").ifBlank { o.optString("url") }
            if (stream.isBlank()) return@mapNotNull null
            MediaEntry(
                kind = Kind.RADIO,
                title = o.optString("name").trim(),
                url = stream,
                image = o.optString("favicon").takeIf { it.startsWith("http") },
                group = o.optString("tags").split(',').firstOrNull()?.trim().orEmpty(),
                home = o.optString("homepage").takeIf { it.startsWith("http") },
            )
        }.distinctBy { it.url }
    }
}
