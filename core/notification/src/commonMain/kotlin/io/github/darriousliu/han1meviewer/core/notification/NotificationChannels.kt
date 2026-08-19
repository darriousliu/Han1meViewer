package io.github.darriousliu.han1meviewer.core.notification

// 通知渠道 id。改了 = 老用户在系统设置里对渠道做过的配置（静音等）全部作废，
// 所以这两个字符串是钉死的。
const val DOWNLOAD_NOTIFICATION_CHANNEL = "download_channel"

const val UPDATE_NOTIFICATION_CHANNEL = "update_channel"

/**
 * 建好 App 所有的通知渠道。宿主启动时调一次；重复调用无害（系统幂等）。
 *
 * Android 建下载与更新两个渠道；iOS/JVM 目前没有通知，空实现。
 */
expect fun createAppNotificationChannels()
