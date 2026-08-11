package com.yenaly.han1meviewer

import androidx.core.net.toUri
import com.yenaly.han1meviewer.HanimeConstants.HANIME_URL
import com.yenaly.han1meviewer.Preferences.loginCookie
import com.yenaly.han1meviewer.logic.network.HProxySelector
import com.yenaly.han1meviewer.logic.network.interceptor.SpeedLimitInterceptor
import com.yenaly.han1meviewer.playback.model.PlaybackDefaults
import com.yenaly.han1meviewer.playback.model.PlaybackEngineType
import com.yenaly.han1meviewer.storage.AppStorage
import com.yenaly.han1meviewer.storage.StorageKey
import com.yenaly.han1meviewer.storage.StorageMutation
import com.yenaly.han1meviewer.storage.StorageSchema
import com.yenaly.han1meviewer.storage.StorageValueKind
import com.yenaly.han1meviewer.ui.navigation.settings.SettingsPreferenceKeys
import com.yenaly.han1meviewer.util.CookieString
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

/**
 * Android compatibility facade while call sites move into commonMain.
 *
 * Values are owned by the typed common storage schema; this object intentionally exposes neither
 * Android SharedPreferences nor raw MMKV APIs.
 */
object Preferences {
    private val authStore
        get() = AppStorage.auth
    private val settingsStore
        get() = AppStorage.settings
    private val uiStateStore
        get() = AppStorage.uiState

    // app related

    /** Whether the user is logged in. This is normally changed together with [loginCookie]. */
    var isAlreadyLogin: Boolean
        get() = authStore.value(StorageSchema.Auth.alreadyLogin)
        set(value) {
            authStore.state(StorageSchema.Auth.alreadyLogin).set(value)
        }

    val loginStateFlow
        get() = authStore.state(StorageSchema.Auth.alreadyLogin).flow

    var usageNoticeAccepted: Boolean
        get() = uiStateStore.value(StorageSchema.UiState.usageNoticeAccepted)
        set(value) {
            uiStateStore.state(StorageSchema.UiState.usageNoticeAccepted).set(value)
        }

    var savedUserId: String
        get() = authStore.value(StorageSchema.Auth.savedUserId)
        set(value) {
            authStore.state(StorageSchema.Auth.savedUserId).set(value)
        }

    /** Saved login cookie in its historical string representation. */
    var loginCookie: CookieString
        get() = CookieString(authStore.value(StorageSchema.Auth.loginCookie))
        set(value) {
            authStore.state(StorageSchema.Auth.loginCookie).set(value.cookie)
        }

    val loginCookieStateFlow
        get() = authStore.state(StorageSchema.Auth.loginCookie).flow

    var cloudFlareCookie: CookieString
        get() = CookieString(authStore.value(StorageSchema.Auth.cloudflareCookie))
        set(value) {
            authStore.state(StorageSchema.Auth.cloudflareCookie).set(value.cookie)
        }

    val cloudFlareCookieStateFlow
        get() = authStore.state(StorageSchema.Auth.cloudflareCookie).flow

    // update related

    var updateNodeId: String
        get() = uiStateStore.value(StorageSchema.UiState.updateNodeId)
        set(value) {
            uiStateStore.state(StorageSchema.UiState.updateNodeId).set(value)
        }

    var lastUpdatePopupTime: Long
        get() = uiStateStore.value(StorageSchema.UiState.lastUpdatePopupTime)
        set(value) {
            uiStateStore.state(StorageSchema.UiState.lastUpdatePopupTime).set(value)
        }

    var lastDismissTime: Long
        get() = uiStateStore.value(StorageSchema.UiState.lastDismissTime)
        set(value) {
            uiStateStore.state(StorageSchema.UiState.lastDismissTime).set(value)
        }

    val updatePopupIntervalDays: Int
        get() = settingsStore.value(StorageSchema.Settings.updatePopupIntervalDays)

    val useCIUpdateChannel: Boolean
        get() = settingsStore.value(StorageSchema.Settings.useCiUpdateChannel)

