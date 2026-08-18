package io.github.darriousliu.han1meviewer.logic.network.plugin

import co.touchlab.kermit.Logger
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.decodeURLPart

/**
 * 替代迁移前的 `UrlLoggingInterceptor`。
 *
 * 没有直接用 Ktor 自带的 `Logging` 插件：那个在 HEADERS/ALL 级别会把请求头一起打出来，
 * 而我们的请求头里带着登录 cookie。这里只打一行解码后的 URL，和旧行为一致。
 */
val UrlLogging = createClientPlugin("UrlLogging") {
    val logger = Logger.withTag("NetworkRequest")
    onRequest { request, _ ->
        logger.i { request.url.buildString().decodeURLPart() }
    }
}
