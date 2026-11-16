package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TripPeriodComparatorTest {
    @Test fun comparesCurrentAndPreviousPeriods() {
        val trips = listOf(
            TripRecord(1_500L, 60_000L, 12f, 1, 90, 1, 0, 90),
            TripRecord(10_500L, 70_000L, 20f, 2, 100, 0, 1, 80),
            TripRecord(101_500L, 160_000L, 8f, 1, 70, 0, 0, 100),
        )
        val result = TripPeriodComparator.compare(trips, 100_000L, 200_000L, 1_000L, 100_000L)
        assertEquals(8f, result.current.distanceKm, 0.001f)
        assertEquals(32f, result.previous.distanceKm, 0.001f)
        assertEquals(-24f, result.distanceDeltaKm, 0.001f)
        assertEquals(-2, result.tripCountDelta)
        assertEquals(20, result.scoreDelta)
    }

    @Test fun emptyPreviousPeriodProducesZeroBaseline() {
        val result = TripPeriodComparator.compare(
            emptyList(), 100_000L, 200_000L, 1_000L, 100_000L
        )
        assertEquals(0, result.current.tripCount)
        assertEquals(0, result.previous.tripCount)
        assertEquals(0f, result.distanceDeltaKm, 0.001f)
    }
}
