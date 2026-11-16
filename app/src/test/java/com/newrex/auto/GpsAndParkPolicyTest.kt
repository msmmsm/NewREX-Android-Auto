package com.newrex.auto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsAndParkPolicyTest {
    @Test fun acceptsOnlyPlausibleGpsDistanceSamples() {
        assertTrue(GpsTripDistancePolicy.accepts(30f, 5_000L))
        assertFalse(GpsTripDistancePolicy.accepts(500f, 1_000L))
        assertFalse(GpsTripDistancePolicy.accepts(20f, 100L))
        assertFalse(GpsTripDistancePolicy.accepts(20f, 121_000L))
        assertFalse(GpsTripDistancePolicy.accepts(2f, 5_000L))
        assertFalse(GpsTripDistancePolicy.accepts(Float.NaN, 5_000L))
        assertFalse(GpsTripDistancePolicy.accepts(-10f, 5_000L))
    }

    @Test fun parkLocationRequiresValidCoordinatesAccuracyAndFreshness() {
        assertTrue(ParkLocationPolicy.accepts(37.0, 29.0, 12f, 60_000L))
        assertFalse(ParkLocationPolicy.accepts(91.0, 29.0, 12f, 60_000L))
        assertFalse(ParkLocationPolicy.accepts(37.0, 181.0, 12f, 60_000L))
        assertFalse(ParkLocationPolicy.accepts(37.0, 29.0, Float.NaN, 60_000L))
        assertFalse(ParkLocationPolicy.accepts(37.0, 29.0, 101f, 60_000L))
        assertFalse(ParkLocationPolicy.accepts(37.0, 29.0, 12f, -1L))
        assertFalse(ParkLocationPolicy.accepts(37.0, 29.0, 12f, 30L * 60 * 1000 + 1))
    }
}
