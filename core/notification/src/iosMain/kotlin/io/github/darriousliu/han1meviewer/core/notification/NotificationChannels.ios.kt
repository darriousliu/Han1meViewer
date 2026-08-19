package io.github.darriousliu.han1meviewer.core.notification

// iOS 没有「渠道」概念（对应物是 UNNotificationCategory + 授权），
// 等有真实通知需求时再接 UserNotifications。
actual fun createAppNotificationChannels() = Unit
