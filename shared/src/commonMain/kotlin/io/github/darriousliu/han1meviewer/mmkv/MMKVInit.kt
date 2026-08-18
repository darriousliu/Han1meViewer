package io.github.darriousliu.han1meviewer.mmkv

/**
 * 初始化 MMKV。
 *
 * **必须早于任何 [io.github.darriousliu.han1meviewer.Preferences] 访问**——Preferences 里的几个
 * StateFlow 在对象初始化时就会读盘。
 */
expect fun initializeMMKV()
