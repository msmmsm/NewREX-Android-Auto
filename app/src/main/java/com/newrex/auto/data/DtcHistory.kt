package com.newrex.auto.data

/** Kalıcı DTC geçmişi için tek bir kodun son bilinen durumu. */
data class DtcHistoryEntry(
    val code: String,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val scanCount: Int,
    val presentInLastScan: Boolean,
    val clearedAtMs: Long? = null
)

/** Zaman sıralı tarama sonuçlarını birleştiren, Android bağımlılığı olmayan geçmiş modeli. */
object DtcHistory {
    const val MAX_ENTRIES = 100

    fun recordScan(
        history: List<DtcHistoryEntry>,
        codes: List<String>,
        scannedAtMs: Long
    ): List<DtcHistoryEntry> {
        val found = codes.map { it.trim().uppercase() }
            .filter { it.matches(Regex("[PCBU][0-3][0-9A-F]{3}")) }
            .toSet()
        val previous = history
            .filter { it.code.matches(Regex("[PCBU][0-3][0-9A-F]{3}")) }
            .associateBy { it.code }
        if (scannedAtMs <= 0L) {
            return previous.values.sortedByDescending { it.lastSeenMs }.take(MAX_ENTRIES)
        }
        val updated = (previous.keys + found).map { code ->
            val old = previous[code]
            when {
                code in found && old == null -> DtcHistoryEntry(
                    code = code,
                    firstSeenMs = scannedAtMs,
                    lastSeenMs = scannedAtMs,
                    scanCount = 1,
                    presentInLastScan = true
                )
                code in found -> old!!.copy(
                    lastSeenMs = maxOf(old.lastSeenMs, scannedAtMs),
                    scanCount = if (scannedAtMs > old.lastSeenMs) old.scanCount + 1 else old.scanCount,
                    presentInLastScan = true,
                    clearedAtMs = null
                )
                else -> old!!.copy(presentInLastScan = false)
            }
        }
        return updated.sortedWith(compareByDescending<DtcHistoryEntry> { it.lastSeenMs }.thenBy { it.code })
            .take(MAX_ENTRIES)
    }

    fun markCleared(history: List<DtcHistoryEntry>, clearedAtMs: Long): List<DtcHistoryEntry> {
        if (clearedAtMs <= 0L) return history
        return history.map { if (it.presentInLastScan) it.copy(presentInLastScan = false, clearedAtMs = clearedAtMs) else it }
    }
}
