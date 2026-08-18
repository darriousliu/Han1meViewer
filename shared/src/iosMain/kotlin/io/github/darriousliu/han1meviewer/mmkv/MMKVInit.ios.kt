package io.github.darriousliu.han1meviewer.mmkv

import com.ctrip.flight.mmkv.MMKVLogLevel
import com.ctrip.flight.mmkv.initialize

actual fun initializeMMKV() {
    initialize(logLevel = MMKVLogLevel.LevelInfo)
}
