package com.newrex.auto.data

/** Canlı medya akışları için sınırlı, deterministik yeniden bağlanma politikası. */
internal object LiveReconnectPolicy {
    private const val MAX_ESTABLISHED_RETRIES = 10
    private const val MAX_INITIAL_RETRIES = 2
    private const val MAX_DELAY_MS = 15_000L

    fun shouldRetry(attemptsAlreadyMade: Int, playedSuccessfully: Boolean): Boolean {
        val limit = if (playedSuccessfully) MAX_ESTABLISHED_RETRIES else MAX_INITIAL_RETRIES
        return attemptsAlreadyMade in 0 until limit
    }

    /** İlk denemeler 1, 2, 4, 8 sn; sonraki denemeler en fazla 15 sn bekler. */
    fun delayMs(attemptIndex: Int): Long {
        require(attemptIndex >= 0) { "Deneme indeksi negatif olamaz" }
        return (1_000L shl attemptIndex.coerceAtMost(4)).coerceAtMost(MAX_DELAY_MS)
    }
}
