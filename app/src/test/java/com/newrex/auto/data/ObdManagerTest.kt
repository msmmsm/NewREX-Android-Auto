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
    fun parsesFirstEcuPayloadWhenElmReturnsMultipleEcuResponses() {
        // ELM327 may concatenate CAN responses from more than one ECU.
        assertEquals(26f, ObdManager.parseSpeed("7E804410D1A7E904410D2A")!!, 0.001f)
        assertEquals(83, ObdManager.parseCoolant("7E80341057B7E90341057C"))
        assertEquals(50.196f, ObdManager.parseFuel("7E803412F807E903412F90")!!, 0.01f)
        assertEquals(1726, ObdManager.parseRpm("7E804410C1AF87E904410C2000"))
    }

    @Test
    fun rejectsUnsupportedOrMalformedPidResponses() {
        assertNull(ObdManager.parseSpeed("NO DATA"))
        assertNull(ObdManager.parseSpeed("410D1"))
        assertNull(ObdManager.parseCoolant("4105"))
        assertNull(ObdManager.parseFuel("412F0"))
        assertNull(ObdManager.parseEngineLoad("4104"))
        assertNull(ObdManager.parseRpm("410C1A"))
        assertNull(ObdManager.parseCoolant("ERROR"))
        assertNull(ObdManager.parseFuel("412FZZ"))
        assertNull(ObdManager.parseVoltage("NO DATA"))
    }

    @Test
    fun decodesDtcCategoriesAccordingToTheTwoCategoryBits() {
        assertEquals(listOf("P0133"), ObdManager.parseDtc("4301330000"))
        assertEquals(listOf("C1123"), ObdManager.parseDtc("4351230000"))
        assertEquals(listOf("B0123"), ObdManager.parseDtc("4381230000"))
        assertEquals(listOf("U0123"), ObdManager.parseDtc("43C1230000"))
    }

    @Test
    fun discoversSupportedPidsFromMode01Bitmaps() {
        assertEquals(setOf(0x0C, 0x0D, 0x20), ObdManager.parseSupportedPids("410000180001", 0x00))
        assertEquals(setOf(0x2F, 0x40), ObdManager.parseSupportedPids("412000020001", 0x20))
        assertNull(ObdManager.parseSupportedPids("NO DATA", 0x00))
    }

    @Test
    fun parsesDtcResponsesFromMultipleCanEcusWithoutMixingHeadersIntoCodes() {
        assertEquals(
            listOf("P0133", "P0456"),
            ObdManager.parseDtc("7E8044301337E904430456")
        )
        assertEquals(
            listOf("P0133", "P0456"),
            ObdManager.parseDtc("7e8 04 43 01 33\r\n7e9 04 43 04 56")
        )
    }

    @Test
    fun ignoresIncompleteDtcBytesAndDeduplicatesRepeatedEcuCodes() {
        assertEquals(listOf("P0133"), ObdManager.parseDtc("4301330"))
        assertEquals(listOf("P0133"), ObdManager.parseDtc("7E8044301337E904430133"))
    }

    @Test
    fun parsesPidBitmapWithCanHeaderAndLineBreaks() {
        assertEquals(
            setOf(0x0C, 0x0D, 0x20),
            ObdManager.parseSupportedPids("7E806410000180001\r\n>", 0x00)
        )
    }

    @Test
    fun parsesEngineLoadPercentage() {
        assertEquals(50, ObdManager.parseEngineLoad("410480"))
        assertNull(ObdManager.parseEngineLoad("NO DATA"))
    }

    @Test
    fun acceptsOnlyPositiveMode04Acknowledgement() {
        assertTrue(ObdManager.hasDtcClearAcknowledgement("44"))
        assertTrue(ObdManager.hasDtcClearAcknowledgement("7E80144"))
        assertTrue(ObdManager.hasDtcClearAcknowledgement("7E8 01 44"))
        assertTrue(ObdManager.hasDtcClearAcknowledgement("7E8 01 44\\r\\n"))
        assertTrue(!ObdManager.hasDtcClearAcknowledgement("NO DATA"))
        assertTrue(!ObdManager.hasDtcClearAcknowledgement("ERROR"))
        assertTrue(!ObdManager.hasDtcClearAcknowledgement("04"))
        assertTrue(!ObdManager.hasDtcClearAcknowledgement("7E803410044"))
    }

    @Test
    fun ignoresEmptyDtcSlotsAndNonDtcResponses() {
        assertTrue(ObdManager.parseDtc("430000").isEmpty())
        assertTrue(ObdManager.parseDtc("NO DATA").isEmpty())
    }
}
