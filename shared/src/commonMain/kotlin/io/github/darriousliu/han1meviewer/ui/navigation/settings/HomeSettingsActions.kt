package io.github.darriousliu.han1meviewer.ui.navigation.settings

import androidx.compose.runtime.Composable
import io.github.darriousliu.han1meviewer.Preferences
import io.github.darriousliu.han1meviewer.logic.model.github.Latest
import io.github.darriousliu.han1meviewer.logic.state.WebsiteState
import io.github.darriousliu.han1meviewer.util.applyAppLanguage
import io.github.darriousliu.han1meviewer.util.toLanguageTagOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 首页设置页背后的**平台能力**。Step 22 三层结构的第三层（前两层见
 * [io.github.darriousliu.han1meviewer.ui.screen.settings.model.HomeSettingsCapabilities]）。
 *
 * 每个方法都有默认空实现，于是 jvm/ios 的 actual 各一行
 * （`= NoopHomeSettingsActions`），以后加方法也不会打断它们——
 * 和 `HomeSettingsCapabilities` 全字段默认 false 是同一个道理。
 *
 * **能不能做**看 `HomeSettingsCapabilities`，**怎么做**看这里。两者要成对翻：
 * 某个 flag 翻 true 之前，对应平台得先把这里的方法实现掉。
 *
 * ### 为什么不是一堆顶层 `expect fun`
 *
 * 因为其中四个动作（三处 `recreate()` + 深层链接弹窗）**需要 Activity**，
 * 而 Android 侧只有全局 `applicationContext`，没有全局 Activity。
 * 顶层 expect 想拿 Activity 就得加个可变全局持有者，那正是
 * `MIGRATION_NEXT.md` 第四节 ⛔ 那一行禁掉的「隐式平台能力注入」。
 * 做成 `@Composable expect fun` 工厂就能在组合里取 `LocalActivity`，
 * 一行全局状态都不用加。
 */
interface HomeSettingsActions {

    // ---- 外观 / 配置（B 类：概念通用，实现按平台分） ----

    /** 深色模式偏好改完之后让它生效。Android 是 AppCompatDelegate + recreate。 */
    fun applyDarkMode() {}

    /** 主题色改完之后让它生效。 */
    fun applyThemeColor() {}

    /**
     * 应用语言改完之后让它生效。
     *
     * 默认实现直接调 commonMain 的 `applyAppLanguage`（本身是 expect fun，
     * 三端各有实现），所以**没有平台需要 override 它**。
     */
    fun applyAppLanguage() = applyAppLanguage(toLanguageTagOrNull(Preferences.appLanguage))

    /**
     * 一次改了一堆偏好之后重建 UI（现在只有「导入备份」用）。
     * 和上面三个 apply 分开命名而不是共用一个「reload」：
     * 那三个将来在 desktop 上可能各有各的做法（主题色不用重建、语言要），
     * 合并了就再也分不开。
     */
    fun reloadUi() {}

    /** 自杀重启。iOS 不允许，那边 `selfRestart` 是 false。 */
    fun restartApp() {}

    // ---- 画中画权限（C 类：Android 的 AppOpsManager 权限页） ----

    fun isPipPermissionGranted(): Boolean = true

    fun openPipPermissionSettings() {}

    // ---- 应用锁（B 类） ----

    /**
     * 返回枚举而不是两个 Boolean，是因为现在要分别报 `not_set_sys_lock` 和
     * `not_compact_lock_screen`，其中一个分支是裸的 `SDK_INT < P`——
     * 那种判断绝不能漏进 commonMain。
     */
    fun deviceLockAvailability(): DeviceLockAvailability = DeviceLockAvailability.Unsupported

    // ---- 数据统计 ----

    fun setAnalyticsEnabled(enabled: Boolean) {}

    // ---- 深层链接（C 类） ----

    /**
     * @return 是否真的把设置页打开了。false 时调用方负责提示「本机不支持」——
     *   版本判断（Android 12L 才有这个页）留在 actual 里，别漏进 commonMain。
     */
    fun openDeepLinkSettings(): Boolean = false

    // ---- 换 App 图标（C 类） ----

    fun switchLauncherIcon(alias: String) {}

    // ---- 检查更新 ----

    val versionFlow: StateFlow<WebsiteState<Latest?>> get() = NoUpdateAvailable

    fun checkUpdate(forceShow: Boolean) {}

    fun showPendingUpdateDialog() {}

    fun onUpdateChannelChanged() {}
}

enum class DeviceLockAvailability {
    Available,

    /** 系统本身没设锁屏 */
    NoSystemLock,

    /** 系统版本太低（Android < P） */
    UnsupportedOsVersion,

    /** 本平台压根没有这个能力 */
    Unsupported,
}

private val NoUpdateAvailable: StateFlow<WebsiteState<Latest?>> =
    MutableStateFlow(WebsiteState.Success(null))

/** 什么都不做的实现，给还没落地任何能力的平台用。 */
object NoopHomeSettingsActions : HomeSettingsActions

@Composable
expect fun rememberHomeSettingsActions(): HomeSettingsActions
