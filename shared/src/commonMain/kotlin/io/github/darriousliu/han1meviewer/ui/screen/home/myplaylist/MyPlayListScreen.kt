package io.github.darriousliu.han1meviewer.ui.screen.home.myplaylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.ui.component.PullRefreshOverlay
import io.github.darriousliu.han1meviewer.ui.component.TextInputDialog
import io.github.darriousliu.han1meviewer.ui.component.appbar.HanimeScaffold
import io.github.darriousliu.han1meviewer.ui.component.content.EmptyContent
import io.github.darriousliu.han1meviewer.ui.viewmodel.MyPlayListViewModelV2
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.add_failed
import io.github.darriousliu.han1meviewer.core.resource.add_success
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.confirm
import io.github.darriousliu.han1meviewer.core.resource.create_new_playlist
import io.github.darriousliu.han1meviewer.core.resource.h_chan_sad
import io.github.darriousliu.han1meviewer.core.resource.load_failed_with_reason
import io.github.darriousliu.han1meviewer.core.resource.my_list
import io.github.darriousliu.han1meviewer.core.resource.playlist_description
import io.github.darriousliu.han1meviewer.core.resource.playlist_title
import org.jetbrains.compose.resources.stringResource

/**
 * 播放列表页面 Screen 层。
 *
 * 持有 [MyPlayListViewModelV2]，管理缓存、下拉刷新、底部弹窗等状态编排。
 * 渲染委托给 [PlaylistContent] 和 [PlaylistBottomSheet]。
 *
 * @param viewModel 播放列表 ViewModel
 * @param navigateBack 返回回调
 * @param onClickItem 点击视频项回调
 * @param onLongClickItem 长按视频项回调
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistScreen(
    viewModel: MyPlayListViewModelV2,
    navigateBack: () -> Unit,
    onClickItem: (String) -> Unit,
    onLongClickItem: (String, String) -> Unit,
    onUiEvent: (PlaylistUiEvent) -> Unit,
) {
    val state by viewModel.myPlaylistsFlow.collectAsState()
    val uiState by viewModel.mainUiState.collectAsState()
    val scrollBehavior = pinnedScrollBehavior(rememberTopAppBarState())
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshState = rememberPullToRefreshState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var temporarilyHideSheetForNavigation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshCompleted.collect { isRefreshing = false }
    }

    LaunchedEffect(Unit) {
        if (uiState.playlists.isEmpty()) viewModel.loadMyPlayList()
    }

    // 原来是 DisposableEffect + LifecycleEventObserver 监听 ON_RESUME，
    // 那正好是 LifecycleResumeEffect 的语义（lifecycle-runtime-compose，KMP）。
    LifecycleResumeEffect(uiState.showSheet) {
        if (uiState.showSheet) temporarilyHideSheetForNavigation = false
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.createPlaylistFlow.collect { result ->
            when (result) {
                is WebsiteState.Error ->
                    onUiEvent(PlaylistUiEvent.ShowMessage(Res.string.add_failed))
                is WebsiteState.Loading -> Unit
                is WebsiteState.Success -> {
                    onUiEvent(PlaylistUiEvent.ShowMessage(Res.string.add_success))
                    viewModel.loadMyPlayList()
                }
            }
        }
    }

    val handleEvent: (PlaylistEvent) -> Unit = { event ->
        when (event) {
            PlaylistEvent.OnBack -> navigateBack()
            PlaylistEvent.OnRefresh -> {
                isRefreshing = true
                viewModel.loadMyPlayList(forceReload = true)
            }
            PlaylistEvent.OnLoadMore -> viewModel.loadMyPlayList(viewModel.playlistPage + 1)
            is PlaylistEvent.OnPlaylistClick -> {
                viewModel.setShowSheet(true)
                viewModel.setListInfo(event.listCode, event.title)
            }
            PlaylistEvent.OnDismissSheet -> {
                temporarilyHideSheetForNavigation = false
                viewModel.setShowSheet(false)
                viewModel.currentPage = 1
                viewModel.clearCurrentList()
            }
            is PlaylistEvent.OnCreatePlaylist -> viewModel.createPlaylist(event.title, event.desc)
        }
    }

    HanimeScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        title = stringResource(Res.string.my_list),
        onBack = navigateBack,
        scrollBehavior = scrollBehavior,
        actions = {
            FilledIconButton(onClick = { showCreateDialog = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(Res.string.create_new_playlist)
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .pullToRefresh(
                    state = refreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = { handleEvent(PlaylistEvent.OnRefresh) })
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (state) {
                is WebsiteState.Loading -> {
                    if (uiState.playlists.isEmpty()) {
                        LoadingIndicator(Modifier.align(Alignment.Center))
                    } else {
                        PlaylistContent(uiState = uiState, onEvent = handleEvent, rawState = state)
                    }
                }

                is WebsiteState.Error -> {
                    if (uiState.playlists.isEmpty()) {
                        EmptyContent(
                            hint = stringResource(
                                Res.string.load_failed_with_reason,
                                (state as WebsiteState.Error).throwable.message.orEmpty()
                            ),
                            picRes = Res.drawable.h_chan_sad
                        )
                    } else {
                        PlaylistContent(uiState = uiState, onEvent = handleEvent, rawState = state)
                    }
                }

                is WebsiteState.Success -> {
                    PlaylistContent(uiState = uiState, onEvent = handleEvent, rawState = state)
                }
            }

            PullRefreshOverlay(state = refreshState, isRefreshing = isRefreshing)

            if (uiState.showSheet && !temporarilyHideSheetForNavigation) {
                PlaylistBottomSheet(
                    listCode = uiState.selectedListCode,
                    onDismiss = { handleEvent(PlaylistEvent.OnDismissSheet) },
                    playListTitle = uiState.selectedListTitle,
                    onClickItem = { item ->
                        temporarilyHideSheetForNavigation = true
                        onClickItem(item)
                    },
                    onLongClickItem = onLongClickItem,
                    vm = viewModel,
                    onUiEvent = onUiEvent,
                )
            }

            if (showCreateDialog) {
                TextInputDialog(
                    title = stringResource(Res.string.create_new_playlist),
                    firstLabel = stringResource(Res.string.playlist_title),
                    secondLabel = stringResource(Res.string.playlist_description),
                    onConfirm = { title, desc ->
                        showCreateDialog = false
                        handleEvent(PlaylistEvent.OnCreatePlaylist(title, desc))
                    },
                    onDismiss = { showCreateDialog = false },
                )
            }
        }
    }
}
