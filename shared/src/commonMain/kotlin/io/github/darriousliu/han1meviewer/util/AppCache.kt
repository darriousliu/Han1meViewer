package io.github.darriousliu.han1meviewer.util

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * 应用缓存的大小与清理。
 *
 * 原来是 androidMain 的 `context.cacheDir` + `File.folderSize`，走
 * `HomeSettingsActions` 的 expect/actual。现在整个进 commonMain——
 * FileKit 的 [FileKit.cacheDir] 三端都有对应目录（Android 的 cacheDir、
 * JVM 的用户缓存目录、iOS 的沙盒 Caches），所以这一项**不需要平台能力抽象**，
 * 从 B 类降成 A 类。
 *
 * 递归求大小要自己写：FileKit 只给单个文件的 [size]，不给目录累加。
 */
suspend fun appCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
    FileKit.cacheDir.calculateSizeRecursively()
}

/**
 * 清空缓存。
 *
 * ⚠️ 与旧实现的差异：原来是 `cacheDir.deleteRecursively()`，**连缓存目录本身一起删**；
 * 现在只删目录**内容**、保留目录。后者更安全——目录被删掉之后有些库不会自己重建。
 *
 * @return 是否全部删成功（有任何一项失败都返回 false）
 */
suspend fun clearAppCache(): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        FileKit.cacheDir.list().forEach { it.delete() }
    }.isSuccess
}

/** 目录递归累加；单个文件直接取 [size]。 */
internal fun PlatformFile.calculateSizeRecursively(): Long =
    if (isDirectory()) list().sumOf { it.calculateSizeRecursively() } else size()
