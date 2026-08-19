package io.github.darriousliu.han1meviewer.feature.checkin

import androidx.compose.runtime.Composable

/** 本平台还没有小组件/日历能力(jvm 端日历可生成 .ics 打开,未做)。 */
actual val checkInCapabilities = CheckInCapabilities()

@Composable
actual fun rememberCheckInActions(): CheckInActions = NoopCheckInActions
