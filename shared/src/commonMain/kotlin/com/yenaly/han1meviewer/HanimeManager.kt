package com.yenaly.han1meviewer

import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.Preferences.isAlreadyLogin
import com.yenaly.han1meviewer.Preferences.loginCookie
import com.yenaly.han1meviewer.logic.network.HCookiesStorage
import com.yenaly.han1meviewer.util.CookieString
import com.yenaly.han1meviewer.util.localizedText

/**
 * 给用户显示的错误信息
 *
 * ぴえん化
 *
 * 是 @Composable 而不是普通属性：数据层的异常现在只带 StringResource，
 * 解析要有 composition 上下文，见 [localizedText]。
 */
@Composable
fun Throwable.pienization(): String = "🥺\n" + localizedText()

// log in and log out

fun logout() {
    isAlreadyLogin = false
    loginCookie = CookieString(EMPTY_STRING)
    HCookiesStorage.clear()
    removeAllCookies()
}

fun login(cookies: String) {
    isAlreadyLogin = true
    loginCookie = CookieString(cookies)
}

fun login(cookies: List<String>) {
    login(cookies.joinToString(";") {
        it.substringBefore(';')
    })
}

expect fun removeAllCookies()
