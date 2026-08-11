package com.yenaly.han1meviewer.playback.media3

import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class Media3PlaybackReducerTest {
    @Test
    fun `quality switch preserves playback continuity and clears old buffering`() {
        val state = PlaybackState(
            phase = PlaybackPhase.Ready,
            isPlaying = true,
            playWhenReady = true,
            positionMs = 10_000L,
            bufferedPositionMs = 50_000L,
            bufferedPercentage = 80,
            speed = 1f,
            selectedQualityId = "1080P",
            errorMessage = "old error",
        )

        val switched = state.prepareMedia3QualitySwitch(
            qualityId = "720P",
            snapshot = Media3ContinuitySnapshot(
                positionMs = 42_500L,
                playWhenReady = false,
                speed = 1.75f,
            ),
        )

        assertEquals(PlaybackPhase.Preparing, switched.phase)
        assertEquals(42_500L, switched.positionMs)
        assertFalse(switched.playWhenReady)
        assertFalse(switched.isPlaying)
        assertEquals(1.75f, switched.speed)
        assertEquals("720P", switched.selectedQualityId)
        assertEquals(0L, switched.bufferedPositionMs)
        assertEquals(0, switched.bufferedPercentage)
        assertNull(switched.errorMessage)
    }

    @Test
    fun `ended and error events converge to non-playing terminal states`() {
        val playing = PlaybackState(
            phase = PlaybackPhase.Ready,
            isPlaying = true,
            playWhenReady = true,
        )

        val ended = playing.reduceMedia3Phase(
            phase = PlaybackPhase.Ended,
            isPlaying = true,
            playWhenReady = true,
        )
        val failed = playing.reduceMedia3Error("network failed")

        assertEquals(PlaybackPhase.Ended, ended.phase)
        assertFalse(ended.isPlaying)
        assertFalse(ended.playWhenReady)
        assertEquals(PlaybackPhase.Error, failed.phase)
        assertFalse(failed.isPlaying)
        assertFalse(failed.playWhenReady)
        assertEquals("network failed", failed.errorMessage)
    }
}