    @OptIn(ExperimentalTime::class)
    val isUpdateDialogVisible: Boolean
        get() {
            val now = kotlin.time.Clock.System.now()
            val lastCheckTime = kotlin.time.Instant.fromEpochSeconds(lastUpdatePopupTime)
            return now > lastCheckTime + updatePopupIntervalDays.days
        }

    // settings

    val switchPlayerKernel: String
        get() = PlaybackEngineType.fromString(
            settingsStore.value(StorageSchema.Settings.switchPlayerKernel),
        ).persistedValue

    val showBottomProgress: Boolean
        get() = settingsStore.value(StorageSchema.Settings.showBottomProgress)

    val playerSpeed: Float
        get() = settingsStore.value(StorageSchema.Settings.playerSpeed).toFloatOrNull()
            ?: PlaybackDefaults.DEFAULT_SPEED

    val slideSensitivity: Int
        get() = settingsStore.value(StorageSchema.Settings.slideSensitivity)

    val longPressSpeedTime: Float
        get() = settingsStore.value(StorageSchema.Settings.longPressSpeedTimes).toFiniteFloatOrNull()
            ?: PlaybackDefaults.DEFAULT_LONG_PRESS_SPEED_MULTIPLIER

    val videoLanguage: String
        get() = settingsStore.value(StorageSchema.Settings.videoLanguage)

    val videoQuality: String
        get() = settingsStore.value(StorageSchema.Settings.defaultVideoQuality)

    val showPlayedIndicator: Boolean
        get() = settingsStore.value(StorageSchema.Settings.showPlayedIndicator)

    val allowPipMode: Boolean
        get() = settingsStore.value(StorageSchema.Settings.allowPipMode)

    val searchGridColumnsConfig: SearchGridColumnsConfig
        get() = SearchGridColumnsConfig(
            compactColumns = settingsStore.value(StorageSchema.Settings.searchGridColumnsCompact),
            mediumColumns = settingsStore.value(StorageSchema.Settings.searchGridColumnsMedium),
            expandedColumns = settingsStore.value(StorageSchema.Settings.searchGridColumnsExpanded),
            largeColumns = settingsStore.value(StorageSchema.Settings.searchGridColumnsLarge),
        )

    val horizontalCardCountConfig: HorizontalCardCountConfig
        get() = HorizontalCardCountConfig(
            narrowCount = settingsStore.value(StorageSchema.Settings.horizontalCardCountNarrow)
                .toFiniteFloatOrNull() ?: HorizontalCardCountConfig.DEFAULT_NARROW_COUNT,
            compactCount = settingsStore.value(StorageSchema.Settings.horizontalCardCountCompact)
                .toFiniteFloatOrNull() ?: HorizontalCardCountConfig.DEFAULT_COMPACT_COUNT,
            mediumCount = settingsStore.value(StorageSchema.Settings.horizontalCardCountMedium)
                .toFiniteFloatOrNull() ?: HorizontalCardCountConfig.DEFAULT_MEDIUM_COUNT,
            expandedCount = settingsStore.value(StorageSchema.Settings.horizontalCardCountExpanded)
                .toFiniteFloatOrNull() ?: HorizontalCardCountConfig.DEFAULT_EXPANDED_COUNT,
        )

    val fakeLauncherIcon: String
        get() = settingsStore.value(StorageSchema.Settings.fakeLauncherIcon)

    val baseUrl: String
        get() {
            if (useCustomMirrorSite && customMirrorSite.isNotBlank()) {
                val url = if (appendCustomMirrorPath) customMirrorSite else customMirrorSite.toRootUrl()
                return url.toRetrofitBaseUrl()
            }
            return settingsStore.value(StorageSchema.Settings.domainName)
        }

    val homeUrl: String
        get() = if (useCustomMirrorSite && customMirrorSite.isNotBlank()) customMirrorSite else baseUrl

    val useCustomMirrorSite: Boolean
        get() = settingsStore.value(StorageSchema.Settings.useCustomMirrorSite)

    val customMirrorSite: String
        get() = settingsStore.value(StorageSchema.Settings.customMirrorSite)

