package com.yenaly.han1meviewer.ui.screen.home.preview.getchupreview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageRequest
import com.yenaly.han1meviewer.DESKTOP_USER_AGENT
import com.yenaly.han1meviewer.currentLocalDate
import com.yenaly.han1meviewer.datetime.shiftYearMonthCode
import com.yenaly.han1meviewer.datetime.yearMonthCode
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import kotlinx.datetime.number

internal fun currentGetchuDateCode(): String {
    val now = currentLocalDate()
    return yearMonthCode(now.year, now.month.number)
}

internal fun shiftGetchuMonthCode(code: String, delta: Int): String {
    return shiftYearMonthCode(code, delta)
}

internal fun getchuDateLabel(code: String): String {
    return "${code.substring(0, 4)}/${code.substring(4, 6).toInt()}"
}

internal fun getchuMonthOptions(centerCode: String): List<String> {
    return (-12..12).map { delta -> shiftGetchuMonthCode(centerCode, delta) }
}

private val GetchuImageHeadersPlugin = createClientPlugin("GetchuImageHeadersPlugin") {
    onRequest { request, _ ->
        val url = request.url.build()
        if (url.host == "www.getchu.com" &&
            url.encodedPath.startsWith("/brandnew/")
        ) {
            request.headers[HttpHeaders.UserAgent] = DESKTOP_USER_AGENT
            request.headers[HttpHeaders.Referrer] = "https://www.getchu.com/"
            request.headers[HttpHeaders.Cookie] = "getchu_adalt_flag=getchu.com; gc=gc"
        }
    }
}

private val getchuImageHttpClient by lazy {
    HttpClient(OkHttp) {
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 10_000
        }
        install(GetchuImageHeadersPlugin)
        engine {
            config {
                dns(HDns())
                proxySelector(HProxySelector())
                followRedirects(true)
                followSslRedirects(true)
            }
        }
    }
}

@Composable
internal fun getchuImageRequest(url: String?): ImageRequest {
    val context = LocalContext.current
    return ImageRequest.Builder(context)
        .data(url)
        .build()
}

internal fun createGetchuImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(httpClient = { getchuImageHttpClient }))
        }
        .build()
}
