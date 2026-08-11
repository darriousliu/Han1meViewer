package com.yenaly.han1meviewer.logic.network

import com.yenaly.han1meviewer.logic.network.interceptor.CloudflareInterceptor
import com.yenaly.han1meviewer.logic.network.cookie.CookiePair
import com.yenaly.han1meviewer.logic.network.cookie.MainSiteCookieRepository
import com.yenaly.yenaly_libs.utils.applicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cache
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.File

internal actual fun createPlatformNetworkClient(profile: NetworkClientProfile): HttpClient =
    HttpClient(OkHttp) {
        configureCommonNetworkClient(
            profile = profile,
            // Retrofit delegated all redirects, including POST redirects, to OkHttp.
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
                when (profile) {
                    NetworkClientProfile.Site -> {
                        addInterceptor(CloudflareInterceptor(applicationContext))
                        cache(AndroidNetworkPlatformResources.cache)
                        proxySelector(HProxySelector())
                        dns(AndroidNetworkPlatformResources.dns)
                    }

                    NetworkClientProfile.Getchu -> {
                        proxySelector(HProxySelector())
                        dns(AndroidNetworkPlatformResources.dns)
                    }

                    NetworkClientProfile.GitHub -> {
                        dns(GitHubDns)
                    }
                }

                // OkHttpConfig installs false defaults before this block. Keep these last.
                followRedirects(true)
                followSslRedirects(true)
            }
        }
    }

internal object AndroidNetworkPlatformResources {
    val cache: Cache by lazy {
        Cache(
            directory = File(applicationContext.cacheDir, "http_cache"),
            maxSize = 10L * 1024L * 1024L,
        )
    }

    val dns: HDns by lazy(::HDns)
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
