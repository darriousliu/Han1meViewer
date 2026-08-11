package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import androidx.compose.ui.graphics.Color
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

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
 * 从周列表构建月份及其起始周位置。
 *
 * @param year 目标年份
 * @param weeks 周列表（由 [buildYearWeeks] 生成）
 * @return 月份与起始周索引的列表
 */
internal fun buildMonthLabels(
    year: Int,
    weeks: List<List<LocalDate?>>,
): List<Pair<Int, Int>> {
    val labels = mutableListOf<Pair<Int, Int>>()
    for (month in 1..12) {
        val firstDay = LocalDate(year, month, 1)
        val weekIdx = weeks.indexOfFirst { week -> firstDay in week }
        if (weekIdx >= 0) {
            labels.add(month to weekIdx)
        }
    }
    return labels
}
