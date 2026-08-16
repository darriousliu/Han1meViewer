package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.screen.settings.MpvChoiceDialog
import com.yenaly.han1meviewer.ui.screen.settings.MpvPlayerSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.MpvPlayerSettingsUiState

@Composable
fun MpvPlayerSettingsRouteScreen() {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    var activeDialog by remember { mutableStateOf<MpvChoiceDialog?>(null) }
    val uiState = remember(refreshKey, context) { buildMpvPlayerSettingsUiState(context) }

    MpvPlayerSettingsScreen(
        state = uiState,
        profileOptions = listOf(
            stringResource(R.string.profile_fast) to "fast",
            stringResource(R.string.profile_gpu_hq) to "gpu-hq",
        ),
        hwdecOptions = listOf(
            stringResource(R.string.decoding_auto) to "Auto",
            stringResource(R.string.decoding_hw) to "HW",
            stringResource(R.string.decoding_hw_plus) to "HW+",
            stringResource(R.string.decoding_vulkan_copy) to "Vulkan",
            stringResource(R.string.decoding_vulkan) to "Vulkan+",
            stringResource(R.string.decoding_sw) to "SW",
        ),
        activeDialog = activeDialog,
        onOpenProfileDialog = { activeDialog = MpvChoiceDialog.Profile },
        onOpenHwdecDialog = { activeDialog = MpvChoiceDialog.Hwdec },
        onOpenCustomParamsDialog = { activeDialog = MpvChoiceDialog.CustomParams },
        onDismissDialog = { activeDialog = null },
        onProfileChange = {
            Preferences.mpvProfile = it
            refreshKey++
        },
        onEnableGpuNextRendererChange = {
            Preferences.enableGPUNextRenderer = it
            refreshKey++
        },
        onInterpolationChange = {
            Preferences.mpvInterpolation = it
            refreshKey++
        },
        onDebandChange = {
            Preferences.mpvDeband = it
            refreshKey++
        },
        onFramedropChange = {
            Preferences.mpvFramedrop = it
            refreshKey++
        },
        onHwdecChange = {
            Preferences.mpvHwdec = it
            refreshKey++
        },
        onCacheSecsChange = {
            Preferences.mpvCacheSecs = it
            refreshKey++
        },
        onTlsVerifyChange = {
            Preferences.mpvTlsVerify = it
            refreshKey++
        },
        onNetworkTimeoutChange = {
            Preferences.mpvNetworkTimeout = it
            refreshKey++
        },
        onCustomParamsChange = {
            Preferences.customMpvParams = it
            refreshKey++
        },
    )
}

private fun buildMpvPlayerSettingsUiState(context: Context): MpvPlayerSettingsUiState {
    val profile = Preferences.mpvProfile
    val hwdec = Preferences.mpvHwdec
    return MpvPlayerSettingsUiState(
        profile = profile,
        profileDisplay = when (profile) {
            "fast" -> context.getString(R.string.profile_fast)
            "gpu-hq" -> context.getString(R.string.profile_gpu_hq)
            else -> profile
        },
        enableGpuNextRenderer = Preferences.enableGPUNextRenderer,
        interpolation = Preferences.mpvInterpolation,
        deband = Preferences.mpvDeband,
        framedrop = Preferences.mpvFramedrop,
        hwdec = hwdec,
        hwdecDisplay = "${context.getString(R.string.mpv_hwdec_summary)} ($hwdec)",
        cacheSecs = Preferences.mpvCacheSecs,
        cacheSecsSummary = "${context.getString(R.string.mpv_cache_secs_summary)} (${Preferences.mpvCacheSecs} S)",
        tlsVerify = Preferences.mpvTlsVerify,
        networkTimeout = Preferences.mpvNetworkTimeout,
        networkTimeoutSummary = "${context.getString(R.string.mpv_network_timeout_summary)} (${Preferences.mpvNetworkTimeout} S)",
        customParams = Preferences.customMpvParams,
    )
}
