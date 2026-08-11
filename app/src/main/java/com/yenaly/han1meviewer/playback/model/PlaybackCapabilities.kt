package com.yenaly.han1meviewer.playback.model

data class PlaybackCapabilities(
    val supportsQualitySelection: Boolean = true,
    val supportsPlaybackSpeed: Boolean = true,
    val supportsSuperResolution: Boolean = false,
    val supportsEmbeddedSubtitles: Boolean = false,
    val supportsExternalSubtitles: Boolean = false,
)
