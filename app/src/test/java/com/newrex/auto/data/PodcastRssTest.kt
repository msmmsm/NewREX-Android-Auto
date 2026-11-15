package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PodcastRssTest {
    @Test fun resolvesRelativeEnclosureAgainstFeedUrl() {
        assertEquals(
            "https://pod.example.org/audio/episode.mp3",
            PodcastRss.resolveEnclosureUrl("https://pod.example.org/rss/feed.xml", "../audio/episode.mp3"),
        )
    }

    @Test fun preservesAbsoluteEnclosureUrl() {
        assertEquals(
            "https://cdn.example.org/episode.mp3",
            PodcastRss.resolveEnclosureUrl("https://pod.example.org/rss.xml", "https://cdn.example.org/episode.mp3"),
        )
    }

    @Test fun rejectsEmptyOrUnresolvableEnclosureUrl() {
        assertNull(PodcastRss.resolveEnclosureUrl("https://pod.example.org/rss.xml", ""))
        assertNull(PodcastRss.resolveEnclosureUrl("not a URL", "episode.mp3"))
    }

    @Test fun parsesCommonRfc822DateWithAndWithoutSeconds() {
        assertEquals("2 Ekim 2002", PodcastRss.formatDate("Wed, 02 Oct 2002 13:00:00 GMT"))
        assertEquals("2 Ekim 2002", PodcastRss.formatDate("Wed, 02 Oct 2002 13:00 GMT"))
    }

    @Test fun invalidDateReturnsEmptyString() {
        assertEquals("", PodcastRss.formatDate("not-a-date"))
    }
    @Test fun rejectsUnsafeEnclosureSchemesAndCredentials() {
        assertNull(PodcastRss.resolveEnclosureUrl("https://pod.example.org/rss.xml", "file:///tmp/episode.mp3"))
        assertNull(PodcastRss.resolveEnclosureUrl("https://pod.example.org/rss.xml", "ftp://cdn.example.org/episode.mp3"))
        assertNull(PodcastRss.resolveEnclosureUrl("https://pod.example.org/rss.xml", "https://user:pass@cdn.example.org/episode.mp3"))
    }

    @Test fun resolvesRelativeEnclosureOnlyFromValidHttpFeed() {
        assertNull(PodcastRss.resolveEnclosureUrl("content://provider/feed.xml", "episode.mp3"))
        assertNull(PodcastRss.resolveEnclosureUrl("https://user:pass@pod.example.org/feed.xml", "episode.mp3"))
    }

    @Test fun validatesFeedUrlsBeforeNetworkAccess() {
        assertEquals(true, PodcastRss.isSafeFeedUrl("https://pod.example.org/feed.xml"))
        assertEquals(true, PodcastRss.isSafeFeedUrl("http://192.168.1.20:8080/feed.xml"))
        assertEquals(false, PodcastRss.isSafeFeedUrl("file:///tmp/feed.xml"))
        assertEquals(false, PodcastRss.isSafeFeedUrl("https://user:pass@pod.example.org/feed.xml"))
        assertEquals(false, PodcastRss.isSafeFeedUrl("https://pod.example.org/" + "a".repeat(4100)))
    }

    @Test fun rejectsOversizedEnclosureAddress() {
        assertNull(PodcastRss.resolveEnclosureUrl("https://pod.example.org/rss.xml", "https://cdn.example.org/" + "a".repeat(4100)))
    }
}
