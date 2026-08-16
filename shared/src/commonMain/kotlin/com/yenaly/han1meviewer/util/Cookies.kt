package com.yenaly.han1meviewer.util

import co.touchlab.kermit.Logger
import com.yenaly.han1meviewer.Preferences
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding

private val logger = Logger.withTag("CookieString")

/**
 * 主要用於 [HCookiesStorage][com.yenaly.han1meviewer.logic.network.HCookiesStorage]，最好不要用到其他地方。
 *
 * ⚠️ 一律用 [CookieEncoding.RAW]：这里的值来自用户粘贴/服务端下发的原始 cookie 串，
 * 用默认的 URI_ENCODING 会被二次编码。
 */
fun CookieString.toLoginCookieList(domain: String): List<Cookie> {
    val cookieList = mutableListOf<Cookie>().also {
        it += preferencesCookieList(domain)
    }
    cookie.split(';').forEach { cookie ->
        if (cookie.isNotBlank()) {
            val name = cookie.substringBefore('=').trim()
            val value = cookie.substringAfter('=').trim()
            val cleanedName = name.filter { it.code in 0x20..0x7E && it != '\n' && it != '\r' }
            val cleanValue = value.filter { it.code in 0x20..0x7E && it != '\n' && it != '\r' }
            if (cleanedName.isNotEmpty()) {
                cookieList += Cookie(
                    name = cleanedName,
                    value = cleanValue,
                    encoding = CookieEncoding.RAW,
                    domain = domain,
                )
            } else {
                logger.w { "无效键值: $cookie" }
            }
        }
    }
    return cookieList
}

/**
 * 每次退出登入後都會清除cookie，但是這樣可能會清除掉很多保存在cookie中的偏好，比如影片語言之類。
 *
 * 讓[preferencesCookieList]成爲 存在偏好設置 但不存在個人信息 的[emptyList]
 */
private fun preferencesCookieList(domain: String): List<Cookie> = listOf(
    Cookie(
        name = "user_lang",
        value = Preferences.videoLanguage,
        encoding = CookieEncoding.RAW,
        domain = domain,
    )
)
