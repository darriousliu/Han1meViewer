package io.github.darriousliu.han1meviewer.mmkv

import com.ctrip.flight.mmkv.MMKVLogLevel
import com.ctrip.flight.mmkv.initialize
import java.io.File

/**
 * desktopApp 目前没有任何读 Preferences 的路径，这个 actual 只为让 jvm target 编译通过。
 *
 * 真要在桌面端跑起来还需要 `io.github.darriousliu:mmkv-kotlin-nativelib-macos`（或对应平台）
 * 提供本地库，本次没加。
 */
actual fun initializeMMKV() {
    val rootDir = File(System.getProperty("user.home"), ".han1meviewer/mmkv")
    rootDir.mkdirs()
    initialize(rootDir = rootDir.absolutePath, logLevel = MMKVLogLevel.LevelInfo)
}
