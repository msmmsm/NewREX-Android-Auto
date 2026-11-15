// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import android.content.Context
import java.security.MessageDigest

/** Radyo adreslerinin kısa süreli hata geçmişini saklar; ham URL depolanmaz. */
object RadioRecovery {
    private const val PREFS = "omnidrive_radio_recovery"
    private const val PREFIX = "stream_"
    private const val MAX_RECORDS = 40
    private const val HISTORY_TTL_MS = 7L * 24 * 60 * 60 * 1000
    private lateinit var preferences: android.content.SharedPreferences

    data class History(val failures: Int, val lastFailureAt: Long, val cooldownUntil: Long, val lastState: String)

    fun init(context: Context) {
        if (!::preferences.isInitialized) synchronized(this) {
            if (!::preferences.isInitialized) preferences =
                context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    @Synchronized
    fun record(station: MediaEntry, success: Boolean, state: String = "") {
        if (!::preferences.isInitialized || station.kind != Kind.RADIO) return
        val key = key(station.url)
        if (success) {
            preferences.edit().remove(key).apply()
            return
        }
        val failures = (read(key)?.failures ?: 0) + 1
        val now = System.currentTimeMillis()
        val until = now + cooldownMs(failures)
        preferences.edit().putString(key, "$failures|$now|$until|$state".take(256)).apply()
        prune(now)
    }

    fun isCoolingDown(url: String, now: Long = System.currentTimeMillis()): Boolean =
        if (!::preferences.isInitialized) false else (read(key(url))?.cooldownUntil ?: 0L) > now

    fun history(url: String): History? = if (::preferences.isInitialized) read(key(url)) else null

    private fun read(key: String): History? {
        val raw = preferences.getString(key, null) ?: return null
        val parts = raw.split('|', limit = 4)
        if (parts.size != 4) return null
        val failures = parts[0].toIntOrNull()?.coerceAtLeast(0) ?: return null
        val failedAt = parts[1].toLongOrNull()?.takeIf { it > 0 } ?: return null
        val until = parts[2].toLongOrNull()?.takeIf { it >= failedAt } ?: return null
        return History(failures, failedAt, until, parts[3])
    }

    private fun prune(now: Long) {
        val entries = preferences.all.keys.filter { it.startsWith(PREFIX) }
            .mapNotNull { k -> read(k)?.let { k to it.lastFailureAt } }
        val remove = (entries.filter { now - it.second > HISTORY_TTL_MS }.map { it.first } +
            entries.sortedByDescending { it.second }.drop(MAX_RECORDS).map { it.first }).toSet()
        if (remove.isNotEmpty()) preferences.edit().also { edit -> remove.forEach(edit::remove) }.apply()
    }

    private fun key(url: String): String = PREFIX + MessageDigest.getInstance("SHA-256")
        .digest(url.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    internal fun cooldownMs(failures: Int): Long = when (failures.coerceAtLeast(1)) {
        1 -> 5 * 60_000L
        2 -> 15 * 60_000L
        else -> 60 * 60_000L
    }

    /** Benzer isimli alternatiflerde aynı istasyon ve güncel katalog kalitesini öne alır. */
    internal fun rank(original: MediaEntry, candidate: MediaEntry): Int {
        val originalName = original.title.normName()
        val candidateName = candidate.title.normName()
        var score = when {
            candidateName == originalName -> 1000
            candidateName.startsWith("$originalName ") || originalName.startsWith("$candidateName ") -> 700
            else -> 0
        }
        if (original.countryCode != null && candidate.countryCode == original.countryCode) score += 80
        if (candidate.directoryReportsHealthy == true) score += 60
        if (candidate.codec != null) score += 10
        if (candidate.bitrateKbps != null) score += 10
        score += (candidate.votes ?: 0).coerceIn(0, 100) / 10
        return score
    }
}
