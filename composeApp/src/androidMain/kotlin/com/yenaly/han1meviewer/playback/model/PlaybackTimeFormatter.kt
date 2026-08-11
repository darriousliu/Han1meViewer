package com.yenaly.han1meviewer.playback.model

import com.yenaly.han1meviewer.platform.NumberFormatter

fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    val seconds = totalSeconds % 60L
    val minutes = totalSeconds / 60L % 60L
    val hours = totalSeconds / 3_600L
    return if (hours > 0L) {
        "${NumberFormatter.formatInteger(hours, 2)}:" +
            "${NumberFormatter.formatInteger(minutes, 2)}:" +
            NumberFormatter.formatInteger(seconds, 2)
    } else {
        "${NumberFormatter.formatInteger(minutes, 2)}:" +
            NumberFormatter.formatInteger(seconds, 2)
    }
}
