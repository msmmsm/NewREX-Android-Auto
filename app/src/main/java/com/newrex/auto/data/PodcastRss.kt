// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import java.net.URI
import java.text.SimpleDateFormat
import java.util.Locale

/** RSS enclosure adresleri ve yayın tarihleri için toleranslı yardımcılar. */
internal object PodcastRss {
    private val dateFormats = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss z",
        "EEE, dd MMM yyyy HH:mm Z",
        "EEE, dd MMM yyyy HH:mm z",
        "dd MMM yyyy HH:mm:ss Z",
        "dd MMM yyyy HH:mm Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )

    fun resolveEnclosureUrl(feedUrl: String, enclosureUrl: String): String? {
        if (enclosureUrl.isBlank()) return null
        val enclosure = runCatching { URI(enclosureUrl) }.getOrNull() ?: return null
        if (enclosure.isAbsolute) return enclosureUrl
        val feed = runCatching { URI(feedUrl) }.getOrNull() ?: return null
        if (feed.scheme !in setOf("http", "https")) return null
        return runCatching { feed.resolve(enclosure).toString() }.getOrNull()
    }

    fun formatDate(raw: String): String {
        val value = raw.trim()
        if (value.isEmpty()) return ""
        for (pattern in dateFormats) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }.parse(value)
            }.getOrNull() ?: continue
            return SimpleDateFormat("d MMMM yyyy", Locale("tr", "TR")).format(parsed)
        }
        return ""
    }
}
