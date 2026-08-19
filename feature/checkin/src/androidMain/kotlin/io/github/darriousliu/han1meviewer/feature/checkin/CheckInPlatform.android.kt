package io.github.darriousliu.han1meviewer.feature.checkin

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.provider.CalendarContract
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.calendar_desc
import io.github.darriousliu.han1meviewer.core.resource.calendar_location
import io.github.darriousliu.han1meviewer.core.resource.calendar_title
import io.github.darriousliu.han1meviewer.core.resource.no_calendar_app
import io.github.darriousliu.han1meviewer.core.resource.widget_not_supported
import io.github.darriousliu.han1meviewer.core.resource.widget_pin_not_supported_manual_add
import com.dokar.sonner.ToasterState
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.showShort
import io.github.darriousliu.han1meviewer.feature.checkin.widget.CheckInWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import org.jetbrains.compose.resources.getString

actual val checkInCapabilities = CheckInCapabilities(
    addWidget = true,
    addCalendarEvent = true,
)

@Composable
actual fun rememberCheckInActions(): CheckInActions {
    val activity = checkNotNull(LocalActivity.current)
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    return remember(activity, toaster, scope) {
        AndroidCheckInActions(activity, toaster, scope)
    }
}

private class AndroidCheckInActions(
    private val activity: Activity,
    private val toaster: ToasterState,
    private val scope: CoroutineScope,
) : CheckInActions {

    override fun addWidget() {
        val manager = AppWidgetManager.getInstance(activity)
        scope.launch {
            toaster.showShort(getString(Res.string.widget_pin_not_supported_manual_add))
        }
        if (manager.isRequestPinAppWidgetSupported) {
            manager.requestPinAppWidget(
                ComponentName(activity, CheckInWidgetProvider::class.java),
                null, null,
            )
        } else {
            scope.launch { toaster.showShort(getString(Res.string.widget_not_supported)) }
        }
    }

    override fun addCalendarEvent(date: LocalDate) {
        scope.launch {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                setDataAndType(CalendarContract.Events.CONTENT_URI, "vnd.android.cursor.dir/event")
                putExtra(
                    CalendarContract.Events.TITLE,
                    getString(Res.string.calendar_title, date.month.number, date.day),
                )
                putExtra(CalendarContract.Events.DESCRIPTION, getString(Res.string.calendar_desc))
                putExtra(
                    CalendarContract.Events.EVENT_LOCATION,
                    getString(Res.string.calendar_location),
                )
                putExtra(
                    CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                    date.toJavaLocalDate().atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli(),
                )
                putExtra(
                    CalendarContract.EXTRA_EVENT_END_TIME,
                    date.plus(1, DateTimeUnit.DAY).toJavaLocalDate()
                        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                        .toEpochMilli(),
                )
                putExtra(CalendarContract.Events.ALL_DAY, true)
                putExtra(
                    CalendarContract.Events.AVAILABILITY,
                    CalendarContract.Events.AVAILABILITY_FREE,
                )
            }
            try {
                activity.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                toaster.showShort(getString(Res.string.no_calendar_app))
            }
        }
    }

    override fun setReportFullscreen(fullscreen: Boolean) {
        activity.updateReportWindowMode(fullscreen)
    }
}

/** 报表全屏:锁横屏 + 隐系统栏;退出时还原。 */
private fun Activity.updateReportWindowMode(isFullscreen: Boolean) {
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
