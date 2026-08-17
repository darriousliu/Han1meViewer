package com.yenaly.han1meviewer.ui.component

import androidx.compose.ui.window.DialogProperties

/** iOS 没有 `decorFitsSystemWindows` 的对应物，安全区由弹窗内容自己的 padding 处理。 */
actual fun fullScreenDialogProperties(): DialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
)
