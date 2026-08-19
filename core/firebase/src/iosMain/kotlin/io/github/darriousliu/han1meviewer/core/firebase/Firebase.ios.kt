package io.github.darriousliu.han1meviewer.core.firebase

import kotlinx.cinterop.ExperimentalForeignApi
import swiftPMImport.Han1meViewer.core.core.firebase.FIRRemoteConfig

@OptIn(ExperimentalForeignApi::class)
actual object Firebase {
    private val remoteConfig by lazy { FIRRemoteConfig.remoteConfig() }

    actual fun getBoolean(key: String): Boolean =
        remoteConfig.configValueForKey(key).boolValue

    actual fun getString(key: String): String =
        remoteConfig.configValueForKey(key).stringValue

    actual fun getLong(key: String): Long =
        remoteConfig.configValueForKey(key).numberValue.longLongValue

    actual fun getDouble(key: String): Double =
        remoteConfig.configValueForKey(key).numberValue.doubleValue
}
