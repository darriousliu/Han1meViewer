package io.github.darriousliu.han1meviewer.ui.component.appbar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.darriousliu.han1meviewer.ui.component.content.EmptyContent
import io.github.darriousliu.han1meviewer.ui.preview.ComponentPreview
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_pause_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_play_arrow_24
import io.github.darriousliu.han1meviewer.core.resource.pause_all
import io.github.darriousliu.han1meviewer.core.resource.start_all
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HanimeScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            HanimeTopAppBar(
                title = title,
                onBack = onBack,
                subtitle = subtitle,
                actions = actions,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        content = content,
    )
}

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
                        painter = painterResource(Res.drawable.ic_baseline_play_arrow_24),
                        contentDescription = stringResource(Res.string.start_all),
                    )
                }
                FilledIconButton(onClick = { }, enabled = false) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_baseline_pause_24),
                        contentDescription = stringResource(Res.string.pause_all),
                    )
                }
            }
        ) {
            EmptyContent("空空的")
        }
    }
}