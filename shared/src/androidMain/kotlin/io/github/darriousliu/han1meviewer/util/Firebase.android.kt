package io.github.darriousliu.han1meviewer.util

import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.Firebase as AndroidFirebase

actual object Firebase {
    actual fun getBoolean(key: String): Boolean =
        AndroidFirebase.remoteConfig.getBoolean(key)

    actual fun getString(key: String): String =
        AndroidFirebase.remoteConfig.getString(key)

    actual fun getLong(key: String): Long =
        AndroidFirebase.remoteConfig.getLong(key)

    actual fun getDouble(key: String): Double =
        AndroidFirebase.remoteConfig.getDouble(key)
}