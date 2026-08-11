package com.yenaly.han1meviewer.playback.core

import com.yenaly.han1meviewer.playback.model.PlaybackSource

internal class DefaultPlaybackController(
    internal val engine: PlaybackEngine,
) : PlaybackController {
    override val engineType = engine.engineType
    override val capabilities = engine.capabilities
    override val state = engine.state

    override fun load(
        source: PlaybackSource,
        qualityId: String?,
        startPositionMs: Long,
        playWhenReady: Boolean,
    ) {
        engine.load(
            source = source,
            qualityId = qualityId,
            startPositionMs = startPositionMs.coerceAtLeast(0L),
            playWhenReady = playWhenReady,
        )
    }

    override fun play() = engine.play()

    override fun pause() = engine.pause()

    override fun togglePlayPause() {
        if (state.value.isPlaying || state.value.playWhenReady) {
            pause()
        } else {
            play()
        }
    }

    override fun seekTo(positionMs: Long) = engine.seekTo(positionMs.coerceAtLeast(0L))

    override fun setSpeed(speed: Float) {
        require(speed > 0f) { "Playback speed must be positive." }
        engine.setSpeed(speed)
    }

    override fun setHighFrequencyProgressUpdates(enabled: Boolean) =
        engine.setHighFrequencyProgressUpdates(enabled)

    override fun selectQuality(qualityId: String) {
        require(qualityId.isNotBlank()) { "Quality id cannot be blank." }
        engine.selectQuality(qualityId)
    }

    override fun setSuperResolution(index: Int) {
        require(index >= 0) { "Super-resolution index cannot be negative." }
        engine.setSuperResolution(index)
    }

    override fun release() = engine.release()
}

internal val PlaybackController.renderHandle: PlaybackRenderHandle
    get() = checkNotNull((this as? DefaultPlaybackController)?.engine?.renderHandle) {
        "PlaybackController was not created by PlaybackControllerFactory."
    }
