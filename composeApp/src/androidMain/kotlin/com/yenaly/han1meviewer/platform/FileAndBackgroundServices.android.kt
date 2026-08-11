package com.yenaly.han1meviewer.platform

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.work.WorkManager
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.logic.dao.DownloadDatabase
import com.yenaly.han1meviewer.logic.entity.download.HanimeDownloadEntity
import com.yenaly.han1meviewer.logic.model.github.Latest
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys
import com.yenaly.han1meviewer.util.SafFileManager
import com.yenaly.han1meviewer.util.openDownloadedHanimeVideoLocally
import com.yenaly.han1meviewer.worker.HUpdateWorker
import com.yenaly.han1meviewer.worker.HanimeDownloadManagerV2
import com.yenaly.han1meviewer.worker.HanimeDownloadWorker
import com.yenaly.yenaly_libs.ActivityManager
import com.yenaly.yenaly_libs.utils.applicationContext
import com.yenaly.yenaly_libs.utils.awaitActivityResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

private object AndroidFileAccess : FileAccess {
    override suspend fun chooseDownloadDirectory(): PlatformActionResult<DownloadDirectorySelection> {
        val activity = ActivityManager.currentActivity.get()
            ?: return PlatformActionResult.Unavailable(UnavailableReason.ActivityUnavailable)
        return try {
            val result = activity.awaitActivityResult(
                ActivityResultContracts.StartActivityForResult(),
                SafFileManager.buildOpenDirectoryIntent(),
            )
            val data = result.data
            if (result.resultCode != Activity.RESULT_OK || data == null) {
                PlatformActionResult.Cancelled
            } else {
                SafFileManager.persistUriPermission(activity, data)
                Preferences.editSettings {
                    putBoolean(SettingsPreferenceKeys.USE_PRIVATE_STORAGE, false)
                }
                PlatformActionResult.Success(
                    DownloadDirectorySelection(
                        reference = data.data?.toString()?.let(::PlatformFileRef),
                        confirmationText = data.toString(),
                    ),
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            failureResult(failure)
        }
    }

    override fun selectedDownloadDirectory(): PlatformActionResult<PlatformFileRef?> =
        PlatformActionResult.Success(
            Preferences.safDownloadPath
                ?.takeIf(String::isNotBlank)
                ?.let(::PlatformFileRef),
        )

    override fun selectedDownloadDirectoryDisplayName(): PlatformActionResult<String?> =
        actionResult {
            val uri = SafFileManager.getSavedUri() ?: return@actionResult null
            DocumentFile.fromTreeUri(applicationContext, uri)?.name ?: uri.toString()
        }

    override fun privateDownloadDirectoryDisplayName(): PlatformActionResult<String> =
        actionResult {
            applicationContext.getExternalFilesDir(null)?.absolutePath.orEmpty()
        }

    override fun restoreDefaultDownloadDirectory(): PlatformActionResult<Unit> = actionResult {
        Preferences.editSettings {
            putBoolean(SettingsPreferenceKeys.USE_PRIVATE_STORAGE, true)
            remove(SafFileManager.KEY_TREE_URI)
        }
    }

    override fun hasDownloadDirectoryAccess(): PlatformActionResult<Boolean> = actionResult {
        SafFileManager.checkSafPermissions(applicationContext)
    }

    override suspend fun scanAndImportDownloads(): PlatformActionResult<Unit> = try {
        SafFileManager.scanAndImportHanimeDownloads(
            applicationContext,
            DownloadDatabase.instance.hanimeDownloadDao,
        )
        PlatformActionResult.Success(Unit)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        failureResult(failure)
    }

    override fun migratePrivateDownloads(
        onProgress: (DownloadMigrationProgress) -> Unit,
    ): PlatformActionResult<Unit> = actionResult {
        SafFileManager.migratePrivateToSaf(
            context = applicationContext,
            dao = DownloadDatabase.instance.hanimeDownloadDao,
        ) { migrated, total ->
            onProgress(DownloadMigrationProgress(migrated, total))
        }
    }

    override fun deleteDownloadedVideoFolder(videoCode: String): PlatformActionResult<Unit> {
        if (videoCode.isBlank()) {
            return PlatformActionResult.Unavailable(UnavailableReason.InvalidInput)
        }
        return actionResult {
            SafFileManager.deleteDownloadVideoFolder(applicationContext, videoCode)
        }
    }

    override fun openDownloadedVideo(
        reference: PlatformFileRef,
        onFileNotFound: () -> Unit,
    ): PlatformActionResult<Unit> {
        if (reference.value.isBlank()) {
            return PlatformActionResult.Unavailable(UnavailableReason.InvalidInput)
        }
        val activity = ActivityManager.currentActivity.get()
            ?: return PlatformActionResult.Unavailable(UnavailableReason.ActivityUnavailable)
        return actionResult {
            activity.openDownloadedHanimeVideoLocally(reference.value, onFileNotFound)
        }
    }
}

private object AndroidBackgroundJobScheduler : BackgroundJobScheduler {
    override val runningDownloadCount: Flow<Int>
        get() = HanimeDownloadWorker.getRunningWorkInfoCount(applicationContext)

    override fun pruneCompletedJobs(): PlatformActionResult<Unit> = actionResult {
        WorkManager.getInstance(applicationContext).pruneWork()
    }

    override suspend fun initializeDownloads(): PlatformActionResult<Unit> = suspendActionResult {
        HanimeDownloadManagerV2.init()
    }

    override fun enqueueUpdate(request: UpdateJobRequest): PlatformActionResult<Unit> =
        actionResult {
            HUpdateWorker.enqueue(
                applicationContext,
                Latest(
                    version = request.version,
                    changelog = request.changelog,
                    downloadLink = request.downloadLink,
                    nodeId = request.nodeId,
                ),
            )
        }

    override suspend fun collectUpdateOutput(): PlatformActionResult<Unit> = suspendActionResult {
        HUpdateWorker.collectOutput(applicationContext)
    }

    override fun enqueueDownload(
        request: DownloadJobRequest,
        redownload: Boolean,
        waiting: Boolean,
    ): PlatformActionResult<Unit> = actionResult {
        HanimeDownloadManagerV2.addTask(
            HanimeDownloadWorker.Args(
                quality = request.quality,
                downloadUrl = request.downloadUrl,
                videoType = request.videoType,
                hanimeName = request.hanimeName,
                videoCode = request.videoCode,
                coverUrl = request.coverUrl,
            ),
            redownload = redownload,
            waiting = waiting,
        )
    }

    override fun resumeDownload(
        entity: HanimeDownloadEntity,
    ): PlatformActionResult<Unit> = actionResult {
        HanimeDownloadManagerV2.resumeTask(entity)
    }

    override fun pauseDownload(
        entity: HanimeDownloadEntity,
    ): PlatformActionResult<Unit> = actionResult {
        HanimeDownloadManagerV2.stopTask(entity)
    }

    override fun deleteDownload(
        entity: HanimeDownloadEntity,
    ): PlatformActionResult<Unit> = actionResult {
        HanimeDownloadManagerV2.deleteTask(entity)
    }

    override fun setMaxConcurrentDownloadCount(value: Int): PlatformActionResult<Unit> =
        actionResult {
            HanimeDownloadManagerV2.maxConcurrentDownloadCount = value
        }
}

actual fun fileAccess(): FileAccess = AndroidFileAccess

actual fun backgroundJobScheduler(): BackgroundJobScheduler = AndroidBackgroundJobScheduler

private inline fun <T> actionResult(action: () -> T): PlatformActionResult<T> = try {
    PlatformActionResult.Success(action())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    failureResult(failure)
}

private suspend inline fun <T> suspendActionResult(
    crossinline action: suspend () -> T,
): PlatformActionResult<T> = try {
    PlatformActionResult.Success(action())
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: Exception) {
    failureResult(failure)
}

private fun failureResult(failure: Exception): PlatformActionResult.Failure =
    PlatformActionResult.Failure(
        failure.message?.takeIf(String::isNotBlank)
            ?: failure::class.simpleName
            ?: "Platform operation failed",
        failure,
    )
