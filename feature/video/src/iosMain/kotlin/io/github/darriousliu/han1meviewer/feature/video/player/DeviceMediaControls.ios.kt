package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.runtime.Composable

/** iOS的亮度/音量能力本轮不实现。 */
@Composable
actual fun rememberDeviceMediaControls(): DeviceMediaControls = NoopDeviceMediaControls
