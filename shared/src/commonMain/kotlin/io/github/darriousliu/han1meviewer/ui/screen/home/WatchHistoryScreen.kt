package io.github.darriousliu.han1meviewer.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.darriousliu.han1meviewer.core.common.LOCAL_DATE_TIME_FORMAT
import io.github.darriousliu.han1meviewer.core.storage.entity.WatchHistoryEntity
import io.github.darriousliu.han1meviewer.core.model.HanimeInfo
import io.github.darriousliu.han1meviewer.core.model.OnlineWatchHistorySort
import io.github.darriousliu.han1meviewer.core.common.state.PageLoadingState
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.core.ui.component.ConfirmDialog
import io.github.darriousliu.han1meviewer.core.ui.component.LoadMoreFooter
import io.github.darriousliu.han1meviewer.core.ui.component.PageContent
import io.github.darriousliu.han1meviewer.core.ui.component.VideoCardItem
import io.github.darriousliu.han1meviewer.core.ui.component.appbar.HanimeScaffold
import io.github.darriousliu.han1meviewer.core.ui.component.content.EmptyContent
import io.github.darriousliu.han1meviewer.core.ui.component.content.ErrorContent
import io.github.darriousliu.han1meviewer.core.ui.component.lazy.LazyColumn
import io.github.darriousliu.han1meviewer.core.ui.component.lazy.LazyVerticalGrid
import io.github.darriousliu.han1meviewer.core.ui.preview.ComponentPreview
import io.github.darriousliu.han1meviewer.ui.preview.fakeHomePageVideos
import io.github.darriousliu.han1meviewer.ui.screen.home.videogrid.canLoadMore
import io.github.darriousliu.han1meviewer.core.ui.rememberVideoGridColumns
import io.github.darriousliu.han1meviewer.core.ui.theme.SpacingNormal
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.close
import io.github.darriousliu.han1meviewer.core.resource.delete
import io.github.darriousliu.han1meviewer.core.resource.delete_failed
import io.github.darriousliu.han1meviewer.core.resource.delete_history
import io.github.darriousliu.han1meviewer.core.resource.delete_success
import io.github.darriousliu.han1meviewer.core.resource.help
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_access_time_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_delete_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_help_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_history_24
import io.github.darriousliu.han1meviewer.core.resource.ic_baseline_play_circle_outline_24
import io.github.darriousliu.han1meviewer.core.resource.load_failed_retry
import io.github.darriousliu.han1meviewer.core.resource.local
import io.github.darriousliu.han1meviewer.core.resource.long_press_to_delete_all_histories
import io.github.darriousliu.han1meviewer.core.resource.ok
import io.github.darriousliu.han1meviewer.core.resource.online
import io.github.darriousliu.han1meviewer.core.resource.popular
import io.github.darriousliu.han1meviewer.core.resource.sort_by_newest
import io.github.darriousliu.han1meviewer.core.resource.sort_by_oldest
import io.github.darriousliu.han1meviewer.core.resource.sure_to_delete_all_histories
import io.github.darriousliu.han1meviewer.core.resource.sure_to_delete_s
import io.github.darriousliu.han1meviewer.core.resource.watch_history
import io.github.darriousliu.han1meviewer.core.resource.watch_history_clear_all
import io.github.darriousliu.han1meviewer.core.resource.watch_history_delete_all_title
import io.github.darriousliu.han1meviewer.core.resource.watch_history_empty_description
import io.github.darriousliu.han1meviewer.core.resource.watch_history_empty_title
import io.github.darriousliu.han1meviewer.core.resource.watch_history_minutes_short
import io.github.darriousliu.han1meviewer.core.resource.watch_history_online_help
import io.github.darriousliu.han1meviewer.core.resource.watch_history_released_at
import io.github.darriousliu.han1meviewer.core.resource.watch_history_resume_watch
import io.github.darriousliu.han1meviewer.core.resource.watch_history_total_count
import io.github.darriousliu.han1meviewer.core.resource.watch_history_watched_at
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
@Composable
fun WatchHistoryTabScreen(
    localHistoriesFlow: Flow<List<WatchHistoryEntity>>,
    onlineItems: StateFlow<List<HanimeInfo>>,
    onlineState: StateFlow<PageLoadingState<*>>,
    onlineSort: StateFlow<OnlineWatchHistorySort>,
    onlineLoadedPageCount: StateFlow<Int>,
    onlineIsLoadingMore: StateFlow<Boolean>,
    onlineRefreshing: () -> Boolean,
    onlineDeleteStateFlow: SharedFlow<WebsiteState<Boolean>>,
    onBack: () -> Unit,
    onOpenLocalVideo: (WatchHistoryEntity) -> Unit,
    onDeleteLocalHistory: (WatchHistoryEntity) -> Unit,
    onDeleteAllLocalHistories: () -> Unit,
    onOpenOnlineVideo: (HanimeInfo) -> Unit,
    onDeleteOnlineVideo: (HanimeInfo) -> Unit,
    onRefreshOnline: (OnlineWatchHistorySort) -> Unit,
    onLoadMoreOnline: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val localHistories by localHistoriesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentOnlineItems by onlineItems.collectAsState()
    val currentOnlineState by onlineState.collectAsState()
    val currentOnlineSort by onlineSort.collectAsState()
    val currentOnlineLoadedPageCount by onlineLoadedPageCount.collectAsState()
    val currentOnlineIsLoadingMore by onlineIsLoadingMore.collectAsState()
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteAllLocalDialog by rememberSaveable { mutableStateOf(false) }

    val helpMessage = if (pagerState.currentPage == 1) {
        stringResource(Res.string.watch_history_online_help)
    } else {
        stringResource(Res.string.long_press_to_delete_all_histories)
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1 && currentOnlineItems.isEmpty() && currentOnlineLoadedPageCount == 0 && currentOnlineState is PageLoadingState.Loading) {
            onRefreshOnline(currentOnlineSort)
        }
    }

    ConfirmDialog(
        visible = showHelpDialog,
        title = stringResource(Res.string.help),
        message = helpMessage,
        confirmText = stringResource(Res.string.ok),
        dismissText = stringResource(Res.string.close),
        onConfirm = { showHelpDialog = false },
        onDismiss = { showHelpDialog = false },
    )

    ConfirmDialog(
        visible = showDeleteAllLocalDialog,
        title = stringResource(Res.string.watch_history_delete_all_title),
        message = stringResource(Res.string.sure_to_delete_all_histories),
        confirmText = stringResource(Res.string.watch_history_clear_all),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = {
            onDeleteAllLocalHistories()
            showDeleteAllLocalDialog = false
        },
        onDismiss = { showDeleteAllLocalDialog = false },
    )

    HanimeScaffold(
        title = stringResource(Res.string.watch_history),
        onBack = onBack,
        actions = {
            FilledIconButton(onClick = { showHelpDialog = true }) {
                Icon(
                    painter = painterResource(Res.drawable.ic_baseline_help_24),
                    contentDescription = stringResource(Res.string.help),
                )
            }
            if (pagerState.currentPage == 0) {
                FilledIconButton(
                    onClick = { showDeleteAllLocalDialog = true },
                    enabled = localHistories.isNotEmpty(),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_baseline_delete_24),
                        contentDescription = stringResource(Res.string.watch_history_clear_all),
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    text = { Text(stringResource(Res.string.local)) },
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    text = { Text(stringResource(Res.string.online)) },
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    0 -> WatchHistoryScreen(
                        histories = localHistories,
                        onBack = onBack,
                        onOpenVideo = onOpenLocalVideo,
                        onDeleteHistory = onDeleteLocalHistory,
                        onDeleteAllHistories = onDeleteAllLocalHistories,
                        useScaffold = false,
                        showHelpAction = false,
                        showDeleteAllAction = false,
                    )

                    else -> OnlineWatchHistoryScreen(
                        items = currentOnlineItems,
                        state = currentOnlineState,
                        sort = currentOnlineSort,
                        loadedPageCount = currentOnlineLoadedPageCount,
                        isLoadingMore = currentOnlineIsLoadingMore,
                        refreshing = onlineRefreshing(),
                        deleteStateFlow = onlineDeleteStateFlow,
                        onOpenVideo = onOpenOnlineVideo,
                        onDeleteVideo = onDeleteOnlineVideo,
                        onRefresh = onRefreshOnline,
                        onLoadMore = onLoadMoreOnline,
                    )
                }
            }
        }
    }
}

