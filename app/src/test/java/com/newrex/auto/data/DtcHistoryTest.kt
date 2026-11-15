package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DtcHistoryTest {
    @Test fun recordsFirstAndRepeatedObservationsWithoutCountingDuplicateTimestamp() {
        val first = DtcHistory.recordScan(emptyList(), listOf("p0300", "P0300"), 1_000L)
        assertEquals(1, first.size)
        assertEquals(1, first.single().scanCount)
        assertEquals(1_000L, first.single().firstSeenMs)
        assertTrue(first.single().presentInLastScan)

        val sameMoment = DtcHistory.recordScan(first, listOf("P0300"), 1_000L)
        assertEquals(1, sameMoment.single().scanCount)

        val later = DtcHistory.recordScan(sameMoment, listOf("P0300"), 2_000L)
        assertEquals(2, later.single().scanCount)
        assertEquals(1_000L, later.single().firstSeenMs)
        assertEquals(2_000L, later.single().lastSeenMs)
    }

    @Test fun aVerifiedEmptyScanMarksNotReportedButDoesNotDeleteHistory() {
        val first = DtcHistory.recordScan(emptyList(), listOf("P0300"), 1_000L)
        val empty = DtcHistory.recordScan(first, emptyList(), 2_000L)
        assertEquals(1, empty.size)
        assertFalse(empty.single().presentInLastScan)
        assertEquals(1, empty.single().scanCount)
        assertEquals(1_000L, empty.single().lastSeenMs)
    }

    @Test fun clearAcknowledgementPreservesHistoryAndRecordsTime() {
        val first = DtcHistory.recordScan(emptyList(), listOf("P0300"), 1_000L)
        val cleared = DtcHistory.markCleared(first, 3_000L)
        assertEquals(1, cleared.size)
        assertFalse(cleared.single().presentInLastScan)
        assertEquals(3_000L, cleared.single().clearedAtMs)
    }

    @Test fun ignoresInvalidCodesAndKeepsNewestHundred() {
        val input = (1..105).map { index ->
            DtcHistoryEntry("P%04X".format(index), index.toLong(), index.toLong(), 1, true)
        } + DtcHistoryEntry("INVALID", 999L, 999L, 1, true)
        val updated = DtcHistory.recordScan(input, emptyList(), 2_000L)
        assertEquals(100, updated.size)
        assertFalse(updated.any { it.code == "INVALID" })
        assertEquals("P0069", updated.first().code)
    }
}
