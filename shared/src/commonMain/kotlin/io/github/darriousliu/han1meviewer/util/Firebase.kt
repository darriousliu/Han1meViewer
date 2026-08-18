package io.github.darriousliu.han1meviewer.util

expect object Firebase {
    fun getBoolean(key: String): Boolean
    fun getString(key: String): String
    fun getLong(key: String): Long
    fun getDouble(key: String): Double
}