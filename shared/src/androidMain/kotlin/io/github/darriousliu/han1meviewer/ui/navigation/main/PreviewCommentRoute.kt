package io.github.darriousliu.han1meviewer.ui.navigation.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.darriousliu.han1meviewer.core.common.PREVIEW_COMMENT_PREFIX
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.R
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.core.ui.component.BottomSheetHandler
import io.github.darriousliu.han1meviewer.core.navigation.PreviewCommentRoute
import io.github.darriousliu.han1meviewer.feature.comment.ChildCommentScreen
import io.github.darriousliu.han1meviewer.feature.comment.CommentMessage
import io.github.darriousliu.han1meviewer.feature.comment.CommentScreen
import io.github.darriousliu.han1meviewer.feature.comment.CommentViewModel
import io.github.darriousliu.han1meviewer.feature.comment.PreviewCommentPrefetcher
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.there_is_a_small_issue
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewCommentRouteScreen(
    route: PreviewCommentRoute,
    onBack: () -> Unit,
) {
    // NavEntry 作用域 + dateCode 构造注入：一个日期页一个实例，随页面存亡。
    val viewModel: CommentViewModel = viewModel(
        factory = CommentViewModel.factory(route.dateCode),
    )
    val comments = viewModel.videoCommentFlow
    val commentState = viewModel.videoCommentStateFlow
    val commentUiState = remember(route.dateCode) {
        viewModel.getCommentUiState(route.dateCode)
    }
    var childCommentId by rememberSaveable { mutableStateOf(commentUiState.childCommentId) }
    val childSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(
            SheetValue.Hidden,
            SheetValue.PartiallyExpanded,
            SheetValue.Expanded,
        ),
    )
    val scope = rememberCoroutineScope()
    val prefetchedComments = PreviewCommentPrefetcher.here(viewModel)
        .commentFlow
        .collectAsStateWithLifecycle()
        .value
    val hasPrefetchedComments = prefetchedComments.isNotEmpty()
    val reportMessages = remember { kotlinx.coroutines.flow.MutableSharedFlow<CommentMessage>() }
    val reportReasons = viewModel.reportReasons.collectAsStateWithLifecycle().value

    LaunchedEffect(route.dateCode, hasPrefetchedComments, prefetchedComments) {
        if (hasPrefetchedComments) {
            viewModel.updateComments(prefetchedComments)
        } else {
            viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode)
        }
    }

    DisposableEffect(Unit) {
        PreviewCommentPrefetcher.here(viewModel)
            .tag(PreviewCommentPrefetcher.Scope.PREVIEW_COMMENT_ACTIVITY)
        onDispose {
            PreviewCommentPrefetcher.bye(PreviewCommentPrefetcher.Scope.PREVIEW_COMMENT_ACTIVITY)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.postCommentFlow.collect { state ->
            if (state is WebsiteState.Success) {
                viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.postReplyFlow.collect { state ->
            if (state is WebsiteState.Success) {
                viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.commentLikeFlow.collect { state ->
            if (state is WebsiteState.Success) {
                viewModel.handleCommentLike(state.info)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reportMessage.collect { msg ->
            val text = getString(msg.resource, *msg.args.toTypedArray())
            reportMessages.emit(CommentMessage(text))
        }
    }

    childCommentId?.let { currentCommentId ->
        ModalBottomSheet(
            onDismissRequest = {
                childCommentId = null
                viewModel.setChildCommentId(route.dateCode, null)
                viewModel.clearVideoReplyList()
            },
            sheetState = childSheetState,
            dragHandle = null
        ) {
            LaunchedEffect(currentCommentId) {
                viewModel.getCommentReply(currentCommentId)
            }
            BottomSheetHandler()
            val mappedReportFlow = remember(viewModel.reportMessage) {
                viewModel.reportMessage.map { message ->
                    val text = getString(message.resource, *message.args.toTypedArray())
                    CommentMessage(text)
                }
            }
            ChildCommentScreen(
                commentsFlow = viewModel.videoReplyFlow,
                commentStateFlow = viewModel.videoReplyStateFlow,
                reportMessageFlow = mappedReportFlow,
                postReplyStateFlow = viewModel.postReplyFlow,
                commentLikeStateFlow = viewModel.commentLikeFlow,
                reportReasons = reportReasons,
                isAlreadyLogin = Preferences.isAlreadyLogin,
                onRefresh = { viewModel.getCommentReply(currentCommentId) },
                onReply = { _, text ->
                    viewModel.postReply(currentCommentId, text)
                },
                onReport = { comment, reason ->
                    viewModel.reportComment(
                        reason.reasonKey ?: reason.value,
                        viewModel.currentUserId,
                        "${Preferences.baseUrl}watch?v=${viewModel.code}",
                        comment.reportableType,
                        comment.reportableId,
                    )
                },
                onThumbUp = { comment ->
                    viewModel.likeChildComment(
                        true, 0, comment,
                        likeCommentStatus = comment.post.likeCommentStatus,
                    )
                },
                onThumbDown = { comment ->
                    viewModel.likeChildComment(
                        false, 0, comment,
                        unlikeCommentStatus = comment.post.unlikeCommentStatus,
                    )
                },
                onCommentLikeSuccess = viewModel::handleCommentLike,
                onReplyStateChange = { isReplying ->
                    if (isReplying) {
                        scope.launch { childSheetState.expand() }
                    }
                },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.latest_hanime_comment, route.date)) },
                navigationIcon = {
                    FilledIconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        CommentScreen(
            commentsFlow = comments,
            commentStateFlow = commentState,
            reportMessageFlow = reportMessages,
            currentSortType = viewModel.currentSortType,
            reportReasons = reportReasons,
            isPreviewCommentPrefetched = hasPrefetchedComments,
            isAlreadyLogin = Preferences.isAlreadyLogin,
            onRefresh = { viewModel.getComment(PREVIEW_COMMENT_PREFIX, route.dateCode) },
            onReply = { comment, text ->
                if (!Preferences.isAlreadyLogin) return@CommentScreen
                val replyTargetId = comment.replyTargetIdOrNull
                if (replyTargetId == null) {
                    scope.launch {
                        reportMessages.emit(
                            CommentMessage(getString(Res.string.there_is_a_small_issue))
                        )
                    }
                    return@CommentScreen
                }
                viewModel.postReply(replyTargetId, text)
            },
            onReport = { comment, reason ->
                viewModel.reportComment(
                    reason.reasonKey ?: reason.value,
                    viewModel.currentUserId,
                    "${Preferences.baseUrl}watch?v=${viewModel.code}",
                    comment.reportableType,
                    comment.reportableId,
                )
            },
            onThumbUp = { comment ->
                if (!Preferences.isAlreadyLogin) return@CommentScreen
                if (comment.isChildComment) {
                    viewModel.likeChildComment(
                        true,
                        0,
                        comment,
                        likeCommentStatus = comment.post.likeCommentStatus
                    )
                } else {
                    viewModel.likeComment(
                        true,
                        0,
                        comment,
                        likeCommentStatus = comment.post.likeCommentStatus
                    )
                }
            },
            onThumbDown = { comment ->
                if (!Preferences.isAlreadyLogin) return@CommentScreen
                if (comment.isChildComment) {
                    viewModel.likeChildComment(
                        false,
                        0,
                        comment,
                        unlikeCommentStatus = comment.post.unlikeCommentStatus
                    )
                } else {
                    viewModel.likeComment(
                        false,
                        0,
                        comment,
                        unlikeCommentStatus = comment.post.unlikeCommentStatus
                    )
                }
            },
            onViewMoreReplies = { comment ->
                comment.replyTargetIdOrNull?.let {
                    childCommentId = it
                    viewModel.setChildCommentId(route.dateCode, it)
                }
            },
            onSortChange = viewModel::setSortType,
            onComposeComment = { text ->
                viewModel.currentUserId?.let { id ->
                    viewModel.postComment(id, viewModel.code, PREVIEW_COMMENT_PREFIX, text)
                } ?: scope.launch {
                    reportMessages.emit(
                        CommentMessage(getString(Res.string.there_is_a_small_issue))
                    )
                }
            },
            initialFirstVisibleItemIndex = commentUiState.firstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset = commentUiState.firstVisibleItemScrollOffset,
            onCommentScrollChange = { index, offset ->
                viewModel.setCommentScrollState(route.dateCode, index, offset)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        )
    }
}
