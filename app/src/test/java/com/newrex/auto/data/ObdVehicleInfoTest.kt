package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObdVehicleInfoTest {
    @Test fun readsOnlyPidsAdvertisedByMode09Bitmap() {
        assertEquals(
            setOf(0x02, 0x04, 0x06, 0x0A),
            ObdVehicleInfo.parseSupportedPids("7E806490055000000")
        )
        assertNull(ObdVehicleInfo.parseSupportedPids("NO DATA"))
        assertNull(ObdVehicleInfo.parseSupportedPids("490000"))
    }

    @Test fun parsesSeventeenCharacterVinAndRejectsMalformedValues() {
        assertEquals(
            "1HGCM82633A004352",
            ObdVehicleInfo.parseVin("490201314847434D383236333341303034333532")
        )
        assertNull(ObdVehicleInfo.parseVin("490201314847434D"))
        assertNull(ObdVehicleInfo.parseVin("NO DATA"))
    }

    @Test fun parsesVinAcrossIsoTpContinuationFrame() {
        assertEquals(
            "1HGCM82633A004352",
            ObdVehicleInfo.parseVin("490201314847434D3832363333213341303034333532")
        )
    }

    @Test fun parsesCalibrationIdsAndEcuNameAsAscii() {
        assertEquals(listOf("ABC"), ObdVehicleInfo.parseAsciiValues("490401414243", 0x04))
        assertEquals(listOf("ENGINE"), ObdVehicleInfo.parseAsciiValues("490A01454E47494E45", 0x0A))
        assertEquals(emptyList<String>(), ObdVehicleInfo.parseAsciiValues("490401414243", 0x02))
    }

    @Test fun parsesCalibrationVerificationNumbersAsFourByteHexValues() {
        assertEquals(
            listOf("12345678", "90ABCDEF"),
            ObdVehicleInfo.parseCvns("4906021234567890ABCDEF")
        )
        assertEquals(emptyList<String>(), ObdVehicleInfo.parseCvns("NO DATA"))
    }
}
