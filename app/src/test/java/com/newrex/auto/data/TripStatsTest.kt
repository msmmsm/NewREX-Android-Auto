package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TripStatsTest {
    @Test fun rejectsNonFiniteNegativeAndImplausibleSpeedSamples() {
        TripStats.reset()
        TripStats.onSpeed(Float.NaN, 1_000L)
        TripStats.onSpeed(Float.POSITIVE_INFINITY, 2_000L)
        TripStats.onSpeed(-1f, 3_000L)
        TripStats.onSpeed(80f, 4_000L)
        assertEquals(0f, TripStats.maxKmh.value, 0.001f)
        assertEquals(0, TripStats.harshAccel.value)
        assertEquals(0, TripStats.harshBrake.value)
    }

    @Test fun ignoresOutOfOrderSamplesForAccelerationAndMovingTime() {
        TripStats.reset()
        TripStats.onSpeed(10f, 10_000L)
        TripStats.onSpeed(20f, 9_000L)
        assertEquals(0, TripStats.harshAccel.value)
        assertEquals(0L, TripStats.movingMs)
        TripStats.onSpeed(20f, 11_000L)
        assertEquals(0, TripStats.harshAccel.value)
    }

    @Test fun countsPlausibleHarshAccelerationAndBrakingOncePerCooldown() {
        TripStats.reset()
        TripStats.onSpeed(5f, 1_000L)
        TripStats.onSpeed(9f, 2_000L)
        assertEquals(1, TripStats.harshAccel.value)
        TripStats.onSpeed(5f, 2_500L)
        assertEquals(1, TripStats.harshBrake.value)
        assertEquals(1, TripStats.harshAccel.value)
    }

    @Test fun tracksMaximumSpeedOnlyForPlausibleSamples() {
        TripStats.reset()
        TripStats.onSpeed(20f, 1_000L)
        TripStats.onSpeed(30f, 2_000L)
        TripStats.onSpeed(90f, 3_000L)
        assertEquals(108f, TripStats.maxKmh.value, 0.01f)
    }
}
