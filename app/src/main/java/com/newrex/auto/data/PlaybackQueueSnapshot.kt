// OmniDrive — Kalıcı medya kuyruğu kaydı
package com.newrex.auto.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Yeniden başlatma kurtarması için sınırlı ve doğrulanmış medya kuyruğu.
 * Kimlik doğrulama başlıkları/çerezleri saklanmaz; yalnızca MediaEntry'nin açık alanları tutulur.
 */
data class PlaybackQueueSnapshot(
    val entries: List<MediaEntry>,
    val index: Int,
    val positionMs: Long,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("version", VERSION)
        .put("entries", JSONArray().also { array -> entries.forEach { array.put(it.toJson()) } })
        .put("index", index)
        .put("positionMs", positionMs)

    companion object {
        const val MAX_ENTRIES = 50
        const val MAX_POSITION_MS = 7L * 24 * 60 * 60 * 1000
        private const val VERSION = 1

        fun create(entries: List<MediaEntry>, index: Int, positionMs: Long): PlaybackQueueSnapshot? {
            if (entries.isEmpty() || index !in entries.indices) return null
            // Büyük IPTV listelerinde seçili öğeyi koruyan 50 öğelik pencere saklanır.
            val start = if (entries.size <= MAX_ENTRIES) 0 else (index - MAX_ENTRIES / 2).coerceIn(0, entries.size - MAX_ENTRIES)
            val window = entries.drop(start).take(MAX_ENTRIES)
            val safeEntries = window.mapNotNull(::validateEntry)
            if (safeEntries.size != window.size) return null
            return PlaybackQueueSnapshot(
                entries = safeEntries,
                index = index - start,
                positionMs = positionMs.coerceIn(0L, MAX_POSITION_MS),
            )
        }

        fun fromJson(json: JSONObject): PlaybackQueueSnapshot {
            require(json.optInt("version", -1) == VERSION) { "Desteklenmeyen kuyruk sürümü" }
            val array = json.optJSONArray("entries") ?: error("Kuyruk listesi bulunamadı")
            require(array.length() in 1..MAX_ENTRIES) { "Kuyruk boyutu geçersiz" }
            val entries = (0 until array.length()).map { i ->
                val item = array.optJSONObject(i) ?: error("Kuyruk öğesi geçersiz")
                val kind = runCatching { Kind.valueOf(item.optString("kind")) }.getOrNull()
                    ?: error("Medya türü geçersiz")
                val url = item.optString("url")
                val uri = runCatching { java.net.URI(url) }.getOrNull()
                    ?: error("Medya adresi geçersiz")
                require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
                    "Yalnızca HTTP(S) adresleri geri yüklenebilir"
                }
                require(!uri.host.isNullOrBlank() && !containsCredentials(uri)) {
                    "Kimlik bilgisi içeren veya sunucusu olmayan adres geri yüklenemez"
                }
                require(url.length <= 4096 && !item.optString("title").isBlank()) { "Medya alanı geçersiz" }
                MediaEntry(
                    kind = kind,
                    title = item.optString("title").take(300),
                    url = url.take(4096),
                    image = item.optString("image").takeIf { it.isNotBlank() }?.take(4096),
                    group = item.optString("group").take(300),
                    home = item.optString("home").takeIf { it.isNotBlank() }?.take(4096),
                )
            }
            val index = json.optInt("index", -1)
            require(index in entries.indices) { "Kuyruk konumu geçersiz" }
            val position = json.optLong("positionMs", 0L)
            require(position >= 0L) { "Oynatma konumu negatif olamaz" }
            return PlaybackQueueSnapshot(entries, index, position.coerceAtMost(MAX_POSITION_MS))
        }

        private fun validateEntry(entry: MediaEntry): MediaEntry? {
            if (entry.title.isBlank() || entry.url.length > 4096) return null
            val uri = runCatching { java.net.URI(entry.url) }.getOrNull() ?: return null
            if (!uri.scheme.equals("http", true) && !uri.scheme.equals("https", true)) return null
            if (uri.host.isNullOrBlank() || containsCredentials(uri)) return null
            return entry.copy(
                title = entry.title.take(300),
                image = safeOptionalUrl(entry.image),
                group = entry.group.take(300),
                home = safeOptionalUrl(entry.home),
            )
        }

        private fun containsCredentials(uri: java.net.URI): Boolean {
            if (uri.rawUserInfo != null) return true
            val sensitiveKeys = setOf("user", "username", "password", "pass", "token", "access_token", "auth", "authorization", "api_key", "apikey", "signature", "sig", "jwt", "session")
            return uri.rawQuery.orEmpty().split("&").any { part ->
                val key = part.substringBefore("=").lowercase(java.util.Locale.ROOT)
                key in sensitiveKeys
            }
        }

        private fun safeOptionalUrl(value: String?): String? {
            if (value.isNullOrBlank() || value.length > 4096) return null
            val uri = runCatching { java.net.URI(value) }.getOrNull() ?: return null
            if (!uri.scheme.equals("http", true) && !uri.scheme.equals("https", true)) return null
            if (uri.host.isNullOrBlank() || containsCredentials(uri)) return null
            return value
        }
    }
}
