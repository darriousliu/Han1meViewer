package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import com.yenaly.han1meviewer.ui.activity.AndroidMainActivity
import com.yenaly.han1meviewer.ui.screen.video.VideoRouteHostScreen

@Composable
fun VideoRouteScreen(
    activity: AndroidMainActivity,
    route: VideoRoute,
) {
    VideoRouteHostScreen(activity = activity, route = route)
}
