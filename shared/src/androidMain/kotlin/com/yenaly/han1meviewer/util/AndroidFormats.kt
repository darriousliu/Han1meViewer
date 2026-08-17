package com.yenaly.han1meviewer.util

/*
 * 文件大小格式化已经拆到 commonMain 的 `FileSizeFormat.kt`，
 * 这里只剩两个真正离不开 Android/JVM 的：`folderSize` 要 java.io.File，
 * `decodeFromStringByBase64` 要 android.util.Base64。
 * 两者的调用点都在 settings 的 route 里，那些 route 不迁。
 */

import android.util.Base64
import java.io.File

val File?.folderSize: Long
    get() = this?.listFiles()?.sumOf { file ->
        if (file.isDirectory) file.folderSize else file.length()
    } ?: 0L

fun String.decodeFromStringByBase64(flag: Int = Base64.DEFAULT): String =
    String(Base64.decode(toByteArray(), flag))
