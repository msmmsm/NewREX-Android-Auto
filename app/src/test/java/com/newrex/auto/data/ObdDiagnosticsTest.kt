package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObdDiagnosticsTest {
    @Test fun parsesFreezeFrameWithCanHeaderAndFrameNumber() {
        assertEquals(
            0 to listOf(0x1A, 0xF8),
            ObdDiagnostics.parseFreezeFrameBytes("7E806420C001AF8", 0x0C, 2)
        )
        assertEquals(
            0 to listOf(0x2A),
            ObdDiagnostics.parseFreezeFrameBytes("7E804420D002A", 0x0D, 1)
        )
    }

    @Test fun parsesFreezeFrameSupportedPidBitmap() {
        assertEquals(
            setOf(0x04, 0x05, 0x0C, 0x0D),
            ObdDiagnostics.parseSupportedFreezeFramePids("7E806420018180000")
        )
        assertNull(ObdDiagnostics.parseSupportedFreezeFramePids("NO DATA"))
        assertNull(ObdDiagnostics.parseSupportedFreezeFramePids("42000018"))
    }

    @Test fun rejectsMissingOrTruncatedFreezeFrameData() {
        assertNull(ObdDiagnostics.parseFreezeFrameBytes("NO DATA", 0x0C, 2))
        assertNull(ObdDiagnostics.parseFreezeFrameBytes("420C001A", 0x0C, 2))
        assertNull(ObdDiagnostics.parseFreezeFrameBytes("420C00", 0x0C, 2))
        assertNull(ObdDiagnostics.parseFreezeFrameBytes("420C001AF8", 0x0C, 0))
    }

    @Test fun returnsTurkishDescriptionOnlyForKnownDtcCodes() {
        assertEquals(
            "Rastgele veya birden fazla silindirde tekleme algılandı",
            ObdDiagnostics.description("p0300")
        )
        assertNull(ObdDiagnostics.description("P9999"))
    }

    @Test fun convertsRawPidByteToPercentage() {
        assertEquals(0, ObdDiagnostics.percent(0))
        assertEquals(50, ObdDiagnostics.percent(128))
        assertEquals(100, ObdDiagnostics.percent(255))
    }
}
