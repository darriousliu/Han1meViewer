package com.yenaly.han1meviewer.util

import kotlin.jvm.JvmInline

/**
 * 原本和 `toLoginCookieList` 一起放在 androidMain 的 `util/Cookies.kt`，
 * 那个扩展依赖 okhttp 的 `Cookie`，等 Step 5 换成 Ktor 才能上移；
 * 这个 value class 本身没有平台依赖，先跟着 [com.yenaly.han1meviewer.Preferences] 进 commonMain。
 */
@JvmInline
value class CookieString(val cookie: String)
