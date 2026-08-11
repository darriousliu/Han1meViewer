package com.yenaly.han1meviewer.logic.network.service

import com.yenaly.han1meviewer.logic.network.RawNetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters

class HanimeCommentService(
    client: HttpClient,
    baseUrl: String,
) {
    private val transport = KtorServiceTransport(client, baseUrl)

    suspend fun getComments(type: String, code: String): RawNetworkResponse =
        transport.raw(pathSegments = listOf("loadComment")) {
            url.parameters.append("type", type)
            url.parameters.append("id", code)
        }

    suspend fun getCommentReply(commentId: String): RawNetworkResponse =
        transport.raw(pathSegments = listOf("loadReplies")) {
            url.parameters.append("id", commentId)
        }

    suspend fun postComment(
        csrfToken: String?,
        currentUserId: String,
        type: String,
        targetUserId: String,
        text: String,
        count: Int = 1,
        isPolitical: Int = 0,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = postForm(
        pathSegments = listOf("createComment"),
        csrfTokenHeader = csrfToken_1,
        parameters = Parameters.build {
            csrfToken?.let { append("_token", it) }
            append("comment-user-id", currentUserId)
            append("comment-type", type)
            append("comment-foreign-id", targetUserId)
            append("comment-text", text)
            append("comment-count", count.toString())
            append("comment-is-political", isPolitical.toString())
        },
    )

    suspend fun postCommentReply(
        csrfToken: String?,
        replyCommentId: String,
        text: String,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = postForm(
        pathSegments = listOf("replyComment"),
        csrfTokenHeader = csrfToken_1,
        parameters = Parameters.build {
            csrfToken?.let { append("_token", it) }
            append("reply-comment-id", replyCommentId)
            append("reply-comment-text", text)
        },
    )

    suspend fun likeComment(
        csrfToken: String?,
        foreignType: String,
        foreignId: String?,
        isPositive: Int,
        likeUserId: String?,
        commentLikesCount: Int,
        commentLikesSum: Int,
        likeCommentStatus: Int,
        unlikeCommentStatus: Int,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = postForm(
        pathSegments = listOf("commentLike"),
        csrfTokenHeader = csrfToken_1,
        parameters = Parameters.build {
            csrfToken?.let { append("_token", it) }
            append("foreign_type", foreignType)
            foreignId?.let { append("foreign_id", it) }
            append("is_positive", isPositive.toString())
            likeUserId?.let { append("comment-like-user-id", it) }
            append("comment-likes-count", commentLikesCount.toString())
            append("comment-likes-sum", commentLikesSum.toString())
            append("like-comment-status", likeCommentStatus.toString())
            append("unlike-comment-status", unlikeCommentStatus.toString())
        },
    )

    suspend fun submitReport(
        userId: String?,
        csrfToken: String?,
        redirectUrl: String,
        reportableId: String?,
        reportableType: String?,
        reason: String,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse {
        requireNotNull(userId) { "Path parameter userId must not be null." }
        return postForm(
            pathSegments = listOf("user", userId, "report"),
            csrfTokenHeader = csrfToken_1,
            parameters = Parameters.build {
                csrfToken?.let { append("_token", it) }
                append("redirect-url", redirectUrl)
                reportableId?.let { append("reportable-id", it) }
                reportableType?.let { append("reportable-type", it) }
                append("reason", reason)
            },
        )
    }

    private suspend fun postForm(
        pathSegments: List<String>,
        csrfTokenHeader: String?,
        parameters: Parameters,
    ): RawNetworkResponse = transport.raw(
        method = HttpMethod.Post,
        pathSegments = pathSegments,
    ) {
        setBody(FormDataContent(parameters))
        csrfTokenHeader?.let { header("X-CSRF-TOKEN", it) }
    }
}
