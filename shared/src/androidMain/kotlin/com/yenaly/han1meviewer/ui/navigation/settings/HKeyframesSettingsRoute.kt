package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.screen.settings.HKeyframeSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.HKeyframeSettingsUiState
import com.yenaly.han1meviewer.ui.screen.settings.HKeyframesScreen
import com.yenaly.han1meviewer.ui.screen.settings.SharedHKeyframesScreen
import com.yenaly.han1meviewer.ui.viewmodel.SettingsViewModel
import com.yenaly.han1meviewer.util.copyToClipboard
import com.yenaly.han1meviewer.util.decodeFromStringByBase64
import com.yenaly.han1meviewer.util.showShortToast
import org.jetbrains.compose.resources.stringResource
import han1meviewer.shared.generated.resources.h_keyframes_disable_tip
import han1meviewer.shared.generated.resources.h_keyframes_enable_tip
import han1meviewer.shared.generated.resources.Res
import kotlinx.serialization.json.Json

@Composable
fun HKeyframesTopBarActions(onImportClick: () -> Unit) {
    FilledIconButton(onClick = onImportClick) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.h_keyframes_import_shared),
        )
    }
}

@Composable
fun HKeyframesRouteScreen(
    onOpenVideo: (String) -> Unit,
    showImportDialog: Boolean,
    onImportDialogDismiss: () -> Unit,
) {
    val viewModel: SettingsViewModel = viewModel()
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
                    showShortToast(R.string.h_keyframes_shared_by_other_not_detected)
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
            showShortToast(R.string.modify_success)
        },
        onDeleteKeyframe = { videoCode, keyframe ->
            viewModel.removeHKeyframe(videoCode, keyframe)
            showShortToast(R.string.delete_success)
        },
        onUpdateKeyframe = { videoCode, oldKeyframe, newKeyframe ->
            viewModel.modifyHKeyframe(videoCode, oldKeyframe, newKeyframe)
            showShortToast(R.string.modify_success)
        },
        onCopyShareContent = {
            it.copyToClipboard()
            showShortToast(R.string.copy_to_clipboard)
        },
    )

    sharedHKeyframeEntity?.let { entity ->
        ConfirmDialog(
            visible = true,
            title = stringResource(R.string.h_keyframes_shared_by_other_detected),
            message = stringResource(
                R.string.shared_h_keyframe_detected_msg,
                entity.title,
                entity.videoCode,
                entity.keyframes.size,
            ).trimIndent(),
            confirmText = stringResource(R.string.confirm),
            dismissText = stringResource(R.string.cancel),
            onConfirm = {
                viewModel.insertHKeyframes(entity.copy(lastModifiedTime = System.currentTimeMillis()))
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
        val toJson = toBase64.decodeFromStringByBase64()
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
        title = { Text(stringResource(R.string.h_keyframes_import_shared)) },
        text = {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(stringResource(R.string.h_keyframes_import_shared_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(content) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
    val context = LocalContext.current
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
