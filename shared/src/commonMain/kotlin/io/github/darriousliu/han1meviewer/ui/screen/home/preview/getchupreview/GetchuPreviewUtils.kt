package io.github.darriousliu.han1meviewer.ui.screen.home.preview.getchupreview

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageRequest
import io.github.darriousliu.han1meviewer.core.network.HanimeNetwork
import io.github.darriousliu.han1meviewer.core.common.util.currentLocalDate

internal fun currentGetchuDateCode(): String {
    val now = currentLocalDate()
    return formatGetchuMonthCode(now.year, now.monthNumber)
}

internal fun shiftGetchuMonthCode(code: String, delta: Int): String {
    var year = code.substring(0, 4).toInt()
    var month = code.substring(4, 6).toInt() + delta
    while (month < 1) {
        month += 12
        year -= 1
    }
    while (month > 12) {
        month -= 12
        year += 1
    }
    return formatGetchuMonthCode(year, month)
}

/** common stdlib 没有 `String.format`，自己拼。 */
private fun formatGetchuMonthCode(year: Int, month: Int): String =
    year.toString().padStart(4, '0') + month.toString().padStart(2, '0')

internal fun getchuDateLabel(code: String): String {
    return "${code.substring(0, 4)}/${code.substring(4, 6).toInt()}"
}

internal fun getchuMonthOptions(centerCode: String): List<String> {
    return (-12..12).map { delta -> shiftGetchuMonthCode(centerCode, delta) }
}

@Composable
internal fun getchuImageRequest(url: String?): ImageRequest {
    val context = LocalPlatformContext.current
    return ImageRequest.Builder(context)
        .data(url)
        .build()
}

/**
 * getchu 的图片要带 Referer/Cookie 才取得到，而 HClientSpec.GETCHU 已经通过
 * `defaultRequest` 配好了那三个头，直接复用，不用再写一遍 OkHttp 拦截器。
 *
 * 传 lambda 而不是 client 实例：用户改代理/DNS 会触发
 * [HanimeNetwork.rebuildNetwork]，这样能自动用上重建后的 client。
 */
internal fun createGetchuImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory(httpClient = { HanimeNetwork.getchuClient })) }
        .build()
