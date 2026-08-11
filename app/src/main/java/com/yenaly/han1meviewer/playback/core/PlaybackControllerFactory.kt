package com.yenaly.han1meviewer.playback.core

import android.content.Context
import com.yenaly.han1meviewer.playback.media3.Media3PlaybackEngine
import com.yenaly.han1meviewer.playback.model.PlaybackEngineType
import com.yenaly.han1meviewer.playback.mpv.MpvPlaybackEngine

object PlaybackControllerFactory {
    fun create(
        context: Context,
        engineType: PlaybackEngineType,
    ): PlaybackController {
        val engine = when (engineType) {
            PlaybackEngineType.Media3 -> Media3PlaybackEngine(context.applicationContext)
            PlaybackEngineType.Mpv -> MpvPlaybackEngine(context.applicationContext)
        }
        return DefaultPlaybackController(engine)
    }

    internal fun create(engine: PlaybackEngine): PlaybackController =
        DefaultPlaybackController(engine)
}
