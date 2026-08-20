package io.github.darriousliu.han1meviewer.feature.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.darriousliu.han1meviewer.core.storage.entity.download.HanimeDownloadEntity

/**
 * 下载页的平台能力可见性。没有该能力的平台对应入口整个不渲染。
 */
data class DownloadCapabilities(
    /** 外部播放器打开已下载文件(android=Intent;jvm=Desktop.open)。 */
    val externalPlayback: Boolean = false,
    /** 「读取下载目录」扫描导入(依赖 SAF 自定义目录,Android 专属)。 */
    val importFromDirectory: Boolean = false,
)

expect val downloadCapabilities: DownloadCapabilities

/**
 * 下载页的平台文件操作(SAF 删除/目录扫描/外部播放)。
 * 下载**任务**的暂停/恢复走 [LocalDownloadTaskEngine]——引擎目前在宿主层,分开注入。
 */
interface DownloadPlatformActions {

    /** 删除该视频的本地下载文件(夹);数据库记录由调用方处理。 */
    fun deleteDownloadedFiles(videoCode: String) {}

    /** 外部播放器打开;文件缺失或平台没有该能力时回调 [onFileNotFound]。 */
    fun playExternally(videoUri: String, onFileNotFound: () -> Unit) {
        onFileNotFound()
    }

    /** 「读取下载目录」前置是否满足(已选自定义目录且权限有效)。 */
    fun canImportFromDirectory(): Boolean = false

    /** 扫描下载目录并导入数据库;返回是否成功。 */
    suspend fun importFromDirectory(): Boolean = false
}

object NoopDownloadPlatformActions : DownloadPlatformActions

@Composable
expect fun rememberDownloadPlatformActions(): DownloadPlatformActions

/**
 * 下载任务引擎的控制面。引擎实现(WorkManager 队列)在宿主层,
 * 由平台入口经 CompositionLocal 注入;没有引擎的平台用 [NoopDownloadTaskEngine]。
 */
interface DownloadTaskEngine {
    fun pause(entity: HanimeDownloadEntity) {}
    fun resume(entity: HanimeDownloadEntity) {}
    fun delete(entity: HanimeDownloadEntity) {}

    /** 同时下载数上限变化(设置页写偏好后同步引擎)。 */
    fun setMaxConcurrent(count: Int) {}
}

object NoopDownloadTaskEngine : DownloadTaskEngine

val LocalDownloadTaskEngine = staticCompositionLocalOf<DownloadTaskEngine> {
    NoopDownloadTaskEngine
}
