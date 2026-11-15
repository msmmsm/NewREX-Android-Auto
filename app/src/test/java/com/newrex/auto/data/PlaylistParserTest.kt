package com.newrex.auto.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistParserTest {
    @Test fun hlsMediaManifestIsNotParsedAsChannels() {
        val input = """
            #EXTM3U
            #EXT-X-TARGETDURATION:6
            #EXT-X-MEDIA-SEQUENCE:10
            segment001.ts
            segment002.ts
            #EXT-X-ENDLIST
        """.trimIndent()
        assertTrue(PlaylistParser.parse(input, "Test", "https://cdn.example/live/index.m3u8").isEmpty())
    }

    @Test fun hlsMasterManifestIsNotParsedAsChannels() {
        val input = """
            #EXTM3U
            #EXT-X-STREAM-INF:BANDWIDTH=1280000
            low/index.m3u8
        """.trimIndent()
        assertTrue(PlaylistParser.parse(input, "Test", "https://cdn.example/master.m3u8").isEmpty())
    }

    @Test fun resolvesRelativeChannelUrlAgainstPlaylistUrl() {
        val result = PlaylistParser.parse(
            "#EXTM3U\n#EXTINF:-1,Test Kanal\nstreams/test.m3u8",
            "Test",
            "https://example.com/lists/turkey/channels.m3u",
        )
        assertEquals("https://example.com/lists/turkey/streams/test.m3u8", result.single().url)
    }

    @Test fun preservesCommaInChannelTitleAndQuotedAttribute() {
        val result = PlaylistParser.parse(
            """#EXTM3U
#EXTINF:-1 tvg-name="Kanal, HD" group-title="News, World",Haber, Türkiye
https://example.com/live.m3u8""",
            "Test",
        )
        assertEquals("Haber, Türkiye", result.single().title)
    }

    @Test fun handlesBomCrLfAndAbsoluteUrls() {
        val result = PlaylistParser.parse(
            "\uFEFF#EXTM3U\r\n#EXTINF:-1,TRT 1\r\nhttps://cdn.example/trt.m3u8\r\n",
            "Türkiye",
            "https://example.com/list.m3u",
        )
        assertEquals("TRT 1", result.single().title)
        assertEquals("https://cdn.example/trt.m3u8", result.single().url)
    }

    @Test fun ignoresCommentLinesAndEntriesWithoutExtinf() {
        val result = PlaylistParser.parse(
            "#EXTM3U\n#EXTVLCOPT:http-user-agent=Test\nhttps://example.com/a.m3u8",
            "Test",
        )
        assertEquals("a.m3u8", result.single().title)
    }
}
