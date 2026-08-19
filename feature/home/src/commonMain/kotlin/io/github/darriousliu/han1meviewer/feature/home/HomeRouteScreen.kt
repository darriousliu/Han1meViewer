package io.github.darriousliu.han1meviewer.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.navigation3.runtime.result.ResultEffect
import io.github.darriousliu.han1meviewer.core.common.util.HOUR_MINUTE_FORMAT
import io.github.darriousliu.han1meviewer.core.common.util.currentLocalDate
import io.github.darriousliu.han1meviewer.core.common.util.setPlainText
import io.github.darriousliu.han1meviewer.core.model.Announcement
import io.github.darriousliu.han1meviewer.core.navigation.HomeRefreshRequested
import io.github.darriousliu.han1meviewer.core.navigation.LoginSucceeded
import io.github.darriousliu.han1meviewer.core.repository.DatabaseRepo
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.checkout_exit
import io.github.darriousliu.han1meviewer.core.resource.confirm_to_exit
import io.github.darriousliu.han1meviewer.core.resource.copy_to_clipboard
import io.github.darriousliu.han1meviewer.core.resource.do_more
import io.github.darriousliu.han1meviewer.core.resource.exit
import io.github.darriousliu.han1meviewer.core.resource.finished_masturbating
import io.github.darriousliu.han1meviewer.core.storage.entity.CheckInType
import io.github.darriousliu.han1meviewer.core.storage.getHanimeShareText
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.TripleButtonDialog
import io.github.darriousliu.han1meviewer.core.ui.component.showShort
import io.github.darriousliu.han1meviewer.feature.checkin.CheckInCalendarViewModel
import io.github.darriousliu.han1meviewer.feature.home.component.AnnouncementDialog
import io.github.darriousliu.han1meviewer.feature.main.LocalMainHostActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun HomeRouteScreen(
    isDrawerOpen: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToSearchAdvanced: (Map<String, String>) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val hostActions = LocalMainHostActions.current
    val toaster = LocalToaster.current
    // NavEntry 作用域;抽屉头部/宿主要的会话信息经 HomeSessionStore 共享,不共享实例
    val viewModel: HomePageViewModel = koinViewModel()
    val checkInViewModel: CheckInCalendarViewModel = koinViewModel()

    // 登录成功 / 账号页请求刷新：都经结果总线在首页 entry 内消费
    ResultEffect<LoginSucceeded> { viewModel.getHomePage() }
    ResultEffect<HomeRefreshRequested> { viewModel.getHomePage() }
    val confirmToExit = stringResource(Res.string.confirm_to_exit)
    val finishedMasturbating = stringResource(Res.string.finished_masturbating)
    val doMore = stringResource(Res.string.do_more)
    val checkoutExit = stringResource(Res.string.checkout_exit)
    val exitText = stringResource(Res.string.exit)
    val copiedHint = stringResource(Res.string.copy_to_clipboard)
    var showExitDialog by remember { mutableStateOf(false) }
    var announcement by remember { mutableStateOf<Announcement?>(null) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    CompositionLocalProvider(
        LocalSearchHistoryQuery provides { keyword: String ->
            DatabaseRepo.SearchHistory.loadAll(keyword).first().map { it.query }
        }
    ) {
        HomePageScreen(
            viewModel = viewModel,
            isDrawerOpen = isDrawerOpen,
            onEvent = { event ->
                when (event) {
                    is HomeUiEvent.OpenDrawer -> onOpenDrawer()
                    is HomeUiEvent.NavigateToPreview -> onNavigateToPreview()
                    is HomeUiEvent.OpenSearchPage -> onNavigateToSearch(event.query)
                    is HomeUiEvent.NavigateToSearchAdvanced -> onNavigateToSearchAdvanced(event.params)
                    is HomeUiEvent.OpenVideo -> onNavigateToVideo(event.videoCode)
                    is HomeUiEvent.LongPressVideoCopy -> {
                        scope.launch {
                            clipboard.setPlainText(
                                getHanimeShareText(event.videoTitle, event.videoCode)
                            )
                        }
                        toaster.showShort(copiedHint)
                    }
                    is HomeUiEvent.ShowAnnouncementDialog -> { announcement = event.announcement }
                    is HomeUiEvent.ShowExitDialog -> { showExitDialog = true }
                    is HomeUiEvent.ShowRefreshError -> {
                        scope.launch { toaster.showShort(getString(event.messageRes)) }
                    }
                }
            }
        )
    }

    if (showExitDialog) {
        TripleButtonDialog(
            visible = true,
            title = confirmToExit,
            message = finishedMasturbating,
            negativeText = doMore,
            neutralText = checkoutExit,
            positiveText = exitText,
            onNegative = { showExitDialog = false },
            onNeutral = {
                checkInViewModel.addRecord(
                    currentLocalDate(),
                    Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                        .time.format(HOUR_MINUTE_FORMAT),
                    CheckInType.MASTURBATION.storeName, "", "",
                )
                hostActions.onExitApp()
            },
            onPositive = { hostActions.onExitApp() },
            onDismiss = { showExitDialog = false },
        )
    }

    announcement?.let { data ->
        AnnouncementDialog(
            announcementData = data,
            onDismiss = { announcement = null },
            onSaveImage = { imageUrl ->
                scope.launch(Dispatchers.IO) { saveImageToGallery(imageUrl) }
            },
        )
    }
}
