package com.yenaly.han1meviewer

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** yyyy-MM-dd */
val LOCAL_DATE_FORMAT = LocalDate.Formats.ISO

/** yyyy-MM-dd HH:mm */
val LOCAL_DATE_TIME_FORMAT = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
}

/** yyyy-MM-dd HH:mm:ss */
val LOCAL_DATE_TIME_SECONDS_FORMAT = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
    char(':')
    second()
}

/** HH:mm */
val LOCAL_TIME_FORMAT = LocalTime.Format {
    hour()
    char(':')
    minute()
}

/** MM月dd日 */
val MONTH_DAY_FORMAT = LocalDate.Format {
    monthNumber()
    char('月')
    day()
    char('日')
}

/** yyyy年MM月dd日 */
val FULL_DATE_FORMAT = LocalDate.Format {
    year()
    char('年')
    monthNumber()
    char('月')
    day()
    char('日')
}

fun currentLocalDateTime(): LocalDateTime =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

fun currentLocalDate(): LocalDate = currentLocalDateTime().date

fun currentYearMonth(): YearMonth = currentLocalDate().let { YearMonth(it.year, it.month) }
