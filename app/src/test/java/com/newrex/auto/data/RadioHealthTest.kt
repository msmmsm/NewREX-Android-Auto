// NewREX Android Auto — Copyright (C) 2026 NewREX — GPL-3.0-or-later (bkz. NOTICE.md)
package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

class RadioHealthTest {
    @Test fun classifiesCommonHttpFailures() {
        assertEquals(RadioHealth.State.REDIRECT, RadioHealth.classifyStatus(302, null).first)
        assertEquals(RadioHealth.State.AUTH_REQUIRED, RadioHealth.classifyStatus(403, null).first)
        assertEquals(RadioHealth.State.NOT_FOUND, RadioHealth.classifyStatus(404, null).first)
        assertEquals(RadioHealth.State.RATE_LIMITED, RadioHealth.classifyStatus(429, null).first)
        assertEquals(RadioHealth.State.SERVER_ERROR, RadioHealth.classifyStatus(503, null).first)
    }

    @Test fun rejectsHtmlPagesMasqueradingAsRadioStreams() {
        assertEquals(
            RadioHealth.State.INVALID_CONTENT,
            RadioHealth.classifyStatus(200, "text/html").first
        )
        assertEquals(
            RadioHealth.State.HEALTHY,
            RadioHealth.classifyStatus(206, "audio/mpeg").first
        )
    }

    @Test fun refusesPrivateAndReservedNetworkAddresses() {
        assertEquals(false, RadioHealth.isPublicAddress(InetAddress.getByName("127.0.0.1")))
        assertEquals(false, RadioHealth.isPublicAddress(InetAddress.getByName("10.0.0.8")))
        assertEquals(false, RadioHealth.isPublicAddress(InetAddress.getByName("192.168.1.1")))
        assertEquals(false, RadioHealth.isPublicAddress(InetAddress.getByName("100.64.0.1")))
        assertEquals(false, RadioHealth.isPublicAddress(InetAddress.getByName("fc00::1")))
        assertEquals(true, RadioHealth.isPublicAddress(InetAddress.getByName("8.8.8.8")))
    }

    @Test fun distinguishesSlowAndFastFirstByteLatency() {
        assertEquals(RadioHealth.State.HEALTHY, RadioHealth.latencyState(1999))
        assertEquals(RadioHealth.State.SLOW, RadioHealth.latencyState(2000))
        assertEquals(RadioHealth.State.SLOW, RadioHealth.latencyState(4500))
    }
}
