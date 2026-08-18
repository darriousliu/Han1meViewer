package com.yenaly.han1meviewer.ui.navigation.main

import android.content.Intent
import androidx.navigation3.runtime.NavBackStack
import com.yenaly.han1meviewer.ui.navigation.HanimeRoute
import com.yenaly.han1meviewer.ui.navigation.VideoRoute
import com.yenaly.han1meviewer.ui.navigation.navigateSafely

/*
 * 深链分派——原 `MainNavigationActions.kt` 的后半。
 *
 * 前半（`navigateDrawerDestination`）零阻塞，Step 20 上移 commonMain；
 * 这半吃 `android.content.Intent`，留在 androidMain。
 *
 * 文件名没跟着叫 MainNavigationActions：**两个源集不能有同名的顶层文件**，
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

    // 原来这里还处理 startSearchFromTag / startSearchFromMap / startVideoCode 三个 extra，
    // 全仓没有任何生产者（跨 Activity 时代的遗留），Step 17 合并 Activity 时一并删除。
}
