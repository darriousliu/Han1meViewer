package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** iOS还没有播放能力，本轮只实现 Android。落地时把这里换成真实现。 */
@Composable
actual fun rememberVideoPlayerController(key: Any?): VideoPlayerController =
    NoopVideoPlayerController

@Composable
actual fun VideoSurface(controller: VideoPlayerController, modifier: Modifier) {
    Box(modifier.background(Color.Black))
}
