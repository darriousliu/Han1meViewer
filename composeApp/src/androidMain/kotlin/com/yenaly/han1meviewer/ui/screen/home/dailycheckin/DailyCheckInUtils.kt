package com.yenaly.han1meviewer.ui.screen.home.dailycheckin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.provider.CalendarContract
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.currentLocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import java.text.DateFormatSymbols

/**
 * 使用 Android 当前语言环境格式化星期名称。
 */
internal fun LocalDate.localizedWeekdayName(): String {
    val calendarDay = dayOfWeek.isoDayNumber % 7 + 1
    return DateFormatSymbols.getInstance().weekdays[calendarDay]
}

/**
 * 计算连续打卡天数。
 *
 * @param records 各日期打卡记录数
 * @param month 目标月份
 * @return 当前连续天数与当月最佳连续天数的 Pair
 */
fun computeStreaks(
    records: Map<LocalDate, Int>,
    month: YearMonth,
): Pair<Int, Int> {
    val today = currentLocalDate()
    var currentStreak = 0
    var cursor = today
    while ((records[cursor] ?: 0) > 0) {
        currentStreak++
        cursor = cursor.minus(1, DateTimeUnit.DAY)
    }

    var bestStreak = 0
    var streak = 0
    for (date in month.days) {
        if ((records[date] ?: 0) > 0) {
            streak++
            if (streak > bestStreak) bestStreak = streak
        } else {
            streak = 0
        }
    }
    return currentStreak to bestStreak
}

/**
 * 创建日历事件，用于向系统日历添加未来打卡提醒。
 *
 * @param context Android Context
 * @param date 提醒日期
 */
fun createCalendarEvent(context: Context, date: LocalDate) {
    val timeZone = TimeZone.currentSystemDefault()
    val intent = Intent(Intent.ACTION_INSERT).apply {
        setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.dir/event")
        putExtra(
            CalendarContract.Events.TITLE,
            context.getString(R.string.calendar_title, date.month.number, date.day)
        )
        putExtra(CalendarContract.Events.DESCRIPTION, context.getString(R.string.calendar_desc))
        putExtra(
            CalendarContract.Events.EVENT_LOCATION,
            context.getString(R.string.calendar_location)
        )
        putExtra(
            CalendarContract.EXTRA_EVENT_BEGIN_TIME,
            date.atStartOfDayIn(timeZone).toEpochMilliseconds()
        )
        putExtra(
            CalendarContract.EXTRA_EVENT_END_TIME,
            date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
        )
        putExtra(CalendarContract.Events.ALL_DAY, true)
        putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
    }
    try {
        context.startActivity(intent)
    } catch (_: android.content.ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_calendar_app, Toast.LENGTH_SHORT).show()
    }
}

/**
 * 根据是否全屏切换 Activity 的屏幕方向与系统栏可见性。
 *
 * @param isFullscreen 是否进入全屏模式
 */
fun Activity.updateReportWindowMode(isFullscreen: Boolean) {
    requestedOrientation = if (isFullscreen) {
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    } else {
        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.insetsController?.apply {
            if (isFullscreen) {
                hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
            }
        }
    } else {
        @Suppress("DEPRECATION")
        run {
            window.decorView.systemUiVisibility = if (isFullscreen) {
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN
            } else {
                View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
}
