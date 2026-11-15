// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
// OmniDrive — Podcast çevrimdışı indirme yönetimi
package com.newrex.auto.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Android DownloadManager ile uygulamaya özel alana podcast indirir.
 * İndirme kayıtlarında açık yayın URL'si saklanmaz; yalnızca SHA-256 anahtarı ve sistem indirme kimliği tutulur.
 */
object PodcastDownloads {
    enum class State { NONE, PENDING, RUNNING, PAUSED, SUCCESSFUL, FAILED }

    data class Status(
        val state: State,
        val downloadedBytes: Long = 0,
        val totalBytes: Long = -1,
    ) {
        val progress: Int
            get() = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
    }

    private data class Record(val key: String, val id: Long, val title: String, val image: String?, val group: String)

    private lateinit var context: Context
    private lateinit var manager: DownloadManager
    private val lock = Any()
    private val statusCache = ConcurrentHashMap<Long, Status>()
    val changes = MutableStateFlow(0)
    private const val PREFS_NAME = "omnidrive_podcast_downloads"
    private const val RECORDS_KEY = "records"

    fun init(context: Context) {
        if (::manager.isInitialized) return
        synchronized(lock) {
            if (::manager.isInitialized) return
            this.context = context.applicationContext
            manager = this.context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        }
    }

    /** Başarılı indirme varsa DownloadManager'ın uygulamaya verdiği içerik URI'sini döndürür. */
    fun localUri(url: String): Uri? {
        val record = find(url) ?: return null
        if (statusById(record.id).state != State.SUCCESSFUL) return null
        return runCatching { manager.getUriForDownloadedFile(record.id) }.getOrNull()
    }

    fun status(url: String): Status {
        val record = find(url) ?: return Status(State.NONE)
        return statusCache[record.id] ?: statusById(record.id)
    }

