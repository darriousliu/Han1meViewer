package io.github.darriousliu.han1meviewer.ui.navigation.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.result.ResultEffect
import io.github.darriousliu.han1meviewer.R
import io.github.darriousliu.han1meviewer.core.storage.getHanimeShareText
import io.github.darriousliu.han1meviewer.core.repository.DatabaseRepo
import io.github.darriousliu.han1meviewer.core.storage.entity.CheckInType
import io.github.darriousliu.han1meviewer.core.model.Announcement
import io.github.darriousliu.han1meviewer.core.navigation.HomeRefreshRequested
import io.github.darriousliu.han1meviewer.core.navigation.LoginSucceeded
import io.github.darriousliu.han1meviewer.core.ui.component.TripleButtonDialog
import io.github.darriousliu.han1meviewer.feature.home.HomePageScreen
import io.github.darriousliu.han1meviewer.feature.home.HomePageViewModel
import io.github.darriousliu.han1meviewer.feature.main.LocalMainHostActions
import io.github.darriousliu.han1meviewer.feature.home.HomeUiEvent
import io.github.darriousliu.han1meviewer.feature.home.LocalSearchHistoryQuery
import io.github.darriousliu.han1meviewer.feature.home.component.AnnouncementDialog
import io.github.darriousliu.han1meviewer.feature.home.saveImageToGallery
import io.github.darriousliu.han1meviewer.feature.checkin.CheckInCalendarViewModel
import io.github.darriousliu.han1meviewer.core.common.util.currentLocalDate
import io.github.darriousliu.han1meviewer.core.common.util.setPlainText
import io.github.darriousliu.han1meviewer.util.showShortToast
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRouteScreen(
    isDrawerOpen: Boolean,
    onOpenDrawer: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToSearchAdvanced: (Map<String, String>) -> Unit,
    onNavigateToVideo: (String) -> Unit,
) {
    val activity = checkNotNull(LocalActivity.current)
    val hostActions = LocalMainHostActions.current
    // HomePageViewModel 是刻意的例外：Activity 作用域（首页常驻栈底，实际生命周期一致），
    // 抽屉头部与宿主的登出刷新共用同一实例。这里按 owner 取到宿主创建的那份。
    val viewModel: HomePageViewModel = viewModel(
        viewModelStoreOwner = activity as ComponentActivity,
    )
    val checkInViewModel: CheckInCalendarViewModel = koinViewModel()

    // 登录成功 / 账号页请求刷新：都经结果总线在首页 entry 内消费
    ResultEffect<LoginSucceeded> { viewModel.getHomePage() }
    ResultEffect<HomeRefreshRequested> { viewModel.getHomePage() }
    val confirmToExit = stringResource(R.string.confirm_to_exit)
    val finishedMasturbating = stringResource(R.string.finished_masturbating)
    val doMore = stringResource(R.string.do_more)
    val checkoutExit = stringResource(R.string.checkout_exit)
    val exit = stringResource(R.string.exit)
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
                        showShortToast(R.string.copy_to_clipboard)
                    }
                    is HomeUiEvent.ShowAnnouncementDialog -> { announcement = event.announcement }
                    is HomeUiEvent.ShowExitDialog -> { showExitDialog = true }
                    is HomeUiEvent.ShowRefreshError -> {
                        scope.launch { showShortToast(getString(event.messageRes)) }
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
            positiveText = exit,
            onNegative = { showExitDialog = false },
            onNeutral = {
                checkInViewModel.addRecord(
                    currentLocalDate(),
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
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
            // 存图走 MediaStore，是 Android 独有的能力，留在 route
            onSaveImage = { imageUrl ->
                scope.launch(Dispatchers.IO) { saveImageToGallery(activity, imageUrl) }
            },
        )
    }
}
