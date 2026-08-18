package io.github.darriousliu.han1meviewer.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.format.DateTimeFormatter

/**
 * dailycheckin 域把 java.time 的 `ofPattern` 重写成 kotlinx-datetime Format DSL，
 * 这里直接和 java.time 原 pattern 对拍（期望值是原实现跑出来的，不是手算的）。
 *
 * 覆盖个位月/日（零补齐）和闰日。`HH:mm` 是打卡记录**存库格式**，
 * `MonthlyStats.computeStats` 靠 `time.substringBefore(":")` 解析时段成就，
 * 格式变了统计会坏。
 */
class DateFormatTest {

    private val dates = listOf(
        LocalDate(2026, 1, 5),
        LocalDate(2026, 12, 31),
        LocalDate(2024, 2, 29),  // 闰日
        LocalDate(2026, 8, 17),
    )

    @Test
    fun fullDate_matchesJavaTime() {
        val java = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
        for (d in dates) {
            assertEquals(d.toJavaLocalDate().format(java), d.format(CHINESE_FULL_DATE_FORMAT))
        }
    }

    @Test
    fun monthDay_matchesJavaTime() {
        val java = DateTimeFormatter.ofPattern("MM月dd日")
        for (d in dates) {
            assertEquals(d.toJavaLocalDate().format(java), d.format(CHINESE_MONTH_DAY_FORMAT))
        }
    }

    @Test
    fun hourMinute_matchesJavaTime_andKeepsDbShape() {
        val java = DateTimeFormatter.ofPattern("HH:mm")
        val times = listOf(LocalTime(0, 0), LocalTime(9, 5), LocalTime(23, 59))
        for (t in times) {
            val formatted = t.format(HOUR_MINUTE_FORMAT)
            assertEquals(t.toJavaLocalTime().format(java), formatted)
            // 存库格式约束：substringBefore(":") 必须拿到两位小时
            assertEquals(2, formatted.substringBefore(":").length)
        }
    }

    @Test
    fun weekdayFormat_matchesJavaTimeOnAndroidJvm() {
        // androidJvm 的 actual 本身就是 DateTimeFormatter，对拍它等于锁住 pattern 不被改坏
        val java = DateTimeFormatter.ofPattern("MM月dd日 EEEE")
        for (d in dates) {
            assertEquals(d.toJavaLocalDate().format(java), formatMonthDayWithWeekday(d))
        }
    }
}
