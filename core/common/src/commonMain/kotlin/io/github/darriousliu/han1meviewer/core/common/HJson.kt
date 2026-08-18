package io.github.darriousliu.han1meviewer.core.common

import kotlinx.serialization.json.Json

/**
 * 全局 JSON 实例。原本在 androidMain 的 `HanimeManager.kt` 里，
 * Step 5 网络层上移 commonMain 后 `HanimeNetwork` 的 ContentNegotiation 要用，跟着搬过来。
 */
val HJson = Json {
    ignoreUnknownKeys = true
}
