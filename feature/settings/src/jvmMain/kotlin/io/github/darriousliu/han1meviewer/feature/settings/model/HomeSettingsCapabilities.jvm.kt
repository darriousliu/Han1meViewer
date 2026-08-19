package io.github.darriousliu.han1meviewer.feature.settings.model

/**
 * 桌面端目前落地了的能力。每落地一个就在这里翻一个 flag，
 * 同时给 `HomeSettingsActions.jvm.kt` 加对应的 override。
 */
actual val homeSettingsCapabilities: HomeSettingsCapabilities = HomeSettingsCapabilities(
    // 深色模式由 `HanimeTheme` 直接观察偏好，三端一致
    darkModeOverride = true,
    // 语言切换：Locale.setDefault 实现
    appLanguageOverride = true,
)
