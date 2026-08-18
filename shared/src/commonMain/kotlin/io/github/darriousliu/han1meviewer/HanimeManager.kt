package io.github.darriousliu.han1meviewer

import androidx.compose.runtime.Composable
import io.github.darriousliu.han1meviewer.core.storage.Preferences.isAlreadyLogin
import io.github.darriousliu.han1meviewer.core.storage.Preferences.loginCookie
import io.github.darriousliu.han1meviewer.core.network.HCookiesStorage
import io.github.darriousliu.han1meviewer.core.common.util.CookieString
import io.github.darriousliu.han1meviewer.core.common.util.localizedText
import io.github.darriousliu.han1meviewer.core.common.EMPTY_STRING

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
