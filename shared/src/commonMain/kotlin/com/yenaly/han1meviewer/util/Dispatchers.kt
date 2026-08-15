package com.yenaly.han1meviewer.util

import kotlinx.coroutines.CoroutineDispatcher

/**
 * [kotlinx.coroutines.Dispatchers.IO] 不在 commonMain 里（JS/Wasm 没有），
 * 所以用 expect/actual 暴露给多平台代码。
 */
internal expect val ioDispatcher: CoroutineDispatcher
