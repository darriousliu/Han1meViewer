package com.yenaly.han1meviewer.util

import platform.Foundation.NSUserDefaults

/**
 * iOS 认 `AppleLanguages`：Foundation 在进程启动时读它来决定 bundle 语言。
 * 跟随系统就是把这个 key 删掉。
 *
 * ⚠️ 写进去的值要下次启动才影响 Foundation 层；本次会话内的文案刷新靠根组合的
 * `key(appLanguage)` 重建组合树（compose-resources 自己会重新解析）。
 */
actual fun applyAppLanguage(languageTag: String?) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (languageTag == null) {
        defaults.removeObjectForKey("AppleLanguages")
    } else {
        defaults.setObject(listOf(languageTag), "AppleLanguages")
    }
}
