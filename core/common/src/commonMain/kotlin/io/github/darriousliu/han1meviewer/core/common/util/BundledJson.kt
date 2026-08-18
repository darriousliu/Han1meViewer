package io.github.darriousliu.han1meviewer.core.common.util

import io.github.darriousliu.han1meviewer.core.common.HJson
import io.github.darriousliu.han1meviewer.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** 读取 `composeResources/files` 下的 JSON；资源缺失或内容无效时返回 null。 */
@OptIn(ExperimentalResourceApi::class)
suspend inline fun <reified T> loadBundledJson(path: String): T? = runCatching {
    HJson.decodeFromString<T>(Res.readBytes(path).decodeToString())
}.getOrNull()
