package com.yenaly.han1meviewer.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

// desktop 的 ClipEntry 收的是 Any，取用时按 AWT Transferable 解（见 ClipEntry.asAwtTransferable）
@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextClipEntry(text: String): ClipEntry = ClipEntry(StringSelection(text))
