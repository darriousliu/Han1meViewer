package io.github.darriousliu.han1meviewer.ui.navigation.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.provider.CalendarContract
import android.view.View
import android.view.WindowInsetsController
import android.widget.Toast
import io.github.darriousliu.han1meviewer.R
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate

/*
 * 从 `dailycheckin/DailyCheckInUtils.kt` 拆出来的平台副作用半：屏幕层只发回调，
 * 这两个函数由 `DailyCheckInRoute` 执行。纯计算那半在 commonMain 的
 * `dailycheckin/ContributionUtils.kt`。
 */

/**
 * 创建日历事件，用于向系统日历添加未来打卡提醒。
 *
 * @param context Android Context
 * @param date 提醒日期
 */
fun createCalendarEvent(context: Context, date: LocalDate) {
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
            date.toJavaLocalDate().atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )
        putExtra(
            CalendarContract.EXTRA_EVENT_END_TIME,
            date.plus(1, DateTimeUnit.DAY).toJavaLocalDate()
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                .toEpochMilli()
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
