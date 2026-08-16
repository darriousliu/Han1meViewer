package com.yenaly.han1meviewer.util

import android.app.Application
import android.content.Context
import android.content.res.Resources
import kotlin.math.roundToInt

/** Android 平台能力的最小入口；由 [Application.onCreate] 初始化。 */
object AndroidAppContext {
    lateinit var application: Application
        private set

    fun initialize(application: Application) {
        this.application = application
    }
}

val application: Application
    get() = AndroidAppContext.application

val applicationContext: Context
    get() = AndroidAppContext.application.applicationContext

/** 将 dp 数值转换为像素，供仍使用传统 Android View 尺寸的代码使用。 */
val Number.dpPx: Int
    get() = (toFloat() * AndroidAppContext.application.resources.displayMetrics.density).roundToInt()

val Number.dpF: Float
    get() = toFloat() * AndroidAppContext.application.resources.displayMetrics.density

val appScreenWidth: Int
    get() = applicationContext.resources.displayMetrics.widthPixels

val statusBarHeight: Int
    get() = systemDimensionPixelSize("status_bar_height")

val navBarHeight: Int
    get() = systemDimensionPixelSize("navigation_bar_height")

private fun systemDimensionPixelSize(name: String): Int {
    val resources: Resources = applicationContext.resources
    val resourceId = resources.getIdentifier(name, "dimen", "android")
    return resourceId.takeIf { it != 0 }?.let(resources::getDimensionPixelSize) ?: 0
}
