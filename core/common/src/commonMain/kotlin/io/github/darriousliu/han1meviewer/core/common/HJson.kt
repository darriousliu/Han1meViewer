package io.github.darriousliu.han1meviewer.core.common

import kotlinx.serialization.json.Json

/**
 * 全局 JSON 实例，也用于 `HanimeNetwork` 的 ContentNegotiation。
 */
val HJson = Json {
    ignoreUnknownKeys = true
}
