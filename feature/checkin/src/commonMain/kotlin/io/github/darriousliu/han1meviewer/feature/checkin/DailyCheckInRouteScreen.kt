package io.github.darriousliu.han1meviewer.feature.checkin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.showShort
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@Composable
fun DailyCheckInRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val actions = rememberCheckInActions()
    val capabilities = checkInCapabilities
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    DailyCheckInScreen(
        onBack = onBack,
        onAddWidget = if (capabilities.addWidget) actions::addWidget else null,
        onNavigateToVideo = onNavigateToVideo,
        onAddCalendarEvent = if (capabilities.addCalendarEvent) actions::addCalendarEvent else null,
        onFullscreenChange = actions::setReportFullscreen,
        onMessage = { messageRes ->
            scope.launch { toaster.showShort(getString(messageRes)) }
        },
    )
}
