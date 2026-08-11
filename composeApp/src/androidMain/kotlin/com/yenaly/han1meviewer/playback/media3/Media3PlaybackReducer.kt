package com.yenaly.han1meviewer.playback.media3

import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackState

/** Values that must survive replacing a Media3 media item during a quality switch. */
internal data class Media3ContinuitySnapshot(
    val positionMs: Long,
    val playWhenReady: Boolean,
    val speed: Float,
)

internal fun PlaybackState.prepareMedia3QualitySwitch(
    qualityId: String,
    snapshot: Media3ContinuitySnapshot,
): PlaybackState = copy(
    phase = PlaybackPhase.Preparing,
    isPlaying = false,
    playWhenReady = snapshot.playWhenReady,
    positionMs = snapshot.positionMs.coerceAtLeast(0L),
    bufferedPositionMs = 0L,
    bufferedPercentage = 0,
    speed = snapshot.speed,
    selectedQualityId = qualityId,
    errorMessage = null,
)

internal fun PlaybackState.reduceMedia3Phase(
    phase: PlaybackPhase,
    isPlaying: Boolean,
    playWhenReady: Boolean,
): PlaybackState = copy(
    phase = phase,
    isPlaying = isPlaying && phase != PlaybackPhase.Ended,
    playWhenReady = playWhenReady && phase != PlaybackPhase.Ended,
    errorMessage = null,
)

internal fun PlaybackState.reduceMedia3Error(message: String): PlaybackState = copy(
    phase = PlaybackPhase.Error,
    isPlaying = false,
    playWhenReady = false,
    errorMessage = message,
)
