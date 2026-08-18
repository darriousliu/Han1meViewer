package io.github.darriousliu.han1meviewer.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.logic.NetworkRepo
import io.github.darriousliu.han1meviewer.logic.model.CommentPlace
import io.github.darriousliu.han1meviewer.logic.model.ReportReason
import io.github.darriousliu.han1meviewer.logic.model.VideoCommentArgs
import io.github.darriousliu.han1meviewer.logic.model.VideoComments
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.ui.screen.video.CommentSortType
import io.github.darriousliu.han1meviewer.ui.viewmodel.CsrfTokenStore.csrfToken
import io.github.darriousliu.han1meviewer.core.common.util.loadBundledJson
import io.github.darriousliu.han1meviewer.core.common.util.localizedTextOrNull
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.cancel_thumb_down_success
import io.github.darriousliu.han1meviewer.core.resource.cancel_thumb_up_success
import io.github.darriousliu.han1meviewer.core.resource.report_failed
import io.github.darriousliu.han1meviewer.core.resource.report_success
import io.github.darriousliu.han1meviewer.core.resource.thumb_down_success
import io.github.darriousliu.han1meviewer.core.resource.thumb_up_success
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

private val logger = Logger.withTag("CommentViewModel")

/**
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2022/06/28 028 14:18
 */