    /** Ekran açıkken tüm OmniDrive indirmelerinin durumunu tek DownloadManager sorgusuyla yeniler. */
    fun refresh() {
        val records = readRecords()
        if (records.isEmpty()) {
            statusCache.clear()
            return
        }
        val ids = records.map { it.id }.toLongArray()
        val cursor = runCatching { manager.query(DownloadManager.Query().setFilterById(*ids)) }.getOrNull()
            ?: return
        val refreshed = HashMap<Long, Status>()
        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val stateColumn = it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            val downloadedColumn = it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
            val totalColumn = it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
            while (it.moveToNext()) {
                val state = when (it.getInt(stateColumn)) {
                    DownloadManager.STATUS_PENDING -> State.PENDING
                    DownloadManager.STATUS_RUNNING -> State.RUNNING
                    DownloadManager.STATUS_PAUSED -> State.PAUSED
                    DownloadManager.STATUS_SUCCESSFUL -> State.SUCCESSFUL
                    else -> State.FAILED
                }
                refreshed[it.getLong(idColumn)] = Status(
                    state,
                    it.getLong(downloadedColumn).coerceAtLeast(0),
                    it.getLong(totalColumn),
                )
            }
        }
        records.forEach { refreshed.putIfAbsent(it.id, Status(State.FAILED)) }
        val changed = statusCache.toMap() != refreshed
        statusCache.clear()
        statusCache.putAll(refreshed)
        if (changed) changes.value++
    }

    /** Yalnızca HTTP(S) podcast enclosure adresleri indirilebilir. */
    fun enqueue(url: String, title: String, image: String? = null, group: String = ""): Long? {
        if (!isHttpUrl(url)) return null
        synchronized(lock) {
            val existing = find(url)
            if (existing != null) {
                val state = statusById(existing.id).state
                if (state == State.PENDING || state == State.RUNNING || state == State.PAUSED || state == State.SUCCESSFUL)
                    return existing.id
                runCatching { manager.remove(existing.id) }
                statusCache.remove(existing.id)
            }
            val request = runCatching {
                DownloadManager.Request(Uri.parse(url))
                    .setTitle(title.take(120).ifBlank { "Podcast bölümü" })
                    .setDescription("OmniDrive • Podcast")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(false)
                    .apply {
                        val fileName = fileNameFor(url, title)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Android 10+: genel İndirilenler klasörü; uygulama dışı oynatıcılar da dosyaya erişebilir.
                            setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS, "OmniDrive Podcasts/$fileName"
                            )
                        } else {
                            setDestinationInExternalFilesDir(context, "Podcasts", fileName)
                        }
                    }
            }.getOrNull() ?: return null
            val id = runCatching { manager.enqueue(request) }.getOrNull() ?: return null
            val records = readRecords().filterNot { it.key == key(url) }.toMutableList()
            records += Record(key(url), id, title.take(120), image?.take(2048), group.take(300))
            writeRecords(records)
            statusCache[id] = Status(State.PENDING)
            changes.value++
            return id
        }
    }

    /** İndirmeyi veya indirilmiş dosyayı kaldırır. */
    fun delete(url: String): Boolean {
        synchronized(lock) {
            val record = find(url) ?: return false
            runCatching { manager.remove(record.id) }
            statusCache.remove(record.id)
            writeRecords(readRecords().filterNot { it.key == record.key })
            changes.value++
            return true
        }
    }

    fun downloadedCount(): Int = readRecords().count { statusById(it.id).state == State.SUCCESSFUL }

    /** RSS erişimi olmasa bile indirilmiş bölümleri DownloadManager kayıtlarından listeler. */
    fun downloadedEntries(): List<MediaEntry> {
        val records = readRecords()
        if (records.isEmpty()) return emptyList()
        val byId = records.associateBy { it.id }
        val cursor = runCatching {
            manager.query(DownloadManager.Query().setFilterById(*records.map { it.id }.toLongArray()))
        }.getOrNull() ?: return emptyList()
        val result = ArrayList<MediaEntry>()
        cursor.use {
            val idColumn = it.getColumnIndex(DownloadManager.COLUMN_ID)
            val uriColumn = it.getColumnIndex(DownloadManager.COLUMN_URI)
            val statusColumn = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (idColumn < 0 || uriColumn < 0 || statusColumn < 0) return emptyList()
            while (it.moveToNext()) {
                if (it.getInt(statusColumn) != DownloadManager.STATUS_SUCCESSFUL) continue
                val record = byId[it.getLong(idColumn)] ?: continue
                val sourceUrl = it.getString(uriColumn) ?: continue
                if (!isHttpUrl(sourceUrl)) continue
                result += MediaEntry(Kind.PODCAST, record.title, sourceUrl, record.image, record.group)
            }
        }
        return result
    }

    private fun statusById(id: Long): Status {
        val cursor = runCatching { manager.query(DownloadManager.Query().setFilterById(id)) }.getOrNull()
            ?: return Status(State.FAILED)
        cursor.use {
            if (!it.moveToFirst()) return Status(State.FAILED)
            val state = when (it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_PENDING -> State.PENDING
                DownloadManager.STATUS_RUNNING -> State.RUNNING
                DownloadManager.STATUS_PAUSED -> State.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> State.SUCCESSFUL
                else -> State.FAILED
            }
            val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return Status(state, downloaded.coerceAtLeast(0), total)
        }
    }

    private fun find(url: String): Record? = readRecords().firstOrNull { it.key == key(url) }

    private fun readRecords(): List<Record> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(RECORDS_KEY, "[]")
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val key = item.optString("key")
                val id = item.optLong("id", -1)
                if (key.length != 64 || id <= 0) null
                else Record(key, id, item.optString("title"), item.optString("image").ifBlank { null }, item.optString("group"))
            }
        }.getOrDefault(emptyList())
    }

    private fun writeRecords(records: List<Record>) {
        val array = JSONArray()
        records.forEach { array.put(JSONObject().put("key", it.key).put("id", it.id).put("title", it.title)
            .put("image", it.image ?: "").put("group", it.group)) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(RECORDS_KEY, array.toString()).apply()
    }

    private fun isHttpUrl(value: String): Boolean {
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) && !uri.host.isNullOrBlank()
    }

    internal fun keyForUrl(value: String): String = sha256(value)

    private fun key(value: String): String = keyForUrl(value)

    internal fun fileNameFor(url: String, title: String): String {
        val base = title.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_', '.').take(48).ifBlank { "podcast" }
        val path = runCatching { URI(url).path.orEmpty() }.getOrDefault("")
        val extension = path.substringAfterLast('.', "").lowercase()
            .takeIf { it.matches(Regex("[a-z0-9]{2,5}")) } ?: "audio"
        return "${base}_${sha256(url).take(12)}.$extension"
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
