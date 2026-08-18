package io.github.darriousliu.han1meviewer.ui.screen.video.player

import androidx.compose.runtime.Composable

/** 桌面端的亮度/音量能力本轮不实现。 */
@Composable
actual fun rememberDeviceMediaControls(): DeviceMediaControls = NoopDeviceMediaControls
