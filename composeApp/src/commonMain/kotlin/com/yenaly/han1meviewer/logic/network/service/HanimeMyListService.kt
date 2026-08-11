package com.yenaly.han1meviewer.logic.network.service

import com.yenaly.han1meviewer.logic.network.NetworkUpload
import com.yenaly.han1meviewer.logic.network.RawNetworkResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Parameters
import io.ktor.http.quote

class HanimeMyListService(
    client: HttpClient,
    baseUrl: String,
) {
    private val transport = KtorServiceTransport(client, baseUrl)

    suspend fun getMyListItems(
        userId: String,
        listType: String,
        page: Int,
    ): RawNetworkResponse = getWithQuery(
        pathSegments = listOf("user", userId, listType),
        parameters = Parameters.build { append("page", page.toString()) },
    )

    suspend fun getOnlineWatchHistories(
        userId: String,
        sort: String,
        page: Int,
    ): RawNetworkResponse = getWithQuery(
        pathSegments = listOf("user", userId, "histories"),
        parameters = Parameters.build {
            append("sort", sort)
            append("page", page.toString())
        },
    )

    suspend fun getUserAccountPage(userId: String): RawNetworkResponse =
        transport.raw(pathSegments = listOf("user", userId, "edit"))

    suspend fun getUploadedVideos(
        userId: String,
        sort: String,
        page: Int,
    ): RawNetworkResponse = getWithQuery(
        pathSegments = listOf("user", userId, "uploaded"),
        parameters = Parameters.build {
            append("sort", sort)
            append("page", page.toString())
        },
    )

    suspend fun getUploadingVideos(
        userId: String,
        sort: String,
        page: Int,
    ): RawNetworkResponse = getWithQuery(
        pathSegments = listOf("user", userId, "uploading"),
        parameters = Parameters.build {
            append("sort", sort)
            append("page", page.toString())
        },
    )

    suspend fun updateUserAccountProfile(
        userId: String,
        csrfToken: String?,
        method: String = "patch",
        type: String = "profile",
        name: String,
        email: String,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = formRequest(
        method = HttpMethod.Post,
        pathSegments = listOf("user", userId),
        csrfTokenHeader = csrfToken_1,
        parameters = Parameters.build {
            csrfToken?.let { append("_token", it) }
            append("_method", method)
            append("type", type)
            append("name", name)
            append("email", email)
        },
    )

    suspend fun updateUserAccountPassword(
        userId: String,
        csrfToken: String?,
        method: String = "patch",
        type: String = "password",
        oldPassword: String,
        newPassword: String,
        newPasswordConfirm: String,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = formRequest(
        method = HttpMethod.Post,
        pathSegments = listOf("user", userId),
        csrfTokenHeader = csrfToken_1,
        parameters = Parameters.build {
            csrfToken?.let { append("_token", it) }
            append("_method", method)
            append("type", type)
            append("password_old", oldPassword)
            append("password_new", newPassword)
            append("password_new_confirm", newPasswordConfirm)
        },
    )

    suspend fun updateUserAccountAvatar(
        userId: String,
        csrfToken: String,
        method: String,
        type: String,
        photo: NetworkUpload,
    ): RawNetworkResponse = transport.raw(
        method = HttpMethod.Post,
        pathSegments = listOf("user", userId),
    ) {
        setBody(MultiPartFormDataContent(formData {
            appendRetrofitTextPart("_token", csrfToken)
            appendRetrofitTextPart("_method", method)
            appendRetrofitTextPart("type", type)
            append(
                key = "photo",
                value = ChannelProvider(
                    size = photo.contentLength,
                    block = photo.openChannel,
                ),
                headers = Headers.build {
                    append(HttpHeaders.ContentDisposition, "filename=${photo.filename.quote()}")
                    append(HttpHeaders.ContentType, photo.contentType)
                },
            )
        }))
    }

    suspend fun deleteOnlineWatchHistory(
        videoCode: String,
        tab: String = "histories",
        csrfToken: String?,
    ): RawNetworkResponse = formRequest(
        method = HttpMethod.Delete,
        pathSegments = listOf("user", "tab-item", videoCode),
        csrfTokenHeader = csrfToken,
        parameters = Parameters.build { append("tab", tab) },
    )

    suspend fun getMyPlayListItems(
        listCode: String,
        page: Int,
    ): RawNetworkResponse = getWithQuery(
        pathSegments = listOf("playlist"),
        parameters = Parameters.build {
            append("list", listCode)
            append("page", page.toString())
        },
    )

    suspend fun deleteMyListItems(
        listType: String,
        videoCode: String,
        count: Int = 1,
        csrfToken: String?,
    ): RawNetworkResponse = formRequest(
        method = HttpMethod.Post,
        pathSegments = listOf("deletePlayitem"),
        csrfTokenHeader = csrfToken,
        parameters = Parameters.build {
            append("playlist_id", listType)
            append("video_id", videoCode)
            append("count", count.toString())
        },
    )

    suspend fun addToMyFavVideo(
        videoCode: String,
        likeStatus: String,
        csrfToken: String?,
        userId: String?,
        isPositive: Int = 1,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = formRequest(
        method = HttpMethod.Post,
        pathSegments = listOf("like"),
        csrfTokenHeader = csrfToken_1,
        parameters = Parameters.build {
            append("like-foreign-id", videoCode)
            append("like-status", likeStatus)
            csrfToken?.let { append("_token", it) }
            userId?.let { append("like-user-id", it) }
            append("like-is-positive", isPositive.toString())
        },
    )

    suspend fun rateVideo(
        videoCode: String,
        isPositive: Int,
        likeStatus: String,
        unlikeStatus: String,
        likesCount: Int,
        unlikesCount: Int,
        csrfToken: String?,
        userId: String?,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = formRequest(
        method = HttpMethod.Post,
        pathSegments = listOf("like"),
        csrfTokenHeader = csrfToken_1,
        parameters = Parameters.build {
            append("like-foreign-id", videoCode)
            append("like-is-positive", isPositive.toString())
            append("like-status", likeStatus)
            append("unlike-status", unlikeStatus)
            append("likes-count", likesCount.toString())
            append("unlikes-count", unlikesCount.toString())
            csrfToken?.let { append("_token", it) }
            userId?.let { append("like-user-id", it) }
        },
    )

    suspend fun getPlaylists(
        userId: String,
        page: Int,
    ): RawNetworkResponse = getWithQuery(
        pathSegments = listOf("user", userId, "playlists"),
        parameters = Parameters.build { append("page", page.toString()) },
    )

    suspend fun createPlaylist(
        csrfToken: String?,
        videoCode: String,
        title: String,
        description: String,
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = formRequest(
        method = HttpMethod.Post,
        pathSegments = listOf("createPlaylist"),
        csrfTokenHeader = csrfToken_1,
        parameters = Parameters.build {
            csrfToken?.let { append("_token", it) }
            append("create-playlist-video-id", videoCode)
            append("playlist-title", title)
            append("playlist-description", description)
        },
    )

    suspend fun addToMyList(
        csrfToken: String?,
        listCode: String,
        videoCode: String,
        isChecked: Boolean,
        userId: String = "",
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = formRequest(
        method = HttpMethod.Post,
        pathSegments = listOf("save"),
        csrfTokenHeader = csrfToken_1,
        parameters = Parameters.build {
            csrfToken?.let { append("_token", it) }
            append("input_id", listCode)
            append("video_id", videoCode)
            append("is_checked", isChecked.toString())
            append("user_id", userId)
        },
    )

    suspend fun modifyPlaylist(
        listCode: String,
        title: String,
        description: String,
        delete: String?,
        csrfToken: String?,
        method: String? = "PUT",
        csrfToken_1: String? = csrfToken,
    ): RawNetworkResponse = formRequest(
        method = HttpMethod.Post,
        pathSegments = listOf("playlist", listCode),
        csrfTokenHeader = csrfToken_1,
        parameters = Parameters.build {
            append("playlist-title", title)
            append("playlist-description", description)
            delete?.let { append("playlist-delete", it) }
            csrfToken?.let { append("_token", it) }
            method?.let { append("_method", it) }
        },
    )

    private suspend fun getWithQuery(
        pathSegments: List<String>,
        parameters: Parameters,
    ): RawNetworkResponse = transport.raw(pathSegments = pathSegments) {
        url.parameters.appendAll(parameters)
    }

    private suspend fun formRequest(
        method: HttpMethod,
        pathSegments: List<String>,
        csrfTokenHeader: String?,
        parameters: Parameters,
    ): RawNetworkResponse = transport.raw(
        method = method,
        pathSegments = pathSegments,
    ) {
        setBody(FormDataContent(parameters))
        csrfTokenHeader?.let { header("X-CSRF-TOKEN", it) }
    }
}

private fun FormBuilder.appendRetrofitTextPart(name: String, value: String) {
    append(
        key = name,
        value = value.encodeToByteArray(),
        headers = Headers.build {
            append("Content-Transfer-Encoding", "binary")
            append(HttpHeaders.ContentType, "text/plain; charset=utf-8")
        },
    )
}
