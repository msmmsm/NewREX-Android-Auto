// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import java.net.URI

/** M3U kanal listesi ayrıştırıcısı. HLS medya/ana manifestleri kanal listesi olarak yorumlanmaz. */
internal object PlaylistParser {
    private val groupNames = mapOf(
        "animation" to "Animasyon", "auto" to "Otomobil", "business" to "Ekonomi", "classic" to "Klasik",
        "comedy" to "Komedi", "cooking" to "Yemek", "culture" to "Kültür", "documentary" to "Belgesel",
        "education" to "Eğitim", "entertainment" to "Eğlence", "family" to "Aile", "general" to "Genel",
        "kids" to "Çocuk", "legislative" to "Meclis", "lifestyle" to "Yaşam", "movies" to "Film",
        "music" to "Müzik", "news" to "Haber", "outdoor" to "Doğa", "relax" to "Rahatlama",
        "religious" to "Dini", "series" to "Dizi", "science" to "Bilim", "shop" to "Alışveriş",
        "sports" to "Spor", "travel" to "Gezi", "weather" to "Hava durumu", "undefined" to "Diğer",
    )
    private val hiddenNames = listOf(
        "diyanet", "semerkand", "lalegul", "lalegül", "dost tv", "diyar tv", "sat 7", "sat-7",
        "al-zahra", "al-rafidain", "sercem", "ilke tv", "qaf tv", "hilal tv", "rehber tv",
        "berat tv", "mesaj tv", "kanal hayat",
    )

    fun parse(text: String, fallbackGroup: String, sourceUrl: String? = null): List<MediaEntry> {
        if (text.length > 8 * 1024 * 1024) return emptyList()
        val lines = text.removePrefix("\uFEFF").lineSequence().take(100_001).map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.size > 100_000) return emptyList()
        // HLS playlistlerinde EXT-X-TARGETDURATION, EXT-X-MEDIA-SEQUENCE vb. bulunur.
        // Bunların altındaki segment URI'leri TV kanalı değildir.
        if (lines.any { it.startsWith("#EXT-X-", ignoreCase = true) }) return emptyList()

        val out = ArrayList<MediaEntry>()
        var name: String? = null
        var logo: String? = null
        var group: String? = null
        for (line in lines) {
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    logo = attribute(line, "tvg-logo")
                    group = attribute(line, "group-title")
                    name = extInfTitle(line)
                }
                line.startsWith("#") -> Unit
                else -> {
                    val channelName = name?.ifBlank { null } ?: line.substringAfterLast('/').ifBlank { line }
                    val isHidden = group?.contains("religious", ignoreCase = true) == true ||
                        hiddenNames.any { channelName.contains(it, ignoreCase = true) }
                    if (!isHidden) {
                        val resolved = resolveUrl(line, sourceUrl)
                        if (resolved != null && out.size < 10_000) {
                            val groupName = group?.split(';')?.firstOrNull()?.trim()?.ifBlank { null }
                                ?.let { groupNames[it.lowercase()] ?: it } ?: fallbackGroup
                            out += MediaEntry(Kind.TV, channelName.take(200), resolved, logo?.let(::safeLogoUrl), groupName.take(80))
                        }
                    }
                    name = null
                    logo = null
                    group = null
                }
            }
        }
        return out
    }

    /** EXTINF başlığını tırnak dışındaki ilk virgülden ayırır; başlıktaki virgülleri korur. */
    private fun extInfTitle(line: String): String? {
        var quoted = false
        for (index in line.indices) {
            when (line[index]) {
                '"' -> quoted = !quoted
                ',' -> if (!quoted) return line.substring(index + 1).trim().ifBlank { null }
            }
        }
        return null
    }

    private fun attribute(line: String, key: String): String? =
        Regex("(?:^|\\s)$key=\"([^\"]*)\"", RegexOption.IGNORE_CASE)
            .find(line)?.groupValues?.get(1)

    private fun safeLogoUrl(value: String): String? = runCatching { URI(value) }.getOrNull()?.let { uri ->
        if (uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.toASCIIString().length <= 4096) uri.toASCIIString() else null
    }

    /** Yalnızca HTTP(S) kaynak listelerinin göreli adresleri çözümlenir. */
    private fun resolveUrl(value: String, sourceUrl: String?): String? {
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        if (uri.isAbsolute) return value.takeIf { uri.scheme?.lowercase() in setOf("http", "https", "rtsp") && !uri.host.isNullOrBlank() && uri.rawUserInfo == null && value.length <= 4096 }
        if (sourceUrl == null) return null
        val base = runCatching { URI(sourceUrl) }.getOrNull() ?: return null
        if (base.scheme?.lowercase() !in setOf("http", "https") || base.host.isNullOrBlank() || base.rawUserInfo != null) return null
        return runCatching { base.resolve(uri) }.getOrNull()?.let { resolved ->
            if (resolved.scheme?.lowercase() in setOf("http", "https") && !resolved.host.isNullOrBlank() && resolved.rawUserInfo == null && resolved.toASCIIString().length <= 4096) resolved.toASCIIString() else null
        }
    }
}
