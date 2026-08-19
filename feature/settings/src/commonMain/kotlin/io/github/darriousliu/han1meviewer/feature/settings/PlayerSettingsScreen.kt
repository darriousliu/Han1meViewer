package io.github.darriousliu.han1meviewer.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.darriousliu.han1meviewer.core.ui.component.ChoiceDialog
import io.github.darriousliu.han1meviewer.core.ui.component.SettingNavigationItem
import io.github.darriousliu.han1meviewer.core.ui.component.SettingSliderItem
import io.github.darriousliu.han1meviewer.core.ui.component.SettingSwitchItem
import io.github.darriousliu.han1meviewer.core.ui.component.lazy.LazyColumn
import io.github.darriousliu.han1meviewer.core.ui.preview.ComponentPreview
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.baseline_atomic_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_player_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_seek_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_speed2_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_speed_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_touch_24
import io.github.darriousliu.han1meviewer.core.resource.current_slide_sensitivity
import io.github.darriousliu.han1meviewer.core.resource.default_playback_speed
import io.github.darriousliu.han1meviewer.core.resource.long_press_speed_multiplier
import io.github.darriousliu.han1meviewer.core.resource.moderate
import io.github.darriousliu.han1meviewer.core.resource.mpv_advanced_settings
import io.github.darriousliu.han1meviewer.core.resource.mpv_settings_disabled_summary
import io.github.darriousliu.han1meviewer.core.resource.show_bottom_progress
import io.github.darriousliu.han1meviewer.core.resource.slide_sensitivity
import io.github.darriousliu.han1meviewer.core.resource.switch_player_kernel
import org.jetbrains.compose.resources.stringResource

data class PlayerSettingsUiState(
    val kernel: String,
    val kernelDisplay: String,
    val mpvSettingsEnabled: Boolean,
    val mpvSettingsSummary: String,
    val showBottomProgress: Boolean,
    val playerSpeed: String,
    val playerSpeedLabel: String,
    val longPressSpeedTimes: String,
    val longPressSpeedTimesLabel: String,
    val slideSensitivity: Int,
    val slideSensitivitySummary: String,
)

private enum class PlayerChoiceDialog {
    Kernel,
    Speed,
    LongPressSpeed,
}

@Composable
fun PlayerSettingsScreen(
    state: PlayerSettingsUiState,
    kernelOptions: List<Pair<String, String>>,
    speedOptions: List<Pair<String, String>>,
    longPressSpeedOptions: List<Pair<String, String>>,
    onKernelChange: (String) -> Unit,
    onShowBottomProgressChange: (Boolean) -> Unit,
    onPlayerSpeedChange: (String) -> Unit,
    onLongPressSpeedChange: (String) -> Unit,
    onSlideSensitivityChange: (Int) -> Unit,
    onOpenMpvSettings: () -> Unit,
) {
    var activeDialog by rememberSaveable { mutableStateOf<PlayerChoiceDialog?>(null) }

    ChoiceDialog(
        visible = activeDialog == PlayerChoiceDialog.Kernel,
        title = stringResource(Res.string.switch_player_kernel),
        options = kernelOptions,
        selectedValue = state.kernel,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onKernelChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == PlayerChoiceDialog.Speed,
        title = stringResource(Res.string.default_playback_speed),
        options = speedOptions,
        selectedValue = state.playerSpeed,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onPlayerSpeedChange(it)
        },
    )

    ChoiceDialog(
        visible = activeDialog == PlayerChoiceDialog.LongPressSpeed,
        title = stringResource(Res.string.long_press_speed_multiplier),
        options = longPressSpeedOptions,
        selectedValue = state.longPressSpeedTimes,
        onDismiss = { activeDialog = null },
        onSelect = {
            activeDialog = null
            onLongPressSpeedChange(it)
        },
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            SettingNavigationItem(
                title = stringResource(Res.string.switch_player_kernel),
                valueText = state.kernelDisplay,
                iconRes = Res.drawable.baseline_atomic_24,
                onClick = { activeDialog = PlayerChoiceDialog.Kernel },
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(Res.string.mpv_advanced_settings),
                summary = state.mpvSettingsSummary,
                iconRes = Res.drawable.baseline_player_24,
                onClick = onOpenMpvSettings,
                enabled = state.mpvSettingsEnabled,
                valueText = null,
            )
        }

        item {
            SettingSwitchItem(
                title = stringResource(Res.string.show_bottom_progress),
                checked = state.showBottomProgress,
                iconRes = Res.drawable.baseline_seek_24,
                onCheckedChange = onShowBottomProgressChange,
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(Res.string.default_playback_speed),
                valueText = state.playerSpeedLabel,
                iconRes = Res.drawable.baseline_speed2_24,
                onClick = { activeDialog = PlayerChoiceDialog.Speed },
            )
        }

        item {
            SettingNavigationItem(
                title = stringResource(Res.string.long_press_speed_multiplier),
                valueText = state.longPressSpeedTimesLabel,
                iconRes = Res.drawable.baseline_touch_24,
                onClick = { activeDialog = PlayerChoiceDialog.LongPressSpeed },
            )
        }

        item {
            SettingSliderItem(
                title = stringResource(Res.string.slide_sensitivity),
                summary = state.slideSensitivitySummary,
                value = state.slideSensitivity,
                valueRange = 1..9,
                iconRes = Res.drawable.baseline_speed_24,
                onValueChange = onSlideSensitivityChange,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 760)
@Composable
private fun PlayerSettingsScreenPreview() {
    ComponentPreview {
        PlayerSettingsScreen(
            state = PlayerSettingsUiState(
                kernel = "ExoPlayer",
                kernelDisplay = "ExoPlayer",
                mpvSettingsEnabled = false,
                mpvSettingsSummary = stringResource(Res.string.mpv_settings_disabled_summary),
                showBottomProgress = true,
                playerSpeed = "1.0",
                playerSpeedLabel = "1.0x",
                longPressSpeedTimes = "2.5",
                longPressSpeedTimesLabel = "2.5倍",
                slideSensitivity = 5,
                slideSensitivitySummary = stringResource(
                    Res.string.current_slide_sensitivity,
                    stringResource(Res.string.moderate)
                ),
            ),
            kernelOptions = listOf(
                "MediaPlayer" to "MediaPlayer",
                "ExoPlayer" to "ExoPlayer",
                "MpvPlayer" to "MpvPlayer"
            ),
            speedOptions = listOf("1.0x" to "1.0", "1.25x" to "1.25", "1.5x" to "1.5"),
            longPressSpeedOptions = listOf("2.0倍" to "2", "2.5倍" to "2.5", "3.0倍" to "3"),
            onKernelChange = {},
            onShowBottomProgressChange = {},
            onPlayerSpeedChange = {},
            onLongPressSpeedChange = {},
            onSlideSensitivityChange = {},
            onOpenMpvSettings = {},
        )
    }
}
