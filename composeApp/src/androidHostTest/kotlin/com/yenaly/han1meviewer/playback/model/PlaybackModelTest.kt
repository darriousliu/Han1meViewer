package com.yenaly.han1meviewer.playback.model

import com.yenaly.han1meviewer.HanimeLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaybackModelTest {
    @Test
    fun `legacy engine values map to supported engines`() {
        assertEquals(PlaybackEngineType.Media3, PlaybackEngineType.fromString("ExoPlayer"))
        assertEquals(PlaybackEngineType.Media3, PlaybackEngineType.fromString("MediaPlayer"))
        assertEquals(PlaybackEngineType.Media3, PlaybackEngineType.fromString("SystemMediaPlayer"))
        assertEquals(PlaybackEngineType.Mpv, PlaybackEngineType.fromString("MpvPlayer"))
        assertEquals(PlaybackEngineType.Mpv, PlaybackEngineType.fromString("MPV"))
        assertEquals("Media3", PlaybackEngineType.Media3.persistedValue)
        assertEquals("Mpv", PlaybackEngineType.Mpv.persistedValue)
        assertEquals(PlaybackEngineType.Media3, PlaybackEngineType.fromString(null))
    }

    @Test
    fun `source resolves requested then preferred then first quality`() {
        val source = source(preferredQualityId = "720P")

        assertEquals("1080P", source.resolveQuality("1080P").id)
        assertEquals("720P", source.resolveQuality().id)
        assertEquals("1080P", source.copy(preferredQualityId = "missing").resolveQuality().id)
    }

    @Test
    fun `quality headers override source headers`() {
        val quality = QualityVariant(
            id = "1080P",
            uri = "https://example.com/video.mp4",
            headers = mapOf("Referer" to "quality", "X-Quality" to "1080P"),
        )
        val source = PlaybackSource(
            id = "video",
            title = "Video",
            qualities = listOf(quality),
            headers = mapOf("Referer" to "source", "User-Agent" to "test"),
        )

        assertEquals(
            mapOf("Referer" to "quality", "User-Agent" to "test", "X-Quality" to "1080P"),
            source.headersFor(quality),
        )
        assertEquals(true, quality.isRemote)
        assertEquals(false, quality.isLocal)
        assertEquals(
            true,
            QualityVariant("local", uri = "content://media/video/1").isLocal,
        )
    }

    @Test
    fun `source rejects duplicate quality ids`() {
        val duplicate = listOf(
            QualityVariant("same", uri = "https://example.com/one.mp4"),
            QualityVariant("same", uri = "https://example.com/two.mp4"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            PlaybackSource(id = "video", title = "Video", qualities = duplicate)
        }
    }

    @Test
    fun `resolution map converts to player source without preferences`() {
        val resolutions = LinkedHashMap<String, HanimeLink>().apply {
            put("1080P", HanimeLink("https://example.com/video.mp4", "mp4"))
            put("720P", HanimeLink("https://example.com/video.m3u8", null))
        }

        val source = resolutions.toPlaybackSource(
            id = "123",
            title = "Video",
            coverUrl = "https://example.com/poster.jpg",
            preferredQualityId = "720P",
            headers = mapOf("Referer" to "https://example.com"),
        )

        assertEquals("123", source.id)
        assertEquals("https://example.com/poster.jpg", source.posterUri)
        assertEquals("video/mp4", source.qualities[0].mimeType)
        assertEquals(null, source.qualities[1].mimeType)
        assertEquals("720P", source.resolveQuality().id)
    }

    @Test
    fun `playback time formatter supports minutes hours and negative input`() {
        assertEquals("00:00", formatPlaybackTime(-1L))
        assertEquals("01:05", formatPlaybackTime(65_999L))
        assertEquals("01:01:01", formatPlaybackTime(3_661_000L))
    }

    @Test
    fun `legacy slide sensitivity keeps nonlinear high sensitivity mapping`() {
        assertEquals(1, PlaybackDefaults.progressSlideDivisor(1))
        assertEquals(5, PlaybackDefaults.progressSlideDivisor(5))
        assertEquals(7, PlaybackDefaults.progressSlideDivisor(6))
        assertEquals(10, PlaybackDefaults.progressSlideDivisor(7))
        assertEquals(20, PlaybackDefaults.progressSlideDivisor(8))
        assertEquals(40, PlaybackDefaults.progressSlideDivisor(9))
    }

    private fun source(preferredQualityId: String?) = PlaybackSource(
        id = "video",
        title = "Video",
        preferredQualityId = preferredQualityId,
        qualities = listOf(
            QualityVariant("1080P", uri = "https://example.com/1080.mp4"),
            QualityVariant("720P", uri = "https://example.com/720.mp4"),
        ),
    )
}
