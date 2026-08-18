package com.yenaly.han1meviewer.util

/**
 * 应用内语言切换。
 *
 * `Preferences.appLanguage` 存的是 Android 资源限定符风格的值
 * （`system` / `zh-rCN` / `zh` / `ja` / `en`），而三端的平台 API 都要 BCP-47，
 * 所以中间隔一层 [toLanguageTagOrNull]。
 *
 * 各平台怎么落地见对应 actual：Android 用平台标准的 per-app language API，
 * iOS 写 `AppleLanguages`，JVM 改进程默认 Locale。
 */

/**
 * 偏好值 → BCP-47 语言标签；`null` 表示跟随系统。
 *
 * `"zh"` 在本项目里指**繁体**（原实现是 `Locale.TRADITIONAL_CHINESE`），
 * 对应 `composeResources/values`（默认包就是繁体）；简体是 `values-zh-rCN`。
 * 映射成 `zh-TW` 之后 compose-resources 找不到 `values-zh-rTW`，
 * 回落到默认包——正好还是繁体，行为不变。
 */
fun toLanguageTagOrNull(preferenceValue: String): String? = when (preferenceValue) {
    "system" -> null
    "zh-rCN" -> "zh-CN"
    "zh" -> "zh-TW"
    else -> preferenceValue      // ja / en 本身就是合法标签
}

/**
 * 把语言应用到平台上。
 *
 * @param languageTag BCP-47 标签；`null` = 跟随系统
 */
expect fun applyAppLanguage(languageTag: String?)
