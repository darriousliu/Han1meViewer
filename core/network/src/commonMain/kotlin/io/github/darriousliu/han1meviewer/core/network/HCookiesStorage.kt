package io.github.darriousliu.han1meviewer.core.network

import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.network.HCookiesStorage.addCookie
import io.github.darriousliu.han1meviewer.core.network.toLoginCookieList
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 用於管理 Cookie，替代迁移前的 `HCookieJar`（okhttp `CookieJar`）。
 *
 * #issue-71: 我竟然栽倒在 Cookie 管理上好幾年了！你去看我以前的管理方式，
 * 是完全錯誤的，竟然還能維持應用正常運行，太離譜了！怪不得切換簡體繁體一直不起作用！
 *
 * ⚠️ 与 okhttp 版本的一处行为差异：okhttp 的 `saveFromResponse` 是拿整份响应 cookie
 * **整体替换**掉该 host 的旧记录，而 Ktor 的 [CookiesStorage] 是**逐条** [addCookie]，
 * 这里按 name 覆盖、其余保留。后者才是 cookie 的标准语义（服务端本来就不会每次都重发全量）。
 */
object HCookiesStorage : CookiesStorage {

    private val mutex = Mutex()

    /** host -> (cookie name -> cookie) */
    private val cookieMap = mutableMapOf<String, MutableMap<String, Cookie>>()

    override suspend fun get(requestUrl: Url): List<Cookie> {
        val host = requestUrl.host
        val cookies = mutableListOf<Cookie>()
        mutex.withLock { cookieMap[host]?.values?.let(cookies::addAll) }

        cookies += Preferences.loginCookieStateFlow.value.toLoginCookieList(host)
        cookies += Preferences.cloudFlareCookieStateFlow.value.toLoginCookieList(host)

        Logger.d(tag = "HCookiesStorage") { "get for $host: $cookies" }
        return cookies
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (cookie.name.isBlank()) return
        mutex.withLock {
            cookieMap.getOrPut(requestUrl.host) { mutableMapOf() }[cookie.name] = cookie
        }
    }

    /** 退出登入时调用，见 `logout()`。 */
    fun clear() {
        cookieMap.clear()
    }

    override fun close() = Unit
}
