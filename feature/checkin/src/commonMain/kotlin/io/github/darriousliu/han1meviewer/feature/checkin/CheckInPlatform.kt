package io.github.darriousliu.han1meviewer.feature.checkin

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate

/**
 * 打卡页的平台能力可见性。全字段默认 false:没有该能力的平台对应入口整个不渲染。
 */
data class CheckInCapabilities(
    /** 添加桌面小组件(Android 的 AppWidget;iOS 对应物是 WidgetKit,未做)。 */
    val addWidget: Boolean = false,
    /** 向系统日历添加提醒。 */
    val addCalendarEvent: Boolean = false,
)

expect val checkInCapabilities: CheckInCapabilities

/**
 * 打卡页的平台操作。方法全带默认空实现;UI 是否展示入口由 [checkInCapabilities] 决定,
 * 两者要成对维护(落地一个能力=加 override+翻 flag)。
 */
interface CheckInActions {

    fun addWidget() {}

    fun addCalendarEvent(date: LocalDate) {}

    /** 报表全屏切换(锁横屏+隐系统栏);屏幕销毁时会以 false 复位调入,必须随时可安全调用。 */
    fun setReportFullscreen(fullscreen: Boolean) {}
}

object NoopCheckInActions : CheckInActions

@Composable
expect fun rememberCheckInActions(): CheckInActions
