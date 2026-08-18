package io.github.darriousliu.han1meviewer.core.storage

import io.github.darriousliu.han1meviewer.core.common.HanimeConstants
import io.github.darriousliu.han1meviewer.core.storage.Preferences

// 站点基础 URL。
//
// 读 Preferences，所以跟着存储层走，没能留在 :core:common——常量模块不该
// 反过来依赖存储。
//
// 注意是 get()：用户在设置里换镜像站后即时生效，不需要重启 App。

val HANIME_BASE_URL: String get() = Preferences.baseUrl

val HANIME_LOGIN_URL: String get() = HANIME_BASE_URL + "login"
