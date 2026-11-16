package com.newrex.auto.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDataValidationTest {
    private val now = 2_000_000L

    private fun trip(
        start: Long = 1_000_000L,
        end: Long = 1_600_000L,
        km: Float = 8f,
        movingMinutes: Int = 8,
        maxKmh: Int = 90,
        harshAccel: Int = 1,
        harshBrake: Int = 0,
        score: Int = 92,
    ) = TripRecord(start, end, km, movingMinutes, maxKmh, harshAccel, harshBrake, score)

    @Test fun acceptsPlausibleTripRecord() {
        assertTrue(trip().isValid(now))
    }

    @Test fun rejectsInvalidTripTimesAndFutureRecords() {
        assertFalse(trip(start = 0L).isValid(now))
        assertFalse(trip(start = 1_600_001L, end = 1_600_000L).isValid(now))
        assertFalse(trip(end = now + 5 * 60 * 1000L + 1L).isValid(now))
        assertFalse(trip(start = 1L, end = 1L + 73L * 60 * 60 * 1000).isValid(now + 73L * 60 * 60 * 1000))
    }

    @Test fun rejectsImpossibleTripMetrics() {
        assertFalse(trip(km = Float.NaN).isValid(now))
        assertFalse(trip(km = -1f).isValid(now))
        assertFalse(trip(movingMinutes = 20).isValid(now))
        assertFalse(trip(maxKmh = 261).isValid(now))
        assertFalse(trip(harshBrake = -1).isValid(now))
        assertFalse(trip(score = 101).isValid(now))
    }

    @Test fun validatesFuelEntriesAndAllowsUnknownOdometer() {
        assertTrue(FuelEntry(now, 38f, 1_900f, 0f).isValid(now))
        assertFalse(FuelEntry(now, Float.NaN, 1_900f, 10_000f).isValid(now))
        assertFalse(FuelEntry(now, 201f, 1_900f, 10_000f).isValid(now))
        assertFalse(FuelEntry(now, 38f, -1f, 10_000f).isValid(now))
        assertFalse(FuelEntry(now + 5 * 60 * 1000L + 1L, 38f, 1_900f, 10_000f).isValid(now))
    }
}
