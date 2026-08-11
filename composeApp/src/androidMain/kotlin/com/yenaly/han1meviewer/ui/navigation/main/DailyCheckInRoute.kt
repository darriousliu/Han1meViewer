package com.yenaly.han1meviewer.ui.navigation.main

import android.widget.Toast
import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.platform.PlatformActionResult
import com.yenaly.han1meviewer.platform.getOrThrow
import com.yenaly.han1meviewer.platform.platformServices
import com.yenaly.han1meviewer.ui.activity.AndroidMainActivity
import com.yenaly.han1meviewer.ui.screen.home.DailyCheckInScreen
import com.yenaly.han1meviewer.ui.screen.home.dailycheckin.createCalendarEvent
import com.yenaly.han1meviewer.ui.screen.home.dailycheckin.localizedWeekdayName
import com.yenaly.han1meviewer.ui.screen.home.dailycheckin.updateReportWindowMode

@Composable
fun DailyCheckInRouteScreen(
    activity: AndroidMainActivity,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    DailyCheckInScreen(
        onBack = onBack,
        onAddWidget = {
            Toast.makeText(
                activity,
                R.string.widget_pin_not_supported_manual_add,
                Toast.LENGTH_SHORT
            ).show()
            when (val result = platformServices().homeWidget.requestPin()) {
                is PlatformActionResult.Unavailable -> {
                    Toast.makeText(activity, R.string.widget_not_supported, Toast.LENGTH_SHORT)
                        .show()
                }

                is PlatformActionResult.Success -> Unit
                else -> result.getOrThrow()
            }
        },
        onNavigateToVideo = onNavigateToVideo,
        weekdayLabel = { it.localizedWeekdayName() },
        onScheduleCalendar = { createCalendarEvent(activity, it) },
        onSuckBackDone = {
            Toast.makeText(activity, R.string.suck_back_done, Toast.LENGTH_SHORT).show()
        },
        onReportFullscreenChanged = { activity.updateReportWindowMode(it) },
    )
}
