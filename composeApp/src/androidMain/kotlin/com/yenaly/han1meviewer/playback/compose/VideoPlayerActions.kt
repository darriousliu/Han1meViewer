package com.yenaly.han1meviewer.playback.compose

/**
 * Required semantic actions emitted by the Compose player.
 *
 * The UI never owns a playback engine and deliberately has no default/no-op callbacks. A route or
 * controller adapter must decide how each action changes playback or platform state.
 */
interface VideoPlayerActions {
    fun onBack()

    fun onHome()

    fun onTogglePlayPause()

    fun onRetry()

    fun onReplay()

    fun onSeekTo(positionMs: Long)

    fun onSelectQuality(qualityId: String)

    fun onSetSpeed(speed: Float)

    fun onSetSuperResolution(index: Int)

    fun onOpenKeyframes()

    fun onDismissKeyframes()

    fun onSelectKeyframe(positionMs: Long)

    fun onAddKeyframe()

    fun onToggleFullscreen()

    fun onControlsVisibilityChanged(visible: Boolean)

    fun onLockChanged(locked: Boolean)

    fun onBrightnessChanged(fraction: Float)

    fun onVolumeChanged(fraction: Float)

    fun onRestartFromBeginning()
}
