package com.yenaly.han1meviewer.ui.component

import androidx.compose.ui.window.DialogProperties

actual fun fullScreenDialogProperties(): DialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)
