package io.github.darriousliu.han1meviewer.core.common.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.ClipEntry

fun copyTextToClipboard(
    text: CharSequence?,
    label: CharSequence? = null,
) {
    val clipboard = applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun CharSequence?.copyToClipboard(label: CharSequence? = null) {
    copyTextToClipboard(this, label)
}

val textFromClipboard: CharSequence?
    get() {
        val context = applicationContext
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip ?: return null
        return clip.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
    }

actual fun plainTextClipEntry(text: String): ClipEntry =
    ClipEntry(ClipData.newPlainText(null, text))
