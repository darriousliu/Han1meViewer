package io.github.darriousliu.han1meviewer.feature.main

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 宿主(平台入口)提供给导航图的少量全局语义。
 *
 * 导航图与 route 包装只依赖本接口,不得依赖具体 Activity 类型。
 * 方法全部带默认空实现:没有对应能力的平台不需要写任何代码。
 */
interface MainHostActions {

    /** 用户在栈底选择退出 App。桌面端可关窗口;iOS 不允许自杀退出,保持空实现。 */
    fun onExitApp() {}

    /** 弹出登出确认;[closeCurrentPageOnConfirm] 为 true 时确认后同时退出当前页。 */
    fun onLogout(closeCurrentPageOnConfirm: Boolean = false) {}

    /** 页面浏览埋点;没有统计后端的平台保持空实现。 */
    fun onScreenView(screenClassName: String) {}

    /** 重启进程使网络层配置生效(换域名/hosts)。做不到自杀重启的平台保持空实现。 */
    fun onRestartApp() {}
}

object NoopMainHostActions : MainHostActions

/** 由宿主在组合根部提供;[MainHostActions] 的深层消费方(如设置脚手架)从这里取。 */
val LocalMainHostActions = staticCompositionLocalOf<MainHostActions> { NoopMainHostActions }
