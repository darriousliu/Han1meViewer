package com.yenaly.han1meviewer.ui.navigation.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.yenaly.han1meviewer.PlayerDefaults
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.screen.settings.PlayerSettingsScreen
import com.yenaly.han1meviewer.ui.screen.settings.PlayerSettingsUiState
import com.yenaly.han1meviewer.ui.view.video.HJzvdStd
import com.yenaly.han1meviewer.ui.view.video.HMediaKernel

@Composable
fun PlayerSettingsRouteScreen(
    onNavigateToMpvSettings: () -> Unit,
) {
    val context = LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val uiState = buildPlayerSettingsUiState(context, refreshKey)

    PlayerSettingsScreen(
        state = uiState,
        // 第四项不是 HMediaKernel.Type 的成员——那个枚举每项都要带一个
        // Class<out JZMediaInterface>，而 Compose 播放器根本不经过 jzvd。
        kernelOptions = HMediaKernel.Type.entries.map { it.name to it.name } +
                (PlayerDefaults.KERNEL_EXO_COMPOSE to PlayerDefaults.KERNEL_EXO_COMPOSE),
        speedOptions = HJzvdStd.speedStringArray.zip(HJzvdStd.speedArray.map { it.toString() }),
        longPressSpeedOptions = listOf(
            stringResource(R.string.d_speed_times, 1f) to "1",
            stringResource(R.string.d_speed_times, 1.5f) to "1.5",
            stringResource(R.string.d_speed_times, 2f) to "2",
            "${
                stringResource(
                    R.string.d_speed_times,
                    2.5f
                )
            } (${stringResource(R.string.default_)})" to "2.5",
            stringResource(R.string.d_speed_times, 2.8f) to "2.8",
            stringResource(R.string.d_speed_times, 3f) to "3",
            stringResource(R.string.d_speed_times, 3.2f) to "3.2",
            stringResource(R.string.d_speed_times, 3.5f) to "3.5",
            stringResource(R.string.d_speed_times, 3.8f) to "3.8",
            stringResource(R.string.d_speed_times, 4f) to "4",
        ),
        onKernelChange = {
            Preferences.switchPlayerKernel = it
            refreshKey++
        },
        onShowBottomProgressChange = {
            Preferences.showBottomProgress = it
            refreshKey++
        },
        onPlayerSpeedChange = {
            Preferences.playerSpeed = it.toFloatOrNull() ?: PlayerDefaults.SPEED
            refreshKey++
        },
        onLongPressSpeedChange = {
            Preferences.longPressSpeedTime = it.toFloatOrNull() ?: PlayerDefaults.LONG_PRESS_SPEED_TIMES
            refreshKey++
        },
        onSlideSensitivityChange = {
            Preferences.slideSensitivity = it
            refreshKey++
        },
        onOpenMpvSettings = onNavigateToMpvSettings,
    )
}

/**
 * @param refreshKey 只用来触发重算——`Preferences` 不是可观察状态，
 *   改完得靠它把这个 composable 拉一遍。
 */
@Composable
private fun buildPlayerSettingsUiState(context: Context, refreshKey: Int): PlayerSettingsUiState {
    val kernel = Preferences.switchPlayerKernel
    val isMpvPlayer = kernel == HMediaKernel.Type.MpvPlayer.name
    val currentSpeed = Preferences.playerSpeed
    val currentLongPressSpeed = Preferences.longPressSpeedTime
    val speedDisplay = HJzvdStd.speedStringArray.getOrElse(
        HJzvdStd.speedArray.indexOfFirst { it == currentSpeed }.takeIf { it >= 0 }
            ?: HJzvdStd.DEF_SPEED_INDEX
    ) { HJzvdStd.speedStringArray[HJzvdStd.DEF_SPEED_INDEX] }
    val longPressDisplay = context.getString(R.string.d_speed_times, currentLongPressSpeed)
    return PlayerSettingsUiState(
        kernel = kernel,
        kernelDisplay = kernel,
        mpvSettingsEnabled = isMpvPlayer,
        mpvSettingsSummary = if (isMpvPlayer) {
            context.getString(R.string.mpv_advanced_settings_summary)
        } else {
            context.getString(R.string.mpv_settings_disabled_summary)
        },
        showBottomProgress = Preferences.showBottomProgress,
        playerSpeed = currentSpeed.toString(),
        playerSpeedLabel = speedDisplay,
        longPressSpeedTimes = currentLongPressSpeed.toOptionValue(),
        longPressSpeedTimesLabel = longPressDisplay,
        slideSensitivity = Preferences.slideSensitivity,
        slideSensitivitySummary = toPrettySensitivityString(Preferences.slideSensitivity),
    )
}

/**
 * [longPressSpeedOptions] 里的 value 是手写的 `"2"` `"3"` 这种，没有小数点，
 * 而 `Preferences.longPressSpeedTime` 现在是 Float（迁移前 SharedPreferences 存的是 String）。
 * 直接 `toString()` 会得到 `"2.0"`，下拉框就选不中了，所以整数值要去掉 `.0`。
 *
 * `speedOptions` 不用管：它的 value 本来就是 `speedArray.map { it.toString() }` 生成的。
 */
private fun Float.toOptionValue(): String =
    if (this == toInt().toFloat()) toInt().toString() else toString()
