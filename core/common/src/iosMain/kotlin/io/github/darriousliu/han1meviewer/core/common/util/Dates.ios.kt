package io.github.darriousliu.han1meviewer.core.common.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter

/**
 * iOS 侧用 `NSDateFormatter`，`dateFormat` 是同一套 Unicode pattern，
 * `EEEE` 同样跟随系统 locale。
 */
actual fun formatMonthDayWithWeekday(date: LocalDate): String {
    val components = NSDateComponents().apply {
        year = date.year.toLong()
        month = date.month.number.toLong()
        day = date.day.toLong()
    }
    val nsDate = NSCalendar.currentCalendar.dateFromComponents(components) ?: return ""
    val formatter = NSDateFormatter().apply { dateFormat = "MM月dd日 EEEE" }
    return formatter.stringFromDate(nsDate)
}
