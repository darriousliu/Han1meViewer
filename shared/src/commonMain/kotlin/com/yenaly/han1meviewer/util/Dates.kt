package com.yenaly.han1meviewer.util

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn
import kotlin.time.Clock

fun currentLocalDate(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

fun currentYearMonth(): YearMonth = currentLocalDate().let { YearMonth(it.year, it.month) }
