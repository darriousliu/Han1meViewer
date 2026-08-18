package io.github.darriousliu.han1meviewer

import io.github.darriousliu.han1meviewer.core.common.HanimeConstants

/**
 * 站内链接的拼接与解析。原本在 androidMain 的 `HanimeManager.kt`，
 * 都是纯字符串操作，`Parser` 上移 commonMain 时要用到 [toVideoCode]，跟着搬过来。
 *
 * `HanimeManager.kt` 留下的是真正依赖 Android 的部分：`hanimeSpannedTitle`（parseAsHtml）、
 * `pienization`（Composable）、`login`/`logout`（webkit CookieManager）。
 */

fun getHanimeVideoLink(videoCode: String) = HANIME_BASE_URL + "watch?v=" + videoCode

fun getHanimeSearchLink(artist: String) = HANIME_BASE_URL + "search?query=" + artist

/**
 * 獲取 Hanime 影片分享文本
 */
fun getHanimeShareText(title: String, videoCode: String): String = buildString {
    appendLine(title)
    appendLine(getHanimeVideoLink(videoCode))
    append("- From Han1meViewer -")
}

/**
 * 獲取 Hanime 影片分享文本
 */
fun getHanimeSearchShareText(artist: String): String = buildString {
    appendLine(artist)
    appendLine(getHanimeSearchLink(artist))
    append("- From Han1meViewer -")
}

/**
 * 獲取 Hanime 影片**官方**下載地址
 */
fun getHanimeVideoDownloadLink(videoCode: String) = HANIME_BASE_URL + "download?v=" + videoCode

/**
 * 如果添加备选网址别忘了确认这个正则，见 [HanimeConstants.HANIME_URL]
 */
val videoUrlRegex = Regex(
    """(?:(?:https?:)?//[^\s"'<>/]+|(?:hanime(?:1|one)|javchu)\.(?:com|me))?(?:/[^/?#\s"'<>]+)*/watch\?(?:[^#\s"'<>]*&)?v=(\d+)"""
)

fun String.toVideoCode() = videoUrlRegex.find(this)?.groupValues?.get(1)
