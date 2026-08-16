package com.yenaly.han1meviewer.logic.network.interceptor

import com.yenaly.han1meviewer.DownloadDefaults
import okhttp3.Interceptor
import okhttp3.Response

class SpeedLimitInterceptor(var maxSpeed: Long) : Interceptor {

    companion object {
        // 真正的定义已抽到 commonMain 的 DownloadDefaults（Preferences 上移 commonMain 需要），
        // 这里保留别名，现有调用点不用改。
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
