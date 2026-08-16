package com.yenaly.han1meviewer

import com.yenaly.han1meviewer.util.restartApplication

object HCrashHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread, e: Throwable) {
        e.printStackTrace()
        restartApplication(killProcess = true)
    }
}
