package io.github.darriousliu.han1meviewer.util

import androidx.compose.ui.text.intl.Locale

object DisplayTextLocalizer {

    private val viewsRegex = Regex("^(.+?)(万次|萬次|次)$")
    private val relativeTimeRegex = Regex("^(?:ge)?(.+?)(分钟|分鐘|小时|小時|天|周|週|个月|個月|年)前$")

    fun localizeViews(text: String): String {
        val match = viewsRegex.matchEntire(text.trim()) ?: return text
        val count = match.groupValues[1]
        val unit = match.groupValues[2]
        return when (language()) {
            Locale.SIMPLIFIED_CHINESE.language -> when (unit) {
                "万次", "萬次" -> "${count}万次"
                else -> "${count}次"
            }

            Locale.ENGLISH.language -> when (unit) {
                "万次", "萬次" -> "${count.toKViews()} views"
                else -> "$count views"
            }

            Locale.JAPANESE.language -> when (unit) {
                "万次", "萬次" -> "${count}万回"
                else -> "${count}回"
            }

            else -> when (unit) {
                "万次", "萬次" -> "${count}萬次"
                else -> "${count}次"
            }
        }
    }

    fun localizeRelativeTime(text: String): String {
        val match = relativeTimeRegex.matchEntire(text.trim()) ?: return text
        val count = match.groupValues[1]
        val unit = match.groupValues[2]
        return when (language()) {
            Locale.SIMPLIFIED_CHINESE.language -> "$count${unit.toSimplifiedUnit()}前"
            Locale.ENGLISH.language -> "$count ${unit.toEnglishUnit(count)} ago"
            Locale.JAPANESE.language -> "$count${unit.toJapaneseUnit()}前"
            else -> "$count${unit.toTraditionalUnit()}前"
        }
    }

    private fun language(): String = LanguageHelper.preferredLanguage.language

    private fun String.toSimplifiedUnit(): String = when (this) {
        "分鐘", "分钟" -> "分钟"
        "小時", "小时" -> "小时"
        "週", "周" -> "周"
        "個月", "个月" -> "个月"
        else -> this
    }

    private fun String.toTraditionalUnit(): String = when (this) {
        "分钟", "分鐘" -> "分鐘"
        "小时", "小時" -> "小時"
        "周", "週" -> "週"
        "个月", "個月" -> "個月"
        else -> this
    }

    private fun String.toJapaneseUnit(): String = when (this) {
        "分钟", "分鐘" -> "分"
        "小时", "小時" -> "時間"
        "天" -> "日"
        "周", "週" -> "週間"
        "个月", "個月" -> "か月"
        "年" -> "年"
        else -> this
    }

    private fun String.toEnglishUnit(count: String): String {
        val singular = count == "1"
        return when (this) {
            "分钟", "分鐘" -> if (singular) "minute" else "minutes"
            "小时", "小時" -> if (singular) "hour" else "hours"
            "天" -> if (singular) "day" else "days"
            "周", "週" -> if (singular) "week" else "weeks"
            "个月", "個月" -> if (singular) "month" else "months"
            "年" -> if (singular) "year" else "years"
            else -> this
        }
    }

    /**
     * 「1.5 万次」里的 1.5 换算成 15K：十进制小数点右移一位。
     *
     * 原来用 `BigDecimal(this).multiply(TEN).stripTrailingZeros().toPlainString()`，
     * commonMain 没有 BigDecimal，改成纯字符串移位——避开 Double 的精度问题，
     * 输出与原实现逐例一致。解析不了就退回原来的兜底分支。
     */
    private fun String.toKViews(): String = shiftDecimalPointRight(trim())?.let { "${it}K" }
        ?: "${this}0K"

    private fun shiftDecimalPointRight(value: String): String? {
        if (value.isEmpty()) return null
        val negative = value.startsWith('-')
        val digits = if (negative || value.startsWith('+')) value.substring(1) else value
        val dot = digits.indexOf('.')
        val intPart: String
        val fracPart: String
        if (dot < 0) {
            intPart = digits
            fracPart = ""
        } else {
            intPart = digits.substring(0, dot)
            fracPart = digits.substring(dot + 1)
            if (fracPart.contains('.')) return null
        }
        if (intPart.isEmpty() && fracPart.isEmpty()) return null
        if (!intPart.all { it.isDigit() } || !fracPart.all { it.isDigit() }) return null

        // 右移一位：小数第一位并进整数部分，没有小数位就补个 0
        val shiftedInt = intPart + (fracPart.firstOrNull() ?: '0')
        val shiftedFrac = fracPart.drop(1).trimEnd('0')

        val normalizedInt = shiftedInt.trimStart('0').ifEmpty { "0" }
        val body = if (shiftedFrac.isEmpty()) normalizedInt else "$normalizedInt.$shiftedFrac"
        return if (negative && body != "0") "-$body" else body
    }
}
