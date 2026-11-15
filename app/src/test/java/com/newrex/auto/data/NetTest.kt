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

    @Test fun zeroLengthReadFallsBackToSingleByteWithoutLosingData() {
        val source = "abc".toByteArray()
        val stream = object : java.io.InputStream() {
            private var first = true
            private var index = 0
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (first) {
                    first = false
                    return 0
                }
                if (index >= source.size) return -1
                val count = minOf(length, source.size - index)
                source.copyInto(buffer, offset, index, index + count)
                index += count
                return count
            }
            override fun read(): Int = if (index < source.size) source[index++].toInt() and 0xff else -1
        }
        assertEquals("abc", Net.readBounded(stream, 3))
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
