package com.yenaly.han1meviewer.playback.model

object PlaybackDefaults {
    const val DEFAULT_SPEED = 1.0f
    const val DEFAULT_SPEED_INDEX = 2
    const val DEFAULT_PROGRESS_SLIDE_SENSITIVITY = 5
    const val DEFAULT_LONG_PRESS_SPEED_MULTIPLIER = 2.5f
    const val DEFAULT_KEYFRAME_COUNTDOWN_SECONDS = 10

    val SPEED_OPTIONS: List<Float> = listOf(
        0.5f,
        0.75f,
        1.0f,
        1.25f,
        1.5f,
        1.75f,
        2.0f,
        2.25f,
        2.5f,
        2.75f,
        3.0f,
    )

    val SPEED_LABELS: List<String> = SPEED_OPTIONS.map { "${it}x" }

    fun progressSlideDivisor(sensitivity: Int): Int = when (sensitivity.coerceIn(1, 9)) {
        in 1..5 -> sensitivity.coerceIn(1, 5)
        6 -> 7
        7 -> 10
        8 -> 20
        else -> 40
    }
}
