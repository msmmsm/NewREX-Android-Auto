package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TripAnalyticsTest {
    @Test fun summarizesOnlyTripsInsideSelectedPeriod() {
        val trips = listOf(
            TripRecord(2_000L, 62_000L, 10f, 1, 80, 1, 0, 92),
            TripRecord(70_000L, 190_000L, 25f, 2, 110, 0, 2, 80),
            TripRecord(250_000L, 310_000L, 5f, 1, 60, 0, 0, 100),
        )
        val summary = TripAnalytics.summarize(trips, 1_000L, 200_000L)
        assertEquals(2, summary.tripCount)
        assertEquals(35f, summary.distanceKm, 0.001f)
        assertEquals(3L, summary.movingMinutes)
        assertEquals(86, summary.averageScore)
        assertEquals(110, summary.maxKmh)
        assertEquals(3L, summary.harshEvents)
    }

    @Test fun returnsEmptySummaryForInvalidPeriodOrNoTrips() {
        val trips = listOf(TripRecord(2_000L, 62_000L, 10f, 1, 80, 1, 0, 92))
        assertEquals(0, TripAnalytics.summarize(trips, 200_000L, 100_000L).tripCount)
        assertEquals(0, TripAnalytics.summarize(trips, 100_000L, 200_000L).tripCount)
    }

    @Test fun excludesInvalidTripRecords() {
        val trips = listOf(
            TripRecord(2_000L, 62_000L, 10f, 1, 80, 1, 0, 92),
            TripRecord(70_000L, 60_000L, 25f, 2, 110, 0, 2, 80),
        )
        val summary = TripAnalytics.summarize(trips, 1_000L, 200_000L)
        assertEquals(1, summary.tripCount)
        assertEquals(10f, summary.distanceKm, 0.001f)
    }
}
