package com.yenaly.han1meviewer.logic.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS 用 Darwin engine。
 *
 * androidJvmMain 那边挂在 OkHttp engine 上的能力（DoH/自定义 DNS、`ProxySelector`、
 * 磁盘缓存、限速、强制 HTTP/1.1）Darwin 都没有对应物，暂时不支持；
 * 真要补得走 `NSURLSessionConfiguration`（代理）和自己实现 DoH。
 */
internal actual fun createPlatformHttpClient(
    spec: HClientSpec,
    sharedConfig: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(Darwin) {
    sharedConfig()
}
