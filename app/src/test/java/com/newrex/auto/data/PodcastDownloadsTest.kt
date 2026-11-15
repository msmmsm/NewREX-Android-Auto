package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastDownloadsTest {
    @Test fun downloadKeyIsStableAndDoesNotExposeTheUrl() {
        val url = "https://podcast.example/episode.mp3?access_token=secret"
        val key = PodcastDownloads.keyForUrl(url)
        assertEquals(64, key.length)
        assertTrue(key.matches(Regex("[a-f0-9]{64}")))
        assertEquals(key, PodcastDownloads.keyForUrl(url))
        assertFalse(key.contains("secret"))
        assertFalse(key.contains("podcast"))
    }

    @Test fun generatedFileNameCannotEscapePodcastDirectory() {
        val name = PodcastDownloads.fileNameFor(
            "https://podcast.example/audio.m4a?token=secret",
            "../../Türkiye: Günlük / Haber?"
        )
        assertFalse(name.contains("/"))
        assertFalse(name.contains("\\"))
        assertTrue(name.matches(Regex("[A-Za-z0-9._-]+")))
        assertTrue(name.endsWith(".m4a"))
    }

    @Test fun acceptsCommonPodcastMediaMimeTypes() {
        assertTrue(PodcastDownloads.isAllowedDownloadedMime("audio/mpeg"))
        assertTrue(PodcastDownloads.isAllowedDownloadedMime("audio/mp4"))
        assertTrue(PodcastDownloads.isAllowedDownloadedMime("application/ogg"))
        assertTrue(PodcastDownloads.isAllowedDownloadedMime("application/octet-stream"))
        assertTrue(PodcastDownloads.isAllowedDownloadedMime("video/mp4"))
    }

    @Test fun rejectsNonMediaMimeTypesAndMissingMime() {
        assertFalse(PodcastDownloads.isAllowedDownloadedMime("text/html"))
        assertFalse(PodcastDownloads.isAllowedDownloadedMime("application/json"))
        assertFalse(PodcastDownloads.isAllowedDownloadedMime(null))
    }

    @Test fun enforcesPositivePerEpisodeSizeLimit() {
        assertTrue(PodcastDownloads.isAllowedDownloadedSize(1))
        assertTrue(PodcastDownloads.isAllowedDownloadedSize(PodcastDownloads.MAX_EPISODE_BYTES))
        assertFalse(PodcastDownloads.isAllowedDownloadedSize(0))
        assertFalse(PodcastDownloads.isAllowedDownloadedSize(-1))
        assertFalse(PodcastDownloads.isAllowedDownloadedSize(PodcastDownloads.MAX_EPISODE_BYTES + 1))
    }

    @Test fun unknownExtensionUsesSafeAudioSuffix() {
        val name = PodcastDownloads.fileNameFor("https://podcast.example/audio", "Bölüm")
        assertTrue(name.endsWith(".audio"))
    }
}
