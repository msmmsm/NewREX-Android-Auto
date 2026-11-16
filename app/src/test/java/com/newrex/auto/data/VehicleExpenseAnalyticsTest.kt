package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleExpenseAnalyticsTest {
    @Test fun totalsFuelAndServiceOnlyInsideSelectedPeriod() {
        val fuel = listOf(
            FuelEntry(1_000L, 30f, 1_500f, 10_000f),
            FuelEntry(2_000L, 25f, 1_250f, 10_500f),
            FuelEntry(9_000L, 20f, 1_000f, 11_000f),
        )
        val services = listOf(
            ServiceRecord(1_500L, "Yağ değişimi", 10_200f, 2_000f, ""),
            ServiceRecord(8_000L, "Lastik", 10_900f, 3_000f, ""),
        )
        val summary = VehicleExpenseAnalytics.summarize(fuel, services, 1_000L, 5_000L)
        assertEquals(2_750f, summary.fuelTotal, 0.001f)
        assertEquals(2_000f, summary.serviceTotal, 0.001f)
        assertEquals(4_750f, summary.total, 0.001f)
        assertEquals(2, summary.fuelEntries)
        assertEquals(1, summary.serviceEntries)
    }

    @Test fun excludesInvalidRecordsAndRejectsInvalidPeriod() {
        val invalidFuel = FuelEntry(2_000L, Float.NaN, 1_000f, 10_000f)
        val invalidService = ServiceRecord(2_000L, "", 10_000f, 500f, "")
        val summary = VehicleExpenseAnalytics.summarize(
            listOf(invalidFuel), listOf(invalidService), 1_000L, 5_000L
        )
        assertEquals(0f, summary.total, 0.001f)
        assertEquals(0, summary.fuelEntries)
        assertEquals(0, summary.serviceEntries)
        assertEquals(0f, VehicleExpenseAnalytics.summarize(
            emptyList(), emptyList(), 5_000L, 1_000L
        ).total, 0.001f)
    }
}
