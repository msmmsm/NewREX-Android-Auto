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
}
