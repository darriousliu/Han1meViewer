package io.github.darriousliu.han1meviewer.feature.settings

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.dokar.sonner.ToasterState
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.default_path_restored
import io.github.darriousliu.han1meviewer.core.resource.directory_saved
import io.github.darriousliu.han1meviewer.core.resource.go_to_settings
import io.github.darriousliu.han1meviewer.core.resource.no_directory_selected
import io.github.darriousliu.han1meviewer.core.resource.permission_permanently_denied_title
import io.github.darriousliu.han1meviewer.core.resource.storage_permission_denied_toast
import io.github.darriousliu.han1meviewer.core.resource.storage_permission_settings_message
import io.github.darriousliu.han1meviewer.core.resource.unknown_error
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.storage.SafFileManager
import io.github.darriousliu.han1meviewer.core.storage.dao.DownloadDatabase
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.showShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

actual val downloadSettingsCapabilities = DownloadSettingsCapabilities(
    chooseLocation = true,
    migrateDownloads = true,
)

/** AlertDialog 等非 Compose 场景要用的、组合期解好的固定文案。 */
private class PermissionDialogStrings(
    val title: String,
    val message: String,
    val goToSettings: String,
    val cancel: String,
)

@Composable
actual fun rememberDownloadSettingsActions(onLocationChanged: () -> Unit): DownloadSettingsActions {
    val context = LocalContext.current
    val activity = checkNotNull(LocalActivity.current)
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val currentOnLocationChanged by rememberUpdatedState(onLocationChanged)
    val permissionStrings = PermissionDialogStrings(
        title = stringResource(Res.string.permission_permanently_denied_title),
        message = stringResource(Res.string.storage_permission_settings_message),
        goToSettings = stringResource(Res.string.go_to_settings),
        cancel = stringResource(Res.string.cancel),
    )
    val unknownError = stringResource(Res.string.unknown_error)

    val directoryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            SafFileManager.persistUriPermission(context, data)
            Preferences.isUsePrivateStorage = false
            scope.launch {
                toaster.showShort(getString(Res.string.directory_saved, data.toString()))
            }
            currentOnLocationChanged()
        } else {
            scope.launch { toaster.showShort(getString(Res.string.no_directory_selected)) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) return@rememberLauncherForActivityResult
        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (activity.shouldShowRequestPermissionRationale(permission)) {
            scope.launch {
                toaster.showShort(getString(Res.string.storage_permission_denied_toast))
            }
        } else {
            AlertDialog.Builder(context)
                .setTitle(permissionStrings.title)
                .setMessage(permissionStrings.message)
                .setPositiveButton(permissionStrings.goToSettings) { _, _ ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        this.data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
                .setNegativeButton(permissionStrings.cancel, null)
                .show()
        }
    }

    return remember(context, activity, toaster) {
        AndroidDownloadSettingsActions(
            context = context,
            toaster = toaster,
            scope = scope,
            unknownError = unknownError,
            directoryLauncher = directoryLauncher,
            permissionLauncher = permissionLauncher,
            onLocationChanged = { currentOnLocationChanged() },
        )
    }
}

private class AndroidDownloadSettingsActions(
    private val context: Context,
    private val toaster: ToasterState,
    private val scope: CoroutineScope,
    private val unknownError: String,
    private val directoryLauncher: ActivityResultLauncher<Intent>,
    private val permissionLauncher: ActivityResultLauncher<String>,
    private val onLocationChanged: () -> Unit,
) : DownloadSettingsActions {

    override fun downloadPathSummary(): String {
        if (Preferences.isUsePrivateStorage) {
            return context.getExternalFilesDir(null)?.absolutePath.orEmpty()
        }
        val uri = SafFileManager.getSavedUri() ?: return unknownError
        return DocumentFile.fromTreeUri(context, uri)?.name ?: uri.toString()
    }

    override fun isUsingDefaultLocation(): Boolean = Preferences.isUsePrivateStorage

    override fun chooseDownloadLocation() {
        directoryLauncher.launch(SafFileManager.buildOpenDirectoryIntent())
    }

    override fun restoreDefaultLocation() {
        Preferences.isUsePrivateStorage = true
        Preferences.safDownloadPath = null
        scope.launch { toaster.showShort(getString(Res.string.default_path_restored)) }
        onLocationChanged()
    }

    override fun canMigrate(): Boolean =
        !Preferences.isUsePrivateStorage &&
                !Preferences.safDownloadPath.isNullOrBlank() &&
                SafFileManager.checkSafPermissions(context)

    override fun migrateDownloads(
        onProgress: (Int, Int) -> Unit,
        onFinished: (MigrateOutcome) -> Unit,
    ) {
        SafFileManager.migratePrivateToSaf(
            context,
            DownloadDatabase.instance.hanimeDownloadDao,
        ) { migrated, total ->
            when (total) {
                0 -> onFinished(MigrateOutcome.NoFiles)
                -1 -> onFinished(MigrateOutcome.PermissionError)
                else -> {
                    onProgress(migrated, total)
                    if (migrated == total) onFinished(MigrateOutcome.Success)
                }
            }
        }
    }

    override fun ensureStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(permission)
            }
        }
    }
}
