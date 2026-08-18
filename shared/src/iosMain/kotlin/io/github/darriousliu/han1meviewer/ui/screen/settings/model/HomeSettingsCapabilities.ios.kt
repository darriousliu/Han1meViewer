package io.github.darriousliu.han1meviewer.ui.screen.settings.model

/**
 * iOS目前落地了的能力。每落地一个就在这里翻一个 flag，
 * 同时给 `HomeSettingsActions.ios.kt` 加对应的 override。
 */
actual val homeSettingsCapabilities: HomeSettingsCapabilities = HomeSettingsCapabilities(
    // Step 23：深色模式改成由 `HanimeTheme` 直接观察偏好，三端一致
    darkModeOverride = true,
    // Step 23：语言切换有了 AppleLanguages 实现
    appLanguageOverride = true,
)
