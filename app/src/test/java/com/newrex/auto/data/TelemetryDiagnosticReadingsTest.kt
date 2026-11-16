package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryDiagnosticReadingsTest {
    @Test fun recordsReadingsByStableParameterIdAndKeepsLatestValue() {
        TelemetryHub.clearObdData()
        val first = reading(value = 20.0, timestamp = 100L)
        val latest = reading(value = 25.0, timestamp = 200L)

        TelemetryHub.recordObdReading(first)
        TelemetryHub.recordObdReading(latest)

        val stored = TelemetryHub.obdDiagnosticReadings.value.getValue("vehicle.speed")
        assertEquals(25.0, stored.value, 0.0)
        assertEquals(200L, stored.timestampEpochMs)
        assertEquals(1, TelemetryHub.obdDiagnosticReadings.value.size)
        assertEquals(200L, TelemetryHub.obdLastUpdatedMs.value)
        TelemetryHub.clearObdData()
    }

    @Test fun invalidReadingRemovalDoesNotLeaveStaleValue() {
        TelemetryHub.clearObdData()
        TelemetryHub.recordObdReading(reading())
        TelemetryHub.removeObdReading("vehicle.speed")
        assertFalse(TelemetryHub.obdDiagnosticReadings.value.containsKey("vehicle.speed"))
        TelemetryHub.clearObdData()
    }

    @Test fun disconnectClearsAllDiagnosticReadingProvenance() {
        TelemetryHub.recordObdReading(reading())
        TelemetryHub.clearObdData()
        assertTrue(TelemetryHub.obdDiagnosticReadings.value.isEmpty())
        assertEquals(null, TelemetryHub.obdLastUpdatedMs.value)
    }

    @Test fun unverifiedEcuAddressCannotBecomeTrustworthyByDefault() {
        val reading = reading()
        assertEquals(DiagnosticValueQuality.UNVERIFIED, reading.quality)
        assertFalse(reading.isTrustworthy())
    }

    private fun reading(
        value: Double = 20.0,
        timestamp: Long = 100L
    ) = DiagnosticReading(
        parameterId = "vehicle.speed",
        value = value,
        unit = "km/h",
        source = "SAE J1979 Mode 01 PID 0D",
        ecuId = "OBD response; ECU address unresolved",
        timestampEpochMs = timestamp,
        quality = DiagnosticValueQuality.UNVERIFIED
    )
}
