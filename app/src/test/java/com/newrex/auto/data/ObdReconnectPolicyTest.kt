package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObdReconnectPolicyTest {
    @Test fun allowsThreeRetriesThenStops() {
        assertTrue(ObdReconnectPolicy.shouldRetry(0))
        assertTrue(ObdReconnectPolicy.shouldRetry(1))
        assertTrue(ObdReconnectPolicy.shouldRetry(2))
        assertFalse(ObdReconnectPolicy.shouldRetry(3))
    }

    @Test fun usesBoundedBackoff() {
        assertEquals(1_000L, ObdReconnectPolicy.delayMs(0))
        assertEquals(3_000L, ObdReconnectPolicy.delayMs(1))
        assertEquals(7_000L, ObdReconnectPolicy.delayMs(2))
        assertEquals(7_000L, ObdReconnectPolicy.delayMs(10))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeRetryIndex() {
        ObdReconnectPolicy.delayMs(-1)
    }
}
