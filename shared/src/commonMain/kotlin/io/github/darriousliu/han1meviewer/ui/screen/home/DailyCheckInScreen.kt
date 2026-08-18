package io.github.darriousliu.han1meviewer.ui.screen.home

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darriousliu.han1meviewer.core.ui.component.ConfirmDialog
import io.github.darriousliu.han1meviewer.core.ui.component.appbar.HanimeScaffold
import io.github.darriousliu.han1meviewer.ui.screen.home.dailycheckin.CheckInDialog
import io.github.darriousliu.han1meviewer.ui.screen.home.dailycheckin.ContributionReportDialog
import io.github.darriousliu.han1meviewer.ui.screen.home.dailycheckin.DailyCheckInContent
import io.github.darriousliu.han1meviewer.ui.screen.home.dailycheckin.DailyCheckInEvent
import io.github.darriousliu.han1meviewer.ui.screen.home.dailycheckin.DailyCheckInUiState
import io.github.darriousliu.han1meviewer.ui.viewmodel.CheckInCalendarViewModel
import io.github.darriousliu.han1meviewer.core.common.util.CHINESE_MONTH_DAY_FORMAT
import io.github.darriousliu.han1meviewer.core.common.util.currentLocalDate
import io.github.darriousliu.han1meviewer.core.common.util.currentYearMonth
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.add_widget
import io.github.darriousliu.han1meviewer.core.resource.calendar_dialog_confirm
import io.github.darriousliu.han1meviewer.core.resource.calendar_dialog_message
import io.github.darriousliu.han1meviewer.core.resource.calendar_dialog_title
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.checkin_report
import io.github.darriousliu.han1meviewer.core.resource.forgot_confirm
import io.github.darriousliu.han1meviewer.core.resource.forgot_dismiss
import io.github.darriousliu.han1meviewer.core.resource.forgot_message
import io.github.darriousliu.han1meviewer.core.resource.forgot_title
import io.github.darriousliu.han1meviewer.core.resource.has_cum
import io.github.darriousliu.han1meviewer.core.resource.suck_back_confirm
import io.github.darriousliu.han1meviewer.core.resource.suck_back_dismiss
import io.github.darriousliu.han1meviewer.core.resource.suck_back_done
import io.github.darriousliu.han1meviewer.core.resource.suck_back_message
import io.github.darriousliu.han1meviewer.core.resource.suck_back_title
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.number
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * 打卡日历页面 Screen 层。
 *
 * 作为 V-S-C 架构的胶水层：订阅 ViewModel 状态生成 [DailyCheckInUiState]，
 * 将 [DailyCheckInEvent] 映射到 ViewModel 操作和导航。
 *
 * @param onBack 返回回调
 * @param onAddWidget 添加桌面小组件回调
 * @param onNavigateToVideo 跳转到视频详情回调
 * @param onAddCalendarEvent 请求向系统日历添加提醒（平台副作用，由 route 执行）
 * @param onFullscreenChange 报表全屏状态镜像给平台（Android 锁横屏 + 隐系统栏）；
 *   离开页面时会以 false 复位，route 传入的实现要能在销毁时安全调用
 * @param onMessage 请求显示一条提示（Android 是 Toast）
 * @param viewModel 打卡日历 ViewModel
 */
