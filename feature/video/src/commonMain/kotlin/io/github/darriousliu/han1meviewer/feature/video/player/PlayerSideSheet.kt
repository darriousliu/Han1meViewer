package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.darriousliu.han1meviewer.core.common.PlayerDefaults
import io.github.darriousliu.han1meviewer.core.common.util.formatVideoTime
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.here_is_empty
import io.github.darriousliu.han1meviewer.core.resource.long_press_to_add_h_keyframe
import io.github.darriousliu.han1meviewer.core.resource.super_resolution_off
import io.github.darriousliu.han1meviewer.core.resource.super_resolution_performance
import io.github.darriousliu.han1meviewer.core.resource.super_resolution_quality
import io.github.darriousliu.han1meviewer.core.storage.entity.HKeyframeEntity
import org.jetbrains.compose.resources.stringResource

/**
 * 右侧 240dp 全高滑入面板（对应 `jz_layout_speed.xml` 的 PopupWindow + pop_animation）。
 * 倍速 / 画质 / 超分 / H 帧列表共用一个面板；点面板外关闭；
 * 选倍速/超分/画质后关闭，点 H 帧 seek 但**不关**（jzvd 行为）。
 */
@Composable
internal fun PlayerSideSheet(
    menu: PlayerMenu,
    currentSpeedIndex: Int,
    onSelectSpeed: (Int) -> Unit,
    qualityKeys: List<String>,
    currentQuality: String?,
    onSelectQuality: (String) -> Unit,
    showSuperResolution: Boolean,
    currentSuperResolutionIndex: Int,
    onSelectSuperResolution: (Int) -> Unit,
    hKeyframes: HKeyframeEntity?,
    onSeekToKeyframe: (Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (menu != PlayerMenu.None) {
            // 面板外的透明捕获层：点一下就关（PopupWindow outside-touchable 的等价）
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    )
            )
        }
        AnimatedVisibility(
            visible = menu != PlayerMenu.None,
            enter = slideInHorizontally(tween(200)) { it },
            exit = slideOutHorizontally(tween(200)) { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .background(FrostedGlassColor),
            ) {
                when (menu) {
                    PlayerMenu.Speed -> SelectionList(
                        options = PlayerDefaults.SPEED_LABELS,
                        selectedIndex = currentSpeedIndex,
                        onSelect = onSelectSpeed,
                    )

                    PlayerMenu.Clarity -> SelectionList(
                        options = qualityKeys,
                        selectedIndex = qualityKeys.indexOf(currentQuality),
                        onSelect = { index -> onSelectQuality(qualityKeys[index]) },
                    )

                    PlayerMenu.SuperResolution -> if (showSuperResolution) {
                        SelectionList(
                            options = listOf(
                                stringResource(Res.string.super_resolution_off),
                                stringResource(Res.string.super_resolution_performance),
                                stringResource(Res.string.super_resolution_quality),
                            ),
                            selectedIndex = currentSuperResolutionIndex,
                            onSelect = onSelectSuperResolution,
                        )
                    }

                    PlayerMenu.HKeyframes -> HKeyframeList(
                        hKeyframes = hKeyframes,
                        onSeekToKeyframe = onSeekToKeyframe,
                    )

                    PlayerMenu.None -> Unit
                }
            }
        }
    }
}

@Composable
private fun SelectionList(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        items(options.size) { index ->
            Text(
                text = options[index],
                color = if (index == selectedIndex) {
                    MaterialTheme.colorScheme.primary
                } else {
                    PlayerWhite
                },
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun HKeyframeList(
    hKeyframes: HKeyframeEntity?,
    onSeekToKeyframe: (Long) -> Unit,
) {
    val keyframes = hKeyframes?.keyframes.orEmpty()
    if (keyframes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.here_is_empty) + "\n" +
                        stringResource(Res.string.long_press_to_add_h_keyframe),
                color = PlayerWhite80,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(keyframes) { keyframe ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSeekToKeyframe(keyframe.position) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    text = formatVideoTime(keyframe.position),
                    color = PlayerWhite,
                    fontSize = 16.sp,
                )
                val prompt = keyframe.prompt
                if (!prompt.isNullOrBlank()) {
                    Text(
                        text = prompt,
                        color = PlayerWhite80,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
