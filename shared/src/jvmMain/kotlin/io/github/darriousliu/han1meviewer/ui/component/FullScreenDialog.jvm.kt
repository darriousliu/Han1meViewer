package io.github.darriousliu.han1meviewer.ui.component

import androidx.compose.ui.window.DialogProperties

/** 桌面端没有系统栏这回事，只要撑满就行。 */
actual fun fullScreenDialogProperties(): DialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
)
