package io.github.darriousliu.han1meviewer.core.common.util

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList

object LanguageHelper {
    val preferredLanguage: Locale
        get() = LocaleList.current[0]
}

val Locale.Companion.CHINESE: Locale
    get() = Locale("zh")

val Locale.Companion.SIMPLIFIED_CHINESE: Locale
    get() = Locale("zh-CN")

val Locale.Companion.ENGLISH: Locale
    get() = Locale("en")

val Locale.Companion.JAPANESE: Locale
    get() = Locale("ja")