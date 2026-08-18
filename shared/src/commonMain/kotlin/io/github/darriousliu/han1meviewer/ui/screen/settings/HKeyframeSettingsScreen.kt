package io.github.darriousliu.han1meviewer.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.darriousliu.han1meviewer.core.ui.component.SettingNavigationItem
import io.github.darriousliu.han1meviewer.core.ui.component.SettingSliderItem
import io.github.darriousliu.han1meviewer.core.ui.component.SettingSwitchItem
import io.github.darriousliu.han1meviewer.core.ui.component.lazy.LazyColumn
import io.github.darriousliu.han1meviewer.core.ui.preview.ComponentPreview
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.baseline_count_down_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_h_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_manage_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_online_manage_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_share_24
import io.github.darriousliu.han1meviewer.core.resource.baseline_share_first_24
import io.github.darriousliu.han1meviewer.core.resource.custom
import io.github.darriousliu.han1meviewer.core.resource.h_keyframe_manage
import io.github.darriousliu.han1meviewer.core.resource.h_keyframes_enable
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_alert_24
import io.github.darriousliu.han1meviewer.core.resource.manage
import io.github.darriousliu.han1meviewer.core.resource.shared
import io.github.darriousliu.han1meviewer.core.resource.shared_h_keyframe_manage
import io.github.darriousliu.han1meviewer.core.resource.shared_h_keyframe_manage_tip
import io.github.darriousliu.han1meviewer.core.resource.shared_h_keyframes_enable
import io.github.darriousliu.han1meviewer.core.resource.shared_h_keyframes_enable_tip
import io.github.darriousliu.han1meviewer.core.resource.shared_h_keyframes_use_first
import io.github.darriousliu.han1meviewer.core.resource.shared_h_keyframes_use_first_tip
import io.github.darriousliu.han1meviewer.core.resource.show_prompt_when_countdown
import io.github.darriousliu.han1meviewer.core.resource.when_countdown_remind
import org.jetbrains.compose.resources.stringResource

data class HKeyframeSettingsUiState(
    val hKeyframesEnable: Boolean,
    val hKeyframesSummary: String,
    val sharedHKeyframesEnable: Boolean,
    val sharedHKeyframesUseFirst: Boolean,
    val showCommentWhenCountdown: Boolean,
    val whenCountdownRemind: Int,
    val whenCountdownRemindSummary: String,
)

@Composable
fun HKeyframeSettingsScreen(
    state: HKeyframeSettingsUiState,
    onHKeyframesEnableChange: (Boolean) -> Unit,
    onOpenHKeyframeManage: () -> Unit,
    onSharedHKeyframesEnableChange: (Boolean) -> Unit,
    onSharedHKeyframesUseFirstChange: (Boolean) -> Unit,
    onOpenSharedHKeyframeManage: () -> Unit,
    onShowCommentWhenCountdownChange: (Boolean) -> Unit,
    onWhenCountdownRemindChange: (Int) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            SettingSwitchItem(
                title = stringResource(Res.string.h_keyframes_enable),
                summary = state.hKeyframesSummary,
                checked = state.hKeyframesEnable,
                iconRes = Res.drawable.baseline_h_24,
                onCheckedChange = onHKeyframesEnableChange,
            )
        }

        if (state.hKeyframesEnable) {
            item { HKeyframeGroupTitle(stringResource(Res.string.manage)) }
            item {
                SettingNavigationItem(
                    title = stringResource(Res.string.h_keyframe_manage),
                    iconRes = Res.drawable.baseline_manage_24,
                    onClick = onOpenHKeyframeManage,
                )
            }

            item { HKeyframeGroupTitle(stringResource(Res.string.shared)) }
            item {
                SettingSwitchItem(
                    title = stringResource(Res.string.shared_h_keyframes_enable),
                    summary = stringResource(Res.string.shared_h_keyframes_enable_tip),
                    checked = state.sharedHKeyframesEnable,
                    iconRes = Res.drawable.baseline_share_24,
                    onCheckedChange = onSharedHKeyframesEnableChange,
                )
            }

            if (state.sharedHKeyframesEnable) {
                item {
                    SettingSwitchItem(
                        title = stringResource(Res.string.shared_h_keyframes_use_first),
                        summary = stringResource(Res.string.shared_h_keyframes_use_first_tip),
                        checked = state.sharedHKeyframesUseFirst,
                        iconRes = Res.drawable.baseline_share_first_24,
                        onCheckedChange = onSharedHKeyframesUseFirstChange,
                    )
                }
                item {
                    SettingNavigationItem(
                        title = stringResource(Res.string.shared_h_keyframe_manage),
                        summary = stringResource(Res.string.shared_h_keyframe_manage_tip),
                        iconRes = Res.drawable.baseline_online_manage_24,
                        onClick = onOpenSharedHKeyframeManage,
                    )
                }
            }

            item { HKeyframeGroupTitle(stringResource(Res.string.custom)) }
            item {
                SettingSwitchItem(
                    title = stringResource(Res.string.show_prompt_when_countdown),
                    checked = state.showCommentWhenCountdown,
                    iconRes = Res.drawable.baseline_count_down_24,
                    onCheckedChange = onShowCommentWhenCountdownChange,
                )
            }
            item {
                SettingSliderItem(
                    title = stringResource(Res.string.when_countdown_remind),
                    summary = state.whenCountdownRemindSummary,
                    value = state.whenCountdownRemind,
                    valueRange = 5..30,
                    iconRes = Res.drawable.ic_baseline_alert_24,
                    onValueChange = onWhenCountdownRemindChange,
                )
            }
        }
    }
}

@Composable
private fun HKeyframeGroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun HKeyframeSettingsScreenPreview() {
    ComponentPreview {
        HKeyframeSettingsScreen(
            state = HKeyframeSettingsUiState(
                hKeyframesEnable = true,
                hKeyframesSummary = "开启后，播放器顶部会显示🥵",
                sharedHKeyframesEnable = true,
                sharedHKeyframesUseFirst = false,
                showCommentWhenCountdown = false,
                whenCountdownRemind = 10,
                whenCountdownRemindSummary = "将会在 10 秒前倒数计时提醒 (預設)",
            ),
            onHKeyframesEnableChange = {},
            onOpenHKeyframeManage = {},
            onSharedHKeyframesEnableChange = {},
            onSharedHKeyframesUseFirstChange = {},
            onOpenSharedHKeyframeManage = {},
            onShowCommentWhenCountdownChange = {},
            onWhenCountdownRemindChange = {},
        )
    }
}
