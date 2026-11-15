package com.newrex.auto.data

/** ELM327 Bluetooth bağlantısı için sınırlı yeniden deneme politikası. */
internal object ObdReconnectPolicy {
    private const val MAX_RETRIES = 3
    private val BACKOFF_MS = longArrayOf(1_000L, 3_000L, 7_000L)

    fun shouldRetry(retriesAlreadyUsed: Int): Boolean =
        retriesAlreadyUsed in 0 until MAX_RETRIES

    fun delayMs(retryIndex: Int): Long {
        require(retryIndex >= 0) { "Deneme indeksi negatif olamaz" }
        return BACKOFF_MS[retryIndex.coerceAtMost(BACKOFF_MS.lastIndex)]
    }
}
