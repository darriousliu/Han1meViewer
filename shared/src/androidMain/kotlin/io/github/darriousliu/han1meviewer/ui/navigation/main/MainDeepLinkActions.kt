package io.github.darriousliu.han1meviewer.ui.navigation.main

import android.content.Intent
import androidx.navigation3.runtime.NavBackStack
import io.github.darriousliu.han1meviewer.core.navigation.HanimeRoute
import io.github.darriousliu.han1meviewer.core.navigation.VideoRoute
import io.github.darriousliu.han1meviewer.core.navigation.navigateSafely
import io.github.darriousliu.han1meviewer.feature.main.navigateDrawerDestination

/*
 * 深链分派。抽屉目的地导航（`navigateDrawerDestination`）零阻塞、在 commonMain；
 * 这半吃 `android.content.Intent`，留在 androidMain。
 *
 * 文件名没叫 MainNavigationActions：**两个源集不能有同名的顶层文件**，
 * 否则报 `Duplicate JVM class name 'MainNavigationActionsKt'`。
 */

fun NavBackStack<HanimeRoute>.handleMainIntent(intent: Intent) {
    if (intent.action == Intent.ACTION_VIEW) {
        val uri = intent.data ?: return
        when (uri.scheme) {
            "http", "https" -> {
                val videoCode = uri.getQueryParameter("v")
                if (videoCode != null) {
                    navigateSafely(VideoRoute(videoCode))
                }
            }

            "file", "content" -> {
                navigateSafely(VideoRoute("-1", uri.toString()))
            }
        }
        return
    }
}
