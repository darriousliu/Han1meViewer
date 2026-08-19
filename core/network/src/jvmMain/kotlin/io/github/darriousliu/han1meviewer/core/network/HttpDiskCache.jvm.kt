package io.github.darriousliu.han1meviewer.core.network

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import okhttp3.Cache
import java.io.File

/** 桌面端的磁盘缓存放在 FileKit 缓存目录下的 `http_cache`。 */
internal actual fun createHttpDiskCache(): Cache? =
    Cache(directory = File(FileKit.cacheDir.file, "http_cache"), maxSize = HTTP_CACHE_SIZE)
