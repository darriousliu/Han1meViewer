package io.github.darriousliu.han1meviewer.feature.mylist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darriousliu.han1meviewer.core.storage.getHanimeShareText
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.showShort
import io.github.darriousliu.han1meviewer.feature.mylist.playlist.PlaylistScreen
import io.github.darriousliu.han1meviewer.feature.mylist.playlist.PlaylistUiEvent
import io.github.darriousliu.han1meviewer.core.common.util.setPlainText
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.copy_to_clipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MyPlaylistRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val clipboard = LocalClipboard.current
    val viewModel: MyPlayListViewModelV2 = koinViewModel()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val copiedHint = stringResource(Res.string.copy_to_clipboard)
    PlaylistScreen(
        viewModel = viewModel,
        navigateBack = onBack,
        onClickItem = onNavigateToVideo,
        onLongClickItem = { videoCode, title ->
            scope.launch { clipboard.setPlainText(getHanimeShareText(title, videoCode)) }
            toaster.showShort(copiedHint)
        },
        // 屏幕只发事件，提示由这里弹。getString 是挂起函数，要包 scope.launch。
        onUiEvent = { event ->
            when (event) {
                is PlaylistUiEvent.ShowMessage ->
                    scope.launch { toaster.showShort(getString(event.messageRes)) }
            }
        },
    )
}
