package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveReconnectPolicyTest {
    @Test fun initialPlaybackGetsOnlyTwoRetries() {
        assertTrue(LiveReconnectPolicy.shouldRetry(0, false))
        assertTrue(LiveReconnectPolicy.shouldRetry(1, false))
        assertFalse(LiveReconnectPolicy.shouldRetry(2, false))
    }

    @Test fun establishedPlaybackGetsTenRetries() {
        assertTrue(LiveReconnectPolicy.shouldRetry(9, true))
        assertFalse(LiveReconnectPolicy.shouldRetry(10, true))
    }

    @Test fun delayUsesBoundedExponentialBackoff() {
        assertEquals(1_000L, LiveReconnectPolicy.delayMs(0))
        assertEquals(2_000L, LiveReconnectPolicy.delayMs(1))
        assertEquals(4_000L, LiveReconnectPolicy.delayMs(2))
        assertEquals(8_000L, LiveReconnectPolicy.delayMs(3))
        assertEquals(15_000L, LiveReconnectPolicy.delayMs(4))
        assertEquals(15_000L, LiveReconnectPolicy.delayMs(20))
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeAttemptIsRejected() {
        LiveReconnectPolicy.delayMs(-1)
    }
}
