package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdManagerTest {
    @Test
    fun parsesStandardPidResponses() {
        assertEquals(26f, ObdManager.parseSpeed("410D1A")!!, 0.001f)
        assertEquals(1726, ObdManager.parseRpm("410C1AF8"))
        assertEquals(83, ObdManager.parseCoolant("41057B"))
        assertEquals(50.196f, ObdManager.parseFuel("412F80")!!, 0.01f)
        assertEquals(12.6f, ObdManager.parseVoltage("12.6V")!!, 0.001f)
    }

    @Test
    fun rejectsUnsupportedOrMalformedPidResponses() {
        assertNull(ObdManager.parseSpeed("NO DATA"))
        assertNull(ObdManager.parseRpm("410C1A"))
        assertNull(ObdManager.parseCoolant("ERROR"))
        assertNull(ObdManager.parseFuel("412FZZ"))
        assertNull(ObdManager.parseVoltage("NO DATA"))
    }

    @Test
    fun decodesDtcCategoriesAccordingToTheTwoCategoryBits() {
        assertEquals(listOf("P0133"), ObdManager.parseDtc("4301330000"))
        assertEquals(listOf("C1123"), ObdManager.parseDtc("4341230000"))
        assertEquals(listOf("B0123"), ObdManager.parseDtc("4381230000"))
        assertEquals(listOf("U0123"), ObdManager.parseDtc("43C1230000"))
    }

    @Test
    fun ignoresEmptyDtcSlotsAndNonDtcResponses() {
        assertTrue(ObdManager.parseDtc("430000").isEmpty())
        assertTrue(ObdManager.parseDtc("NO DATA").isEmpty())
    }
}
