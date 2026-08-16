package com.yenaly.han1meviewer.util

import com.yenaly.han1meviewer.HJson
import han1meviewer.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** 读取 `composeResources/files` 下的 JSON；资源缺失或内容无效时返回 null。 */
@OptIn(ExperimentalResourceApi::class)
internal suspend inline fun <reified T> loadBundledJson(path: String): T? = runCatching {
    HJson.decodeFromString<T>(Res.readBytes(path).decodeToString())
}.getOrNull()
