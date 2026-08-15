package com.yenaly.han1meviewer

/**
 * 站点基础 URL。因为要读 [Preferences]，暂时留在 androidMain，
 * 等 Preferences 迁到 MMKV 后可以整体提升到 commonMain。
 */
val HANIME_BASE_URL: String get() = Preferences.baseUrl

val HANIME_LOGIN_URL: String get() = HANIME_BASE_URL + "login"
