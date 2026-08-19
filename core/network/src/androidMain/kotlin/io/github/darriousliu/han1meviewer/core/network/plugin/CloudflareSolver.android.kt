package io.github.darriousliu.han1meviewer.core.network.plugin

import io.github.darriousliu.han1meviewer.core.network.AndroidCloudflareSolver
import io.github.darriousliu.han1meviewer.core.common.util.application
import io.github.darriousliu.han1meviewer.core.common.util.isMainProcess

/**
 * 过盾要拉起 Activity 跑 WebView，只在主进程有意义；
 * 子进程碰到 403 走插件的放行分支。
 */
private val instance: CloudflareSolver? by lazy {
    if (isMainProcess) AndroidCloudflareSolver(application) else null
}

internal actual fun platformCloudflareSolver(): CloudflareSolver? = instance
