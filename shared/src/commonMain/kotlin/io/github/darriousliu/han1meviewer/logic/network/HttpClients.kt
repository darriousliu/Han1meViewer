package io.github.darriousliu.han1meviewer.logic.network

import io.github.darriousliu.han1meviewer.BuildConfig
import io.github.darriousliu.han1meviewer.DESKTOP_USER_AGENT
import io.github.darriousliu.han1meviewer.USER_AGENT
import io.github.darriousliu.han1meviewer.logic.network.plugin.CloudflareChallenge
import io.github.darriousliu.han1meviewer.logic.network.plugin.UrlLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

/**
 * 四个用途不同的 client。迁移前对应 `ServiceCreator` 里的四个 `OkHttpClient`。
 */
enum class HClientSpec(val connectTimeoutMillis: Long) {
    /** 主站。带 cookie、Cloudflare 过盾、磁盘缓存、代理。 */
    HANIME(15_000L),

    /** GitHub API。走固定 DNS + Bearer token，不带 cookie 不走代理。 */
    GITHUB(15_000L),

    /** getchu。固定一组请求头，不带 cookie。 */
    GETCHU(15_000L),

    /** 视频下载。HTTP/1.1 + 限速，不带 cookie 不走缓存。 */
    DOWNLOAD(5_000L),
}

/**
 * 由平台侧提供 engine。
 *
 * OkHttp 那些没有 Ktor 对应物的能力（DoH/自定义 DNS、`ProxySelector`、磁盘缓存、
 * okio `Throttler` 限速、强制 HTTP/1.1）都在 androidJvmMain 的 actual 里，
 * 以 engine config 的形式保留；iosMain 用 Darwin，这些能力暂时没有。
 *
 * 注意是由平台侧**创建** client 而不是只返回 engine——这样 client 拥有 engine，
 * [HanimeNetwork.rebuildNetwork] 里 `close()` 能真正把连接池释放掉。
 */
internal expect fun createPlatformHttpClient(
    spec: HClientSpec,
    sharedConfig: HttpClientConfig<*>.() -> Unit,
): HttpClient

internal fun buildHttpClient(spec: HClientSpec): HttpClient = createPlatformHttpClient(spec) {
    // 迁移前是 Retrofit + okhttp，非 2xx 不抛异常、由调用方自己看 code。保持一致。
    expectSuccess = false

    install(HttpTimeout) {
        connectTimeoutMillis = spec.connectTimeoutMillis
    }
    install(UrlLogging)

    when (spec) {
        HClientSpec.HANIME -> {
            defaultRequest { header(HttpHeaders.UserAgent, USER_AGENT) }
            install(HttpCookies) { storage = HCookiesStorage }
            install(CloudflareChallenge)
        }

        HClientSpec.GITHUB -> {
            defaultRequest {
                header(HttpHeaders.UserAgent, USER_AGENT)
                bearerAuth(BuildConfig.HA_GITHUB_TOKEN)
            }
        }

        HClientSpec.GETCHU -> {
            defaultRequest {
                header(HttpHeaders.UserAgent, DESKTOP_USER_AGENT)
                header(HttpHeaders.Referrer, "https://www.getchu.com/")
                header(HttpHeaders.Cookie, "getchu_adalt_flag=getchu.com; gc=gc")
                header(
                    HttpHeaders.Accept,
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                )
                header(HttpHeaders.AcceptLanguage, "ja,en-US;q=0.9,en;q=0.8")
                header(HttpHeaders.CacheControl, "no-cache")
            }
        }

        HClientSpec.DOWNLOAD -> {
            defaultRequest { header(HttpHeaders.UserAgent, USER_AGENT) }
        }
    }
}
