package io.github.darriousliu.han1meviewer.feature.download

import androidx.compose.runtime.Composable

/** 本平台还没有下载文件操作(引擎/沙盒管理随下载体系落地)。 */
actual val downloadCapabilities = DownloadCapabilities()

@Composable
actual fun rememberDownloadPlatformActions(): DownloadPlatformActions =
    NoopDownloadPlatformActions
