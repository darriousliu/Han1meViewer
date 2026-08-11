package com.yenaly.han1meviewer.util

import android.app.Activity
import android.util.Log
import androidx.fragment.app.Fragment
import com.yenaly.han1meviewer.platform.FirebaseEventName
import com.yenaly.han1meviewer.platform.FirebaseParameterName
import com.yenaly.han1meviewer.platform.firebasePlatform

fun Activity.logScreenViewEvent(fragment: Fragment) {
    logScreenViewEvent(fragment.javaClass.simpleName)
}

fun Activity.logScreenViewEvent(screenClassName: String) {
    // example: AndroidMainActivity-HomeRouteScreen
    val screenName = this@logScreenViewEvent.javaClass.simpleName +
        "-" + screenClassName
    Log.d("logScreenViewEvent", "screenName: $screenName")
    firebasePlatform().logEvent(
        FirebaseEventName.ScreenView,
        mapOf(
            FirebaseParameterName.ScreenName to screenName,
            FirebaseParameterName.ScreenClass to screenClassName,
        ),
    )
}