@Composable
fun DailyCheckInScreen(
    onBack: () -> Unit,
    onAddWidget: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onAddCalendarEvent: (LocalDate) -> Unit = {},
    onFullscreenChange: (Boolean) -> Unit = {},
    onMessage: (StringResource) -> Unit = {},
    viewModel: CheckInCalendarViewModel = viewModel(),
) {
    var showReport by rememberSaveable { mutableStateOf(false) }
    var isReportFullscreen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isReportFullscreen) {
        onFullscreenChange(isReportFullscreen)
    }

    // 离开页面必须复位——全屏状态下直接退出，方向和系统栏要解锁
    DisposableEffect(Unit) {
        onDispose {
            onFullscreenChange(false)
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val yearRecords by viewModel.yearRecords.collectAsStateWithLifecycle()
    val yearStats by viewModel.yearStats.collectAsStateWithLifecycle()
    val yearRecordEntities by viewModel.yearRecordEntities.collectAsStateWithLifecycle()

    val today = remember { currentLocalDate() }

    var forgotDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var suckBackDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var calendarDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var checkInDialogDate by remember { mutableStateOf<LocalDate?>(null) }
    var showEasterEgg by remember { mutableStateOf("") }
    var eggVisible by remember { mutableStateOf(false) }

    var reportSelectedYear by remember { mutableIntStateOf(today.year) }
    var reportViewMode by remember { mutableStateOf("year") }
    var reportSelectedMonth by remember { mutableIntStateOf(today.month.number) }

    val anchorMonth = remember { currentYearMonth() }
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }

    LaunchedEffect(uiState.currentMonth) {
        val monthsDiff = anchorMonth.monthsUntil(uiState.currentMonth)
        val targetPage = initialPage + monthsDiff
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                val pageMonth = anchorMonth.plus(page - initialPage, DateTimeUnit.MONTH)
                if (pageMonth != uiState.currentMonth) {
                    if (pageMonth > uiState.currentMonth) viewModel.nextMonth()
                    else viewModel.previousMonth()
                }
            }
    }

    LaunchedEffect(showEasterEgg) {
        if (showEasterEgg.isNotEmpty()) {
            eggVisible = true
            kotlinx.coroutines.delay(1500)
            eggVisible = false
        }
    }


    val handleEvent: (DailyCheckInEvent) -> Unit = { event ->
        when (event) {
            is DailyCheckInEvent.OnDateClick -> {
                when {
                    event.date > today -> {
                        calendarDialogDate = event.date
                    }

                    event.date < today && (uiState.records[event.date] ?: 0) == 0 -> {
                        forgotDialogDate = event.date
                    }

                    else -> {
                        checkInDialogDate = event.date
                    }
                }
            }

            is DailyCheckInEvent.OnDateLongClick -> {
                val count = uiState.records[event.date] ?: 0
                if (count > 0 && event.date < today) {
                    suckBackDialogDate = event.date
                } else if (count > 0) {
                    viewModel.clearCheckIn(event.date)
                }
            }

            DailyCheckInEvent.OnPreviousMonth -> viewModel.previousMonth()
            DailyCheckInEvent.OnNextMonth -> viewModel.nextMonth()
            DailyCheckInEvent.OnTodayCheckIn -> {
                checkInDialogDate = today
            }

            DailyCheckInEvent.OnTodayClear -> viewModel.clearCheckIn(today)
            DailyCheckInEvent.OnShowReport -> {
                showReport = true
            }
        }
    }

    val scrollBehavior = pinnedScrollBehavior(rememberTopAppBarState())
    HanimeScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        title = stringResource(Res.string.has_cum),
        onBack = onBack,
        scrollBehavior = scrollBehavior,
        actions = {
            FilledIconButton(onClick = { showReport = true }) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(Res.string.checkin_report)
                )
            }
            FilledIconButton(onClick = onAddWidget) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(Res.string.add_widget)
                )
            }
        },
    ) { innerPadding ->
        DailyCheckInContent(
            paddingValues = innerPadding,
            uiState = uiState,
            onEvent = handleEvent,
            onNavigateToVideo = onNavigateToVideo,
            showEasterEgg = showEasterEgg,
            eggVisible = eggVisible,
            pagerState = pagerState,
            anchorMonth = anchorMonth,
            initialPage = initialPage,
        )
    }

    ConfirmDialog(
        visible = forgotDialogDate != null,
        title = stringResource(Res.string.forgot_title),
        message = forgotDialogDate?.let {
            stringResource(
                Res.string.forgot_message,
                it.format(CHINESE_MONTH_DAY_FORMAT)
            )
        } ?: "",
        confirmText = stringResource(Res.string.forgot_confirm),
        dismissText = stringResource(Res.string.forgot_dismiss),
        onConfirm = {
            forgotDialogDate?.let { checkInDialogDate = it }
            forgotDialogDate = null
        },
        onDismiss = { forgotDialogDate = null },
    )

    ConfirmDialog(
        visible = calendarDialogDate != null,
        title = stringResource(Res.string.calendar_dialog_title),
        message = calendarDialogDate?.let {
            stringResource(
                Res.string.calendar_dialog_message,
                it.format(CHINESE_MONTH_DAY_FORMAT)
            )
        } ?: "",
        confirmText = stringResource(Res.string.calendar_dialog_confirm),
        dismissText = stringResource(Res.string.cancel),
        onConfirm = {
            calendarDialogDate?.let { onAddCalendarEvent(it) }
            calendarDialogDate = null
        },
        onDismiss = { calendarDialogDate = null },
    )

    ConfirmDialog(
        visible = suckBackDialogDate != null,
        title = stringResource(Res.string.suck_back_title),
        message = suckBackDialogDate?.let {
            stringResource(
                Res.string.suck_back_message,
                it.format(CHINESE_MONTH_DAY_FORMAT),
                uiState.records[it] ?: 0
            )
        } ?: "",
        confirmText = stringResource(Res.string.suck_back_confirm),
        dismissText = stringResource(Res.string.suck_back_dismiss),
        onConfirm = {
            suckBackDialogDate?.let {
                viewModel.clearCheckIn(it)
                onMessage(Res.string.suck_back_done)
            }
            suckBackDialogDate = null
        },
        onDismiss = { suckBackDialogDate = null },
    )

    checkInDialogDate?.let { date ->
        CheckInDialog(
            date = date,
            onLoadRecords = { d, cb -> viewModel.getRecordsByDate(d, cb) },
            onLoadWatchHistory = { limit, cb -> viewModel.getRecentWatchHistory(limit, cb) },
            onLoadSideDishCoverMap = { records, cb -> viewModel.getSideDishCoverMap(records, cb) },
            onGetCountByDate = { d, cb -> viewModel.getCountByDate(d, cb) },
            onAddRecord = { d, time, type, sideDishes, feeling ->
                viewModel.addRecord(d, time, type, sideDishes, feeling)
            },
            onDeleteRecord = { record, onDone -> viewModel.deleteRecord(record, onDone) },
            onNavigateToVideo = onNavigateToVideo,
            onEasterEgg = { msg -> showEasterEgg = msg },
            onDismiss = { checkInDialogDate = null },
        )
    }

    if (showReport) {
        ContributionReportDialog(
            selectedYear = reportSelectedYear,
            viewMode = reportViewMode,
            selectedMonth = reportSelectedMonth,
            yearRecords = yearRecords,
            yearStats = yearStats,
            yearRecordEntities = yearRecordEntities,
            onYearChange = { reportSelectedYear = it },
            onViewModeChange = { reportViewMode = it },
            onMonthChange = { reportSelectedMonth = it },
            onDismiss = {
                showReport = false
                isReportFullscreen = false
            },
            isFullscreen = isReportFullscreen,
            onToggleFullscreen = { isReportFullscreen = !isReportFullscreen },
            onLoadYearRecords = { viewModel.loadYearRecords(it) },
        )
    }
}
