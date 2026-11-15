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
            if (entries.isEmpty() || entries.size > MAX_ENTRIES) return null
            if (index !in entries.indices) return null
            val safeEntries = entries.mapNotNull(::validateEntry)
            if (safeEntries.size != entries.size) return null
            return PlaybackQueueSnapshot(
                entries = safeEntries,
                index = index,
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
                require(!item.optString("title").isBlank()) { "Medya başlığı boş" }
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
            if (uri.host.isNullOrBlank()) return null
            return entry.copy(
                title = entry.title.take(300),
                image = entry.image?.take(4096),
                group = entry.group.take(300),
                home = entry.home?.take(4096),
            )
        }
    }
}
