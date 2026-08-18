package io.github.darriousliu.han1meviewer.core.network

import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.core.common.util.copyTo
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import java.util.zip.ZipInputStream

private val logger = Logger.withTag(HUpdater.TAG)

/**
 * 把更新包下载到 [File]。
 *
 * 和 [HUpdater] 的检查逻辑不同，这一步留在 androidMain：产物是 APK，安装走
 * `Intent.ACTION_VIEW` + FileProvider，iOS 走 App Store 根本没有对应动作；
 * CI 渠道的 zip 解包也依赖 JVM 的 [ZipInputStream]。
 *
 * 迁移前走的是 Retrofit `@Streaming` 的 `githubService.request(url)`。
 * Ktor 这边必须用 `prepareGet {}.execute {}`——直接 `get()` 会把整个 APK
 * 先读进内存（响应体默认是要缓存下来的）。
 *
 * @param url update url
 */
suspend fun File.injectUpdate(
    url: String,
    progress: (suspend (Int, Long, Long) -> Unit)? = null,
) {
    HanimeNetwork.githubClient.prepareGet(url).execute { res ->
        val contentLength = res.contentLength() ?: -1L
        logger.i { "content length: $contentLength" }
        res.bodyAsChannel().toInputStream().use { stream ->
            if (url.endsWith("zip")) {
                logger.d { "Injecting update from zip ($url)" }
                ZipInputStream(stream).use { zip ->
                    zip.nextEntry
                    this.outputStream().use {
                        // 估摸着压缩率为0.56左右，稍微估算解压后大小，防止进度卡在100%时间过长
                        zip.copyTo(it, (contentLength * 1.79).toLong(), progress = progress)
                    }
                }
            } else {
                logger.d { "Injecting update from release ($url)" }
                this.outputStream().use {
                    stream.copyTo(it, contentLength, progress = progress)
                }
            }
        }
    }
}
