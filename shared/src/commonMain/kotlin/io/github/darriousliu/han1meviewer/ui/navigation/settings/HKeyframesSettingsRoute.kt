@file:OptIn(ExperimentalTime::class)

package io.github.darriousliu.han1meviewer.ui.navigation.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.storage.entity.HKeyframeEntity
import io.github.darriousliu.han1meviewer.ui.component.ConfirmDialog
import io.github.darriousliu.han1meviewer.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.ui.component.showShort
import io.github.darriousliu.han1meviewer.ui.screen.settings.HKeyframeSettingsScreen
import io.github.darriousliu.han1meviewer.ui.screen.settings.HKeyframeSettingsUiState
import io.github.darriousliu.han1meviewer.ui.screen.settings.HKeyframesScreen
import io.github.darriousliu.han1meviewer.ui.screen.settings.SharedHKeyframesScreen
import io.github.darriousliu.han1meviewer.ui.viewmodel.SettingsViewModel
import io.github.darriousliu.han1meviewer.core.common.util.setPlainText
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.confirm
import io.github.darriousliu.han1meviewer.core.resource.copy_to_clipboard
import io.github.darriousliu.han1meviewer.core.resource.delete_success
import io.github.darriousliu.han1meviewer.core.resource.h_keyframes_disable_tip
import io.github.darriousliu.han1meviewer.core.resource.h_keyframes_enable_tip
import io.github.darriousliu.han1meviewer.core.resource.h_keyframes_import_shared
import io.github.darriousliu.han1meviewer.core.resource.h_keyframes_import_shared_hint
import io.github.darriousliu.han1meviewer.core.resource.h_keyframes_shared_by_other_detected
import io.github.darriousliu.han1meviewer.core.resource.h_keyframes_shared_by_other_not_detected
import io.github.darriousliu.han1meviewer.core.resource.modify_success
import io.github.darriousliu.han1meviewer.core.resource.shared_h_keyframe_detected_msg
import kotlin.io.encoding.Base64
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource

@Composable
fun HKeyframesTopBarActions(onImportClick: () -> Unit) {
    FilledIconButton(onClick = onImportClick) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(Res.string.h_keyframes_import_shared),
        )
    }
}

@Composable
fun HKeyframesRouteScreen(
    onOpenVideo: (String) -> Unit,
    showImportDialog: Boolean,
    onImportDialogDismiss: () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val viewModel: SettingsViewModel = viewModel()
    val toaster = LocalToaster.current
    // stringResource 是 composable，下面那些回调里用不了，先解开
    val notDetected = stringResource(Res.string.h_keyframes_shared_by_other_not_detected)
    val modifySuccess = stringResource(Res.string.modify_success)
    val deleteSuccess = stringResource(Res.string.delete_success)
    val copiedHint = stringResource(Res.string.copy_to_clipboard)
    val items by viewModel.loadAllHKeyframes()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var sharedHKeyframeEntity by remember { mutableStateOf<HKeyframeEntity?>(null) }

    if (showImportDialog) {
        ImportSharedHKeyframeDialog(
            onDismiss = onImportDialogDismiss,
            onConfirm = { content ->
                val entity = parseSharedHKeyframe(content)
                if (entity != null) {
                    sharedHKeyframeEntity = entity
                    onImportDialogDismiss()
                } else {
                    toaster.showShort(notDetected)
                }
            },
        )
    }

    HKeyframesScreen(
        items = items,
        onOpenVideo = onOpenVideo,
        onDeleteEntity = { entity ->
            viewModel.deleteHKeyframes(entity)
        },
        onUpdateEntityTitle = { entity, newTitle ->
            viewModel.updateHKeyframes(entity.copy(title = newTitle))
            toaster.showShort(modifySuccess)
        },
        onDeleteKeyframe = { videoCode, keyframe ->
            viewModel.removeHKeyframe(videoCode, keyframe)
            toaster.showShort(deleteSuccess)
        },
        onUpdateKeyframe = { videoCode, oldKeyframe, newKeyframe ->
            viewModel.modifyHKeyframe(videoCode, oldKeyframe, newKeyframe)
            toaster.showShort(modifySuccess)
        },
        onCopyShareContent = {
            scope.launch { clipboard.setPlainText(it) }
            toaster.showShort(copiedHint)
        },
    )

    sharedHKeyframeEntity?.let { entity ->
        ConfirmDialog(
            visible = true,
            title = stringResource(Res.string.h_keyframes_shared_by_other_detected),
            message = stringResource(
                Res.string.shared_h_keyframe_detected_msg,
                entity.title,
                entity.videoCode,
                entity.keyframes.size,
            ).trimIndent(),
            confirmText = stringResource(Res.string.confirm),
            dismissText = stringResource(Res.string.cancel),
            onConfirm = {
                viewModel.insertHKeyframes(entity.copy(lastModifiedTime = Clock.System.now().toEpochMilliseconds()))
                sharedHKeyframeEntity = null
            },
            onDismiss = { sharedHKeyframeEntity = null },
        )
    }
}

