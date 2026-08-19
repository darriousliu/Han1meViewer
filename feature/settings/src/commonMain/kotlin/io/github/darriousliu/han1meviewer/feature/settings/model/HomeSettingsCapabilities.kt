package io.github.darriousliu.han1meviewer.feature.settings.model

/**
 * 本平台**支持**哪些设置项。不支持的直接不渲染，而不是渲染成灰的。
 *
 * 这是三层结构的中间一层：
 *
 * 1. **数据**（[HomeSettingsUiState]）——全平台都是全的
 * 2. **可见性**（本类）——各平台按实际情况引用那份数据
 * 3. **能力**（`HomeSettingsActions`）——expect/actual
 *
 * 全平台通用的设置项压根不在这里出现，因为它们永远显示；
 * 这里只列「概念通用、实现按平台分」和「Android 独有」两类。
 *
 * **所有字段默认 false**，所以新平台的 actual 就是一行
 * `HomeSettingsCapabilities()`，新增一个 flag 也不会打断它们。
 * 用 `expect object` 就做不到这点——expect 声明的成员不能带默认值。
 */
data class HomeSettingsCapabilities(
    /** 画中画。Android 有，iOS 有（AVPictureInPicture），desktop 无。 */
    val pictureInPicture: Boolean = false,
    /** 移动数据提醒。要能区分计费网络，桌面端没这个概念。 */
    val mobileDataWarning: Boolean = false,
    /** 预测式返回。Android 13+ 独有。 */
    val predictiveBack: Boolean = false,
    /** 下载。依赖 SAF / 后台 worker，目前只有 Android 一套。 */
    val downloads: Boolean = false,
    /** 「默认打开方式」设置页。Android 独有。 */
    val deepLinkSettings: Boolean = false,
    /** 深色模式偏好能否真正生效。见 [HomeSettingsCapabilities] 的说明与 `HanimeTheme` 的注释。 */
    val darkModeOverride: Boolean = false,
    /** 应用内切换语言。Android 靠 `createConfigurationContext` + recreate。 */
    val appLanguageOverride: Boolean = false,
    /** 动态取色（Material You）。Android 12+ 独有。 */
    val dynamicColor: Boolean = false,
    /** 应用内检查更新。现在挂在 androidMain 的 `AppViewModel` 上。 */
    val updateCheck: Boolean = false,
    /** CI 更新渠道。要能侧载 APK，Android 独有。 */
    val ciUpdateChannel: Boolean = false,
    /** 数据统计开关。Android 与 iOS 都链了 FirebaseAnalytics，desktop 没有。 */
    val analytics: Boolean = false,
    /** 应用锁。Android BiometricPrompt / iOS LocalAuthentication。 */
    val appLock: Boolean = false,
    /** 换 App 图标。基于 activity-alias，Android 独有。 */
    val fakeLauncherIcon: Boolean = false,
    /** 能不能自杀重启。iOS 不允许。 */
    val selfRestart: Boolean = false,
)

expect val homeSettingsCapabilities: HomeSettingsCapabilities