    val appendCustomMirrorPath: Boolean
        get() = settingsStore.value(StorageSchema.Settings.appendCustomMirrorPath)

    private fun String.toRetrofitBaseUrl(): String = if (endsWith('/')) this else "$this/"

    private fun String.toRootUrl(): String {
        val uri = toUri()
        return "${uri.scheme}://${uri.encodedAuthority}"
    }

    val selectedBaseUrl: String
        get() = settingsStore.value(StorageSchema.Settings.selectedBaseUrl)

    val useBuiltInHosts: Boolean
        get() = settingsStore.value(StorageSchema.Settings.useBuiltInHosts)

    val customHostsData: String
        get() = settingsStore.value(StorageSchema.Settings.customHostsData)

    val useDoH: Boolean
        get() = settingsStore.value(StorageSchema.Settings.useDoh)

    val dohPreset: String
        get() = settingsStore.value(StorageSchema.Settings.dohPreset)

    val dohCustomUrl: String
        get() = settingsStore.value(StorageSchema.Settings.dohCustomUrl)

    val dohBootstrapIps: String
        get() = settingsStore.value(StorageSchema.Settings.dohBootstrapIps)

    val dohTimeoutSeconds: Int
        get() = settingsStore.value(StorageSchema.Settings.dohTimeoutSeconds)

    val whenCountdownRemind: Int
        get() = settingsStore.value(StorageSchema.Settings.whenCountdownRemind) * 1_000

    val showCommentWhenCountdown: Boolean
        get() = settingsStore.value(StorageSchema.Settings.showCommentWhenCountdown)

    val hKeyframesEnable: Boolean
        get() = settingsStore.value(StorageSchema.Settings.hKeyframesEnable)

    val sharedHKeyframesEnable: Boolean
        get() = settingsStore.value(StorageSchema.Settings.sharedHKeyframesEnable)

    val sharedHKeyframesUseFirst: Boolean
        get() = settingsStore.value(StorageSchema.Settings.sharedHKeyframesUseFirst)

    val proxyType: Int
        get() = settingsStore.value(StorageSchema.Settings.proxyType)

    val proxyIp: String
        get() = settingsStore.value(StorageSchema.Settings.proxyIp)

    val proxyPort: Int
        get() = settingsStore.value(StorageSchema.Settings.proxyPort)

    val isAnalyticsEnabled: Boolean
        get() = settingsStore.value(StorageSchema.Settings.useAnalytics)

    val downloadCountLimit: Int
        get() = settingsStore.value(StorageSchema.Settings.downloadCountLimit)

    val collapseDownloadedGroup: Boolean
        get() = settingsStore.value(StorageSchema.Settings.collapseDownloadedGroup)

    val isUsePrivateStorage: Boolean
        get() = settingsStore.value(StorageSchema.Settings.usePrivateStorage)

    val safDownloadPath: String?
        get() = settingsStore.value(StorageSchema.Settings.safDownloadPath)

    val useDarkMode: String
        get() = settingsStore.value(StorageSchema.Settings.useDarkMode)

    val useDynamicColor: Boolean
        get() = settingsStore.value(StorageSchema.Settings.useDynamicColor)

    val themeColor: String?
        get() = settingsStore.value(StorageSchema.Settings.themeColor)

    val allowResumePlayback: Boolean
        get() = settingsStore.value(StorageSchema.Settings.allowResumePlayback)

    val searchArtistIgnoreVideoType: Boolean
        get() = settingsStore.value(StorageSchema.Settings.searchArtistIgnoreVideoType)

    val disableMobileDataWarning: Boolean
        get() = settingsStore.value(StorageSchema.Settings.disableMobileDataWarning)

    val disablePredictiveBack: Boolean
        get() = settingsStore.value(StorageSchema.Settings.disablePredictiveBack)

    val tabletMode: Boolean
        get() = settingsStore.value(StorageSchema.Settings.tabletMode)

    val mpvProfile: String
        get() = settingsStore.value(StorageSchema.Settings.mpvProfile)

