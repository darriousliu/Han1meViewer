package com.yenaly.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeShareText
import com.yenaly.han1meviewer.ui.screen.home.myplaylist.PlaylistScreen
import com.yenaly.han1meviewer.ui.screen.home.myplaylist.PlaylistUiEvent
import com.yenaly.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import com.yenaly.han1meviewer.util.copyTextToClipboard
import com.yenaly.han1meviewer.util.showShortToast
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@Composable
fun MyPlaylistRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val viewModel: MyPlayListViewModelV2 = viewModel()
    val scope = rememberCoroutineScope()
    PlaylistScreen(
        viewModel = viewModel,
        navigateBack = onBack,
        onClickItem = onNavigateToVideo,
        onLongClickItem = { videoCode, title ->
            copyTextToClipboard(getHanimeShareText(title, videoCode))
            showShortToast(R.string.copy_to_clipboard)
        },
        // 屏幕原来自己弹 Toast，进 commonMain 之后没有 Toast，改成发事件到这里。
        // getString 是挂起函数，要包 scope.launch（和 HomeRoute 一致）。
        onUiEvent = { event ->
            when (event) {
                is PlaylistUiEvent.ShowMessage ->
                    scope.launch { showShortToast(getString(event.messageRes)) }
            }
        },
    )
}
