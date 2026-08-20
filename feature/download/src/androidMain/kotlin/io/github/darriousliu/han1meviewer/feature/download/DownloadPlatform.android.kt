package io.github.darriousliu.han1meviewer.feature.download

import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.dokar.sonner.ToasterState
import io.github.darriousliu.han1meviewer.core.common.FILE_PROVIDER_AUTHORITY
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.action_not_support
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.storage.SafFileManager
import io.github.darriousliu.han1meviewer.core.storage.dao.DownloadDatabase
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.showShort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.io.File

private val logger = Logger.withTag("DownloadPlatform")

actual val downloadCapabilities = DownloadCapabilities(
    externalPlayback = true,
    importFromDirectory = true,
)

@Composable
actual fun rememberDownloadPlatformActions(): DownloadPlatformActions {
    // 外部播放要拉起 Activity,须用组合处的(Activity)Context,不能换 applicationContext
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    return remember(context, toaster, scope) {
        AndroidDownloadPlatformActions(context, toaster, scope)
    }
}

private class AndroidDownloadPlatformActions(
    private val context: Context,
    private val toaster: ToasterState,
    private val scope: CoroutineScope,
) : DownloadPlatformActions {

    override fun deleteDownloadedFiles(videoCode: String) {
        SafFileManager.deleteDownloadVideoFolder(context, videoCode)
    }

    override fun playExternally(videoUri: String, onFileNotFound: () -> Unit) {
        val uri = videoUri.toUri()
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    if (pfd.statSize <= 0) {
                        onFileNotFound()
                        return
                    }
                } ?: run {
                    onFileNotFound()
                    return
                }
            } catch (_: Exception) {
                onFileNotFound()
                return
            }
            startViewIntent(uri)
        } else {
            val videoFile = File(uri.path ?: "")
            if (!videoFile.exists()) {
                onFileNotFound()
                return
            }
            startViewIntent(FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, videoFile))
        }
    }

    private fun startViewIntent(uri: android.net.Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            logger.w(e) { "no activity for external playback" }
            scope.launch { toaster.showShort(getString(Res.string.action_not_support)) }
        }
    }

    override fun canImportFromDirectory(): Boolean =
        !Preferences.safDownloadPath.isNullOrBlank() && !Preferences.isUsePrivateStorage

    override suspend fun importFromDirectory(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!SafFileManager.checkSafPermissions(context)) {
                false
            } else {
                SafFileManager.scanAndImportHanimeDownloads(
                    context,
                    DownloadDatabase.instance.hanimeDownloadDao,
                )
                true
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to import downloaded videos" }
            false
        }
    }
}
