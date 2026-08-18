package io.github.darriousliu.han1meviewer.core.network

import io.github.darriousliu.han1meviewer.core.common.util.applicationContext
import io.github.darriousliu.han1meviewer.core.common.util.isMainProcess
import okhttp3.Cache
import java.io.File

internal actual fun createHttpDiskCache(): Cache? {
    // DiskLruCache 带文件锁，多进程共用同一目录会损坏——子进程不用磁盘缓存
    if (!isMainProcess) return null
    return Cache(directory = File(applicationContext.cacheDir, "http_cache"), maxSize = HTTP_CACHE_SIZE)
}
