package com.yenaly.han1meviewer.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ConfirmDialogPreview() {
    ComponentPreview {
        ConfirmDialog(
            visible = true,
            title = "删除历史记录",
            message = "确定要删除这条记录吗？",
            confirmText = "删除",
            dismissText = "取消",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
