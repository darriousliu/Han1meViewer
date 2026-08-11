package com.yenaly.han1meviewer.platform

object FirebaseEventName {
    const val ScreenView = "screen_view"
    const val SelectContent = "select_content"
}

object FirebaseParameterName {
    const val ScreenName = "screen_name"
    const val ScreenClass = "screen_class"
    const val ItemId = "item_id"
    const val ContentType = "content_type"
}

data class FirebaseRuntimeConfiguration(
    val analyticsCollectionEnabled: Boolean,
    val crashlyticsCollectionEnabled: Boolean,
    val crashlyticsStringKeys: Map<String, String>,
    val remoteConfigMinimumFetchIntervalSeconds: Long,
    val remoteConfigFetchTimeoutSeconds: Long,
    val remoteConfigDefaults: Map<String, Any>,
    val realtimeDatabasePersistenceEnabled: Boolean,
)

interface FirebasePlatform {
    val isAvailable: Boolean

    fun initialize(
        configuration: FirebaseRuntimeConfiguration,
        onRemoteConfigActivated: () -> Unit,
    )

    fun setAnalyticsCollectionEnabled(enabled: Boolean)

    fun logEvent(name: String, parameters: Map<String, String>)

    fun setCrashlyticsKey(key: String, value: Boolean)

    fun setCrashlyticsKey(key: String, value: Int)

    fun remoteConfigBoolean(key: String, fallback: Boolean): Boolean
}

internal class DefaultOnlyFirebasePlatform : FirebasePlatform {
    private var remoteConfigDefaults: Map<String, Any> = emptyMap()

    override val isAvailable: Boolean = false

    override fun initialize(
        configuration: FirebaseRuntimeConfiguration,
        onRemoteConfigActivated: () -> Unit,
    ) {
        remoteConfigDefaults = configuration.remoteConfigDefaults
        onRemoteConfigActivated()
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) = Unit

    override fun logEvent(name: String, parameters: Map<String, String>) = Unit

    override fun setCrashlyticsKey(key: String, value: Boolean) = Unit

    override fun setCrashlyticsKey(key: String, value: Int) = Unit

    override fun remoteConfigBoolean(key: String, fallback: Boolean): Boolean =
        remoteConfigDefaults[key] as? Boolean ?: fallback
}

expect fun firebasePlatform(): FirebasePlatform
