package io.github.darriousliu.han1meviewer.ui.component

import androidx.compose.ui.window.DialogProperties

actual fun fullScreenDialogProperties(): DialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = false,
)
