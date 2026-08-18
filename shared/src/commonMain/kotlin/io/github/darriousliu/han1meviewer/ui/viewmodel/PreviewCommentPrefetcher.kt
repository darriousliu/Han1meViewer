package io.github.darriousliu.han1meviewer.ui.viewmodel

import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.core.model.VideoComments

/**
 * 连通预览页与预览评论页的评论预取器。
 */
class PreviewCommentPrefetcher private constructor(
    private val commentViewModel: CommentViewModel
) {

    object Scope {
        const val PREVIEW_ACTIVITY = 1
        const val PREVIEW_COMMENT_ACTIVITY = 1 shl 1
    }

    companion object {
        private const val TAG = "PreviewCommentPrefetcher"

        private var prefetcher: PreviewCommentPrefetcher? = null

        fun here(viewModel: CommentViewModel): PreviewCommentPrefetcher {
            return prefetcher ?: PreviewCommentPrefetcher(viewModel).also { prefetcher = it }
        }

        fun bye(scope: Int) {
            prefetcher?.also {
                it.activityMask = it.activityMask and scope.inv()
                if (it.activityMask == 0) {
                    prefetcher = null
                    Logger.i(tag = TAG) { "bye executed successfully" }
                } else {
                    if (it.activityMask and Scope.PREVIEW_ACTIVITY != 0) {
                        Logger.i(tag = TAG) {
                            "bye executed failed: prefetcher is still alive cuz of PreviewActivity"
                        }
                    }
                    if (it.activityMask and Scope.PREVIEW_COMMENT_ACTIVITY != 0) {
                        Logger.i(tag = TAG) {
                            "bye executed failed: prefetcher is still alive cuz of PreviewCommentActivity"
                        }
                    }
                }
            }
        }
    }

    private var activityMask = 0

    val commentFlow get() = commentViewModel.videoCommentFlow

    fun tag(scope: Int) {
        activityMask = activityMask or scope
    }

    fun fetch(type: String, code: String) {
        commentViewModel.getComment(type, code)
    }

    fun update(comments: List<VideoComments.VideoComment>) {
        commentViewModel.updateComments(comments)
    }
}
