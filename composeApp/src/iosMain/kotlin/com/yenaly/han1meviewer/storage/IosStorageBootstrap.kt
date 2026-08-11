package com.yenaly.han1meviewer.storage

import com.ctrip.flight.mmkv.initialize as initializeMmkv

internal fun initializeIosStorage() {
    if (AppStorage.isInstalled) return
    initializeMmkv()
    AppStorage.prepareAndInstall()
}
