package io.github.darriousliu.han1meviewer.core.network.plugin

import co.touchlab.kermit.Logger
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 过 Cloudflare 盾的动作。检测和重发是平台无关的，只有「怎么让用户过人机验证」这一步不是：
 * Android 上是拉起 `CloudflareActivity` 用 WebView 跑一遍，iOS 以后可以接 WKWebView。
 */
fun interface CloudflareSolver {
    /** 挂起直到用户完成验证（或放弃）。 */
    suspend fun solve(url: String)
}

object CloudflareChallengeHandler {
    /**
     * 平台的过盾实现。为 null 时插件直接放行 403，行为等同于没装这个插件。
     * 原来是启动时往 var 里注入，废弃后改由 [platformCloudflareSolver] 提供。
     */
    val solver: CloudflareSolver? get() = platformCloudflareSolver()

    internal val mutex = Mutex()
}

/**
 * 各平台的 [CloudflareSolver]。Android 主进程拉起 WebView Activity；
 * 没有实现的平台（以及 Android 的下载 worker 等子进程）返回 null。
 */
internal expect fun platformCloudflareSolver(): CloudflareSolver?

private const val CF_MITIGATED_HEADER = "cf-mitigated"
private const val CF_CHALLENGE = "challenge"

/**
 * 替代迁移前的 `CloudflareInterceptor`。
 *
 * 逻辑一致：403 且 `cf-mitigated: challenge` → 让用户过盾 → 原样重发一次。
 *
 * ⚠️ 一处行为改进：旧实现用 `CountDownLatch.await()` **阻塞** okhttp 的工作线程等 WebView，
 * 现在是真正的挂起。另外加了把锁，多个并发请求同时撞到盾时只会拉起一次验证。
 */
val CloudflareChallenge = createClientPlugin("CloudflareChallenge") {
    val logger = Logger.withTag("CloudflareChallenge")

    on(Send) { request ->
        val call = proceed(request)
        if (!call.response.isCloudflareChallenge()) return@on call

        val solver = CloudflareChallengeHandler.solver
        if (solver == null) {
            logger.w { "hit Cloudflare challenge but no solver registered, giving up" }
            return@on call
        }

        val url = request.url.buildString()
        logger.i { "Cloudflare challenge on $url" }
        CloudflareChallengeHandler.mutex.withLock { solver.solve(url) }
        proceed(HttpRequestBuilder().takeFrom(request))
    }
}

private fun HttpResponse.isCloudflareChallenge(): Boolean =
    status == HttpStatusCode.Forbidden && headers[CF_MITIGATED_HEADER] == CF_CHALLENGE
