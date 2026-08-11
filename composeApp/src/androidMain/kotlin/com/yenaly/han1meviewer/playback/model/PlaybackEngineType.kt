package com.yenaly.han1meviewer.playback.model

enum class PlaybackEngineType(val persistedValue: String) {
    Media3("Media3"),
    Mpv("Mpv"),
    ;

    companion object {
        /** Maps canonical values and legacy player-kernel values already persisted by the app. */
        fun fromString(value: String?): PlaybackEngineType = when {
            value.equals(Media3.persistedValue, ignoreCase = true) -> Media3
            value.equals("ExoPlayer", ignoreCase = true) -> Media3
            value.equals("MediaPlayer", ignoreCase = true) -> Media3
            value.equals("SystemMediaPlayer", ignoreCase = true) -> Media3
            value.equals(Mpv.persistedValue, ignoreCase = true) -> Mpv
            value.equals("MpvPlayer", ignoreCase = true) -> Mpv
            value.equals("MPV", ignoreCase = true) -> Mpv
            else -> Media3
        }
    }
}
