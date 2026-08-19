package io.github.darriousliu.han1meviewer.feature.video.player

/**
 * 流量提醒「本次进程内只确认一次」的标记，对应 jzvd 的
 * `WIFI_TIP_DIALOG_SHOWED` 静态位。进程重启后重新提醒。
 */
object MobileDataWarningSession {
    var accepted: Boolean = false
}
