// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioRecoveryTest {
    private fun station(
        title: String,
        country: String? = "TR",
        healthy: Boolean? = null,
        votes: Int? = null,
        codec: String? = null,
        bitrate: Int? = null,
    ) = MediaEntry(
        kind = Kind.RADIO, title = title, url = "https://radio.example/$title",
        image = null, group = "", countryCode = country, directoryReportsHealthy = healthy,
        votes = votes, codec = codec, bitrateKbps = bitrate,
    )

    @Test fun cooldownUsesBoundedEscalatingIntervals() {
        assertEquals(300_000L, RadioRecovery.cooldownMs(1))
        assertEquals(900_000L, RadioRecovery.cooldownMs(2))
        assertEquals(3_600_000L, RadioRecovery.cooldownMs(3))
        assertEquals(3_600_000L, RadioRecovery.cooldownMs(20))
    }

    @Test fun exactStationMatchOutranksSimilarName() {
        val original = station("Joy Turk")
        val exactButWeak = station("Joy Turk", healthy = false)
        val similarAndHealthy = station("Joy Turk FM", healthy = true, votes = 100)
        assertTrue(RadioRecovery.rank(original, exactButWeak) > RadioRecovery.rank(original, similarAndHealthy))
    }

    @Test fun healthMetadataImprovesAlternativeRank() {
        val original = station("Power FM")
        val weak = station("Power FM", healthy = false)
        val strong = station("Power FM", healthy = true, votes = 100, codec = "MP3", bitrate = 128)
        assertTrue(RadioRecovery.rank(original, strong) > RadioRecovery.rank(original, weak))
    }
}
