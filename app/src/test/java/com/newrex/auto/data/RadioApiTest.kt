package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioApiTest {
    @Test fun acceptsOnlyHttpAndHttpsStreamAddresses() {
        assertTrue(RadioApi.isSafeStreamUrl("https://radio.example/live.mp3"))
        assertTrue(RadioApi.isSafeStreamUrl("http://radio.example:8000/stream"))
        assertFalse(RadioApi.isSafeStreamUrl("javascript:alert(1)"))
        assertFalse(RadioApi.isSafeStreamUrl("file:///etc/passwd"))
        assertFalse(RadioApi.isSafeStreamUrl("https://user:pass@radio.example/live"))
        assertFalse(RadioApi.isSafeStreamUrl("https:///missing-host"))
        assertFalse(RadioApi.isSafeStreamUrl(""))
    }

    @Test fun parserUsesResolvedUrlAndRejectsUnsafeOrBrokenRows() {
        val json = """
            [
              {"name":"İstasyon A","url":"http://fallback.example/live","url_resolved":"https://radio.example/live","favicon":"javascript:bad","homepage":"https://radio.example","tags":"pop, turkish"},
              {"name":"Güvensiz","url":"file:///tmp/a","url_resolved":"javascript:alert(1)"},
              {"name":"Kimlik bilgili","url":"https://user:secret@radio.example/live"},
              {"name":"İstasyon A kopya","url":"https://radio.example/live"}
            ]
        """.trimIndent()
        val stations = RadioApi.parseStations(json)
        assertEquals(1, stations.size)
        assertEquals("İstasyon A", stations.single().title)
        assertEquals("https://radio.example/live", stations.single().url)
        assertEquals(null, stations.single().image)
        assertEquals("https://radio.example", stations.single().home)
        assertEquals("pop", stations.single().group)
    }

    @Test fun invalidResolvedAddressFallsBackToValidOriginalUrl() {
        val stations = RadioApi.parseStations(
            """[{"name":"Yedek adres","url":"http://radio.example/live","url_resolved":"javascript:bad"}]"""
        )
        assertEquals("http://radio.example/live", stations.single().url)
    }

    @Test fun parserPreservesTechnicalAndDirectoryMetadata() {
        val station = RadioApi.parseStations(
            """[{"name":"Teknik Radyo","url":"https://radio.example/live","codec":"MP3","bitrate":128,"countrycode":"tr","votes":42,"clickcount":1200,"lastcheckok":true}]"""
        ).single()
        assertEquals("MP3", station.codec)
        assertEquals(128, station.bitrateKbps)
        assertEquals("TR", station.countryCode)
        assertEquals(42, station.votes)
        assertEquals(1200, station.clickCount)
        assertEquals(true, station.directoryReportsHealthy)
    }

    @Test fun mediaEntryJsonRoundTripKeepsRadioMetadata() {
        val original = RadioApi.parseStations(
            """[{"name":"JSON Radyo","url":"https://radio.example/live","codec":"AAC","bitrate":96,"countrycode":"tr","votes":5,"clickcount":90,"lastcheckok":false}]"""
        ).single()
        val restored = MediaEntry.fromJson(original.toJson())
        assertEquals(original.codec, restored.codec)
        assertEquals(original.bitrateKbps, restored.bitrateKbps)
        assertEquals(original.countryCode, restored.countryCode)
        assertEquals(original.votes, restored.votes)
        assertEquals(original.clickCount, restored.clickCount)
        assertEquals(original.directoryReportsHealthy, restored.directoryReportsHealthy)
    }

    @Test fun countryCodesAreNormalizedAndInvalidValuesRejected() {
        assertEquals("TR", RadioApi.normalizeCountryCode(" tr "))
        assertEquals("US", RadioApi.normalizeCountryCode("us"))
        assertEquals(null, RadioApi.normalizeCountryCode("TUR"))
        assertEquals(null, RadioApi.normalizeCountryCode("tr&limit=999"))
        assertEquals(null, RadioApi.normalizeCountryCode(""))
    }

    @Test fun blankStationNamesReceiveReadableFallback() {
        val stations = RadioApi.parseStations(
            """[{"name":"  ","url":"https://radio.example/live"}]"""
        )
        assertEquals("İsimsiz radyo istasyonu", stations.single().title)
    }
}
