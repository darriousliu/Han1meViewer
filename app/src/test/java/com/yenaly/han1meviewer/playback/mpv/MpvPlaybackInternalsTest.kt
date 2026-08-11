package com.yenaly.han1meviewer.playback.mpv

import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MpvPlaybackInternalsTest {
    @Test
    fun `native pause and playback active properties reduce ready and buffering state`() {
        var runtime = MpvRuntimeState()
        var state = PlaybackState(phase = PlaybackPhase.Ready)

        runtime = runtime.withBooleanProperty("pause", false)
        state = state.reduceMpvRuntime(runtime)
        assertEquals(PlaybackPhase.Buffering, state.phase)
        assertFalse(state.isPlaying)
        assertTrue(state.playWhenReady)

        runtime = runtime.withBooleanProperty("playback-active", true)
        state = state.reduceMpvRuntime(runtime)
        assertEquals(PlaybackPhase.Ready, state.phase)
        assertTrue(state.isPlaying)

        runtime = runtime.withBooleanProperty("pause", true)
        state = state.reduceMpvRuntime(runtime)
        assertFalse(state.isPlaying)
        assertFalse(state.playWhenReady)
    }

    @Test
    fun `MPV terminal reducers preserve duration and expose failures`() {
        val playing = PlaybackState(
            phase = PlaybackPhase.Ready,
            isPlaying = true,
            playWhenReady = true,
            positionMs = 1_000L,
            durationMs = 8_000L,
        )

        val ended = playing.reduceMpvEnded()
        val failed = playing.reduceMpvError("load timed out")

        assertEquals(PlaybackPhase.Ended, ended.phase)
        assertEquals(8_000L, ended.positionMs)
        assertFalse(ended.isPlaying)
        assertEquals(PlaybackPhase.Error, failed.phase)
        assertFalse(failed.playWhenReady)
        assertEquals("load timed out", failed.errorMessage)
    }

    @Test
    fun `late native event cannot complete replacement load generation`() {
        val session = MpvLoadSession()
        session.begin("https://example.com/old.m3u8")
        session.onNativeStart("https://example.com/old.m3u8")
        val lateLoaded = requireNotNull(
            session.snapshotNativeEvent("https://example.com/old.m3u8")
        )
        session.begin("https://example.com/new.m3u8")

        assertFalse(session.complete(lateLoaded))
        assertTrue(session.isPending)
        session.onNativeEnd("https://example.com/old.m3u8")
        val replacementStart = requireNotNull(
            session.onNativeStart("https://example.com/new.m3u8")
        )
        assertTrue(session.isPendingEvent(replacementStart))
        assertFalse(
            session.complete(
                replacementStart.copy(actualPath = "https://example.com/wrong.m3u8")
            )
        )
        val replacementLoaded = requireNotNull(
            session.snapshotNativeEvent("https://example.com/new.m3u8")
        )
        assertTrue(session.complete(replacementLoaded))
        assertFalse(session.isPending)
        assertTrue(session.isLoadedEvent(replacementLoaded))
        val replacementEnd = requireNotNull(
            session.onNativeEnd("https://example.com/new.m3u8")
        )
        assertTrue(session.consumeLoadedEnd(replacementEnd))
        assertFalse(session.consumeLoadedEnd(replacementEnd))

        val failedSession = MpvLoadSession()
        failedSession.begin("https://example.com/fail.m3u8")
        val failedStart = requireNotNull(
            failedSession.onNativeStart("https://example.com/fail.m3u8")
        )
        assertTrue(failedSession.consumePendingEnd(failedStart))
        assertFalse(failedSession.isPending)

        val timeoutSession = MpvLoadSession()
        val timeoutTicket = timeoutSession.begin("fd://42")
        assertTrue(timeoutSession.expire(timeoutTicket))
        val afterTimeout = timeoutSession.begin("fd://84")
        val afterTimeoutStart = requireNotNull(timeoutSession.onNativeStart("fd://84"))
        assertEquals(afterTimeout.generation, afterTimeoutStart.generation)
        assertEquals(20_000L, mpvLoadTimeoutMs(10))
        assertEquals(40_000L, mpvLoadTimeoutMs(30))
        assertEquals(1_250L, mpvSecondsToMillis(1.25))
        assertEquals(0L, mpvSecondsToMillis(Double.NaN))
        assertEquals(8_000L, mpvBufferedPositionMs(7_000L, 5_000L, 8_000L))

        val cancelledSession = MpvLoadSession()
        cancelledSession.begin("https://example.com/same.m3u8")
        cancelledSession.cancel()
        val fresh = cancelledSession.begin("https://example.com/same.m3u8")
        val freshStart = requireNotNull(
            cancelledSession.onNativeStart("https://example.com/same.m3u8")
        )
        assertEquals(fresh.generation, freshStart.generation)
    }

    @Test
    fun `same path retry is still isolated by native generation`() {
        val session = MpvLoadSession()
        session.begin("https://example.com/video.m3u8")
        session.onNativeStart("https://example.com/video.m3u8")
        val oldLoaded = requireNotNull(
            session.snapshotNativeEvent("https://example.com/video.m3u8")
        )

        session.begin("https://example.com/video.m3u8")
        assertFalse(session.complete(oldLoaded))
        session.onNativeEnd("https://example.com/video.m3u8")
        session.onNativeStart("https://example.com/video.m3u8")
        val retryLoaded = requireNotNull(
            session.snapshotNativeEvent("https://example.com/video.m3u8")
        )

        assertTrue(session.complete(retryLoaded))
        assertTrue(retryLoaded.generation > oldLoaded.generation)
    }

    @Test
    fun `pending continuity keeps latest seek and play pause intent`() {
        var pending = MpvPendingPlayback(positionMs = 10_000L, playWhenReady = true)

        pending = pending.seekTo(42_500L)
        pending = pending.copy(playWhenReady = false)

        assertEquals(42_500L, pending.positionMs)
        assertFalse(pending.playWhenReady)
        assertEquals(0L, pending.seekTo(-1L).positionMs)
    }

    @Test
    fun `progress publish gate coalesces native updates until consumed or cancelled`() {
        val gate = MpvProgressPublishGate()

        assertEquals(250L, mpvProgressPublishIntervalMs(highFrequency = false))
        assertEquals(100L, mpvProgressPublishIntervalMs(highFrequency = true))
        assertTrue(gate.requestSchedule())
        assertFalse(gate.requestSchedule())
        assertTrue(gate.consumeScheduled())
        assertFalse(gate.consumeScheduled())
        assertTrue(gate.requestSchedule())
        assertTrue(gate.cancel())
        assertFalse(gate.cancel())
        assertTrue(gate.requestSchedule())
    }

    @Test
    fun `file descriptor owner closes stale ended and released descriptors once`() {
        val closed = mutableListOf<Int>()
        val owner = MpvFileDescriptorOwner { closed += it }

        owner.replaceWith(10)
        owner.replaceWith(11)
        assertTrue(closed.isEmpty())
        owner.onFileLoaded()
        assertEquals(listOf(10), closed)

        owner.onPlaybackEnded()
        owner.onPlaybackEnded()
        assertEquals(listOf(10, 11), closed)

        owner.replaceWith(12)
        owner.release()
        owner.release()
        assertEquals(listOf(10, 11, 12), closed)
    }

    @Test
    fun `custom MPV parameters preserve order and ignore malformed entries`() {
        val parameters = parseMpvCustomParams(
            "profile,gpu-hq; # ignored; invalid; cache-secs,30; profile,fast",
        )

        assertEquals(
            linkedMapOf("profile" to "fast", "cache-secs" to "30"),
            parameters,
        )
    }
}
