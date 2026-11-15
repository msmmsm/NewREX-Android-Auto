package com.newrex.auto.data

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class NetTest {
    @Test fun acceptsOnlySafeHttpRedirectTargets() {
        assertEquals(true, Net.isSafeHttpUrl("https://pod.example.org/feed.xml"))
        assertEquals(false, Net.isSafeHttpUrl("file:///tmp/feed.xml"))
        assertEquals(false, Net.isSafeHttpUrl("https://user:pass@pod.example.org/feed.xml"))
        assertEquals(false, Net.isSafeHttpUrl("https://pod.example.org/" + "x".repeat(4100)))
    }

    @Test fun acceptsResponseExactlyAtByteLimit() {
        val result = Net.readBounded(ByteArrayInputStream("12345".toByteArray()), 5)
        assertEquals("12345", result)
    }

    @Test fun rejectsResponseThatExceedsByteLimit() {
        try {
            Net.readBounded(ByteArrayInputStream("123456".toByteArray()), 5)
            fail("Boyut sınırını aşan yanıt reddedilmeliydi")
        } catch (_: IOException) {
            // Beklenen: akış okunurken sınır aşıldı.
        }
    }

    @Test fun rejectsOversizedDeclaredContentLengthBeforeReading() {
        try {
            Net.readBounded(ByteArrayInputStream(ByteArray(0)), 5, 6)
            fail("Beyan edilen büyük yanıt reddedilmeliydi")
        } catch (_: IOException) {
            // Beklenen: gövde okunmadan reddedildi.
        }
    }
}
