package io.github.darriousliu.han1meviewer.core.storage

import io.github.darriousliu.han1meviewer.core.common.HanimeConstants
import io.github.darriousliu.han1meviewer.core.common.toVideoCode

/**
 * 站内链接的拼接与解析，纯字符串操作。
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
