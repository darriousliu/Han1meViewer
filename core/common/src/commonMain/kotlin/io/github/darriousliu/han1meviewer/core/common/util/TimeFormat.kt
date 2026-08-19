package io.github.darriousliu.han1meviewer.core.common.util

/**
 * 毫秒时间戳格式化成播放器风格的时长文本。
 *
 * 等价替换 `cn.jzvd.JZUtils.stringForTime(long)`（Jzvd 是 Android 专属，
 * commonMain 用不了）：
 *
 * ```
 * timeMs <= 0 || timeMs >= 86_400_000  ->  "00:00"
 * hours > 0                            ->  "%d:%02d:%02d"   // 小时位不补零
 * 否则                                  ->  "%02d:%02d"
 * ```
 *
 * 注意小时位是 `%d` 而不是 `%02d`，所以一小时零两分三秒是 `1:02:03`，不是 `01:02:03`。
 * 这里用 `padStart` 而不是格式化库，既简单也避开了 `sprintf` 的那些边角
 * （见 [formatFileSizeV2] 上面关于 `%.0f` 的注释）。
 *
 * 与 Jzvd 那份的唯一理论差异：它的 `Formatter` 传了 `Locale.getDefault()`，
 * 在用非拉丁数字的 locale 下输出会不同；本 App 只有繁中/简中/英/日四种，都是拉丁数字。
 * Jzvd 那份还有 `HJzvdStd` 和 `HKeyframesRvAdapter` 在用，两套会并存到播放器域迁完。
 */
fun formatVideoTime(timeMs: Long): String {
    if (timeMs <= 0L || timeMs >= 86_400_000L) return "00:00"

    val totalSeconds = timeMs / 1000
    val seconds = (totalSeconds % 60).toInt()
    val minutes = ((totalSeconds / 60) % 60).toInt()
    val hours = (totalSeconds / 3600).toInt()

    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$mm:$ss" else "$mm:$ss"
}
