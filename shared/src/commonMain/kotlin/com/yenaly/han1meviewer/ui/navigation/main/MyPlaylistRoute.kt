package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.ui.component.LocalToaster
import com.yenaly.han1meviewer.ui.component.showShort
import com.yenaly.han1meviewer.ui.screen.home.myplaylist.PlaylistScreen
import com.yenaly.han1meviewer.ui.screen.home.myplaylist.PlaylistUiEvent
import com.yenaly.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import han1meviewer.shared.generated.resources.Res
import han1meviewer.shared.generated.resources.copy_to_clipboard
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun MyPlaylistRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    val viewModel: MyPlayListViewModelV2 = viewModel()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val copiedHint = stringResource(Res.string.copy_to_clipboard)
    PlaylistScreen(
        viewModel = viewModel,
        navigateBack = onBack,
        onClickItem = onNavigateToVideo,
        onLongClickItem = { videoCode, title ->
            onCopy(getHanimeShareText(title, videoCode))
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
