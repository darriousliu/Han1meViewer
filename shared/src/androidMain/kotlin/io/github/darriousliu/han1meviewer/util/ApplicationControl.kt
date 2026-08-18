package io.github.darriousliu.han1meviewer.util

import android.content.Intent
import kotlin.system.exitProcess

fun restartApplication(killProcess: Boolean = true) {
    applicationContext.packageManager
        .getLaunchIntentForPackage(applicationContext.packageName)
        ?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            applicationContext.startActivity(this)
        }
    if (killProcess) exitProcess(0)
}
