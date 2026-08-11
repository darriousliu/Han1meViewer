package com.yenaly.han1meviewer.playback.core

import com.yenaly.han1meviewer.playback.model.PlaybackCapabilities
import com.yenaly.han1meviewer.playback.model.PlaybackEngineType
import com.yenaly.han1meviewer.playback.model.PlaybackSource
import com.yenaly.han1meviewer.playback.model.PlaybackState
import kotlinx.coroutines.flow.StateFlow

/**
 * Player-agnostic control surface consumed by the video feature and Compose UI.
 *
 * Concrete player objects and rendering Views/Surfaces deliberately stay out of this public API.
 */
interface PlaybackController {
    val engineType: PlaybackEngineType
    val capabilities: PlaybackCapabilities
    val state: StateFlow<PlaybackState>

    fun load(
        source: PlaybackSource,
        qualityId: String? = null,
        startPositionMs: Long = 0L,
        playWhenReady: Boolean = true,
    )

    fun play()
    fun pause()
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun setHighFrequencyProgressUpdates(enabled: Boolean)
    fun selectQuality(qualityId: String)
    fun setSuperResolution(index: Int)
    fun release()
}
