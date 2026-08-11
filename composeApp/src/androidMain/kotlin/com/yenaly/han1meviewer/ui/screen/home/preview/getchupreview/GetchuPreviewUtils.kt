package com.yenaly.han1meviewer.ui.screen.home.preview.getchupreview

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ImageRequest
import com.yenaly.han1meviewer.DESKTOP_USER_AGENT
import com.yenaly.han1meviewer.currentLocalDate
import com.yenaly.han1meviewer.datetime.shiftYearMonthCode
import com.yenaly.han1meviewer.datetime.yearMonthCode
import com.yenaly.han1meviewer.logic.network.HDns
import com.yenaly.han1meviewer.logic.network.HProxySelector
import kotlinx.datetime.number
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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

@Composable
internal fun getchuImageRequest(url: String?): ImageRequest {
    val context = LocalContext.current
    return ImageRequest.Builder(context)
        .data(url)
        .build()
}

internal fun createGetchuImageLoader(context: Context): ImageLoader {
    val imageClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .dns(HDns())
        .proxySelector(HProxySelector())
        .addInterceptor { chain ->
            val request = chain.request()
            val url = request.url
            val builder = request.newBuilder()
            if (url.host == "www.getchu.com" && url.encodedPath.startsWith("/brandnew/")) {
                builder
                    .header("User-Agent", DESKTOP_USER_AGENT)
                    .header("Referer", "https://www.getchu.com/")
                    .header("Cookie", "getchu_adalt_flag=getchu.com; gc=gc")
            }
            chain.proceed(builder.build())
        }
        .build()
    return ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { imageClient }))
        }
        .build()
}
