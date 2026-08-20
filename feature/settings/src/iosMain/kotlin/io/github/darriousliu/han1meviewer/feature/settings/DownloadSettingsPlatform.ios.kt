package io.github.darriousliu.han1meviewer.feature.settings

import androidx.compose.runtime.Composable

/**
 * iOS 隐藏下载路径选择(系统限制,固定 FileKit.filesDir)与「匯出下載記錄」
 * (迁移=移动文件+改库路径,用户定 iOS 不提供)。
 */
actual val downloadSettingsCapabilities = DownloadSettingsCapabilities()

@Composable
actual fun rememberDownloadSettingsActions(onLocationChanged: () -> Unit): DownloadSettingsActions =
    NoopDownloadSettingsActions
