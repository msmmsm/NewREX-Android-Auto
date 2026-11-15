package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ToyotaChr2025PassionProfileTest {
    @Test fun identifiesTheConfiguredReferenceVehicle() {
        assertEquals("2025 Toyota C-HR 1.8 Hybrid Passion (Türkiye)", ToyotaChr2025PassionProfile.displayName)
    }
    @Test fun confirmsRearCameraAndSafetySensorsWithoutClaimingVideoAccess() {
        val rear = ToyotaChr2025PassionProfile.capability("rear_camera")!!
        assertEquals(EquipmentStatus.CONFIRMED_PRESENT, rear.equipmentStatus)
        assertEquals(DataAccessStatus.OEM_SYSTEM_ONLY, rear.dataAccess)
        val radar = ToyotaChr2025PassionProfile.capability("front_millimeter_radar")!!
        assertEquals(EquipmentStatus.CONFIRMED_PRESENT, radar.equipmentStatus)
        assertEquals(DataAccessStatus.DIAGNOSTIC_STATUS_ONLY, radar.dataAccess)
    }
    @Test fun doesNotAdvertisePanoramicCameraOnPassionTrim() {
        assertEquals(EquipmentStatus.NOT_INCLUDED_IN_TRIM, ToyotaChr2025PassionProfile.capability("panoramic_view_monitor")!!.equipmentStatus)
    }
    @Test fun unknownSensorIdReturnsNull() {
        assertNull(ToyotaChr2025PassionProfile.capability("unknown_camera"))
    }
}