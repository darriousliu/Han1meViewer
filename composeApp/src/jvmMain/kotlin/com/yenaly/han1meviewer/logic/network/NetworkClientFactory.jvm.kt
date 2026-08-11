package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.logic.network.cookie.CookiePair
import com.yenaly.han1meviewer.logic.network.cookie.MainSiteCookieRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

internal actual fun createPlatformNetworkClient(profile: NetworkClientProfile): HttpClient =
    HttpClient(OkHttp) {
        configureCommonNetworkClient(
            profile = profile,
            // Match the existing JVM-family transport by delegating redirects to OkHttp.
            followRedirectsInClient = false,
            // OkHttp must see cookies from intermediate redirect responses.
            installPortableSiteCookies = false,
        )
        engine {
            config {
                cookieJar(
                    if (profile == NetworkClientProfile.Site) {
                        MainSiteOkHttpCookieJar
                    } else {
                        CookieJar.NO_COOKIES
                    }
                )
                // OkHttpConfig installs false defaults before this block. Keep these last.
                followRedirects(true)
                followSslRedirects(true)
            }
        }
    }

private object MainSiteOkHttpCookieJar : CookieJar {
    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        MainSiteCookieRepository.cookiePairsFor(url.host).mapNotNull { pair ->
            pair.toOkHttpCookie(url.host)
        }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        MainSiteCookieRepository.replaceParsedFromResponse(
            host = url.host,
            responseCookies = cookies.map { cookie ->
                CookiePair(name = cookie.name, value = cookie.value)
            },
        )
    }
}

private fun CookiePair.toOkHttpCookie(host: String): Cookie? = runCatching {
    Cookie.Builder()
        .domain(host)
        .name(name)
        .value(value)
        .build()
}.getOrNull()
