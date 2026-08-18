package io.github.darriousliu.han1meviewer.core.storage.mmkv

import com.ctrip.flight.mmkv.MMKVLogLevel
import com.ctrip.flight.mmkv.initialize
import io.github.darriousliu.han1meviewer.core.common.util.applicationContext

actual fun initializeMMKV() {
    initialize(applicationContext, MMKVLogLevel.LevelInfo)
}
