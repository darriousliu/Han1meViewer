package com.yenaly.han1meviewer.playback.model

data class PlaybackState(
    val source: PlaybackSource? = null,
    val phase: PlaybackPhase = PlaybackPhase.Idle,
    val isPlaying: Boolean = false,
    val playWhenReady: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val bufferedPercentage: Int = 0,
    val speed: Float = PlaybackDefaults.DEFAULT_SPEED,
    val selectedQualityId: String? = null,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val errorMessage: String? = null,
) {
    val progress: Float
        get() = if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }

    val bufferedProgress: Float
        get() = (bufferedPercentage / 100f).coerceIn(0f, 1f)
}
