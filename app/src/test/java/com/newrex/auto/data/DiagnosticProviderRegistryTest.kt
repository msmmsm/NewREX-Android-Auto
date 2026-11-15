package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticProviderRegistryTest {
    @Test fun disconnectedElm327DoesNotAdvertiseReadableServices() {
        val providers = DiagnosticProviderRegistry.snapshots(
            DiagnosticConnectionState.DISCONNECTED,
            obdAdapterSelected = false
        )
        val elm = providers.first { it.kind == DiagnosticProviderKind.STANDARD_OBD_II }
        assertEquals(DiagnosticProviderState.NOT_CONNECTED, elm.state)
        assertTrue(elm.supportedServices.isEmpty())
    }

    @Test fun connectedElm327AdvertisesOnlyImplementedStandardServices() {
        val providers = DiagnosticProviderRegistry.snapshots(
            DiagnosticConnectionState.CONNECTED,
            obdAdapterSelected = true
        )
        val elm = providers.first { it.kind == DiagnosticProviderKind.STANDARD_OBD_II }
        assertEquals(DiagnosticProviderState.CONNECTED, elm.state)
        assertEquals(
            setOf(
                DiagnosticService.MODE_01_LIVE_DATA,
                DiagnosticService.MODE_02_FREEZE_FRAME,
                DiagnosticService.MODE_03_READ_DTC,
                DiagnosticService.MODE_04_CLEAR_DTC,
                DiagnosticService.MODE_09_VEHICLE_INFO
            ),
            elm.supportedServices
        )
        assertTrue(elm.limitation.contains("Toyota özel ECU Data List erişimi değildir"))
    }


    @Test fun connectedStateWithoutSelectedAdapterFailsClosed() {
        val elm = Elm327DiagnosticProvider(
            DiagnosticConnectionState.CONNECTED,
            adapterSelected = false
        ).snapshot()
        assertEquals(DiagnosticProviderState.NOT_CONNECTED, elm.state)
        assertTrue(elm.supportedServices.isEmpty())
    }

    @Test fun connectingAndErrorStatesNeverAppearAsConnected() {
        listOf(
            DiagnosticConnectionState.CONNECTING,
            DiagnosticConnectionState.INITIALIZING,
            DiagnosticConnectionState.ERROR
        ).forEach { state ->
            val elm = Elm327DiagnosticProvider(state, adapterSelected = true).snapshot()
            assertEquals(DiagnosticProviderState.NOT_CONNECTED, elm.state)
            assertTrue(elm.supportedServices.isEmpty())
        }
    }

    @Test fun toyotaOemProviderFailsClosedUntilAValidatedImplementationExists() {
        val provider = ToyotaOemDiagnosticProvider().snapshot()
        assertEquals(DiagnosticProviderState.NOT_IMPLEMENTED, provider.state)
        assertTrue(provider.supportedServices.isEmpty())
        assertTrue(provider.limitation.contains("henüz bağlantı veya ECU sorgusu yapılmaz"))
    }

    @Test fun registryAlwaysKeepsGenericAndOemProvidersSeparate() {
        val providers = DiagnosticProviderRegistry.snapshots(
            DiagnosticConnectionState.CONNECTED,
            obdAdapterSelected = true
        )
        assertEquals(2, providers.size)
        assertEquals(
            setOf(DiagnosticProviderKind.STANDARD_OBD_II, DiagnosticProviderKind.TOYOTA_OEM_ENHANCED),
            providers.map { it.kind }.toSet()
        )
    }

    @Test fun readingRequiresProvenanceUnitEcuTimestampAndValidQuality() {
        val valid = DiagnosticReading(
            parameterId = "engine.coolant.temperature",
            value = 88.0,
            unit = "°C",
            source = "SAE J1979 Mode 01 PID 05",
            ecuId = "emissions_ecu",
            timestampEpochMs = 1_800_000_000_000,
            quality = DiagnosticValueQuality.VALID
        )
        assertTrue(valid.isTrustworthy())
        assertFalse(valid.copy(source = "").isTrustworthy())
        assertFalse(valid.copy(unit = "").isTrustworthy())
        assertFalse(valid.copy(ecuId = "").isTrustworthy())
        assertFalse(valid.copy(timestampEpochMs = 0L).isTrustworthy())
        assertFalse(valid.copy(quality = DiagnosticValueQuality.UNVERIFIED).isTrustworthy())
        assertFalse(valid.copy(value = Double.NaN).isTrustworthy())
    }

    @Test fun nonFiniteMeasurementsAreNeverTrusted() {
        val base = DiagnosticReading(
            "battery.voltage", 12.4, "V", "OBD", "ecu", 1L, DiagnosticValueQuality.VALID
        )
        assertFalse(base.copy(value = Double.POSITIVE_INFINITY).isTrustworthy())
    }
}