    val enableGPUNextRenderer: Boolean
        get() = settingsStore.value(StorageSchema.Settings.enableGpuNextRenderer)

    val mpvInterpolation: Boolean
        get() = settingsStore.value(StorageSchema.Settings.mpvInterpolation)

    val mpvDeband: Boolean
        get() = settingsStore.value(StorageSchema.Settings.mpvDeband)

    val mpvFramedrop: Boolean
        get() = settingsStore.value(StorageSchema.Settings.mpvFramedrop)

    val mpvHwdec: String
        get() = settingsStore.value(StorageSchema.Settings.mpvHwdec)

    val mpvCacheSecs: Int
        get() = settingsStore.value(StorageSchema.Settings.mpvCacheSecs)

    val mpvTlsVerify: Boolean
        get() = settingsStore.value(StorageSchema.Settings.mpvTlsVerify)

    val mpvNetworkTimeout: Int
        get() = settingsStore.value(StorageSchema.Settings.mpvNetworkTimeout)

    val customMpvParams: String
        get() = settingsStore.value(StorageSchema.Settings.customMpvParams)

    val downloadSpeedLimit: Long
        get() {
            val index = settingsStore.value(StorageSchema.Settings.downloadSpeedLimit)
            return SpeedLimitInterceptor.SPEED_BYTES[index]
        }

    fun getBooleanSetting(key: String, defaultValue: Boolean): Boolean =
        readSetting(key, StorageValueKind.Boolean) as? Boolean ?: defaultValue

    fun getIntSetting(key: String, defaultValue: Int): Int =
        readSetting(key, StorageValueKind.Int) as? Int ?: defaultValue

    fun getStringSetting(key: String, defaultValue: String?): String? =
        readSetting(key, StorageValueKind.String) as? String ?: defaultValue

    fun editSettings(block: SettingsEditor.() -> Unit): Boolean {
        val editor = SettingsEditor().apply(block)
        return settingsStore.writeBatch(editor.mutations).isFullySuccessful
    }

    class SettingsEditor internal constructor() {
        internal val mutations = mutableListOf<StorageMutation>()

        fun putBoolean(key: String, value: Boolean) {
            mutations += StorageMutation.Set(requireBooleanKey(key), value)
        }

        fun putInt(key: String, value: Int) {
            mutations += StorageMutation.Set(requireIntKey(key), value)
        }

        fun putString(key: String, value: String?) {
            mutations += if (value == null) {
                StorageMutation.Remove(requireStringKey(key))
            } else {
                StorageMutation.Set(requireStringKey(key), value)
            }
        }

        fun remove(key: String) {
            mutations += StorageMutation.Remove(settingsStore.requireKey(key))
        }
    }

    private fun readSetting(key: String, expectedKind: StorageValueKind): Any? {
        val storageKey = settingsStore.findKey(key) ?: return null
        if (storageKey.codec.storageKind != expectedKind) return null
        return settingsStore.value(storageKey)
    }

    @Suppress("UNCHECKED_CAST")
    private fun requireBooleanKey(name: String): StorageKey<Boolean> =
        settingsStore.requireKey(name).also {
            require(it.codec.storageKind == StorageValueKind.Boolean) {
                "Setting $name is ${it.codec.storageKind}, not Boolean"
            }
        } as StorageKey<Boolean>

    @Suppress("UNCHECKED_CAST")
    private fun requireIntKey(name: String): StorageKey<Int> =
        settingsStore.requireKey(name).also {
            require(it.codec.storageKind == StorageValueKind.Int) {
                "Setting $name is ${it.codec.storageKind}, not Int"
            }
        } as StorageKey<Int>

    @Suppress("UNCHECKED_CAST")
    private fun requireStringKey(name: String): StorageKey<String> =
        settingsStore.requireKey(name).also {
            require(it.codec.storageKind == StorageValueKind.String) {
                "Setting $name is ${it.codec.storageKind}, not String"
            }
        } as StorageKey<String>
}

private fun String.toFiniteFloatOrNull(): Float? =
    toFloatOrNull()?.takeIf(Float::isFinite)
