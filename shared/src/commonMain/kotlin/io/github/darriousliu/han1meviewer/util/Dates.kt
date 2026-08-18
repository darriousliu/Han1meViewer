package io.github.darriousliu.han1meviewer.util

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.char
import kotlinx.datetime.todayIn

fun currentLocalDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun currentYearMonth(): YearMonth = currentLocalDate().let { YearMonth(it.year, it.month) }

/**
 * 「MM月dd日 EEEE」——月日加**设备 locale** 的星期全称（星期一 / Monday / 月曜日）。
 *
 * kotlinx-datetime 没有 locale 感知（`DayOfWeekNames` 要显式给 7 个名字），
 * 而这里要跟设备语言走，所以做成 expect/actual：android + jvm 共用 java.time
 * 的 `DateTimeFormatter`（行为和迁移前逐字节一致），iOS 用 `NSDateFormatter`。
 * 只有打卡页的今日卡片在用。
 */
expect fun formatMonthDayWithWeekday(date: LocalDate): String

/* 打卡域的三个固定格式。原来是 java.time 的 ofPattern("yyyy年MM月dd日" / "MM月dd日" /
 * "HH:mm")，逐字符等价重写（MM/dd/HH 的零补齐 = kotlinx 默认 Padding.ZERO）。
 * androidHostTest 的 DateFormatTest 和 java.time 原 pattern 对拍。 */

/** `yyyy年MM月dd日` */
val CHINESE_FULL_DATE_FORMAT = LocalDate.Format {
    year(); char('年'); monthNumber(); char('月'); day(); char('日')
}

/** `MM月dd日` */
val CHINESE_MONTH_DAY_FORMAT = LocalDate.Format {
    monthNumber(); char('月'); day(); char('日')
}

/** `HH:mm`。打卡记录存库用的就是这个格式，时段成就统计靠 `substringBefore(":")` 解析。 */
val HOUR_MINUTE_FORMAT = LocalTime.Format {
    hour(); char(':'); minute()
}
