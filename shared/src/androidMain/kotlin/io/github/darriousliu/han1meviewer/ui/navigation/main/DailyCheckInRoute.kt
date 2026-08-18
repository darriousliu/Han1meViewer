package io.github.darriousliu.han1meviewer.ui.navigation.main

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.darriousliu.han1meviewer.R
import io.github.darriousliu.han1meviewer.ui.activity.MainActivity
import io.github.darriousliu.han1meviewer.ui.screen.home.DailyCheckInScreen
import io.github.darriousliu.han1meviewer.ui.widget.CheckInWidgetProvider
import io.github.darriousliu.han1meviewer.util.showShortToast
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@Composable
fun DailyCheckInRouteScreen(
    activity: MainActivity,
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    DailyCheckInScreen(
        onBack = onBack,
        onAddWidget = {
            val mgr = AppWidgetManager.getInstance(activity)
            Toast.makeText(
                activity,
                R.string.widget_pin_not_supported_manual_add,
                Toast.LENGTH_SHORT
            ).show()
            if (mgr.isRequestPinAppWidgetSupported) {
                mgr.requestPinAppWidget(
                    ComponentName(activity, CheckInWidgetProvider::class.java),
                    null, null,
                )
            } else {
                Toast.makeText(activity, R.string.widget_not_supported, Toast.LENGTH_SHORT).show()
            }
        },
        onNavigateToVideo = onNavigateToVideo,
        // 屏幕进了 commonMain，三个平台副作用改由这里执行：
        onAddCalendarEvent = { date -> createCalendarEvent(activity, date) },
        // 屏幕销毁时会以 false 复位调进来，这个 lambda 必须随时可安全调用
        onFullscreenChange = { fullscreen -> activity.updateReportWindowMode(fullscreen) },
        onMessage = { messageRes ->
            scope.launch { showShortToast(getString(messageRes)) }
        },
    )
}
