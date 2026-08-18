package io.github.darriousliu.han1meviewer.core.common

/**
 * 从站内链接里抠出影片编号。
 *
 * 和 `HanimeLinks.kt` 里那些拼接函数分开放：那些要读 [Preferences] 的 baseUrl
 * （在 :core:storage 那一层），而这里是纯正则，`Parser` 要用，
 * 放上层会让 :core:parse 反过来依赖 :shared。
 *
 * 如果添加备选网址别忘了确认这个正则，见 [HanimeConstants.HANIME_URL]
 */
val videoUrlRegex = Regex(
    """(?:(?:https?:)?//[^\s"'<>/]+|(?:hanime(?:1|one)|javchu)\.(?:com|me))?(?:/[^/?#\s"'<>]+)*/watch\?(?:[^#\s"'<>]*&)?v=(\d+)"""
)

fun String.toVideoCode() = videoUrlRegex.find(this)?.groupValues?.get(1)
