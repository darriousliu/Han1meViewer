package com.yenaly.han1meviewer.logic.network

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import okhttp3.Cache
import java.io.File

/** 桌面端暂时没有磁盘缓存（和迁移前不注入的行为一致）。 */
internal actual fun createHttpDiskCache(): Cache? =
    Cache(directory = File(FileKit.cacheDir.file, "http_cache"), maxSize = HTTP_CACHE_SIZE)
