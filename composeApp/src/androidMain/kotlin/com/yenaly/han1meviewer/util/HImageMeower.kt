package com.yenaly.han1meviewer.util

import android.util.Log
import coil3.ImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.request.ImageResult
import com.yenaly.yenaly_libs.utils.applicationContext
import com.yenaly.han1meviewer.logic.network.HDns
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig

@Suppress("NOTHING_TO_INLINE")
object HImageMeower {

    private const val TAG = "CoilImageNyanner"

    private val httpClient = HttpClient(OkHttp) {
        followRedirects = false
        install(HttpTimeout) {
            requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 10_000
        }
        engine {
            config {
                dns(HDns())
                followRedirects(true)
                followSslRedirects(true)
            }
        }
    }

    private val imageLoader = ImageLoader.Builder(applicationContext)
        .components {
            add(KtorNetworkFetcherFactory(httpClient = { httpClient }))
        }
        .build()

    suspend fun execute(data: Any): ImageResult {
        Log.d(TAG, "execute: $data")
        return imageLoader.execute(
            ImageRequest.Builder(applicationContext).data(data).build()
        )
    }

    inline fun placeholder(height: Int, width: Int, blur: Int = 8) =
        "https://picsum.photos/$width/$height/?blur=$blur"
}
