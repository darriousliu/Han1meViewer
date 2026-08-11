package com.yenaly.han1meviewer.platform

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.database.database
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

actual fun firebasePlatform(): FirebasePlatform = AndroidFirebasePlatform

private object AndroidFirebasePlatform : FirebasePlatform {
    override val isAvailable: Boolean = true

    override fun initialize(
        configuration: FirebaseRuntimeConfiguration,
        onRemoteConfigActivated: () -> Unit,
    ) {
        Firebase.analytics.setAnalyticsCollectionEnabled(
            configuration.analyticsCollectionEnabled,
        )
        Firebase.crashlytics.apply {
            isCrashlyticsCollectionEnabled = configuration.crashlyticsCollectionEnabled
            configuration.crashlyticsStringKeys.forEach { (key, value) ->
                setCustomKey(key, value)
            }
        }
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(remoteConfigSettings {
                minimumFetchIntervalInSeconds =
                    configuration.remoteConfigMinimumFetchIntervalSeconds
                fetchTimeoutInSeconds = configuration.remoteConfigFetchTimeoutSeconds
            })
            setDefaultsAsync(configuration.remoteConfigDefaults)
            fetchAndActivate().addOnCompleteListener {
                onRemoteConfigActivated()
            }
        }
        if (configuration.realtimeDatabasePersistenceEnabled) {
            Firebase.database.setPersistenceEnabled(true)
        }
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        Firebase.analytics.setAnalyticsCollectionEnabled(enabled)
    }

    override fun logEvent(name: String, parameters: Map<String, String>) {
        val bundle = Bundle(parameters.size).apply {
            parameters.forEach { (key, value) -> putString(key, value) }
        }
        Firebase.analytics.logEvent(name, bundle)
    }

    override fun setCrashlyticsKey(key: String, value: Boolean) {
        Firebase.crashlytics.setCustomKey(key, value)
    }

    override fun setCrashlyticsKey(key: String, value: Int) {
        Firebase.crashlytics.setCustomKey(key, value)
    }

    override fun remoteConfigBoolean(key: String, fallback: Boolean): Boolean =
        Firebase.remoteConfig.getBoolean(key)
}
