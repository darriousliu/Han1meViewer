package io.github.darriousliu.han1meviewer.feature.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.storage.entity.download.HanimeDownloadEntity
import io.github.darriousliu.han1meviewer.core.storage.entity.download.VideoWithCategories
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.confirm
import io.github.darriousliu.han1meviewer.core.resource.create_group_success
import io.github.darriousliu.han1meviewer.core.resource.delete
import io.github.darriousliu.han1meviewer.core.resource.delete_success
import io.github.darriousliu.han1meviewer.core.resource.group_name_empty
import io.github.darriousliu.han1meviewer.core.resource.group_renamed
import io.github.darriousliu.han1meviewer.core.resource.ok
import io.github.darriousliu.han1meviewer.core.resource.permission_error
import io.github.darriousliu.han1meviewer.core.resource.prepare_to_delete_s
import io.github.darriousliu.han1meviewer.core.resource.read_download_dir_message
import io.github.darriousliu.han1meviewer.core.resource.read_download_dir_title
import io.github.darriousliu.han1meviewer.core.resource.read_success
import io.github.darriousliu.han1meviewer.core.resource.select_custom_directory
import io.github.darriousliu.han1meviewer.core.resource.sure_to_delete
import io.github.darriousliu.han1meviewer.core.resource.video_deleted_sure_to_delete_item
import io.github.darriousliu.han1meviewer.core.resource.video_not_exist
import io.github.darriousliu.han1meviewer.core.ui.component.ConfirmDialog
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.showLong
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DownloadRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToLocalVideo: (String, String?) -> Unit,
) {
    val viewModel: DownloadViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val engine = LocalDownloadTaskEngine.current
    val platform = rememberDownloadPlatformActions()
    var showVideoNotExistConfirm by remember { mutableStateOf<VideoWithCategories?>(null) }
    var showDeleteVideoConfirm by remember { mutableStateOf<VideoWithCategories?>(null) }
    var showImportDownloadedConfirm by remember { mutableStateOf(false) }
    var isImportingDownloaded by remember { mutableStateOf(false) }

    val handleEvent: (DownloadEvent) -> Unit = { event ->
        when (event) {
            is DownloadEvent.OnPauseAll -> event.items.forEach { entity ->
                if (entity.isDownloading) engine.pause(entity)
            }
            is DownloadEvent.OnResumeAll -> event.items.forEach { entity ->
                if (!entity.isDownloading) engine.resume(entity)
            }
            is DownloadEvent.OnPauseItem -> engine.pause(event.item)
            is DownloadEvent.OnResumeItem -> engine.resume(event.item)
            is DownloadEvent.OnDeleteDownloadingItem -> engine.delete(event.item)

            is DownloadEvent.OnImportDownloaded -> {
                if (platform.canImportFromDirectory() && !isImportingDownloaded) {
                    showImportDownloadedConfirm = true
                } else {
                    scope.launch {
                        toaster.showLong(getString(Res.string.select_custom_directory))
                    }
                }
            }

            is DownloadEvent.OnOpenDownloadedVideo -> onNavigateToVideo(event.video.video.videoCode)
            is DownloadEvent.OnLocalPlayback -> onNavigateToLocalVideo(
                event.video.video.videoCode, event.video.video.videoUri
            )

            is DownloadEvent.OnExternalPlayback -> {
                platform.playExternally(event.video.video.videoUri) {
                    showVideoNotExistConfirm = event.video
                }
            }

            is DownloadEvent.OnDeleteDownloadedVideo -> showDeleteVideoConfirm = event.video

            is DownloadEvent.OnMoveVideoGroup -> viewModel.updateVideoGroup(
                event.video.video.videoCode, event.groupId
            )

            is DownloadEvent.OnRenameGroup -> {
                viewModel.updateGroupName(event.groupId, event.newName)
                scope.launch {
                    toaster.showLong(getString(Res.string.group_renamed, event.newName))
                }
            }

            is DownloadEvent.OnCreateGroup -> {
                if (event.name.isBlank()) {
                    scope.launch { toaster.showLong(getString(Res.string.group_name_empty)) }
                } else {
                    viewModel.createNewGroup(event.name)
                    scope.launch {
                        toaster.showLong(getString(Res.string.create_group_success, event.name))
                    }
                }
            }

            is DownloadEvent.OnDeleteGroup -> {
                viewModel.deleteGroup(event.group)
                scope.launch { toaster.showLong(getString(Res.string.delete_success)) }
            }

            is DownloadEvent.OnBatchDelete -> event.videos.forEach { video ->
                viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                platform.deleteDownloadedFiles(video.video.videoCode)
            }

            is DownloadEvent.OnBatchMoveGroup -> event.videos.forEach { video ->
                viewModel.updateVideoGroup(video.video.videoCode, event.groupId)
            }

            // 以下事件由 Screen 层自行处理，Route 不关心
            is DownloadEvent.OnToggleGroup,
            is DownloadEvent.OnCreateGroupDialogChange,
            is DownloadEvent.OnPageChange,
            is DownloadEvent.OnToggleMultiSelect,
            is DownloadEvent.OnToggleVideoSelection,
            is DownloadEvent.OnSelectAllCurrentGroup,
            is DownloadEvent.OnBatchMoveRequest -> Unit
        }
    }

    DownloadScreen(
        downloadingFlow = viewModel.loadAllDownloadingHanime(),
        downloadedFlow = viewModel.downloaded,
        downloadedGroupsFlow = viewModel.downloadedGroups,
        collapseDownloadedGroup = Preferences.collapseDownloadedGroup,
        onBack = onBack,
        onLoadDownloaded = {
            viewModel.loadAllDownloadedHanime(
                sortedBy = HanimeDownloadEntity.SortedBy.ID,
                ascending = false,
            )
        },
        onEvent = handleEvent,
        showImportEntry = downloadCapabilities.importFromDirectory,
    )

    ConfirmDialog(
        visible = showImportDownloadedConfirm,
        title = stringResource(Res.string.read_download_dir_title),
        message = stringResource(Res.string.read_download_dir_message),
        confirmText = stringResource(Res.string.ok),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = {
            showImportDownloadedConfirm = false
            isImportingDownloaded = true
            scope.launch {
                val importSucceeded = platform.importFromDirectory()
                isImportingDownloaded = false
                if (importSucceeded) {
                    viewModel.loadAllDownloadedHanime(
                        sortedBy = HanimeDownloadEntity.SortedBy.ID,
                        ascending = false,
                    )
                    toaster.showLong(getString(Res.string.read_success))
                } else {
                    toaster.showLong(getString(Res.string.permission_error))
                }
            }
        },
        onDismiss = { showImportDownloadedConfirm = false },
    )

    showVideoNotExistConfirm?.let { video ->
        ConfirmDialog(
            visible = true,
            title = stringResource(Res.string.video_not_exist),
            message = stringResource(Res.string.video_deleted_sure_to_delete_item),
            confirmText = stringResource(Res.string.delete),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = {
                viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                showVideoNotExistConfirm = null
            },
            onDismiss = { showVideoNotExistConfirm = null },
        )
    }

    showDeleteVideoConfirm?.let { video ->
        ConfirmDialog(
            visible = true,
            title = stringResource(Res.string.sure_to_delete),
            message = stringResource(Res.string.prepare_to_delete_s, video.video.title),
            confirmText = stringResource(Res.string.confirm),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = {
                platform.deleteDownloadedFiles(video.video.videoCode)
                viewModel.deleteDownloadHanimeBy(video.video.videoCode, video.video.quality)
                showDeleteVideoConfirm = null
            },
            onDismiss = { showDeleteVideoConfirm = null },
        )
    }
}
