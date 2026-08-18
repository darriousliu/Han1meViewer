package io.github.darriousliu.han1meviewer.core.common.util

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

/**
 * 把一段纯文本包成 [ClipEntry]。
 *
 * 看着像是 CMP 该自带的东西，但 1.12.0-rc01 的 commonMain 里**确实没有**（逐个核实过）：
 *
 * - [Clipboard] 接口只有 `getClipEntry()` / `setClipEntry()`，收发的都是 [ClipEntry]
 * - [ClipEntry] 是 `expect class`，各平台构造方式压根不同：Android 是 `ClipEntry(ClipData)`，
 *   desktop 是 `ClipEntry(Any)`（里面塞 AWT `Transferable`），iOS 连公开构造器都没有，
 *   只有 `ClipEntry.withPlainText()`——而且它只存在于 iOS 侧，common 里没有对应的 expect 成员
 * - `androidx.compose.foundation` 里那个 `toClipEntry()` 在 `foundation.internal` 包，跨模块不可见
 *
 * 所以这一层只能自己补一个 expect。补完之后界面就能直接
 * `LocalClipboard.current.setPlainText(text)`，不必再从 androidMain 往下传复制回调。
 */
expect fun plainTextClipEntry(text: String): ClipEntry

/**
 * [Clipboard.setClipEntry] 的纯文本版。
 *
 * ⚠️ 是挂起函数（平台剪贴板可能要跨进程），调用点得 `scope.launch { }`。
 */
suspend fun Clipboard.setPlainText(text: String) = setClipEntry(plainTextClipEntry(text))