class CommentViewModel(
    /** 本实例服务的评论目标：视频页是 videoCode，预览评论页是 dateCode。 */
    val code: String,
) : ViewModel() {

    data class CommentUiState(
        val firstVisibleItemIndex: Int = 0,
        val firstVisibleItemScrollOffset: Int = 0,
        val childCommentId: String? = null,
    )

    private val commentUiStateMap = mutableMapOf<String, CommentUiState>()

    var currentUserId: String? = null
    //reportMessage为点击举报按钮之后的响应及错误信息
    private val _reportMessage = MutableSharedFlow<Message>()
    val reportMessage = _reportMessage.asSharedFlow()
    data class Message(
        val resource: StringResource,
        val args: List<Any> = emptyList()
    )

    private val _videoCommentStateFlow =
        MutableStateFlow<WebsiteState<VideoComments>>(WebsiteState.Loading)
    val videoCommentStateFlow = _videoCommentStateFlow.asStateFlow()

    private val _videoReplyStateFlow =
        MutableStateFlow<WebsiteState<VideoComments>>(WebsiteState.Loading)
    val videoReplyStateFlow = _videoReplyStateFlow.asStateFlow()

    private val _videoCommentFlow = MutableStateFlow(emptyList<VideoComments.VideoComment>())
    val videoCommentFlow = _videoCommentFlow.asStateFlow()

    private val _videoReplyFlow = MutableStateFlow(emptyList<VideoComments.VideoComment>())
    val videoReplyFlow = _videoReplyFlow.asStateFlow()

    private val _postCommentFlow =
        MutableSharedFlow<WebsiteState<Unit>>(replay = 0)
    val postCommentFlow = _postCommentFlow.asSharedFlow()

    private val _postReplyFlow =
        MutableSharedFlow<WebsiteState<Unit>>(replay = 0)
    val postReplyFlow = _postReplyFlow.asSharedFlow()

    private val _commentLikeFlow =
        MutableSharedFlow<WebsiteState<VideoCommentArgs>>(replay = 0)
    val commentLikeFlow = _commentLikeFlow.asSharedFlow()
    private val _reportReasons = MutableStateFlow<List<ReportReason>>(emptyList())
    val reportReasons = _reportReasons.asStateFlow()

    init {
        viewModelScope.launch {
            _reportReasons.value =
                loadBundledJson<List<ReportReason>>("files/report_reason.json").orEmpty()
        }
    }

    private val _currentSortType = MutableStateFlow(CommentSortType.LATEST)
    val currentSortType = _currentSortType.asStateFlow()
    fun setSortType(type: CommentSortType) {
        _currentSortType.value = type
    }

    fun getCommentUiState(code: String): CommentUiState {
        return commentUiStateMap[code] ?: CommentUiState()
    }

    fun setCommentScrollState(
        code: String,
        firstVisibleItemIndex: Int,
        firstVisibleItemScrollOffset: Int,
    ) {
        val current = commentUiStateMap[code] ?: CommentUiState()
        commentUiStateMap[code] = current.copy(
            firstVisibleItemIndex = firstVisibleItemIndex,
            firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
        )
    }

    fun setChildCommentId(code: String, childCommentId: String?) {
        val current = commentUiStateMap[code] ?: CommentUiState()
        commentUiStateMap[code] = current.copy(childCommentId = childCommentId)
    }

    fun clearCommentData(){
        _videoCommentFlow.value = emptyList()
    }
    fun getComment(type: String, code: String) {
        viewModelScope.launch {
            _videoCommentStateFlow.value = WebsiteState.Loading
            NetworkRepo.getComments(type, code).collect { state ->
                _videoCommentStateFlow.value = state
                _videoCommentFlow.update { prevList ->
                    when (state) {
                        is WebsiteState.Success -> state.info.videoComment
                        is WebsiteState.Loading -> emptyList()
                        else -> prevList
                    }
                }
            }
        }
    }

    fun updateComments(comments: List<VideoComments.VideoComment>) {
        _videoCommentFlow.update { comments }
    }

    fun getCommentReply(commentId: String) {
        viewModelScope.launch {
            // 每次获取评论回复时，都会重新加载
            _videoReplyStateFlow.value = WebsiteState.Loading
            NetworkRepo.getCommentReply(commentId).collect { state ->
                _videoReplyStateFlow.value = state
                _videoReplyFlow.update { prevList ->
                    when (state) {
                        is WebsiteState.Success -> state.info.videoComment
                        is WebsiteState.Loading -> emptyList()
                        else -> prevList
                    }
                }
            }
        }
    }

    fun postComment(
        currentUserId: String,
        targetUserId: String,
        type: String,
        text: String,
    ) {
        viewModelScope.launch {
            NetworkRepo.postComment(csrfToken, currentUserId, targetUserId, type, text)
                .collect(_postCommentFlow::emit)
        }
    }

    fun postReply(
        replyCommentId: String,
        text: String,
    ) {
        viewModelScope.launch {
            NetworkRepo.postCommentReply(csrfToken, replyCommentId, text)
                .collect(_postReplyFlow::emit)
        }
    }

    fun likeComment(
        isPositive: Boolean, commentPosition: Int,
        comment: VideoComments.VideoComment, likeCommentStatus: Boolean = false,
        unlikeCommentStatus: Boolean = false,
    ) = likeCommentInternal(
        CommentPlace.COMMENT, isPositive, commentPosition,
        comment, likeCommentStatus, unlikeCommentStatus
    )

    fun likeChildComment(
        isPositive: Boolean, commentPosition: Int,
        comment: VideoComments.VideoComment, likeCommentStatus: Boolean = false,
        unlikeCommentStatus: Boolean = false,
    ) = likeCommentInternal(
        CommentPlace.CHILD_COMMENT, isPositive, commentPosition,
        comment, likeCommentStatus, unlikeCommentStatus
    )

    private fun likeCommentInternal(
        commentPlace: CommentPlace,
        isPositive: Boolean,
        commentPosition: Int,
        comment: VideoComments.VideoComment,
        likeCommentStatus: Boolean = false,
        unlikeCommentStatus: Boolean = false,
    ) {
        viewModelScope.launch {
            NetworkRepo.likeComment(
                csrfToken,
                commentPlace,
                comment.post.foreignId,
                isPositive,
                comment.post.likeUserId,
                comment.post.commentLikesCount ?: 0,
                comment.post.commentLikesSum ?: 0,
                likeCommentStatus,
                unlikeCommentStatus,
                commentPosition, comment
            ).collect { argState ->
                _commentLikeFlow.emit(argState)
                if (argState is WebsiteState.Success) {
                    when (commentPlace) {
                        CommentPlace.COMMENT -> _videoCommentFlow.update { prevList ->
                            prevList.map { item ->
                                if (item.reportableId == comment.reportableId){
                                    item.handleCommentLike(argState.info)
                                } else {
                                    item
                                }
                            }
//                            prevList.toMutableList().apply {
//                                this[commentPosition] =
//                                    this[commentPosition].handleCommentLike(argState.info)
//                            }
                        }

                        CommentPlace.CHILD_COMMENT -> _videoReplyFlow.update { prevList ->
                            prevList.map { item ->
                                if (item.reportableId == comment.reportableId){
                                    item.handleCommentLike(argState.info)
                                } else {
                                    item
                                }
//                            prevList.toMutableList().apply {
//                                this[commentPosition] =
//                                    this[commentPosition].handleCommentLike(argState.info)
//                            }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun VideoComments.VideoComment.handleCommentLike(
        args: VideoCommentArgs,
    ) = if (args.isPositive) {
        this.incLikesCount(cancel = post.likeCommentStatus)
    } else {
        this.decLikesCount(cancel = post.unlikeCommentStatus)
    }

    fun handleCommentLike(args: VideoCommentArgs) {
        val messageRes = if (args.isPositive) {
            if (args.comment.post.likeCommentStatus) {
                Res.string.cancel_thumb_up_success
            } else {
                Res.string.thumb_up_success
            }
        } else {
            if (args.comment.post.unlikeCommentStatus) {
                Res.string.cancel_thumb_down_success
            } else {
                Res.string.thumb_down_success
            }
        }
        viewModelScope.launch { _reportMessage.emit(Message(messageRes)) }
    }

    fun reportComment(
        reason: String,
        currentUserId: String?,
        redirectUrl: String,
        reportableType: String?,
        reportableId: String?
    ){
        viewModelScope.launch {
            logger.i { "reportComment" }
            NetworkRepo.reportComment(
                csrfToken = csrfToken,
                reason = reason,
                currentUserId = currentUserId,
                redirectUrl = redirectUrl,
                reportableType = reportableType,
                reportableId = reportableId
            ).collect { state ->
                when(state){
                    is WebsiteState.Error -> {
                        _reportMessage.emit(
                            Message(
                                Res.string.report_failed,
                                listOf(state.throwable.localizedTextOrNull() ?: "unknown")
                            )
                        )
                    }
                    WebsiteState.Loading -> {

                    }
                    is WebsiteState.Success<*> -> {
                        _reportMessage.emit(Message(Res.string.report_success))
                    }
                }
            }
        }
    }
    fun clearVideoReplyList() { _videoReplyFlow.value = emptyList() }

    companion object {

        /**
         * 预览页那个「预取缓冲」实例在 Activity store 里的 key。
         *
         * 必须和预览评论页的实例区分开：同一个 store 里 key 相同的话
         * `viewModel()` 会直接返回已有实例、**静默忽略 factory**，
         * code 就会是先创建的那个实例的值。
         */
        const val PREFETCH_KEY = "CommentViewModel:preview-prefetch"

        /**
         * 一个页面一个实例，[code] 在构造时定死。
         *
         * 之所以不做成 `lateinit var` 在组合里赋值：评论页会被
         * `HorizontalPager(beyondViewportPageCount = 1)` 在首帧预组合，
         * 组合期就要读 [code]，而任何「稍后赋值」的写法都赶不上这一帧。
         */
        fun factory(code: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { CommentViewModel(code) }
        }

        /**
         * 预览页只把它当预取缓冲用——真正拉哪个月由
         * [PreviewCommentPrefetcher.fetch] 每次显式传 code，
         * 这个实例的 [code] 不参与任何逻辑，故留空。
         */
        fun prefetchFactory(): ViewModelProvider.Factory = factory(code = "")
    }
}
