package io.github.darriousliu.han1meviewer.core.common.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter

/** `EEEE` 跟随设备 locale，和迁移前 `DateTimeFormatter.ofPattern` 的行为逐字节一致。 */
actual fun formatMonthDayWithWeekday(date: LocalDate): String =
    date.toJavaLocalDate().format(DateTimeFormatter.ofPattern("MM月dd日 EEEE"))
