package io.github.darriousliu.han1meviewer.core.storage.mmkv

import io.github.darriousliu.han1meviewer.core.storage.Preferences

/**
 * 初始化 MMKV。
 *
 * **必须早于任何 [io.github.darriousliu.han1meviewer.core.storage.Preferences] 访问**——Preferences 里的几个
 * StateFlow 在对象初始化时就会读盘。
 */
expect fun initializeMMKV()
