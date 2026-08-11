package com.yenaly.han1meviewer.ui.component.appbar

import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.ui.component.content.EmptyContent
import com.yenaly.han1meviewer.ui.preview.ComponentPreview

@Preview
@Composable
private fun HanimeScaffoldPreview() {
    ComponentPreview {
        HanimeScaffold(
            title = "组件标题",
            subtitle = {
                Text(
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = "副标题"
                )
            },
            onBack = {},
            actions = {
                FilledIconButton(onClick = { }, enabled = true) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_play_arrow_24),
                        contentDescription = stringResource(R.string.start_all),
                    )
                }
                FilledIconButton(onClick = { }, enabled = false) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_pause_24),
                        contentDescription = stringResource(R.string.pause_all),
                    )
                }
            }
        ) {
            EmptyContent("空空的")
        }
    }
}
