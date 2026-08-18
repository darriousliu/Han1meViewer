package io.github.darriousliu.han1meviewer.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri

infix fun Context.browse(uri: String) {
    startActivity(
        Intent(Intent.ACTION_VIEW, uri.toUri()).apply {
            if (this@browse !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

fun shareText(content: CharSequence, title: CharSequence? = null) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, content)
    }
    applicationContext.startActivity(
        Intent.createChooser(sendIntent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

inline fun <reified T : Activity> Context.startActivity(
    flag: Int? = null,
    extra: Bundle? = null,
) {
    startActivity(
        Intent(this, T::class.java).apply {
            flag?.let { flags = it }
            extra?.let(::putExtras)
            if (this@startActivity !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}
