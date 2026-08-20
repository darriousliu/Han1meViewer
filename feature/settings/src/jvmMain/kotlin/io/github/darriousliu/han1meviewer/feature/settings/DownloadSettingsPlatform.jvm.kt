package io.github.darriousliu.han1meviewer.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.filesDir
import io.github.darriousliu.han1meviewer.core.storage.Preferences

/** 桌面端可选路径;迁移功能随桌面下载引擎落地后开(目标与 Android 一致)。 */
actual val downloadSettingsCapabilities = DownloadSettingsCapabilities(
    chooseLocation = true,
)

@Composable
actual fun rememberDownloadSettingsActions(onLocationChanged: () -> Unit): DownloadSettingsActions {
    val currentOnLocationChanged by rememberUpdatedState(onLocationChanged)
    val directoryLauncher = rememberDirectoryPickerLauncher { directory ->
        if (directory != null) {
            Preferences.safDownloadPath = directory.absolutePath()
            Preferences.isUsePrivateStorage = false
            currentOnLocationChanged()
        }
    }
    return remember {
        object : DownloadSettingsActions {
            override fun downloadPathSummary(): String {
                val custom = Preferences.safDownloadPath
                return if (Preferences.isUsePrivateStorage || custom.isNullOrBlank()) {
                    FileKit.filesDir.absolutePath()
                } else {
                    custom
                }
            }

            override fun isUsingDefaultLocation(): Boolean =
                Preferences.isUsePrivateStorage || Preferences.safDownloadPath.isNullOrBlank()

            override fun chooseDownloadLocation() {
                directoryLauncher.launch()
            }

            override fun restoreDefaultLocation() {
                Preferences.isUsePrivateStorage = true
                Preferences.safDownloadPath = null
                currentOnLocationChanged()
            }
        }
    }
}
