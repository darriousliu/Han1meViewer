package com.yenaly.han1meviewer.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

val Context.activity: Activity?
    get() {
        var current = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

inline fun <reified T : Activity> Context.findActivity(): T =
    findActivityOrNull() ?: error("No activity of type ${T::class.java.simpleName} found")

inline fun <reified T : Activity> Context.findActivityOrNull(): T? {
    var current = this
    while (current is ContextWrapper) {
        if (current is T) return current
        current = current.baseContext
    }
    return null
}

fun Context.requireComponentActivity(): ComponentActivity =
    findActivityOrNull<ComponentActivity>() ?: error("No ComponentActivity found")