@Composable
fun WatchHistoryScreen(
    historiesFlow: Flow<List<WatchHistoryEntity>>,
    onBack: () -> Unit,
    onOpenVideo: (WatchHistoryEntity) -> Unit,
    onDeleteHistory: (WatchHistoryEntity) -> Unit,
    onDeleteAllHistories: () -> Unit,
    useScaffold: Boolean = true,
    showHelpAction: Boolean = true,
    showDeleteAllAction: Boolean = true,
) {
    val histories by historiesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    WatchHistoryScreen(
        histories = histories,
        onBack = onBack,
        onOpenVideo = onOpenVideo,
        onDeleteHistory = onDeleteHistory,
        onDeleteAllHistories = onDeleteAllHistories,
        useScaffold = useScaffold,
        showHelpAction = showHelpAction,
        showDeleteAllAction = showDeleteAllAction,
    )
}

@Composable
private fun WatchHistoryScreen(
    histories: List<WatchHistoryEntity>,
    onBack: () -> Unit,
    onOpenVideo: (WatchHistoryEntity) -> Unit,
    onDeleteHistory: (WatchHistoryEntity) -> Unit,
    onDeleteAllHistories: () -> Unit,
    useScaffold: Boolean,
    showHelpAction: Boolean,
    showDeleteAllAction: Boolean,
) {
    var pendingDelete by remember { mutableStateOf<WatchHistoryEntity?>(null) }
    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }
    var showHelpDialog by rememberSaveable { mutableStateOf(false) }

    ConfirmDialog(
        visible = pendingDelete != null,
        title = stringResource(Res.string.delete_history),
        message = stringResource(Res.string.sure_to_delete_s, pendingDelete?.title.orEmpty()),
        confirmText = stringResource(Res.string.delete),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = {
            pendingDelete?.let(onDeleteHistory)
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null },
    )

    ConfirmDialog(
        visible = showDeleteAllDialog,
        title = stringResource(Res.string.watch_history_delete_all_title),
        message = stringResource(Res.string.sure_to_delete_all_histories),
        confirmText = stringResource(Res.string.watch_history_clear_all),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = {
            onDeleteAllHistories()
            showDeleteAllDialog = false
        },
        onDismiss = { showDeleteAllDialog = false },
    )

    ConfirmDialog(
        visible = showHelpDialog,
        title = stringResource(Res.string.help),
        message = stringResource(Res.string.long_press_to_delete_all_histories),
        confirmText = stringResource(Res.string.ok),
        dismissText = stringResource(Res.string.close),
        onConfirm = { showHelpDialog = false },
        onDismiss = { showHelpDialog = false },
    )

    val content: @Composable (PaddingValues) -> Unit = { paddingValues ->
        if (histories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                EmptyContent(
                    hint = stringResource(Res.string.watch_history_empty_title),
                    subHint = stringResource(Res.string.watch_history_empty_description),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(histories, key = { it.id }) { history ->
                    WatchHistoryCard(
                        history = history,
                        onClick = { onOpenVideo(history) },
                        onLongClick = { pendingDelete = history },
                    )
                }
            }
        }
    }

    if (useScaffold) {
        HanimeScaffold(
            title = stringResource(Res.string.watch_history),
            subtitle = {
                Text(
                    text = stringResource(Res.string.watch_history_total_count, histories.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            onBack = onBack,
            actions = {
                if (showHelpAction) {
                    FilledIconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_baseline_help_24),
                            contentDescription = stringResource(Res.string.help),
                        )
                    }
                }
                if (showDeleteAllAction) {
                    FilledIconButton(
                        onClick = { showDeleteAllDialog = true },
                        enabled = histories.isNotEmpty()
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_baseline_delete_24),
                            contentDescription = stringResource(Res.string.watch_history_clear_all),
                        )
                    }
                }
            },
        ) { paddingValues ->
            content(paddingValues)
        }
    } else {
        content(PaddingValues())
    }
}

@OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun OnlineWatchHistoryScreen(
    items: List<HanimeInfo>,
    state: PageLoadingState<*>,
    sort: OnlineWatchHistorySort,
    loadedPageCount: Int,
    isLoadingMore: Boolean,
    refreshing: Boolean,
    deleteStateFlow: SharedFlow<WebsiteState<Boolean>>,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteVideo: (HanimeInfo) -> Unit,
    onRefresh: (OnlineWatchHistorySort) -> Unit,
    onLoadMore: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val refreshState = rememberPullToRefreshState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<HanimeInfo?>(null) }
    var sortBarVisible by rememberSaveable { mutableStateOf(true) }
    val deleteFailedText = stringResource(Res.string.delete_failed)
    val deleteSuccessText = stringResource(Res.string.delete_success)

    LaunchedEffect(deleteStateFlow, deleteFailedText, deleteSuccessText) {
        deleteStateFlow.collect { deleteState ->
            when (deleteState) {
                is WebsiteState.Error -> snackbarHostState.showSnackbar(message = deleteFailedText)
                is WebsiteState.Success -> snackbarHostState.showSnackbar(message = deleteSuccessText)
                WebsiteState.Loading -> Unit
            }
        }
    }

    LaunchedEffect(gridState.canLoadMore(items, state), isLoadingMore) {
        if (gridState.canLoadMore(items, state) && !isLoadingMore) {
            onLoadMore()
        }
    }

    LaunchedEffect(gridState) {
        var previousIndex = 0
        var previousOffset = 0
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (currentIndex, currentOffset) ->
                sortBarVisible = when {
                    !gridState.canScrollBackward -> true
                    currentIndex < previousIndex -> true
                    currentIndex > previousIndex -> false
                    currentOffset < previousOffset -> true
                    currentOffset > previousOffset -> false
                    else -> sortBarVisible
                }
                previousIndex = currentIndex
                previousOffset = currentOffset
            }
    }

    ConfirmDialog(
        visible = pendingDelete != null,
        title = stringResource(Res.string.delete_history),
        message = stringResource(Res.string.sure_to_delete_s, pendingDelete?.title.orEmpty()),
        confirmText = stringResource(Res.string.delete),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = {
            pendingDelete?.let(onDeleteVideo)
            pendingDelete = null
        },
        onDismiss = { pendingDelete = null },
    )

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = sortBarVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OnlineHistorySortChip(
                    text = stringResource(Res.string.sort_by_newest),
                    selected = sort == OnlineWatchHistorySort.Latest,
                    onClick = { onRefresh(OnlineWatchHistorySort.Latest) },
                )
                OnlineHistorySortChip(
                    text = stringResource(Res.string.popular),
                    selected = sort == OnlineWatchHistorySort.Popular,
                    onClick = { onRefresh(OnlineWatchHistorySort.Popular) },
                )
                OnlineHistorySortChip(
                    text = stringResource(Res.string.sort_by_oldest),
                    selected = sort == OnlineWatchHistorySort.Oldest,
                    onClick = { onRefresh(OnlineWatchHistorySort.Oldest) },
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            state = refreshState,
            onRefresh = { onRefresh(sort) },
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = refreshState,
                    isRefreshing = refreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        ) {
            PageContent(
                isLoading = state is PageLoadingState.Loading && items.isEmpty(),
                isError = state is PageLoadingState.Error,
                isEmpty = state is PageLoadingState.NoMoreData && items.isEmpty(),
                onRetry = { onRefresh(sort) },
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ErrorContent(
                            title = stringResource(Res.string.load_failed_retry),
                            onRetry = { onRefresh(sort) },
                        )
                    }
                },
                empty = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyContent(
                            hint = stringResource(Res.string.watch_history_empty_title),
                            subHint = stringResource(Res.string.watch_history_empty_description),
                        )
                    }
                },
            ) {
                OnlineWatchHistoryGrid(
                    items = items,
                    gridState = gridState,
                    loadedPageCount = loadedPageCount,
                    state = state,
                    isLoadingMore = isLoadingMore,
                    snackbarHostState = snackbarHostState,
                    onOpenVideo = onOpenVideo,
                    onDeleteVideo = { pendingDelete = it },
                )
            }
        }
    }
}

