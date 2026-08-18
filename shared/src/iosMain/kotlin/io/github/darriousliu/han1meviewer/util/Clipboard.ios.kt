package io.github.darriousliu.han1meviewer.util

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

// iOS 的 ClipEntry 只有 internal 构造器，官方入口就是这个工厂（仍是实验 API）
@OptIn(ExperimentalComposeUiApi::class)
actual fun plainTextClipEntry(text: String): ClipEntry = ClipEntry.withPlainText(text)
