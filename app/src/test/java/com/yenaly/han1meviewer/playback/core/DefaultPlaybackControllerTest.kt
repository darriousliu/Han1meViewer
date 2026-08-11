package com.yenaly.han1meviewer.playback.core

import android.view.Surface
import com.yenaly.han1meviewer.playback.model.PlaybackCapabilities
import com.yenaly.han1meviewer.playback.model.PlaybackEngineType
import com.yenaly.han1meviewer.playback.model.PlaybackSource
import com.yenaly.han1meviewer.playback.model.PlaybackState
import com.yenaly.han1meviewer.playback.model.QualityVariant
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DefaultPlaybackControllerTest {
    @Test
    fun `controller normalizes positions and delegates commands`() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackControllerFactory.create(engine)

        controller.load(source(), startPositionMs = -10L, playWhenReady = false)
        controller.seekTo(-20L)
        controller.setSpeed(1.5f)
        controller.setHighFrequencyProgressUpdates(true)
        controller.selectQuality("720P")
        controller.setSuperResolution(2)
        controller.release()

        assertEquals(0L, engine.loadedPositionMs)
        assertEquals(0L, engine.seekPositionMs)
        assertEquals(1.5f, engine.speed)
        assertEquals(true, engine.highFrequencyUpdatesEnabled)
        assertEquals("720P", engine.qualityId)
        assertEquals(2, engine.superResolutionIndex)
        assertEquals(true, engine.released)
    }

    @Test
    fun `toggle pauses pending playback and plays paused state`() {
        val engine = FakePlaybackEngine()
        val controller = PlaybackControllerFactory.create(engine)

        engine.mutableState.value = PlaybackState(playWhenReady = true)
        controller.togglePlayPause()
        assertEquals(1, engine.pauseCalls)

        engine.mutableState.value = PlaybackState()
        controller.togglePlayPause()
        assertEquals(1, engine.playCalls)
    }

    @Test
    fun `controller rejects invalid command values`() {
        val controller = PlaybackControllerFactory.create(FakePlaybackEngine())

        assertThrows(IllegalArgumentException::class.java) { controller.setSpeed(0f) }
        assertThrows(IllegalArgumentException::class.java) { controller.selectQuality(" ") }
        assertThrows(IllegalArgumentException::class.java) { controller.setSuperResolution(-1) }
    }

    private fun source() = PlaybackSource(
        id = "video",
        title = "Video",
        qualities = listOf(QualityVariant("1080P", uri = "https://example.com/video.mp4")),
    )
}

private class FakePlaybackEngine : PlaybackEngine {
    override val engineType = PlaybackEngineType.Media3
    override val capabilities = PlaybackCapabilities()
    val mutableState = MutableStateFlow(PlaybackState())
    override val state = mutableState
    override val renderHandle = object : PlaybackRenderHandle {
        override fun attachSurface(surface: Surface) = Unit
        override fun detachSurface(surface: Surface) = Unit
        override fun updateSurfaceSize(width: Int, height: Int) = Unit
    }

    var loadedPositionMs: Long? = null
    var seekPositionMs: Long? = null
    var speed: Float? = null
    var qualityId: String? = null
    var superResolutionIndex: Int? = null
    var highFrequencyUpdatesEnabled = false
    var playCalls = 0
    var pauseCalls = 0
    var released = false

    override fun load(
        source: PlaybackSource,
        qualityId: String?,
        startPositionMs: Long,
        playWhenReady: Boolean,
    ) {
        loadedPositionMs = startPositionMs
    }

    override fun play() {
        playCalls++
    }

    override fun pause() {
        pauseCalls++
    }

    override fun seekTo(positionMs: Long) {
        seekPositionMs = positionMs
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed
    }

    override fun setHighFrequencyProgressUpdates(enabled: Boolean) {
        highFrequencyUpdatesEnabled = enabled
    }

    override fun selectQuality(qualityId: String) {
        this.qualityId = qualityId
    }

    override fun setSuperResolution(index: Int) {
        superResolutionIndex = index
    }

    override fun release() {
        released = true
    }
}
