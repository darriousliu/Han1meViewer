package com.yenaly.han1meviewer.ui.screen.video

import android.content.res.Configuration
import android.graphics.Rect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import com.yenaly.han1meviewer.FirebaseConstants
import com.yenaly.han1meviewer.HanimeConstants
import com.yenaly.han1meviewer.PermissionRequester
import com.yenaly.han1meviewer.Preferences
import com.yenaly.han1meviewer.R
import com.yenaly.han1meviewer.getHanimeVideoLink
import com.yenaly.han1meviewer.logic.DatabaseRepo
import com.yenaly.han1meviewer.logic.dao.CheckInRecordDatabase
import com.yenaly.han1meviewer.logic.entity.HKeyframeEntity
import com.yenaly.han1meviewer.logic.entity.WatchHistoryEntity
import com.yenaly.han1meviewer.logic.exception.ParseException
import com.yenaly.han1meviewer.logic.model.SearchOption
import com.yenaly.han1meviewer.logic.state.VideoLoadingState
import com.yenaly.han1meviewer.playback.compose.PlaybackSurface
import com.yenaly.han1meviewer.playback.compose.VideoKeyframeCountdownUiState
import com.yenaly.han1meviewer.playback.compose.VideoKeyframeUiState
import com.yenaly.han1meviewer.playback.compose.VideoPlayerActions
import com.yenaly.han1meviewer.playback.compose.VideoPlayerUiState
import com.yenaly.han1meviewer.playback.model.PlaybackDefaults
import com.yenaly.han1meviewer.playback.model.PlaybackPhase
import com.yenaly.han1meviewer.playback.model.PlaybackSource
import com.yenaly.han1meviewer.playback.model.toPlaybackSource
import com.yenaly.han1meviewer.playback.platform.PlaybackPlatformBridge
import com.yenaly.han1meviewer.ui.activity.AndroidMainActivity
import com.yenaly.han1meviewer.ui.bridge.VideoPageHost
import com.yenaly.han1meviewer.ui.component.ConfirmDialog
import com.yenaly.han1meviewer.ui.navigation.main.HomeRoute
import com.yenaly.han1meviewer.ui.navigation.main.VideoRoute
import com.yenaly.han1meviewer.ui.viewmodel.CommentViewModel
import com.yenaly.han1meviewer.ui.viewmodel.VideoPlaybackViewModel
import com.yenaly.han1meviewer.ui.viewmodel.VideoViewModel
import com.yenaly.han1meviewer.util.checkBadGuy
import com.yenaly.han1meviewer.util.loadAssetAs
import com.yenaly.yenaly_libs.utils.OrientationManager
import com.yenaly.yenaly_libs.utils.browse
import com.yenaly.yenaly_libs.utils.copyToClipboard
import com.yenaly.yenaly_libs.utils.shareText
import com.yenaly.yenaly_libs.utils.showShortToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun VideoRouteHostScreen(
    activity: AndroidMainActivity,
    route: VideoRoute,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val videoViewModel: VideoViewModel = viewModel()
    val commentViewModel: CommentViewModel = viewModel()
    val playbackViewModel: VideoPlaybackViewModel = viewModel()
    val platformBridge = remember(activity) { PlaybackPlatformBridge(activity) }

    val videoState by videoViewModel.hanimeVideoStateFlow.collectAsStateWithLifecycle()
    val hostUiState by videoViewModel.videoHostUiStateFlow.collectAsStateWithLifecycle()
    val enginePlaybackState by playbackViewModel.state.collectAsStateWithLifecycle()
    val relatedItems =
        videoViewModel.hanimeVideoFlow.collectAsStateWithLifecycle().value?.relatedHanimes.orEmpty()
    val genres = remember(Preferences.baseUrl) {
        loadAssetAs<List<SearchOption>>(
            if (Preferences.baseUrl == HanimeConstants.HANIME_URL[3]) {
                "search_options/genre_av.json"
            } else {
                "search_options/genre.json"
            }
        ).orEmpty()
    }
    val stringLongPressShare = remember(activity) {
        activity.getString(R.string.long_press_share_to_copy)
    }

    commentViewModel.code = route.videoCode
    videoViewModel.fromDownload = route.videoCode == "-1" || route.localUri != null

    var checkedQuality by remember(route.videoCode, route.localUri) { mutableStateOf<String?>(null) }
    var pendingDownloadPrompt by remember(route.videoCode, route.localUri) {
        mutableStateOf<DownloadPromptState?>(null)
    }
    var pendingSource by remember(route.videoCode, route.localUri) {
        mutableStateOf<PlaybackSource?>(null)
    }
    var pendingQualityId by remember(route.videoCode, route.localUri) {
        mutableStateOf<String?>(null)
    }
    var videoTitle by remember(route.videoCode, route.localUri) { mutableStateOf<String?>(null) }
    var resumePositionMs by rememberSaveable(route.videoCode, route.localUri) {
        mutableLongStateOf(0L)
    }
    var hasStartedPlayback by rememberSaveable(route.videoCode, route.localUri) {
        mutableStateOf(false)
    }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var isLocked by rememberSaveable { mutableStateOf(false) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var keyframePanelVisible by rememberSaveable { mutableStateOf(false) }
    var superResolutionIndex by rememberSaveable { mutableStateOf(0) }
    var brightness by remember { mutableFloatStateOf(platformBridge.currentBrightness()) }
    var volume by remember { mutableFloatStateOf(platformBridge.currentVolume()) }
    var sourceRect by remember { mutableStateOf<Rect?>(null) }
    var hKeyframes by remember(route.videoCode) { mutableStateOf<HKeyframeEntity?>(null) }
    var isSideRelatedCollapsed by rememberSaveable { mutableStateOf(false) }
    var showRestartFromBeginning by remember { mutableStateOf(false) }
    var showAddHKeyframeDialog by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showMeteredNetworkDialog by remember { mutableStateOf(false) }
    var meteredNetworkApproved by remember(route.videoCode, route.localUri) {
        mutableStateOf(false)
    }

    val displayedPlaybackState = pendingSource
        ?.takeIf { enginePlaybackState.source == null }
        ?.let { source ->
            enginePlaybackState.copy(
                source = source,
                phase = PlaybackPhase.Idle,
                positionMs = resumePositionMs,
                selectedQualityId = pendingQualityId ?: source.resolveQuality().id,
            )
        } ?: enginePlaybackState

    val keyframeItems = remember(hKeyframes) {
        hKeyframes?.keyframes.orEmpty()
            .sortedBy(HKeyframeEntity.Keyframe::position)
            .map { VideoKeyframeUiState(it.position, it.prompt) }
    }
    val keyframeCountdown = remember(
        keyframeItems,
        enginePlaybackState.positionMs,
        isFullscreen,
    ) {
        if (!Preferences.hKeyframesEnable || !isFullscreen) return@remember null
        val next = keyframeItems.firstOrNull {
            it.positionMs >= enginePlaybackState.positionMs &&
                it.positionMs - enginePlaybackState.positionMs <= Preferences.whenCountdownRemind
        } ?: return@remember null
        val remainingMs = (next.positionMs - enginePlaybackState.positionMs).coerceAtLeast(0L)
        VideoKeyframeCountdownUiState(
            remainingMs = remainingMs,
            prompt = next.prompt.takeIf { Preferences.showCommentWhenCountdown },
        )
    }

    val routeActions = remember(activity, scope, videoViewModel, genres) {
        VideoRouteActions(
            context = activity,
            scope = scope,
            viewModel = videoViewModel,
            genres = genres,
            requestStoragePermission = { onGranted, onDenied, onPermanentlyDenied ->
                (activity as PermissionRequester).requestStoragePermission(
                    onGranted = onGranted,
                    onDenied = onDenied,
                    onPermanentlyDenied = onPermanentlyDenied,
                )
            },
            onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
            getCheckedQuality = { checkedQuality },
            setCheckedQuality = { checkedQuality = it },
            onStoragePermissionDenied = { activity.navController.popBackStack() },
            onDownloadPermissionDialogCancelled = { activity.navController.popBackStack() },
        )
    }

    fun setFullscreen(enabled: Boolean) {
        val playback = playbackViewModel.state.value
        isFullscreen = enabled
        controlsVisible = true
        platformBridge.setFullscreen(
            fullscreen = enabled,
            videoWidth = playback.videoWidth,
            videoHeight = playback.videoHeight,
        )
    }

    fun beginPlayback() {
        val source = pendingSource ?: enginePlaybackState.source
        if (source == null) {
            showShortToast(R.string.fail_to_get_video_link)
            if (route.videoCode != "-1") activity.browse(getHanimeVideoLink(route.videoCode))
            return
        }
        hasStartedPlayback = true
        if (enginePlaybackState.source == null) {
            playbackViewModel.load(
                source = source,
                qualityId = pendingQualityId,
                startPositionMs = resumePositionMs,
                playWhenReady = true,
            )
        } else {
            playbackViewModel.play()
        }
    }

    fun requestPlaybackStart() {
        if (enginePlaybackState.source != null) {
            beginPlayback()
            return
        }
        val isRemote = pendingSource
            ?.resolveQuality(pendingQualityId)
            ?.isRemote
            ?: false
        if (isRemote && !Preferences.disableMobileDataWarning &&
            !meteredNetworkApproved && platformBridge.isActiveNetworkMetered()
        ) {
            showMeteredNetworkDialog = true
        } else {
            beginPlayback()
        }
    }

    val playerActions = object : VideoPlayerActions {
        override fun onBack() {
            if (isFullscreen) setFullscreen(false) else activity.navController.popBackStack()
        }

        override fun onHome() {
            setFullscreen(false)
            activity.navController.popBackStack(HomeRoute, false)
        }

        override fun onTogglePlayPause() {
            if (enginePlaybackState.isPlaying || enginePlaybackState.playWhenReady) {
                playbackViewModel.pause()
            } else {
                requestPlaybackStart()
            }
        }

        override fun onRetry() {
            hasStartedPlayback = true
            if (enginePlaybackState.source == null) requestPlaybackStart() else playbackViewModel.retry()
        }

        override fun onReplay() {
            hasStartedPlayback = true
            resumePositionMs = 0L
            playbackViewModel.replay()
        }

        override fun onSeekTo(positionMs: Long) = playbackViewModel.seekTo(positionMs)

        override fun onSelectQuality(qualityId: String) {
            pendingQualityId = qualityId
            if (enginePlaybackState.source != null) playbackViewModel.selectQuality(qualityId)
        }

        override fun onSetSpeed(speed: Float) = playbackViewModel.setSpeed(speed)

        override fun onSetSuperResolution(index: Int) {
            superResolutionIndex = index
            playbackViewModel.setSuperResolution(index)
        }

        override fun onOpenKeyframes() {
            keyframePanelVisible = true
        }

        override fun onDismissKeyframes() {
            keyframePanelVisible = false
        }

        override fun onSelectKeyframe(positionMs: Long) {
            playbackViewModel.seekTo(positionMs)
            keyframePanelVisible = false
        }

        override fun onAddKeyframe() {
            if (enginePlaybackState.isPlaying || enginePlaybackState.playWhenReady) {
                showShortToast(R.string.pause_then_long_press)
            } else {
                showAddHKeyframeDialog =
                    enginePlaybackState.positionMs to (videoTitle ?: route.videoCode)
            }
        }

        override fun onToggleFullscreen() = setFullscreen(!isFullscreen)

        override fun onControlsVisibilityChanged(visible: Boolean) {
            controlsVisible = visible
        }

        override fun onLockChanged(locked: Boolean) {
            isLocked = locked
            controlsVisible = true
        }

        override fun onBrightnessChanged(fraction: Float) {
            brightness = platformBridge.setBrightness(fraction)
        }

        override fun onVolumeChanged(fraction: Float) {
            volume = platformBridge.setVolume(fraction)
        }

        override fun onRestartFromBeginning() {
            resumePositionMs = 0L
            showRestartFromBeginning = false
            playbackViewModel.seekTo(0L)
        }
    }

    val pageHost = remember(activity, platformBridge, playbackViewModel, videoViewModel) {
        object : VideoPageHost {
            override fun showCommentBadge(count: Int) {
                videoViewModel.setCommentBadgeCount(count)
            }

            override fun shouldEnterPip(): Boolean {
                val state = playbackViewModel.state.value
                return hasStartedPlayback && state.source != null &&
                    state.phase !in setOf(
                        PlaybackPhase.Idle,
                        PlaybackPhase.Ended,
                        PlaybackPhase.Error,
                    )
            }

            override fun enterPipMode() {
                val state = playbackViewModel.state.value
                platformBridge.enterPictureInPicture(
                    isPlaying = state.isPlaying || state.playWhenReady,
                    sourceRect = sourceRect,
                    videoWidth = state.videoWidth,
                    videoHeight = state.videoHeight,
                )
            }

            override fun onPipModeChanged(isInPip: Boolean) {
                videoViewModel.setPipMode(isInPip)
                controlsVisible = !isInPip
                if (isInPip) {
                    isFullscreen = false
                    platformBridge.setFullscreen(false)
                }
                val height = when {
                    isInPip -> Int.MAX_VALUE
                    Preferences.tabletMode &&
                        activity.resources.configuration.orientation !=
                        Configuration.ORIENTATION_LANDSCAPE -> 350
                    else -> 250
                }
                videoViewModel.setPlayerHeightDp(height)
            }

            override fun togglePlayPause() = playbackViewModel.togglePlayPause()
        }
    }

    BackHandler(enabled = isFullscreen) { setFullscreen(false) }

    DisposableEffect(activity, pageHost, platformBridge) {
        activity.registerCurrentVideoHost(pageHost)
        onDispose {
            activity.registerCurrentVideoHost(null)
            platformBridge.release()
        }
    }

    DisposableEffect(lifecycleOwner, activity, route.videoCode, platformBridge) {
        fun persistProgress() {
            val position = playbackViewModel.state.value.positionMs
            activity.lifecycleScope.launch(Dispatchers.IO) {
                DatabaseRepo.WatchHistory.updateProgress(route.videoCode, position)
            }
        }

        val orientationManager = OrientationManager(activity) { orientation ->
            val state = playbackViewModel.state.value
            if (!Preferences.tabletMode && hasStartedPlayback &&
                !videoViewModel.videoHostUiStateFlow.value.isInPipMode &&
                state.videoWidth >= state.videoHeight &&
                state.videoWidth > 0
            ) {
                when {
                    orientation.isLandscape && !isFullscreen -> setFullscreen(true)
                    orientation == OrientationManager.ScreenOrientation.PORTRAIT && isFullscreen ->
                        setFullscreen(false)
                }
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> persistProgress()
                Lifecycle.Event.ON_STOP -> if (!activity.isInPictureInPictureMode) {
                    setFullscreen(false)
                    playbackViewModel.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(orientationManager)
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            persistProgress()
            lifecycleOwner.lifecycle.removeObserver(orientationManager)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

    LaunchedEffect(route.videoCode, route.localUri) {
        checkedQuality = null
        pendingDownloadPrompt = null
        pendingSource = null
        pendingQualityId = null
        videoTitle = null
        resumePositionMs = 0L
        hasStartedPlayback = false
        meteredNetworkApproved = false
        checkBadGuy(activity, R.raw.akarin)
        videoViewModel.videoCode = route.videoCode
        videoViewModel.getHanimeVideo(route.videoCode, route.localUri)
    }

    LaunchedEffect(videoState, route.videoCode, route.localUri) {
        when (val state = videoState) {
            is VideoLoadingState.Error -> {
                state.throwable.localizedMessage?.let { showShortToast(it) }
                if (state.throwable is ParseException) {
                    activity.browse(getHanimeVideoLink(route.videoCode))
                }
            }

            is VideoLoadingState.Success -> {
                videoTitle = state.info.title
                if (state.info.videoUrls.isNotEmpty()) {
                    pendingSource = state.info.videoUrls.toPlaybackSource(
                        id = route.videoCode,
                        title = state.info.title,
                        coverUrl = state.info.coverUrl,
                        preferredQualityId = Preferences.videoQuality,
                    )
                    pendingQualityId = pendingSource?.resolveQuality()?.id
                }
                if (!videoViewModel.fromDownload) {
                    videoViewModel.insertWatchHistoryWithCover(
                        WatchHistoryEntity(
                            state.info.coverUrl,
                            state.info.title,
                            state.info.uploadTimeMillis,
                            kotlin.time.Clock.System.now().toEpochMilliseconds(),
                            route.videoCode,
                        )
                    )
                }
                val history = withContext(Dispatchers.IO) {
                    DatabaseRepo.WatchHistory.findBy(route.videoCode)
                }
                resumePositionMs = history?.progress
                    ?.takeIf { it > MIN_RESUME_POSITION_MS && Preferences.allowResumePlayback }
                    ?: 0L
            }

            is VideoLoadingState.NoContent -> showShortToast(R.string.video_might_not_exist)
            is VideoLoadingState.Loading -> Unit
        }
    }

    LaunchedEffect(route.videoCode) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            videoViewModel.observeKeyframe(route.videoCode).collect {
                hKeyframes = it
                videoViewModel.hKeyframes = it
            }
        }
    }

    LaunchedEffect(videoViewModel) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            videoViewModel.modifyHKeyframeFlow.collect { (_, reason) -> showShortToast(reason) }
        }
    }

    LaunchedEffect(videoViewModel, route.videoCode) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
            videoViewModel.loadDownloadedFlow.collect { entity ->
                val newQuality = checkedQuality ?: return@collect
                pendingDownloadPrompt = DownloadPromptState(
                    newQuality = newQuality,
                    oldQuality = entity?.quality,
                )
            }
        }
    }

    LaunchedEffect(hasStartedPlayback, resumePositionMs) {
        if (hasStartedPlayback && resumePositionMs > MIN_RESUME_POSITION_MS) {
            showRestartFromBeginning = true
            try {
                delay(RESTART_ACTION_VISIBLE_MS)
            } finally {
                showRestartFromBeginning = false
            }
        }
    }

    LaunchedEffect(enginePlaybackState.isPlaying, enginePlaybackState.playWhenReady) {
        platformBridge.setKeepScreenOn(
            enginePlaybackState.isPlaying || enginePlaybackState.playWhenReady
        )
        videoViewModel.setScrollDisabled(
            enginePlaybackState.isPlaying || enginePlaybackState.playWhenReady
        )
    }

    LaunchedEffect(keyframeCountdown != null) {
        playbackViewModel.setHighFrequencyProgressUpdates(keyframeCountdown != null)
    }

    LaunchedEffect(
        hostUiState.isInPipMode,
        enginePlaybackState.isPlaying,
        enginePlaybackState.playWhenReady,
        enginePlaybackState.videoWidth,
        enginePlaybackState.videoHeight,
        sourceRect,
    ) {
        platformBridge.updatePictureInPictureAction(
            isPlaying = enginePlaybackState.isPlaying || enginePlaybackState.playWhenReady,
            sourceRect = sourceRect,
            videoWidth = enginePlaybackState.videoWidth,
            videoHeight = enginePlaybackState.videoHeight,
        )
    }

    val playerHeight = when {
        Preferences.tabletMode && isSideRelatedCollapsed -> 500.dp
        Preferences.tabletMode -> 400.dp
        else -> 250.dp
    }
    val playerUiState = VideoPlayerUiState(
        playback = displayedPlaybackState,
        capabilities = playbackViewModel.capabilities,
        longPressSpeedMultiplier = Preferences.longPressSpeedTime,
        seekGestureSensitivity = PlaybackDefaults.progressSlideDivisor(
            Preferences.slideSensitivity,
        ),
        controlsVisible = controlsVisible && !hostUiState.isInPipMode,
        showBottomProgress = Preferences.showBottomProgress && !hostUiState.isInPipMode,
        isLocked = isLocked,
        isFullscreen = isFullscreen,
        showPoster = !hasStartedPlayback,
        brightness = brightness,
        volume = volume,
        showRestartFromBeginning = showRestartFromBeginning,
        keyframesEnabled = Preferences.hKeyframesEnable,
        keyframes = keyframeItems,
        keyframePanelVisible = keyframePanelVisible,
        keyframeCountdown = keyframeCountdown,
        superResolutionIndex = superResolutionIndex,
    )

    VideoShellContent(
        isTabletMode = Preferences.tabletMode,
        isInPipMode = hostUiState.isInPipMode,
        relatedItems = relatedItems,
        onHideRelatedInIntroChange = { videoViewModel.hideRelatedInIntro = it },
        onSideRelatedCollapsedChange = { isSideRelatedCollapsed = it },
        onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
        mainContent = {
            VideoPlaybackLayout(
                playerHeight = playerHeight,
                isFullscreen = isFullscreen,
                isInPipMode = hostUiState.isInPipMode,
                collapseLocked = hostUiState.isScrollDisabled,
                onExpandedChange = {
                    videoViewModel.setAppBarExpanded(route.videoCode, it)
                },
                player = {
                    VideoPlayerUi(
                        state = playerUiState,
                        actions = playerActions,
                        surface = {
                            PlaybackSurface(
                                controller = playbackViewModel.controller,
                                modifier = Modifier.matchParentSize(),
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                val bounds = coordinates.boundsInWindow()
                                sourceRect = Rect(
                                    bounds.left.roundToInt(),
                                    bounds.top.roundToInt(),
                                    bounds.right.roundToInt(),
                                    bounds.bottom.roundToInt(),
                                )
                            },
                    )
                },
                details = {
                    VideoRouteContent(
                        videoCode = route.videoCode,
                        videoState = videoState,
                        videoViewModel = videoViewModel,
                        commentViewModel = commentViewModel,
                        fromDownload = videoViewModel.fromDownload,
                        pendingDownloadPrompt = pendingDownloadPrompt,
                        onPendingDownloadPromptChange = { pendingDownloadPrompt = it },
                        onRetry = {
                            videoViewModel.getHanimeVideo(route.videoCode, route.localUri)
                        },
                        onOpenVideo = { item -> activity.showVideoDetailFragment(item.videoCode) },
                        onOpenArtist = routeActions::openArtistSearch,
                        onNavigateToSearch = routeActions::openTagSearch,
                        onToggleSubscribe = routeActions::toggleArtistSubscription,
                        onToggleFavorite = routeActions::toggleFavorite,
                        onRateVideo = routeActions::rateVideo,
                        onManageMyList = routeActions::updateMyListSelection,
                        onQuickCheckIn = { record ->
                            val normalizedRecord = if (record.sideDishes.contains("\u001E")) {
                                record
                            } else {
                                record.copy(
                                    sideDishes = "${record.sideDishes}\u001E${route.videoCode}"
                                )
                            }
                            scope.launch(Dispatchers.IO) {
                                CheckInRecordDatabase.instance.checkInDao()
                                    .insert(normalizedRecord)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        activity,
                                        R.string.checkin,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                        onPrepareDownload = { quality, video ->
                            checkedQuality = quality
                            video?.let(routeActions::startDownloadFlow)
                        },
                        onConfirmDownloadPrompt = { video ->
                            video?.let {
                                routeActions.confirmPendingDownload(it, pendingDownloadPrompt)
                            }
                        },
                        onRequestOpenOfficialDownloadPage =
                            routeActions::openOfficialDownloadPage,
                        onRequestOpenDownloadPermissionSettings =
                            routeActions::openDownloadPermissionSettings,
                        onOpenWebPage = routeActions::openVideoWebPage,
                        onOpenOriginalComic = routeActions::openOriginalComic,
                        onOpenShare = { content, title -> shareText(content, title) },
                        onCopyText = {
                            it.copyToClipboard()
                            showShortToast(R.string.copy_to_clipboard)
                        },
                        onIntroductionLinkClick = routeActions::openIntroductionLink,
                        stringLongPressShare = stringLongPressShare,
                        pageHost = pageHost,
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )
        },
        modifier = Modifier.fillMaxSize(),
    )

    if (showMeteredNetworkDialog) {
        ConfirmDialog(
            visible = true,
            title = activity.getString(R.string.warning),
            message = activity.getString(R.string.mobile_data_playback_warning),
            confirmText = activity.getString(R.string.continues),
            dismissText = activity.getString(R.string.cancel),
            onConfirm = {
                meteredNetworkApproved = true
                showMeteredNetworkDialog = false
                beginPlayback()
            },
            onDismiss = { showMeteredNetworkDialog = false },
        )
    }

    showAddHKeyframeDialog?.let { (currentPosition, title) ->
        ConfirmDialog(
            visible = true,
            title = activity.getString(R.string.add_to_h_keyframe),
            message = buildString {
                appendLine(activity.getString(R.string.sure_to_add_to_h_keyframe))
                append(activity.getString(R.string.current_position_d_ms, currentPosition))
            },
            confirmText = activity.getString(R.string.confirm),
            dismissText = activity.getString(R.string.cancel),
            onConfirm = {
                videoViewModel.appendHKeyframe(
                    route.videoCode,
                    title,
                    HKeyframeEntity.Keyframe(position = currentPosition, prompt = null),
                )
                Firebase.analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT) {
                    param(FirebaseAnalytics.Param.ITEM_ID, FirebaseConstants.H_KEYFRAMES)
                    param(FirebaseAnalytics.Param.CONTENT_TYPE, FirebaseConstants.H_KEYFRAMES)
                }
                showAddHKeyframeDialog = null
            },
            onDismiss = { showAddHKeyframeDialog = null },
        )
    }
}

/**
 * Compose replacement for CoordinatorLayout/AppBarLayout. The player call remains at the same
 * composition position for normal, fullscreen and PiP; only its constraints change.
 */
@Composable
private fun VideoPlaybackLayout(
    playerHeight: Dp,
    isFullscreen: Boolean,
    isInPipMode: Boolean,
    collapseLocked: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    player: @Composable () -> Unit,
    details: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val maxPlayerHeightPx = with(density) { playerHeight.toPx() }
    var playerOffsetPx by remember(playerHeight) { mutableFloatStateOf(0f) }

    fun consumeOffset(delta: Float): Float {
        val old = playerOffsetPx
        playerOffsetPx = (old + delta).coerceIn(-maxPlayerHeightPx, 0f)
        return playerOffsetPx - old
    }

    val nestedScrollConnection = remember(
        maxPlayerHeightPx,
        collapseLocked,
        isFullscreen,
        isInPipMode,
    ) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (collapseLocked || isFullscreen || isInPipMode || available.y >= 0f) {
                    return Offset.Zero
                }
                return Offset(0f, consumeOffset(available.y))
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (collapseLocked || isFullscreen || isInPipMode || available.y <= 0f) {
                    return Offset.Zero
                }
                return Offset(0f, consumeOffset(available.y))
            }
        }
    }

    LaunchedEffect(collapseLocked, isFullscreen, isInPipMode) {
        if (collapseLocked || isFullscreen || isInPipMode) playerOffsetPx = 0f
    }
    LaunchedEffect(playerOffsetPx) {
        onExpandedChange(playerOffsetPx == 0f)
    }

    val visiblePlayerHeight = with(density) {
        (maxPlayerHeightPx + playerOffsetPx).coerceAtLeast(0f).toDp()
    }
    Column(modifier = modifier.nestedScroll(nestedScrollConnection)) {
        Box(
            modifier = if (isFullscreen || isInPipMode) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(visiblePlayerHeight)
            }
        ) {
            player()
        }
        if (!isFullscreen && !isInPipMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                details()
            }
        }
    }
}

private const val MIN_RESUME_POSITION_MS = 5_000L
private const val RESTART_ACTION_VISIBLE_MS = 5_000L
