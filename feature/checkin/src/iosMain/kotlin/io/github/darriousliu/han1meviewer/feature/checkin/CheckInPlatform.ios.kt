package io.github.darriousliu.han1meviewer.feature.checkin

import androidx.compose.runtime.Composable

/** 本平台还没有小组件/日历能力(iOS 对应物是 WidgetKit / EventKit)。 */
actual val checkInCapabilities = CheckInCapabilities()

@Composable
actual fun rememberCheckInActions(): CheckInActions = NoopCheckInActions
