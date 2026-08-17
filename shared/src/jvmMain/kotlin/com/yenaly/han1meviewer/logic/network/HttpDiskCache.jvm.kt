package com.yenaly.han1meviewer.logic.network

import okhttp3.Cache

/** 桌面端暂时没有磁盘缓存（和迁移前不注入的行为一致）。 */
internal actual fun createHttpDiskCache(): Cache? = null
