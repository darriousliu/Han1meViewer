package io.github.darriousliu.han1meviewer.feature.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.darriousliu.han1meviewer.core.common.DownloadDefaults
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.confirm_import
import io.github.darriousliu.han1meviewer.core.resource.import_complete
import io.github.darriousliu.han1meviewer.core.resource.import_progress
import io.github.darriousliu.han1meviewer.core.resource.import_warning
import io.github.darriousliu.han1meviewer.core.resource.no_exportable_files
import io.github.darriousliu.han1meviewer.core.resource.ok
import io.github.darriousliu.han1meviewer.core.resource.path_permission_message
import io.github.darriousliu.han1meviewer.core.resource.permission_error
import io.github.darriousliu.han1meviewer.core.resource.restore_default_message
import io.github.darriousliu.han1meviewer.core.resource.restore_default_path
import io.github.darriousliu.han1meviewer.core.resource.select_download_folder
import io.github.darriousliu.han1meviewer.core.resource.select_folder_message
import io.github.darriousliu.han1meviewer.core.resource.specify_path_first
import io.github.darriousliu.han1meviewer.core.resource.understood
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.ui.component.ConfirmDialog
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.TripleButtonDialog
import io.github.darriousliu.han1meviewer.core.ui.component.showLong
import io.github.darriousliu.han1meviewer.feature.download.LocalDownloadTaskEngine
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun DownloadSettingsRouteScreen() {
    var refreshKey by remember { mutableIntStateOf(0) }
    val actions = rememberDownloadSettingsActions(onLocationChanged = { refreshKey++ })
    val capabilities = downloadSettingsCapabilities
    val engine = LocalDownloadTaskEngine.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()

    var showDownloadPathDialog by remember { mutableStateOf(false) }
    var showRestoreDefaultConfirm by remember { mutableStateOf(false) }
    var showMigrateConfirm by remember { mutableStateOf(false) }
    var showSpecifyPathFirst by remember { mutableStateOf(false) }
    /** 非空=迁移进行中,值为 (已迁移, 总数)。 */
    var migrateProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(Unit) { actions.ensureStoragePermission() }

    val uiState = buildDownloadSettingsUiState(actions, refreshKey)

    DownloadSettingsScreen(
        state = uiState,
        maxDownloadCountLimit = 10,
        maxDownloadSpeedLimitIndex = DownloadDefaults.SPEED_BYTES.lastIndex,
        onOpenDownloadPath = if (capabilities.chooseLocation) {
            { showDownloadPathDialog = true }
        } else {
            null
        },
        onRestoreDefaultPath = { },
        onImportDownloadedFiles = if (capabilities.migrateDownloads) {
            {
                if (actions.canMigrate()) {
                    showMigrateConfirm = true
                } else {
                    showSpecifyPathFirst = true
                }
            }
        } else {
            null
        },
        onDownloadCountLimitChange = { value ->
            Preferences.downloadCountLimit = value
            engine.setMaxConcurrent(value)
            refreshKey++
        },
        onDownloadSpeedLimitChange = { value ->
            Preferences.downloadSpeedLimitIndex = value
            refreshKey++
        },
    )

    // 选下载目录:默认路径状态下没有「恢复默认」中键
    if (!actions.isUsingDefaultLocation()) {
        TripleButtonDialog(
            visible = showDownloadPathDialog,
            title = stringResource(Res.string.select_download_folder),
            message = stringResource(Res.string.select_folder_message),
            negativeText = stringResource(Res.string.cancel),
            neutralText = stringResource(Res.string.restore_default_path),
            positiveText = stringResource(Res.string.ok),
            onNegative = { showDownloadPathDialog = false },
            onNeutral = {
                showDownloadPathDialog = false
                showRestoreDefaultConfirm = true
            },
            onPositive = {
                showDownloadPathDialog = false
                actions.chooseDownloadLocation()
            },
            onDismiss = { showDownloadPathDialog = false },
        )
    } else {
        ConfirmDialog(
            visible = showDownloadPathDialog,
            title = stringResource(Res.string.select_download_folder),
            message = stringResource(Res.string.select_folder_message),
            confirmText = stringResource(Res.string.ok),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = {
                showDownloadPathDialog = false
                actions.chooseDownloadLocation()
            },
            onDismiss = { showDownloadPathDialog = false },
        )
    }

    ConfirmDialog(
        visible = showRestoreDefaultConfirm,
        title = stringResource(Res.string.restore_default_path),
        message = stringResource(Res.string.restore_default_message),
        confirmText = stringResource(Res.string.ok),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = {
            actions.restoreDefaultLocation()
            refreshKey++
            showRestoreDefaultConfirm = false
        },
        onDismiss = { showRestoreDefaultConfirm = false },
    )

    ConfirmDialog(
        visible = showMigrateConfirm,
        title = stringResource(Res.string.confirm_import),
        message = stringResource(Res.string.import_warning),
        confirmText = stringResource(Res.string.ok),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = {
            showMigrateConfirm = false
            migrateProgress = 0 to 0
            actions.migrateDownloads(
                onProgress = { migrated, total -> migrateProgress = migrated to total },
                onFinished = { outcome ->
                    val finishedTotal = migrateProgress?.second ?: 0
                    migrateProgress = null
                    scope.launch {
                        when (outcome) {
                            MigrateOutcome.Success -> {
                                refreshKey++
                                toaster.showLong(
                                    getString(Res.string.import_complete, finishedTotal)
                                )
                            }

                            MigrateOutcome.NoFiles ->
                                toaster.showLong(getString(Res.string.no_exportable_files))

                            MigrateOutcome.PermissionError ->
                                toaster.showLong(getString(Res.string.permission_error))
                        }
                    }
                },
            )
        },
        onDismiss = { showMigrateConfirm = false },
    )

    if (showSpecifyPathFirst) {
        AlertDialog(
            onDismissRequest = { showSpecifyPathFirst = false },
            title = { Text(stringResource(Res.string.specify_path_first)) },
            text = { Text(stringResource(Res.string.path_permission_message)) },
            confirmButton = {
                TextButton(onClick = { showSpecifyPathFirst = false }) {
                    Text(stringResource(Res.string.understood))
                }
            },
        )
    }

    migrateProgress?.let { (migrated, total) ->
        val percent = if (total > 0) migrated * 100 / total else 0
        AlertDialog(
            onDismissRequest = { /* 迁移中不可取消 */ },
            title = { Text(stringResource(Res.string.import_progress)) },
            text = {
                androidx.compose.foundation.layout.Column {
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                    Text(
                        // import_progress_format 是 translatable=false 的固定模板
                        // `%1$d / %2$d（%3$d%%）`;CMP stringResource 不还原 %%,直接手拼
                        text = "$migrated / $total（$percent%）",
                    )
                }
            },
            confirmButton = {},
        )
    }
}

/**
 * @param refreshKey 只用来触发重算——`Preferences` 不是可观察状态,
 *   改完得靠它把这个 composable 拉一遍。
 */
@Composable
private fun buildDownloadSettingsUiState(
    actions: DownloadSettingsActions,
    refreshKey: Int,
): DownloadSettingsUiState {
    @Suppress("UNUSED_EXPRESSION") refreshKey
    val speedIndex = Preferences.downloadSpeedLimitIndex
    return DownloadSettingsUiState(
        downloadPathSummary = actions.downloadPathSummary(),
        downloadCountLimit = Preferences.downloadCountLimit,
        downloadCountLimitSummary = toDownloadCountLimitPrettyString(Preferences.downloadCountLimit),
        downloadSpeedLimitIndex = speedIndex,
        downloadSpeedLimitSummary = DownloadDefaults.SPEED_BYTES[speedIndex]
            .toDownloadSpeedPrettyString(),
    )
}
