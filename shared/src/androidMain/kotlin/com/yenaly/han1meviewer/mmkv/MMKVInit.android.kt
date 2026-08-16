package com.yenaly.han1meviewer.mmkv

import com.ctrip.flight.mmkv.MMKVLogLevel
import com.ctrip.flight.mmkv.initialize
import com.yenaly.han1meviewer.util.applicationContext

actual fun initializeMMKV() {
    initialize(applicationContext, MMKVLogLevel.LevelInfo)
}
