package io.github.darriousliu.han1meviewer.core.network.interceptor

import io.github.darriousliu.han1meviewer.core.common.DownloadDefaults
import okhttp3.Interceptor
import okhttp3.Response
import io.github.darriousliu.han1meviewer.core.storage.Preferences

class SpeedLimitInterceptor(var maxSpeed: Long) : Interceptor {

    companion object {
        // commonMain `DownloadDefaults` 的别名，现有调用点不用改。
        const val NO_LIMIT = DownloadDefaults.NO_LIMIT

        const val NO_LIMIT_INDEX = DownloadDefaults.NO_LIMIT_INDEX

        val SPEED_BYTES = DownloadDefaults.SPEED_BYTES
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body
        return response.newBuilder()
            .body(SpeedLimitResponseBody(body, maxSpeed))
            .build()
    }
}
