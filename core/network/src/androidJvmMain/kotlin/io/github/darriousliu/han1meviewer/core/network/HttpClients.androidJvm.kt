package io.github.darriousliu.han1meviewer.core.network

import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.network.interceptor.SpeedLimitInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.Cache
import okhttp3.CookieJar
import okhttp3.Protocol

/**
 * 这些能力 Ktor 没有对应物，只能留在 OkHttp engine 的 config 里：
 * DoH / 自定义 DNS、`java.net.ProxySelector`、磁盘缓存、okio `Throttler` 限速、强制 HTTP/1.1。
 *
 * 放在 androidJvmMain 而不是 androidMain，是为了 android 和 jvm 两个目标共用同一份源码。
 */

internal const val HTTP_CACHE_SIZE = 10L * 1024 * 1024
/**
 * HANIME client 的 OkHttp 磁盘缓存；返回 null 就没有磁盘缓存。
 *
 * 缓存目录要 `Context.cacheDir`（androidJvmMain 拿不到 Android 的 Context），
 * 且 OkHttp 的 DiskLruCache **不能多进程共用**——Android actual 只在主进程返回实例，
 * 下载 worker 等子进程和 jvm 侧都是 null。原来是 lambda 注入（只在主进程赋值来
 * 表达这条约束），废弃后由 actual 自己判断。
 */
internal expect fun createHttpDiskCache(): Cache?

object OkHttpNetworkConfig {

    /**
     * 共用一个实例：[HDns] 内部缓存了 DoH client 和自定义 hosts 的解析结果，
     * 每个 client 各建一个会白白多解析几次。迁移前 `ServiceCreator` 也是共用一个。
     */
    internal val dns by lazy { HDns() }
}

internal actual fun createPlatformHttpClient(
    spec: HClientSpec,
    sharedConfig: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(OkHttp) {
    sharedConfig()

    engine {
        config {
            // cookie 一律交给 Ktor 的 HttpCookies 插件（HCookiesStorage），
            // 别让 okhttp 再插一手。
            cookieJar(CookieJar.NO_COOKIES)

            when (spec) {
                HClientSpec.HANIME -> {
                    dns(OkHttpNetworkConfig.dns)
                    proxySelector(HProxySelector())
                    createHttpDiskCache()?.let(::cache)
                }

                HClientSpec.GETCHU -> {
                    dns(OkHttpNetworkConfig.dns)
                    proxySelector(HProxySelector())
                }

                HClientSpec.GITHUB -> {
                    dns(GitHubDns)
                }

                HClientSpec.DOWNLOAD -> {
                    dns(OkHttpNetworkConfig.dns)
                    protocols(listOf(Protocol.HTTP_1_1))
                }
            }
        }

        if (spec == HClientSpec.DOWNLOAD) {
            addInterceptor(SpeedLimitInterceptor(maxSpeed = Preferences.downloadSpeedLimit))
        }
    }
}
