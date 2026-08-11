package com.yenaly.han1meviewer.ui.navigation.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.network.interceptor.SpeedLimitInterceptor
import com.yenaly.han1meviewer.platform.FileAccess
import com.yenaly.han1meviewer.platform.PlatformActionResult
import com.yenaly.han1meviewer.platform.backgroundJobScheduler
import com.yenaly.han1meviewer.platform.fileAccess
import com.yenaly.han1meviewer.platform.getOrThrow
import com.yenaly.han1meviewer.ui.activity.AndroidMainActivity
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.component.TripleButtonDialog
import com.yenaly.han1meviewer.ui.screen.settings.DownloadSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.DownloadSettingsUiState
import com.yenaly.han1meviewer.util.showToast
import kotlinx.coroutines.launch

private const val DOWNLOAD_COUNT_LIMIT = "download_count_limit"
private const val DOWNLOAD_SPEED_LIMIT = "download_speed_limit"
@Composable
fun DownloadSettingsRouteScreen(
    activity: AndroidMainActivity,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var showDownloadPathDialog by remember { mutableStateOf(false) }
    var showRestoreDefaultConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val files = remember { fileAccess() }
    val backgroundJobs = remember { backgroundJobScheduler() }
    val uiState = remember(refreshKey, context) { buildDownloadSettingsUiState(context, files) }
    val chooseDownloadDirectory = {
        scope.launch {
            when (val result = files.chooseDownloadDirectory()) {
                is PlatformActionResult.Success -> {
                    context.showToast(R.string.directory_saved, result.value.confirmationText)
                    refreshKey++
                }

                PlatformActionResult.Cancelled -> {
                    context.showToast(R.string.no_directory_selected)
                }

                else -> result.getOrThrow()
            }
        }
    }

    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) return@rememberLauncherForActivityResult
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (activity.shouldShowRequestPermissionRationale(permission)) {
            context.showToast(R.string.storage_permission_denied_toast)
        } else {
            AlertDialog.Builder(context)
                .setTitle(R.string.permission_permanently_denied_title)
                .setMessage(R.string.storage_permission_settings_message)
                .setPositiveButton(R.string.go_to_settings) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermission.launch(permission)
            }
        }
    }

    DownloadSettingsScreen(
        state = uiState,
        maxDownloadCountLimit = 10,
        maxDownloadSpeedLimitIndex = SpeedLimitInterceptor.SPEED_BYTES.lastIndex,
        onOpenDownloadPath = { showDownloadPathDialog = true },
        onRestoreDefaultPath = { },
        onImportDownloadedFiles = {
            importDownloadedFiles(context, activity, onCompleted = { refreshKey++ })
        },
        onDownloadCountLimitChange = { value ->
            Preferences.editSettings { putInt(DOWNLOAD_COUNT_LIMIT, value) }
            backgroundJobs.setMaxConcurrentDownloadCount(value).getOrThrow()
            refreshKey++
        },
        onDownloadSpeedLimitChange = { value ->
            Preferences.editSettings { putInt(DOWNLOAD_SPEED_LIMIT, value) }
            refreshKey++
        },
    )

    if (!Preferences.isUsePrivateStorage) {
        TripleButtonDialog(
            visible = showDownloadPathDialog,
            title = stringResource(R.string.select_download_folder),
            message = stringResource(R.string.select_folder_message),
            negativeText = stringResource(R.string.cancel),
            neutralText = stringResource(R.string.restore_default_path),
            positiveText = stringResource(R.string.ok),
            onNegative = { showDownloadPathDialog = false },
            onNeutral = {
                showDownloadPathDialog = false
                showRestoreDefaultConfirm = true
            },
            onPositive = {
                showDownloadPathDialog = false
                chooseDownloadDirectory()
            },
            onDismiss = { showDownloadPathDialog = false },
        )
    } else {
        ConfirmDialog(
            visible = showDownloadPathDialog,
            title = stringResource(R.string.select_download_folder),
            message = stringResource(R.string.select_folder_message),
            confirmText = stringResource(R.string.ok),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                showDownloadPathDialog = false
                chooseDownloadDirectory()
            },
            onDismiss = { showDownloadPathDialog = false },
        )
    }

    ConfirmDialog(
        visible = showRestoreDefaultConfirm,
        title = stringResource(R.string.restore_default_path),
        message = stringResource(R.string.restore_default_message),
        confirmText = stringResource(R.string.ok),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            files.restoreDefaultDownloadDirectory().getOrThrow()
            refreshKey++
            showRestoreDefaultConfirm = false
            context.showToast(R.string.default_path_restored)
        },
        onDismiss = { showRestoreDefaultConfirm = false },
    )
}

private fun buildDownloadSettingsUiState(
    context: Context,
    files: FileAccess,
): DownloadSettingsUiState {
    val pathSummary = if (Preferences.isUsePrivateStorage) {
        files.privateDownloadDirectoryDisplayName().getOrThrow()
    } else {
        files.selectedDownloadDirectoryDisplayName().getOrThrow()
            ?: context.getString(R.string.unknown_error)
    }
    val speedIndex = Preferences.getIntSetting(
        DOWNLOAD_SPEED_LIMIT,
        SpeedLimitInterceptor.NO_LIMIT_INDEX,
    )
    return DownloadSettingsUiState(
        downloadPathSummary = pathSummary,
        downloadCountLimit = Preferences.downloadCountLimit,
        downloadCountLimitSummary = toDownloadCountLimitPrettyString(
            context,
            Preferences.downloadCountLimit
        ),
        downloadSpeedLimitIndex = speedIndex,
        downloadSpeedLimitSummary = SpeedLimitInterceptor.SPEED_BYTES[speedIndex]
            .toDownloadSpeedPrettyString(context),
    )
}
