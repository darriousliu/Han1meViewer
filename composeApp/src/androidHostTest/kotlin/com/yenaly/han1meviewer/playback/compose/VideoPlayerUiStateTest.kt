package com.yenaly.han1meviewer.playback.compose

import com.yenaly.han1meviewer.playback.model.PlaybackCapabilities
import com.yenaly.han1meviewer.playback.model.PlaybackDefaults
import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackSource
import com.yenaly.han1meviewer.playback.model.PlaybackState
import com.yenaly.han1meviewer.playback.model.QualityVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class VideoPlayerUiStateTest {
    @Test
    fun `progress and buffered fractions use absolute playback positions`() {
        val state = uiState(
            PlaybackState(
                source = source,
                phase = PlaybackPhase.Ready,
                positionMs = 250L,
                durationMs = 1_000L,
                bufferedPositionMs = 600L,
                bufferedPercentage = 99,
            ),
        )

        assertEquals(0.25f, state.progressFraction, 0f)
        assertEquals(0.6f, state.bufferedFraction, 0f)
        assertTrue(state.canSeek)
    }

    @Test
    fun `buffered percentage is fallback when duration is unknown`() {
        val state = uiState(
            PlaybackState(
                source = source,
                phase = PlaybackPhase.Buffering,
                bufferedPercentage = 42,
            ),
        )

        assertEquals(0.42f, state.bufferedFraction, 0f)
        assertTrue(state.isLoading)
        assertFalse(state.canSeek)
    }

    @Test
    fun `poster visibility remains an explicit route decision`() {
        val readyAtStart = PlaybackState(
            source = source,
            phase = PlaybackPhase.Ready,
            playWhenReady = false,
            positionMs = 0L,
        )

        assertTrue(uiState(readyAtStart, showPoster = true).showPoster)
        assertFalse(uiState(readyAtStart, showPoster = false).showPoster)
    }

    @Test
    fun `quality and speed choices come from source and playback defaults`() {
        val state = uiState(
            PlaybackState(
                source = source,
                selectedQualityId = "1080P",
            ),
        )

        assertEquals("1080P", state.selectedQuality?.id)
        assertEquals(
            "720P",
            uiState(PlaybackState(source = source)).selectedQuality?.id,
        )
        assertEquals(PlaybackDefaults.SPEED_OPTIONS, state.availableSpeeds)
        assertEquals(0.5f, state.availableSpeeds.first(), 0f)
        assertEquals(3f, state.availableSpeeds.last(), 0f)
    }

    @Test
    fun `seek fraction conversion clamps to the playable range`() {
        assertEquals(0L, positionFromFraction(-1f, 10_000L))
        assertEquals(2_500L, positionFromFraction(0.25f, 10_000L))
        assertEquals(10_000L, positionFromFraction(2f, 10_000L))
        assertEquals(0L, positionFromFraction(0.5f, 0L))
    }

    @Test
    fun `keyframe countdown rejects negative values`() {
        assertThrows(IllegalArgumentException::class.java) {
            VideoKeyframeCountdownUiState(remainingMs = -1L)
        }
        assertEquals("3", formatKeyframeCountdown(2_000L, Locale.ROOT))
        assertEquals("0.9", formatKeyframeCountdown(900L, Locale.ROOT))
    }

    private fun uiState(
        playback: PlaybackState,
        showPoster: Boolean = false,
    ) = VideoPlayerUiState(
        playback = playback,
        capabilities = PlaybackCapabilities(),
        longPressSpeedMultiplier = PlaybackDefaults.DEFAULT_LONG_PRESS_SPEED_MULTIPLIER,
        seekGestureSensitivity = PlaybackDefaults.DEFAULT_PROGRESS_SLIDE_SENSITIVITY,
        showPoster = showPoster,
    )

    private companion object {
        val source = PlaybackSource(
            id = "video",
            title = "Video",
            posterUri = "https://example.com/poster.jpg",
            preferredQualityId = "720P",
            qualities = listOf(
                QualityVariant("720P", uri = "https://example.com/720.mp4"),
                QualityVariant("1080P", uri = "https://example.com/1080.mp4"),
            ),
        )
    }
}
