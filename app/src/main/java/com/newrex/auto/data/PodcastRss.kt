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

    /** RSS kaynağı için dış ağa çıkmadan önce HTTP(S) adresini doğrula. */
    fun isSafeFeedUrl(value: String): Boolean {
        if (value.isBlank() || value.length > MAX_URL_LENGTH || value.any { it.isWhitespace() || it.isISOControl() }) return false
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        return uri.scheme?.lowercase() in setOf("http", "https") &&
            !uri.host.isNullOrBlank() && uri.rawUserInfo == null
    }

    fun resolveEnclosureUrl(feedUrl: String, enclosureUrl: String): String? {
        if (enclosureUrl.isBlank() || enclosureUrl.length > MAX_URL_LENGTH || enclosureUrl.any { it.isWhitespace() || it.isISOControl() }) return null
        val enclosure = runCatching { URI(enclosureUrl) }.getOrNull() ?: return null
        val resolved = if (enclosure.isAbsolute) enclosure else {
            val feed = runCatching { URI(feedUrl) }.getOrNull() ?: return null
            if (!isSafeFeedUrl(feedUrl)) return null
            runCatching { feed.resolve(enclosure) }.getOrNull() ?: return null
        }
        return resolved.toASCIIString().takeIf {
            resolved.scheme?.lowercase() in setOf("http", "https") && !resolved.host.isNullOrBlank() &&
                resolved.rawUserInfo == null && it.length <= MAX_URL_LENGTH
        }
    }

    private const val MAX_URL_LENGTH = 4096
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
