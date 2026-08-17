package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import androidx.compose.ui.graphics.Color
import com.yenaly.han1meviewer.logic.entity.CheckInType
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/*
 * 从 androidMain 的 `DailyCheckInUtils.kt` 拆出来的纯计算部分。
 * 平台副作用那半（系统日历、横竖屏切换）在 androidMain 的
 * `ui/navigation/main/DailyCheckInRouteUtils.kt`。
 * 原文件里的 `computeStreaks` 是死代码（VM 里有逐行相同的实现），已删。
 */

/**
 * 热力图颜色梯度（0 → 4+ 次）。
 */
internal val contributionColors = listOf(
    Color.Transparent,
    Color(0xFF9BE9A8),
    Color(0xFF40C463),
    Color(0xFF30A14E),
    Color(0xFF216E39),
)

/**
 * 根据打卡次数返回热力图颜色等级。
 *
 * @param count 打卡次数
 * @return 颜色等级 0–4
 */
internal fun getContributionLevel(count: Int): Int = when {
    count <= 0 -> 0
    count == 1 -> 1
    count == 2 -> 2
    count in 3..4 -> 3
    else -> 4
}

/**
 * 将打卡类型转换为对应 emoji。
 *
 * @param type [CheckInType.storeName] 值
 * @return 对应的 emoji 字符
 */
fun typeEmoji(type: String): String = when (type) {
    CheckInType.MASTURBATION.storeName -> "🤜"
    CheckInType.WET_DREAM.storeName -> "💤"
    CheckInType.SEX.storeName -> "💑"
    CheckInType.ORAL.storeName -> "👅"
    CheckInType.OTHER.storeName -> "❓"
    else -> "📊"
}

/**
 * 将一年按周分组，用于热力图渲染。
 *
 * @param year 目标年份
 * @return 每周 7 天的日期列表（null 表示该天不属于这一年）
 */
internal fun buildYearWeeks(year: Int): List<List<LocalDate?>> {
    val start = LocalDate(year, 1, 1)
    val end = LocalDate(year, 12, 31)
    val weeks = mutableListOf<MutableList<LocalDate?>>()
    var currentWeek = MutableList<LocalDate?>(7) { null }
    var dayIndex = start.dayOfWeek.isoDayNumber - 1
    var date = start
    while (date <= end) {
        currentWeek[dayIndex] = date
        dayIndex++
        if (dayIndex == 7) {
            weeks.add(currentWeek)
            currentWeek = MutableList(7) { null }
            dayIndex = 0
        }
        date = date.plus(1, DateTimeUnit.DAY)
    }
    if (currentWeek.any { it != null }) {
        weeks.add(currentWeek)
    }
    return weeks
}

/**
 * 从周列表构建月份标签位置。
 *
 * 原签名收 `monthFormat: String` 再在里面 `String.format`——common 没有
 * `String.format`，改成由调用方把 12 个标签预先格式化好传进来
 * （`stringResource(Res.string.report_month_format, it)`，`%1$d` 是 CMP 认的形状）。
 *
 * @param year 目标年份
 * @param weeks 周列表（由 [buildYearWeeks] 生成）
 * @param monthLabels 12 个已格式化的月份标签（下标 0 = 一月）
 * @return 月份标签与起始周索引的列表
 */
internal fun buildMonthLabels(
    year: Int,
    weeks: List<List<LocalDate?>>,
    monthLabels: List<String>,
): List<Pair<String, Int>> {
    val labels = mutableListOf<Pair<String, Int>>()
    for (month in 1..12) {
        val firstDay = LocalDate(year, month, 1)
        val weekIdx = weeks.indexOfFirst { week -> firstDay in week }
        if (weekIdx >= 0) {
            labels.add(monthLabels[month - 1] to weekIdx)
        }
    }
    return labels
}
