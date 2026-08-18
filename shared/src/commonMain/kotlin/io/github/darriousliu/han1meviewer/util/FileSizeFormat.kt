package io.github.darriousliu.han1meviewer.util

import kotlin.math.roundToLong
import net.sergeych.sprintf.sprintf

/*
 * 从 androidMain 的 `AndroidFormats.kt` 拆出来的。原实现抄自
 * `yenaly_libs/utils/TextUtil.kt`，唯一的平台依赖是
 * `"%.Nf %s".format(Locale.getDefault(), …)`，换成 mp_stools 的 sprintf。
 *
 * 行为对拍基准见 `yenaly_libs/src/test/java/com/yenaly/yenaly_libs/UtilUnitTest.kt`
 * 的 12 条断言，全部逐条比对过。
 *
 * 一点差异：原来传的是 `Locale.getDefault()`，理论上在小数点用逗号的 locale
 * （德语等）下会输出 `1,5 MiB`；sprintf 固定用 `.`。本 App 只有繁中/简中/英/日
 * 四种语言，都用 `.`，所以实际显示不变。
 */

private val siFileSizeUnits = arrayOf("B", "kB", "MB", "GB", "TB")
private val iecFileSizeUnits = arrayOf("B", "KiB", "MiB", "GiB", "TiB")

fun Long.formatFileSizeV2(
    useSi: Boolean = false,
    decimalPlaces: Int = 1,
    stripTrailingZeros: Boolean = true,
): String {
    require(decimalPlaces >= 0) { "decimalPlaces must not be negative" }
    val unit = if (useSi) 1000 else 1024
    if (this < unit) return "$this B"

    val units = if (useSi) siFileSizeUnits else iecFileSizeUnits
    var value = toDouble()
    var unitIndex = 0
    while (value >= unit && unitIndex < units.lastIndex) {
        value /= unit
        unitIndex++
    }

    return if (decimalPlaces == 0 || (stripTrailingZeros && value % 1 == 0.0)) {
        // 不走 sprintf：mp_stools 的 "%.0f" 会留一个尾点（1.0 -> "1." 而不是 "1"），
        // 和 JVM String.format 不一致。整数这条路自己取整。
        // roundToLong 对正数是四舍五入，和 Java "%.0f" 的 HALF_UP 一致。
        "${value.roundToLong()} ${units[unitIndex]}"
    } else {
        "%.${decimalPlaces}f %s".sprintf(value, units[unitIndex])
    }
}

fun Long.formatBytesPerSecond(
    useSi: Boolean = false,
    decimalPlaces: Int = 1,
    stripTrailingZeros: Boolean = true,
): String = formatFileSizeV2(useSi, decimalPlaces, stripTrailingZeros) + "/s"