private val shareRegex = Regex(">>>(.+)<<<")

private fun parseSharedHKeyframe(content: String): HKeyframeEntity? {
    return runCatching {
        val matchResult = shareRegex.find(content) ?: return@runCatching null
        val (toBase64) = matchResult.destructured
        val toJson = Base64.Default.decode(toBase64).decodeToString()
        Json.decodeFromString<HKeyframeEntity>(toJson)
    }.getOrNull()
}

@Composable
private fun ImportSharedHKeyframeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var content by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.h_keyframes_import_shared)) },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(Res.string.h_keyframes_import_shared_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(content) }) {
                Text(stringResource(Res.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

@Composable
fun SharedHKeyframesRouteScreen(
    onOpenVideo: (String) -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel()
    val items by viewModel.loadAllSharedHKeyframes()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    SharedHKeyframesScreen(
        items = items,
        onOpenVideo = onOpenVideo,
    )
}

@Composable
fun HKeyframeSettingsRouteScreen(
    onNavigateToHKeyframes: () -> Unit,
    onNavigateToSharedHKeyframes: () -> Unit,
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    val uiState = buildHKeyframeSettingsUiState(refreshKey)

    HKeyframeSettingsScreen(
        state = uiState,
        onHKeyframesEnableChange = {
            Preferences.hKeyframesEnable = it
            refreshKey++
        },
        onOpenHKeyframeManage = onNavigateToHKeyframes,
        onSharedHKeyframesEnableChange = {
            Preferences.sharedHKeyframesEnable = it
            refreshKey++
        },
        onSharedHKeyframesUseFirstChange = {
            Preferences.sharedHKeyframesUseFirst = it
            refreshKey++
        },
        onOpenSharedHKeyframeManage = onNavigateToSharedHKeyframes,
        onShowCommentWhenCountdownChange = {
            Preferences.showCommentWhenCountdown = it
            refreshKey++
        },
        onWhenCountdownRemindChange = {
            Preferences.whenCountdownRemindSec = it
            refreshKey++
        },
    )
}

/**
 * @param refreshKey 只用来触发重算——`Preferences` 不是可观察状态，
 *   改完得靠它把这个 composable 拉一遍。
 */
@Composable
private fun buildHKeyframeSettingsUiState(refreshKey: Int): HKeyframeSettingsUiState {
    return HKeyframeSettingsUiState(
        hKeyframesEnable = Preferences.hKeyframesEnable,
        hKeyframesSummary = if (Preferences.hKeyframesEnable) {
            stringResource(Res.string.h_keyframes_enable_tip)
        } else {
            stringResource(Res.string.h_keyframes_disable_tip)
        },
        sharedHKeyframesEnable = Preferences.sharedHKeyframesEnable,
        sharedHKeyframesUseFirst = Preferences.sharedHKeyframesUseFirst,
        showCommentWhenCountdown = Preferences.showCommentWhenCountdown,
        whenCountdownRemind = Preferences.whenCountdownRemind / 1000,
        whenCountdownRemindSummary = toPrettyCountdownRemindString(
            Preferences.whenCountdownRemind / 1000
        ),
    )
}
