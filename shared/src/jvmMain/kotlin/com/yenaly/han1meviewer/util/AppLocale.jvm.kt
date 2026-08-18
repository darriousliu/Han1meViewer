package com.yenaly.han1meviewer.util

import java.util.Locale

/**
 * 桌面端没有「应用语言」这种系统设置，直接改进程默认 Locale；
 * compose-resources 解析字符串时读的就是它。
 * 重新解析靠根组合的 `key(appLanguage)`，不重启进程。
 */
actual fun applyAppLanguage(languageTag: String?) {
    Locale.setDefault(
        if (languageTag == null) Locale.getDefault() else Locale.forLanguageTag(languageTag)
    )
}