@Composable
private fun OnlineWatchHistoryGrid(
    items: List<HanimeInfo>,
    gridState: LazyGridState,
    loadedPageCount: Int,
    state: PageLoadingState<*>,
    isLoadingMore: Boolean,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
    onOpenVideo: (HanimeInfo) -> Unit,
    onDeleteVideo: (HanimeInfo) -> Unit,
) {
    val videoColumns = rememberVideoGridColumns()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(videoColumns),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(SpacingNormal),
            horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(Res.string.watch_history_total_count, items.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            items(items, key = { it.videoCode }) { item ->
                VideoCardItem(
                    videoItem = item,
                    onClickVideosItem = { onOpenVideo(item) },
                    onLongClickVideosItem = { _, _ -> onDeleteVideo(item) },
                )
            }
            if (items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LoadMoreFooter(
                        state = state,
                        loadedPage = loadedPageCount,
                        isLoadingMore = isLoadingMore,
                    )
                }
            }
        }
        androidx.compose.material3.SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun OnlineHistorySortChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            labelColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ),
    )
}

/**
 * 毫秒时间戳 -> `yyyy-MM-dd HH:mm`。
 *
 * 原来是 `SimpleDateFormat(..., Locale.getDefault())`，commonMain 没有；
 * [LOCAL_DATE_TIME_FORMAT] 的格式与之完全一致，直接复用。
 */
