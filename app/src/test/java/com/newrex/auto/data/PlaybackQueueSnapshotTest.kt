package com.newrex.auto.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaybackQueueSnapshotTest {
    private val radio = MediaEntry(Kind.RADIO, "Radyo", "https://radio.example/stream", null, "TR")
    private val podcast = MediaEntry(Kind.PODCAST, "Bölüm", "https://pod.example/episode.mp3", null, "Program")

    @Test fun roundTripsQueueAndPosition() {
        val saved = PlaybackQueueSnapshot.create(listOf(radio, podcast), 1, 42_000)!!
        val restored = PlaybackQueueSnapshot.fromJson(saved.toJson())
        assertEquals(listOf(radio, podcast), restored.entries)
        assertEquals(1, restored.index)
        assertEquals(42_000L, restored.positionMs)
    }

    @Test fun rejectsEmptyAndInvalidIndexQueues() {
        assertNull(PlaybackQueueSnapshot.create(emptyList(), 0, 0))
        assertNull(PlaybackQueueSnapshot.create(listOf(radio), 1, 0))
    }

    @Test fun boundsLargeQueuesAroundTheSelectedItem() {
        val entries = List(120) { radio.copy(title = "Radyo $it", url = "https://radio.example/$it") }
        val saved = PlaybackQueueSnapshot.create(entries, 90, 0)!!
        assertEquals(PlaybackQueueSnapshot.MAX_ENTRIES, saved.entries.size)
        assertEquals("Radyo 90", saved.entries[saved.index].title)
    }

    @Test fun rejectsUrlsContainingEmbeddedCredentials() {
        assertNull(PlaybackQueueSnapshot.create(
            listOf(radio.copy(url = "https://user:password@radio.example/stream")), 0, 0
        ))
        assertNull(PlaybackQueueSnapshot.create(
            listOf(radio.copy(url = "https://radio.example/stream?token=secret")), 0, 0
        ))
    }

    @Test fun rejectsNonHttpSchemes() {
        val invalid = radio.copy(url = "file:///private/stream.mp3")
        assertNull(PlaybackQueueSnapshot.create(listOf(invalid), 0, 0))
    }

    @Test fun rejectsCorruptVersionAndInvalidIndex() {
        assertThrows(IllegalArgumentException::class.java) {
            PlaybackQueueSnapshot.fromJson(JSONObject().put("version", 99))
        }
        val saved = PlaybackQueueSnapshot.create(listOf(radio), 0, 0)!!.toJson()
        saved.put("index", 4)
        assertThrows(IllegalArgumentException::class.java) {
            PlaybackQueueSnapshot.fromJson(saved)
        }
    }

    @Test fun clampsUnreasonablyLongPosition() {
        val saved = PlaybackQueueSnapshot.create(
            listOf(radio), 0, PlaybackQueueSnapshot.MAX_POSITION_MS + 10_000
        )!!
        assertEquals(PlaybackQueueSnapshot.MAX_POSITION_MS, saved.positionMs)
    }
}
