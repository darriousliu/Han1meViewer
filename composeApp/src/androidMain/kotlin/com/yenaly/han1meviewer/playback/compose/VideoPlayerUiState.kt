package com.yenaly.han1meviewer.playback.compose

import androidx.compose.runtime.Immutable
import com.yenaly.han1meviewer.playback.model.PlaybackCapabilities
import com.yenaly.han1meviewer.playback.model.PlaybackDefaults
import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackState
import com.yenaly.han1meviewer.playback.model.QualityVariant
import java.util.Locale

/**
 * Immutable rendering model for the Compose player chrome.
 *
 * Playback facts come from [playback]. Visual interaction state and preference-backed gesture
 * tuning are deliberately hoisted so the host can keep them stable across configuration and
 * fullscreen transitions.
 */
@Immutable
data class VideoPlayerUiState(
    val playback: PlaybackState,
    val capabilities: PlaybackCapabilities,
    val longPressSpeedMultiplier: Float,
    val seekGestureSensitivity: Int,
    val controlsVisible: Boolean = true,
    val showBottomProgress: Boolean = true,
    val isLocked: Boolean = false,
    val isFullscreen: Boolean = false,
    val showPoster: Boolean = false,
    val brightness: Float = 0.5f,
    val volume: Float = 0.5f,
    val showRestartFromBeginning: Boolean = false,
    val keyframesEnabled: Boolean = false,
    val keyframes: List<VideoKeyframeUiState> = emptyList(),
    val keyframePanelVisible: Boolean = false,
    val keyframeCountdown: VideoKeyframeCountdownUiState? = null,
    val superResolutionIndex: Int = 0,
    val availableSpeeds: List<Float> = PlaybackDefaults.SPEED_OPTIONS,
) {
    val title: String get() = playback.source?.title.orEmpty()

    val posterUri: String? get() = playback.source?.posterUri

    val qualities: List<QualityVariant> get() = playback.source?.qualities.orEmpty()

    val selectedQuality: QualityVariant?
        get() = playback.source?.let { source ->
            source.qualities.firstOrNull { it.id == playback.selectedQualityId }
                ?: source.resolveQuality()
        }

    val progressFraction: Float
        get() = fraction(playback.positionMs, playback.durationMs)

    val bufferedFraction: Float
        get() = when {
            playback.durationMs > 0L -> fraction(playback.bufferedPositionMs, playback.durationMs)
            else -> (playback.bufferedPercentage / 100f).coerceIn(0f, 1f)
        }

    val isLoading: Boolean
        get() = playback.phase == PlaybackPhase.Preparing || playback.phase == PlaybackPhase.Buffering

    val isError: Boolean get() = playback.phase == PlaybackPhase.Error

    val isEnded: Boolean get() = playback.phase == PlaybackPhase.Ended

    val canSeek: Boolean get() = playback.durationMs > 0L && !isError

    val hasKeyframes: Boolean get() = keyframes.isNotEmpty()
}

@Immutable
data class VideoKeyframeUiState(
    val positionMs: Long,
    val prompt: String? = null,
)

@Immutable
data class VideoKeyframeCountdownUiState(
    val remainingMs: Long,
    val prompt: String? = null,
) {
    init {
        require(remainingMs >= 0L) { "Keyframe countdown cannot be negative." }
    }
}

internal fun formatKeyframeCountdown(
    remainingMs: Long,
    locale: Locale = Locale.getDefault(),
): String = if (remainingMs >= 1_000L) {
    (remainingMs / 1_000L + 1L).toString()
} else {
    String.format(locale, "%.1f", remainingMs.coerceAtLeast(0L) / 1_000f)
}

internal fun fraction(value: Long, total: Long): Float {
    if (total <= 0L) return 0f
    return (value.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
}

internal fun positionFromFraction(fraction: Float, durationMs: Long): Long {
    if (durationMs <= 0L) return 0L
    return (fraction.coerceIn(0f, 1f) * durationMs).toLong().coerceIn(0L, durationMs)
}
