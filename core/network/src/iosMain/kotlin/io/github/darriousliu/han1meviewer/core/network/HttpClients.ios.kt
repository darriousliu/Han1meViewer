package io.github.darriousliu.han1meviewer.core.network

import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.common.ProxyType
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.http.Url

/**
 * iOS 用 Darwin engine。
 *
 * androidJvmMain 那边挂在 OkHttp engine 上的能力里，**代理已经补上**（见下）；
 * 其余（DoH/自定义 DNS、磁盘缓存、限速、强制 HTTP/1.1）Darwin 没有对应物：
 * `NSURLSession` 不提供 DNS hook，**DoH 在这一端明确不做**，
 * 由 `HomeSettingsCapabilities` 把对应设置项藏掉，而不是留着让它运行时失效。
 */
internal actual fun createPlatformHttpClient(
    spec: HClientSpec,
    sharedConfig: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(Darwin) {
    sharedConfig()
    val proxyConfig = buildProxyConfigOrNull()
    if (proxyConfig != null) {
        engine { proxy = proxyConfig }
    }
}

/**
 * 用 Ktor 通用的 [ProxyBuilder]，不自己拼 `connectionProxyDictionary`。
 *
 * Ktor 3.5.2 的 Darwin engine 内部已经有 `setupProxy`，会把 engine config 的
 * `proxy` 映射成 `HTTPEnable/HTTPProxy/HTTPPort` 与 `SOCKSEnable/SOCKSProxy/SOCKSPort`
 * 写进 `NSURLSessionConfiguration.connectionProxyDictionary`——
 * 手写一遍只是把 Ktor 已经做过的事再做一次，还容易写错 key。
 *
 * `SYSTEM` / `DIRECT` 返回 null：`NSURLSession` 默认就跟随系统代理设置。
 *
 * ⚠️ 和 Android/JVM 的差别：那两端用的是**进程级** `ProxySelector`
 * （见 `HProxySelector`），因为代理还得覆盖 WebView（issue-39）和播放器的
 * `DefaultHttpDataSource`；iOS 没有这两个需求，per-client 就够。
 */
private fun buildProxyConfigOrNull(): ProxyConfig? {
    val host = Preferences.proxyIp
    val port = Preferences.proxyPort
    if (host.isBlank() || port !in 0..65535) return null
    return when (Preferences.proxyType) {
        ProxyType.HTTP -> ProxyBuilder.http(Url("http://$host:$port"))
        ProxyType.SOCKS -> ProxyBuilder.socks(host, port)
        else -> null
    }
}
