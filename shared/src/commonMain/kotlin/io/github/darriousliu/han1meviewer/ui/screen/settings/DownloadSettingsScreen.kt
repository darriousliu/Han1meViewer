package io.github.darriousliu.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.darriousliu.han1meviewer.core.ui.component.SettingNavigationItem
import io.github.darriousliu.han1meviewer.core.ui.component.SettingSliderItem
import io.github.darriousliu.han1meviewer.core.ui.component.lazy.LazyColumn
import io.github.darriousliu.han1meviewer.core.ui.preview.ComponentPreview
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.baseline_count_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_export_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_path_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_speed2_24
import io.github.darriousliu.han1meviewer.core.resource.download_count_limit
import io.github.darriousliu.han1meviewer.core.resource.download_path
import io.github.darriousliu.han1meviewer.core.resource.download_speed_limit
import io.github.darriousliu.han1meviewer.core.resource.pref_export_downloads_summary
import io.github.darriousliu.han1meviewer.core.resource.pref_export_downloads_title
import org.jetbrains.compose.resources.stringResource

data class DownloadSettingsUiState(
    val downloadPathSummary: String,
    val downloadCountLimit: Int,
    val downloadCountLimitSummary: String,
    val downloadSpeedLimitIndex: Int,
    val downloadSpeedLimitSummary: String,
)

@Composable
fun DownloadSettingsScreen(
    state: DownloadSettingsUiState,
    maxDownloadCountLimit: Int,
    maxDownloadSpeedLimitIndex: Int,
    onOpenDownloadPath: () -> Unit,
    onRestoreDefaultPath: () -> Unit,
    onImportDownloadedFiles: () -> Unit,
    onDownloadCountLimitChange: (Int) -> Unit,
    onDownloadSpeedLimitChange: (Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.download_path),
                summary = state.downloadPathSummary,
                iconRes = Res.drawable.baseline_path_24,
                onClick = onOpenDownloadPath,
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(Res.string.pref_export_downloads_title),
                summary = stringResource(Res.string.pref_export_downloads_summary),
                iconRes = Res.drawable.baseline_export_24,
                onClick = onImportDownloadedFiles,
            )
        }

        item {
            SettingSliderItem(
                title = stringResource(Res.string.download_count_limit),
                summary = state.downloadCountLimitSummary,
                value = state.downloadCountLimit,
                valueRange = 0..maxDownloadCountLimit,
                iconRes = Res.drawable.baseline_count_24,
                onValueChange = onDownloadCountLimitChange,
            )
        }

        item {
            SettingSliderItem(
                title = stringResource(Res.string.download_speed_limit),
                summary = state.downloadSpeedLimitSummary,
                value = state.downloadSpeedLimitIndex,
                valueRange = 0..maxDownloadSpeedLimitIndex,
                iconRes = Res.drawable.baseline_speed2_24,
                onValueChange = onDownloadSpeedLimitChange,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadSettingsScreenPreview() {
    ComponentPreview {
        DownloadSettingsScreen(
            state = DownloadSettingsUiState(
                downloadPathSummary = "/storage/emulated/0/Android/data/.../files",
                downloadCountLimit = 2,
                downloadCountLimitSummary = "2",
                downloadSpeedLimitIndex = 0,
                downloadSpeedLimitSummary = "无限制",
            ),
            maxDownloadCountLimit = 10,
            maxDownloadSpeedLimitIndex = 5,
            onOpenDownloadPath = {},
            onRestoreDefaultPath = {},
            onImportDownloadedFiles = {},
            onDownloadCountLimitChange = {},
            onDownloadSpeedLimitChange = {},
        )
    }
}
