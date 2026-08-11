package com.yenaly.han1meviewer

import com.yenaly.han1meviewer.platform.AppBuildInfoProvider

/**
 * 我觉得空字符串写出来太逆天了，所以搞了个常量
 */
const val EMPTY_STRING = ""

const val APP_NAME = "Han1meViewer"

// 网络基本设置

const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Mobile Safari/537.36"
const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36"

// 設置發佈日期年份，在搜索的tag裏

/**
 * 發佈日期年份開始於
 */
const val SEARCH_YEAR_RANGE_START = 1990

/**
 * 發佈日期年份結束於
 */
val SEARCH_YEAR_RANGE_END: Int
    get() = AppBuildInfoProvider.current.searchYearRangeEnd

const val VIDEO_COMMENT_PREFIX = "video"

const val PREVIEW_COMMENT_PREFIX = "preview"

// base url

@JvmField
val HANIME_BASE_URL = Preferences.baseUrl

/**
 * 如果添加备选网址别忘了确认[String.toVideoCode]的videoUrlRegex
 */
object HanimeConstants {
    val HANIME_HOSTNAME = arrayOf("hanime1.me","hanime1.com","hanimeone.me","javchu.com")
    val HANIME_URL = arrayOf("https://hanime1.me/","https://hanime1.com/","https://hanimeone.me/","https://javchu.com/")
    val ANIME_URL = arrayOf("https://hanime1.me/","https://hanime1.com/","https://hanimeone.me/")
}

@JvmField
val HANIME_LOGIN_URL = HANIME_BASE_URL + "login"

// github url

const val HA1_GITHUB_URL = "https://github.com/misaka10032w/Han1meViewer"

const val HA1_GITHUB_ISSUE_URL = "$HA1_GITHUB_URL/issues"

const val HA1_GITHUB_FORUM_URL = "$HA1_GITHUB_URL/discussions"

const val HA1_GITHUB_RELEASES_URL = "$HA1_GITHUB_URL/releases"

const val HA1_GITHUB_API_URL = "https://api.github.com/repos/misaka10032w/Han1meViewer/"
const val FIREBASE_REALTIME_DATABASE = "https://han1meviewer-86e5f-default-rtdb.asia-southeast1.firebasedatabase.app/"
// for Shared Preference

const val LOGIN_COOKIE = "cookie"
const val SAVED_USER_ID = "saved_user_id"

const val CLOUDFLARE_COOKIE = "cf_cookie"

const val ALREADY_LOGIN = "already_login"

// Notification

const val DOWNLOAD_NOTIFICATION_CHANNEL = "download_channel"

const val UPDATE_NOTIFICATION_CHANNEL = "update_channel"

// File

val FILE_PROVIDER_AUTHORITY: String
    get() = "${AppBuildInfoProvider.current.applicationId}.fileProvider"
const val GETCHU_BASE_URL = "https://www.getchu.com/"
