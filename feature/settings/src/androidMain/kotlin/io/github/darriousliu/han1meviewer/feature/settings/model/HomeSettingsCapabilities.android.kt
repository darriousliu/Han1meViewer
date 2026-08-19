package io.github.darriousliu.han1meviewer.feature.settings.model

import android.os.Build

/**
 * Android 支持全部设置项。唯一按版本判断的是 [HomeSettingsCapabilities.dynamicColor]。
 *
 * [HomeSettingsCapabilities.deepLinkSettings] 和 [HomeSettingsCapabilities.predictiveBack]
 * **故意不按版本判断**：前者在低版本上就是「显示出来、点了提示不支持」，
 * 后者在 UI 上本来就是 `enabled = false` 的占位。把它们改成按版本隐藏是行为变更；
 * 这两个 flag 在这里只负责「别的平台不显示」。
 */
actual val homeSettingsCapabilities: HomeSettingsCapabilities = HomeSettingsCapabilities(
    pictureInPicture = true,
    mobileDataWarning = true,
    predictiveBack = true,
    downloads = true,
    deepLinkSettings = true,
    darkModeOverride = true,
    appLanguageOverride = true,
    dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    updateCheck = true,
    ciUpdateChannel = true,
    analytics = true,
    appLock = true,
    fakeLauncherIcon = true,
    selfRestart = true,
)
