package io.github.darriousliu.han1meviewer.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.darriousliu.han1meviewer.core.ui.preview.ComponentPreview
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.confirm
import org.jetbrains.compose.resources.stringResource

/**
 * 两个文本框的输入对话框（标题 + 描述）。
 *
 * 替代原来 `LayoutInflater.inflate(R.layout.dialog_playlist_modify_edit_text)` 那套
 * ——同一个 layout 被「新建播放列表」和「修改标题/描述」两处 inflate，
 * 所以这里做成一个参数化组件供两处共用。
 *
 * 和被替代的 XML 版相比有两处**刻意的行为改进**：
 * 1. 原来是「第一个框有焦点、键盘不自动弹」（那是 `AlertController` 检测到自定义 view
 *    含 EditText 的副作用，不是代码写的），用户得多点一下。这里主动弹键盘。
 * 2. 原来 XML 写了 `maxLines="3"`，但配的是 `inputType="text"`——Android 下非
 *    multiLine 的 inputType 会强制单行，所以那个 `maxLines` **当年就没生效**。
 *    这里按写 XML 时的意图来：标题单行、描述最多 3 行。
 *
 * 另外原来经 `showAlertDialog` 会给 Activity decorView 加高斯模糊（`RenderEffect`，
 * Android 独有），CMP 的 [AlertDialog] 没有这个效果——commonMain 已有的
 * [ConfirmDialog] 本来也没有，域内早就不统一，这里接受。
 *
 * 确定按钮**无条件可点**（和原实现一致，空标题也能提交），要加校验是另一件事。
 */
@Composable
fun TextInputDialog(
    title: String,
    firstLabel: String,
    secondLabel: String,
    onConfirm: (first: String, second: String) -> Unit,
    onDismiss: () -> Unit,
    firstInitial: String = "",
    secondInitial: String = "",
    confirmText: String = stringResource(Res.string.confirm),
    dismissText: String = stringResource(Res.string.cancel),
) {
    // key 用初值：同一个对话框换一条播放列表打开时要重新预填
    var first by remember(firstInitial) { mutableStateOf(firstInitial) }
    var second by remember(secondInitial) { mutableStateOf(secondInitial) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it },
                    label = { Text(firstLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
                OutlinedTextField(
                    value = second,
                    onValueChange = { second = it },
                    label = { Text(secondLabel) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(first, second) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TextInputDialogPreview() {
    ComponentPreview {
        TextInputDialog(
            title = "修改标题或描述",
            firstLabel = "播放列表标题",
            secondLabel = "播放列表描述",
            firstInitial = "我的收藏",
            onConfirm = { _, _ -> },
            onDismiss = {},
        )
    }
}
