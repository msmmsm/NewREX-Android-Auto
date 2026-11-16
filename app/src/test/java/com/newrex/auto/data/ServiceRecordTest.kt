package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceRecordTest {
    private val now = 2_000_000L

    private fun record(
        time: Long = now,
        category: String = "Periyodik bakım",
        km: Float? = 85_000f,
        cost: Float = 4_500f,
        note: String = "Yağ ve filtre değişimi",
    ) = ServiceRecord(time, category, km, cost, note)

    @Test fun acceptsValidServiceRecord() {
        assertTrue(record().isValid(now))
        assertTrue(record(km = null, cost = 0f, note = "").isValid(now))
    }

    @Test fun rejectsInvalidServiceRecordFields() {
        assertFalse(record(time = 0L).isValid(now))
        assertFalse(record(time = now + 5 * 60 * 1000L + 1L).isValid(now))
        assertFalse(record(category = "").isValid(now))
        assertFalse(record(category = "x".repeat(41)).isValid(now))
        assertFalse(record(km = Float.NaN).isValid(now))
        assertFalse(record(km = -1f).isValid(now))
        assertFalse(record(cost = Float.POSITIVE_INFINITY).isValid(now))
        assertFalse(record(cost = -1f).isValid(now))
        assertFalse(record(note = "x".repeat(161)).isValid(now))
    }

    @Test fun serializesAndRestoresServiceRecord() {
        val original = record(km = null, cost = 0f, note = "")
        val restored = ServiceRecord.fromJson(original.toJson())
        assertEquals(original, restored)
        assertNull(restored.odometerKm)
    }
}
