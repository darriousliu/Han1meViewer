package com.yenaly.han1meviewer.playback.core

import android.view.Surface
import com.yenaly.han1meviewer.playback.model.PlaybackCapabilities
import com.yenaly.han1meviewer.playback.model.PlaybackEngineType
import com.yenaly.han1meviewer.playback.model.PlaybackSource
import com.yenaly.han1meviewer.playback.model.PlaybackState
import kotlinx.coroutines.flow.StateFlow

internal interface PlaybackEngine {
    val engineType: PlaybackEngineType
    val capabilities: PlaybackCapabilities
    val state: StateFlow<PlaybackState>
    val renderHandle: PlaybackRenderHandle

    fun load(
        source: PlaybackSource,
        qualityId: String?,
        startPositionMs: Long,
        playWhenReady: Boolean,
    )

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun setHighFrequencyProgressUpdates(enabled: Boolean)
    fun selectQuality(qualityId: String)
    fun setSuperResolution(index: Int)
    fun release()
}

/** Internal bridge used only by the Compose rendering layer. */
internal interface PlaybackRenderHandle {
    fun attachSurface(surface: Surface)
    fun detachSurface(surface: Surface)
    fun updateSurfaceSize(width: Int, height: Int)
}
