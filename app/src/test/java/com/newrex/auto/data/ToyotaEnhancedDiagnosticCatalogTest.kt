package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToyotaEnhancedDiagnosticCatalogTest {
    @Test fun catalogIsBoundToTheConfirmedTurkishMarketVehicle() {
        assertEquals(
            "2025 Toyota C-HR 1.8 Hybrid Passion (Türkiye)",
            ToyotaEnhancedDiagnosticCatalog.vehicle
        )
    }

    @Test fun containsHybridBatteryAndAdasDiagnosticSystems() {
        val ids = ToyotaEnhancedDiagnosticCatalog.systems.map { it.id }.toSet()
        assertTrue("hv_battery" in ids)
        assertTrue("hybrid_control" in ids)
        assertTrue("front_radar_pcs" in ids)
        assertTrue("blind_spot" in ids)
    }

    @Test fun noUnverifiedToyotaPidOrRawCanCommandIsPublished() {
        ToyotaEnhancedDiagnosticCatalog.systems.forEach { item ->
            assertTrue(item.accessPath.contains("Toyota OEM teşhis cihazı"))
            assertTrue(item.limitation.isNotBlank())
            assertTrue(item.candidateMeasurements.isNotEmpty())
        }
    }

    @Test fun catalogLookupDoesNotInventUnknownSystems() {
        assertNotNull(ToyotaEnhancedDiagnosticCatalog.find("hv_battery"))
        assertNull(ToyotaEnhancedDiagnosticCatalog.find("invented_ecu"))
    }
}
