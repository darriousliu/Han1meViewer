package com.yenaly.han1meviewer.ios

import com.yenaly.han1meviewer.storage.initializeIosStorage

/** Temporary iOS bootstrap exported to the native shell during the staged UI migration. */
object IosBootstrap {
    fun status(): String {
        initializeIosStorage()
        return "Han1meViewer iOS bootstrap is ready"
    }
}
