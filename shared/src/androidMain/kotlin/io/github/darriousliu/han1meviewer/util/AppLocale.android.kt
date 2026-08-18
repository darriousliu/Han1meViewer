package io.github.darriousliu.han1meviewer.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * 用平台标准的 per-app language API。
 *
 * 比原来手写 `attachBaseContext` + `createConfigurationContext` 好在三点：
 * 和系统设置里的「应用语言」双向联动、进程死了设置还在、重建由 AppCompat 自己管
 * （API 33+ 交给系统，以下 AppCompat 自己重建）。
 *
 * ⚠️ 所以切语言**仍然会重建 Activity**——这是平台行为，绕不过去也不该绕。
 */
actual fun applyAppLanguage(languageTag: String?) {
    AppCompatDelegate.setApplicationLocales(
        if (languageTag == null) LocaleListCompat.getEmptyLocaleList()
        else LocaleListCompat.forLanguageTags(languageTag)
    )
}

/** 读回系统实际生效的值，用来和系统设置里的「应用语言」对齐。 */
fun currentAppLanguageTag(): String? =
    AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()

/**
 * 一次性迁移：老版本把语言存在 `Preferences.appLanguage` 里、靠
 * `MainActivity.attachBaseContext` 手动套上去；AppCompat 那边的 locale list 是空的。
 * 换成 per-app language API 之后，如果不补这一下，**升级上来的用户语言会静默回到跟随系统**。
 *
 * 只在 AppCompat 尚无设置、而偏好里不是「跟随系统」时补一次；之后两边就同源了。
 */
fun migrateAppLanguageToPlatformIfNeeded(preferenceValue: String) {
    if (!AppCompatDelegate.getApplicationLocales().isEmpty) return
    val tag = toLanguageTagOrNull(preferenceValue) ?: return
    applyAppLanguage(tag)
}
