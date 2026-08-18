package io.github.darriousliu.han1meviewer

/**
 * 下载相关默认值。原本散在 androidMain 的 `SpeedLimitInterceptor` 和
 * `HanimeDownloadManagerV2` 里，为了让 [Preferences] 能进 commonMain 抽了出来。
 */
object DownloadDefaults {

    const val NO_LIMIT = 0L

    const val NO_LIMIT_INDEX = 0

    /**
     * 限速档位对应的字节数，索引即 `Preferences.downloadSpeedLimitIndex` 存的值。
     */
    val SPEED_BYTES = longArrayOf(
        /* 不限速 */ NO_LIMIT,
        128 * 1024L, 256 * 1024L, 512 * 1024L,
        1024 * 1024L, 2048 * 1024L, 4096 * 1024L,
        8192 * 1024L, 10240 * 1024L,
    )

    const val MAX_CONCURRENT_DOWNLOAD_DEF = 2
}
