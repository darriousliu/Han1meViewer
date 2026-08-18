package io.github.darriousliu.han1meviewer.ui.navigation.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.ui.screen.settings.MpvChoiceDialog
import io.github.darriousliu.han1meviewer.ui.screen.settings.MpvPlayerSettingsScreen
import io.github.darriousliu.han1meviewer.ui.screen.settings.MpvPlayerSettingsUiState
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.decoding_auto
import io.github.darriousliu.han1meviewer.core.resource.decoding_hw
import io.github.darriousliu.han1meviewer.core.resource.decoding_hw_plus
import io.github.darriousliu.han1meviewer.core.resource.decoding_sw
import io.github.darriousliu.han1meviewer.core.resource.decoding_vulkan
import io.github.darriousliu.han1meviewer.core.resource.decoding_vulkan_copy
import io.github.darriousliu.han1meviewer.core.resource.mpv_cache_secs_summary
import io.github.darriousliu.han1meviewer.core.resource.mpv_hwdec_summary
import io.github.darriousliu.han1meviewer.core.resource.mpv_network_timeout_summary
import io.github.darriousliu.han1meviewer.core.resource.profile_fast
import io.github.darriousliu.han1meviewer.core.resource.profile_gpu_hq
import org.jetbrains.compose.resources.stringResource

@Composable
fun MpvPlayerSettingsRouteScreen() {
    var refreshKey by remember { mutableIntStateOf(0) }
    var activeDialog by remember { mutableStateOf<MpvChoiceDialog?>(null) }
    val uiState = buildMpvPlayerSettingsUiState(refreshKey)

    MpvPlayerSettingsScreen(
        state = uiState,
        profileOptions = listOf(
            stringResource(Res.string.profile_fast) to "fast",
            stringResource(Res.string.profile_gpu_hq) to "gpu-hq",
        ),
        hwdecOptions = listOf(
            stringResource(Res.string.decoding_auto) to "Auto",
            stringResource(Res.string.decoding_hw) to "HW",
            stringResource(Res.string.decoding_hw_plus) to "HW+",
            stringResource(Res.string.decoding_vulkan_copy) to "Vulkan",
            stringResource(Res.string.decoding_vulkan) to "Vulkan+",
            stringResource(Res.string.decoding_sw) to "SW",
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

/**
 * @param refreshKey 只用来触发重算——`Preferences` 不是可观察状态，
 *   改完得靠它把这个 composable 拉一遍。
 */
@Composable
private fun buildMpvPlayerSettingsUiState(refreshKey: Int): MpvPlayerSettingsUiState {
    val profile = Preferences.mpvProfile
    val hwdec = Preferences.mpvHwdec
    return MpvPlayerSettingsUiState(
        profile = profile,
        profileDisplay = when (profile) {
            "fast" -> stringResource(Res.string.profile_fast)
            "gpu-hq" -> stringResource(Res.string.profile_gpu_hq)
            else -> profile
        },
        enableGpuNextRenderer = Preferences.enableGPUNextRenderer,
        interpolation = Preferences.mpvInterpolation,
        deband = Preferences.mpvDeband,
        framedrop = Preferences.mpvFramedrop,
        hwdec = hwdec,
        hwdecDisplay = "${stringResource(Res.string.mpv_hwdec_summary)} ($hwdec)",
        cacheSecs = Preferences.mpvCacheSecs,
        cacheSecsSummary = "${stringResource(Res.string.mpv_cache_secs_summary)} (${Preferences.mpvCacheSecs} S)",
        tlsVerify = Preferences.mpvTlsVerify,
        networkTimeout = Preferences.mpvNetworkTimeout,
        networkTimeoutSummary = "${stringResource(Res.string.mpv_network_timeout_summary)} (${Preferences.mpvNetworkTimeout} S)",
        customParams = Preferences.customMpvParams,
    )
}
