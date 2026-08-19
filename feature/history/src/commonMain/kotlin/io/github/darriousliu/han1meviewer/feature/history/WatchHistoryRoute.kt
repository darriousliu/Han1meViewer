package io.github.darriousliu.han1meviewer.feature.history

import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WatchHistoryRouteScreen(
    onBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val localViewModel: LocalWatchHistoryViewModel = koinViewModel()
    val onlineViewModel: OnlineWatchHistoryViewModel = koinViewModel()
    WatchHistoryTabScreen(
        localHistoriesFlow = localViewModel.loadAllWatchHistories(),
        onlineItems = onlineViewModel.items,
        onlineState = onlineViewModel.state,
        onlineSort = onlineViewModel.selectedSort,
        onlineLoadedPageCount = onlineViewModel.loadedPageCount,
        onlineIsLoadingMore = onlineViewModel.isLoadingMore,
        onlineRefreshing = onlineViewModel::isRefreshing,
        onlineDeleteStateFlow = onlineViewModel.deleteFlow,
        onBack = onBack,
        onOpenLocalVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteLocalHistory = localViewModel::deleteWatchHistory,
        onDeleteAllLocalHistories = localViewModel::deleteAllWatchHistories,
        onOpenOnlineVideo = { onNavigateToVideo(it.videoCode) },
        onDeleteOnlineVideo = onlineViewModel::deleteItem,
        onRefreshOnline = onlineViewModel::refresh,
        onLoadMoreOnline = onlineViewModel::loadNextPage,
    )
}
