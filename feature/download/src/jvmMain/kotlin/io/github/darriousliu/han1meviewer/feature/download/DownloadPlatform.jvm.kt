package io.github.darriousliu.han1meviewer.feature.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Desktop
import java.io.File

/** 目录扫描导入依赖 SAF,桌面端待下载引擎落地后另做;外部播放用系统默认程序。 */
actual val downloadCapabilities = DownloadCapabilities(
    externalPlayback = true,
)

@Composable
actual fun rememberDownloadPlatformActions(): DownloadPlatformActions =
    remember { JvmDownloadPlatformActions }

private object JvmDownloadPlatformActions : DownloadPlatformActions {

    override fun playExternally(videoUri: String, onFileNotFound: () -> Unit) {
        val file = File(videoUri.removePrefix("file://"))
        if (!file.exists() || !Desktop.isDesktopSupported()) {
            onFileNotFound()
            return
        }
        runCatching { Desktop.getDesktop().open(file) }.onFailure { onFileNotFound() }
    }

    override fun deleteDownloadedFiles(videoCode: String) {
        // 桌面端下载引擎未落地,库里也不会有本地文件记录;有记录时按路径删
    }
}