@OptIn(ExperimentalTime::class)
private fun formatWatchDate(epochMillis: Long): String =
    Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .format(LOCAL_DATE_TIME_FORMAT)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WatchHistoryCard(
    history: WatchHistoryEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val fixTimestamp = { ts: Long -> if (ts < 9999999999L) ts * 1000 else ts }
    val watchDate =
        remember(history.watchDate) { formatWatchDate(fixTimestamp(history.watchDate)) }
    val releaseDate =
        remember(history.releaseDate) { formatWatchDate(fixTimestamp(history.releaseDate)) }
    val progressMinutes = remember(history.progress) { history.progress / 60_000 }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = history.coverUrl,
                    contentDescription = history.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                if (progressMinutes > 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(topEnd = 4.dp),
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        Text(
                            text = stringResource(
                                Res.string.watch_history_minutes_short,
                                progressMinutes
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = history.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    FilledIconButton(
                        onClick = onLongClick,
                        modifier = Modifier.size(25.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(Res.string.delete_history),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                WatchHistoryMeta(
                    iconRes = Res.drawable.ic_baseline_access_time_24,
                    label = stringResource(Res.string.watch_history_watched_at, watchDate),
                )
                WatchHistoryMeta(
                    iconRes = Res.drawable.ic_baseline_play_circle_outline_24,
                    label = stringResource(Res.string.watch_history_released_at, releaseDate),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    AssistChip(
                        onClick = onClick,
                        label = {
                            Text(
                                stringResource(Res.string.watch_history_resume_watch),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_baseline_history_24),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer, // 改用 primary 强化引导
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchHistoryMeta(
    iconRes: DrawableResource,
    label: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun WatchHistoryScreenPreview() {
    val previews = fakeHomePageVideos.take(3).mapIndexed { index, item ->
        WatchHistoryEntity(
            id = index + 1,
            title = item.title,
            coverUrl = item.coverUrl,
            videoCode = item.videoCode,
            releaseDate = Clock.System.now().toEpochMilliseconds() - (index + 10) * 86_400_000L,
            watchDate = Clock.System.now().toEpochMilliseconds() - index * 3_600_000L,
            progress = (index + 1) * 12L * 60_000L,
        )
    }
    ComponentPreview {
        WatchHistoryScreen(
            histories = previews,
            onBack = {},
            onOpenVideo = {},
            onDeleteHistory = {},
            onDeleteAllHistories = {},
            useScaffold = true,
            showHelpAction = true,
            showDeleteAllAction = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WatchHistoryEmptyPreview() {
    ComponentPreview {
        WatchHistoryScreen(
            histories = emptyList<WatchHistoryEntity>(),
            onBack = {},
            onOpenVideo = {},
            onDeleteHistory = {},
            onDeleteAllHistories = {},
            useScaffold = true,
            showHelpAction = true,
            showDeleteAllAction = true,
        )
    }
}
