package io.github.darriousliu.han1meviewer.feature.video.player

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_pause_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_play_arrow_24
import io.github.darriousliu.han1meviewer.core.resource.ic_player_lock_24
import io.github.darriousliu.han1meviewer.core.resource.ic_player_lock_open_24
import io.github.darriousliu.han1meviewer.core.resource.ic_player_replay_24
import io.github.darriousliu.han1meviewer.core.resource.player_click_to_restart
import io.github.darriousliu.han1meviewer.core.resource.player_replay
import io.github.darriousliu.han1meviewer.core.resource.player_video_loading_failed
import io.github.darriousliu.han1meviewer.core.resource.start_from_beginning
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** 中央播放/暂停/重播按钮（45dp，`jz_start_button_w_h_normal`）；完播时按钮下带「重播」。 */
@Composable
internal fun PlayerCenterButton(
    visual: PlaybackVisual,
    onPlayPause: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        val isEnded = visual == PlaybackVisual.Ended
        IconButton(
            onClick = if (isEnded) onReplay else onPlayPause,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                painter = painterResource(
                    when {
                        isEnded -> Res.drawable.ic_player_replay_24
                        visual == PlaybackVisual.Playing -> Res.drawable.ic_baseline_pause_24
                        else -> Res.drawable.ic_baseline_play_arrow_24
                    }
                ),
                contentDescription = null,
                tint = PlayerWhite,
                modifier = Modifier.size(45.dp),
            )
        }
        if (isEnded) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.player_replay),
                color = PlayerWhite,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
internal fun PlayerLoadingIndicator(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier.size(45.dp),
        color = PlayerWhite,
    )
}

/** 出错重试布局（jzvd `mRetryLayout`）：文案 + 圆角边框重试钮。 */
@Composable
internal fun PlayerErrorRetry(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.player_video_loading_failed),
            color = PlayerWhite,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(15.dp))
        Text(
            text = stringResource(Res.string.player_click_to_restart),
            color = PlayerWhite,
            fontSize = 13.sp,
            modifier = Modifier
                .border(1.dp, PlayerWhite80, RoundedCornerShape(4.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** 全屏锁定钮：30dp、80% 白（`ic_lock_selector` + per80_transparent_white）。 */
@Composable
internal fun PlayerLockButton(
    locked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onToggle, modifier = modifier.size(44.dp)) {
        Icon(
            painter = painterResource(
                if (locked) Res.drawable.ic_player_lock_24 else Res.drawable.ic_player_lock_open_24
            ),
            contentDescription = null,
            tint = PlayerWhite80,
            modifier = Modifier.size(30.dp),
        )
    }
}

/** 续播「從頭開始」钮：底部居中、圆角 20dp 磨砂、粗体 14sp（`btn_resume_progress`）。 */
@Composable
internal fun PlayerResumeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .frostedCapsule()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.start_from_beginning),
            color = PlayerWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
