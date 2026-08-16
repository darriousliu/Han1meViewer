package com.yenaly.han1meviewer.util

actual object Firebase {
    actual fun getBoolean(key: String): Boolean {
        return false
    }

    actual fun getString(key: String): String {
        return ""
    }

    actual fun getLong(key: String): Long {
        return 0L
    }

    actual fun getDouble(key: String): Double {
        return 0.0
    }
}