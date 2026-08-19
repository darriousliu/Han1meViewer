package io.github.darriousliu.han1meviewer.core.repository

import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.core.common.EMPTY_STRING
import io.github.darriousliu.han1meviewer.core.common.HJson
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.storage.Preferences.isAlreadyLogin
import io.github.darriousliu.han1meviewer.core.common.exception.CloudFlareBlockedException
import io.github.darriousliu.han1meviewer.core.common.exception.HanimeNotFoundException
import io.github.darriousliu.han1meviewer.core.common.exception.IPBlockedException
import io.github.darriousliu.han1meviewer.core.common.exception.LocalizedStateException
import io.github.darriousliu.han1meviewer.core.common.exception.ParseException
import io.github.darriousliu.han1meviewer.core.model.CommentPlace
import io.github.darriousliu.han1meviewer.core.model.CreatorSort
import io.github.darriousliu.han1meviewer.core.model.ModifiedPlaylistArgs
import io.github.darriousliu.han1meviewer.core.model.MyListType
import io.github.darriousliu.han1meviewer.core.model.OnlineWatchHistorySort
import io.github.darriousliu.han1meviewer.core.model.VideoCommentArgs
import io.github.darriousliu.han1meviewer.core.model.VideoComments
import io.github.darriousliu.han1meviewer.core.network.HanimeNetwork
import io.github.darriousliu.han1meviewer.core.common.state.PageLoadingState
import io.github.darriousliu.han1meviewer.core.common.state.VideoLoadingState
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.core.common.util.isSslHandshakeError
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.account_or_password_wrong
import io.github.darriousliu.han1meviewer.core.resource.cloudflare_ip_block_warning
import io.github.darriousliu.han1meviewer.core.resource.cloudflare_network_mismatch
import io.github.darriousliu.han1meviewer.core.resource.not_logged_in_currently
import io.github.darriousliu.han1meviewer.core.resource.ssl_handshake_error
import io.github.darriousliu.han1meviewer.core.resource.video_might_not_exist
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import io.github.darriousliu.han1meviewer.core.parse.Parser

/**
 * @project Hanime1
 * @author Yenaly Liew
 * @time 2022/06/08 008 22:38
 */
object NetworkRepo {

    //<editor-fold desc="Hanime">

    fun getHomePage() = websiteIOFlow(
        request = { HanimeNetwork.hanimeService.getHomePage(Preferences.homeUrl) },
        action = Parser::homePageVer2
    )

    fun getHanimeSearchResult(
        page: Int, query: String?, genre: String?,
        sort: String?, broad: Boolean, date: String?,
        duration: String?, tags: Set<String>, brands: Set<String>,
    ) = pageIOFlow(
        request = {
            HanimeNetwork.hanimeService.getHanimeSearchResult(
                page, query, genre, sort,
                if (broad) "on" else null,
                date, duration, tags.toList(), brands.toList()
            )
        },
        action = Parser::hanimeSearch
    )

    fun getHanimeVideo(videoCode: String) = videoIOFlow(
        request = { HanimeNetwork.hanimeService.getHanimeVideo(videoCode) },
        action = Parser::hanimeVideoVer2
    )

    fun getHanimePreview(date: String) = websiteIOFlow(
        request = { HanimeNetwork.hanimeService.getHanimePreview(date) },
        action = Parser::hanimePreview
    )

    //获取订阅或者可以说是关注列表及它们的更新
    fun getMySubscriptions(page: Int) = websiteIOFlow(
        request = { HanimeNetwork.hanimeService.getMySubscriptions(page) },
        action = Parser::getMySubscriptions
    )
    //</editor-fold>

    //<editor-fold desc="My List">

    fun getMyListItems(userId: String, listType: Any, page: Int) = pageIOFlow(
        request = {
            when (listType) {
                is String ->
                    HanimeNetwork.myListService.getMyListItems(userId, listType, page)

                is MyListType ->
                    HanimeNetwork.myListService.getMyListItems(userId, listType.value, page)

                else ->
                    throw IllegalArgumentException("typeOrId must be String or MyListType")
            }
        },
        action = Parser::myListItems
    )

    fun getMyPlayListItems(page: Int = 1, listCode: String = "0") = pageIOFlow(
        request = {
            HanimeNetwork.myListService.getMyPlayListItems(listCode, page)
        },
        action = Parser::myPlayListItems
    )

    fun getOnlineWatchHistories(
        userId: String,
        sort: OnlineWatchHistorySort,
        page: Int,
    ) = pageIOFlow(
        request = {
            HanimeNetwork.myListService.getOnlineWatchHistories(userId, sort.value, page)
        },
        action = Parser::onlineWatchHistoryItems,
    )

    fun getUserAccountPage(userId: String) = websiteIOFlow(
        request = { HanimeNetwork.myListService.getUserAccountPage(userId) },
        action = Parser::userAccountPage,
    )

    fun getUploadedVideos(
        userId: String,
        sort: CreatorSort,
        page: Int,
    ) = pageIOFlow(
        request = {
            HanimeNetwork.myListService.getUploadedVideos(userId, sort.value, page)
        },
        action = Parser::creatorUploadedItems,
    )

    fun getUploadingVideos(
        userId: String,
        sort: CreatorSort,
        page: Int,
    ) = pageIOFlow(
        request = {
            HanimeNetwork.myListService.getUploadingVideos(userId, sort.value, page)
        },
        action = Parser::creatorUploadingItems,
    )

    fun updateUserAccountProfile(
        userId: String,
        csrfToken: String?,
        name: String,
        email: String,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.updateUserAccountProfile(
                userId = userId,
                csrfToken = csrfToken,
                name = name,
                email = email,
            )
        },
        permittedSuccessCode = intArrayOf(302),
    ) {
        if (it.isBlank()) {
            WebsiteState.Success(Unit)
        } else {
            when (val result = Parser.userAccountPage(it)) {
                is WebsiteState.Error -> WebsiteState.Error(result.throwable)
                else -> WebsiteState.Success(Unit)
            }
        }
    }

    fun updateUserAccountPassword(
        userId: String,
        csrfToken: String?,
        oldPassword: String,
        newPassword: String,
        newPasswordConfirm: String,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.updateUserAccountPassword(
                userId = userId,
                csrfToken = csrfToken,
                oldPassword = oldPassword,
                newPassword = newPassword,
                newPasswordConfirm = newPasswordConfirm,
            )
        },
        permittedSuccessCode = intArrayOf(302),
    ) {
        if (it.isBlank()) {
            WebsiteState.Success(Unit)
        } else {
            when (val result = Parser.userAccountPage(it)) {
                is WebsiteState.Error -> WebsiteState.Error(result.throwable)
                else -> WebsiteState.Success(Unit)
            }
        }
    }

    fun updateUserAccountAvatar(
        userId: String,
        csrfToken: String?,
        photoBytes: ByteArray,
        fileName: String,
    ) = websiteIOFlow(
        request = {
            val body = MultiPartFormDataContent(
                formData {
                    append("_token", csrfToken ?: EMPTY_STRING)
                    append("_method", "patch")
                    append("type", "photo")
                    append(
                        key = "photo",
                        value = photoBytes,
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(
                                HttpHeaders.ContentDisposition,
                                "filename=\"$fileName\"",
                            )
                        },
                    )
                }
            )
            HanimeNetwork.myListService.updateUserAccountAvatar(userId = userId, body = body)
        },
        permittedSuccessCode = intArrayOf(302),
    ) {
        if (it.isBlank()) {
            WebsiteState.Success(Unit)
        } else {
            when (val result = Parser.userAccountPage(it)) {
                is WebsiteState.Error -> WebsiteState.Error(result.throwable)
                else -> WebsiteState.Success(Unit)
            }
        }
    }

    fun deleteOnlineWatchHistory(
        videoCode: String,
        position: Int,
        csrfToken: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.deleteOnlineWatchHistory(
                videoCode = videoCode,
                body = FormDataContent(Parameters.build { append("tab", "histories") }),
                csrfToken = csrfToken,
            )
        },
    ) {
        val jsonObject = HJson.parseToJsonElement(it).jsonObject
        val success = (jsonObject["success"] as? JsonPrimitive)?.booleanOrNull ?: false
        if (success) {
            WebsiteState.Success(position)
        } else {
            WebsiteState.Error(IllegalStateException("cannot delete it ?!"))
        }
    }

    fun deleteMyListItems(
        typeOrCode: Any,
        videoCode: String,
        position: Int,
        token: String?,
    ) = websiteIOFlow(
        request = {
            when (typeOrCode) {
                is String ->
                    HanimeNetwork.myListService.deleteMyListItems(
                        typeOrCode, videoCode,
                        csrfToken = token
                    )

                is MyListType ->
                    HanimeNetwork.myListService.deleteMyListItems(
                        typeOrCode.value, videoCode,
                        csrfToken = token
                    )

                else ->
                    throw IllegalArgumentException("typeOrId must be String or MyListType")
            }
        }
    ) { deleteBody ->
        val jsonObject = HJson.parseToJsonElement(deleteBody).jsonObject
        // ⚠️ 不是 toString()：JsonPrimitive.toString() 会带引号
        val returnVideoCode = (jsonObject["video_id"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        if (videoCode == returnVideoCode) {
            return@websiteIOFlow WebsiteState.Success(position)
        }

        return@websiteIOFlow WebsiteState.Error(IllegalStateException("cannot delete it ?!"))
    }

    fun getPlaylists(page: Int, userId: String ) = websiteIOFlow(
        request = { HanimeNetwork.myListService.getPlaylists(userId, page) },
        action = Parser::playlists
    )

    fun addToMyFavVideo(
        videoCode: String,
        likeStatus: Boolean, // false => "": add fav; true => "1": cancel fav;
        currentUserId: String?,
        token: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.addToMyFavVideo(
                videoCode, if (likeStatus) "1" else EMPTY_STRING,
                token, currentUserId
            )
        }
    ) {
        Logger.d(tag = "add_to_fav_body") { it }
        return@websiteIOFlow WebsiteState.Success(likeStatus)
    }

    fun rateVideo(
        videoCode: String,
        isPositive: Boolean,
        likeStatus: Boolean,
        unlikeStatus: Boolean,
        likesCount: Int,
        unlikesCount: Int,
        currentUserId: String?,
        token: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.rateVideo(
                videoCode = videoCode,
                isPositive = if (isPositive) 1 else 0,
                likeStatus = if (likeStatus) "1" else EMPTY_STRING,
                unlikeStatus = if (unlikeStatus) "1" else EMPTY_STRING,
                likesCount = likesCount,
                unlikesCount = unlikesCount,
                csrfToken = token,
                userId = currentUserId,
            )
        }
    ) {
        Logger.d(tag = "rate_video_body") { it }
        return@websiteIOFlow WebsiteState.Success(isPositive)
    }

    fun createPlaylist(
        videoCode: String,
        title: String,
        description: String,
        csrfToken: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.createPlaylist(
                csrfToken, videoCode, title, description
            )
        },
        permittedSuccessCode = intArrayOf(500)
    ) {
        Logger.d(tag = "create_playlist_body") { it }
        return@websiteIOFlow WebsiteState.Success(Unit)
    }

    fun addToMyList(
        listCode: String,
        videoCode: String,
        isChecked: Boolean,
        position: Int,
        csrfToken: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.addToMyList(
                csrfToken, listCode, videoCode, isChecked
            )
        }
    ) {
        Logger.d(tag = "add_to_playlist_body") { it }
        return@websiteIOFlow WebsiteState.Success(position)
    }

    fun modifyPlaylist(
        listCode: String,
        title: String,
        description: String,
        delete: Boolean,
        csrfToken: String?,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.myListService.modifyPlaylist(
                listCode, title, description,
                if (delete) "on" else null,
                csrfToken
            )
        },
        permittedSuccessCode = intArrayOf(302)
    ) {
        Logger.d(tag = "modify_playlist_body") { it }
        return@websiteIOFlow WebsiteState.Success(
            ModifiedPlaylistArgs(
                title = title, desc = description, isDeleted = delete,
            )
        )
    }

    //</editor-fold>

    //<editor-fold desc="Comment">

    fun getComments(type: String, code: String) = websiteIOFlow(
        request = { HanimeNetwork.commentService.getComments(type, code) },
        action = Parser::comments
    )

    fun getCommentReply(commentId: String) = websiteIOFlow(
        request = { HanimeNetwork.commentService.getCommentReply(commentId) },
        action = Parser::commentReply
    )

    fun postComment(
        csrfToken: String?,
        currentUserId: String,
        targetUserId: String,
        type: String,
        text: String,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.commentService.postComment(
                csrfToken, currentUserId,
                type, targetUserId, text
            )
        }
    ) {
        Logger.d(tag = "post_comment_body") { it }
        return@websiteIOFlow WebsiteState.Success(Unit)
    }

    fun postCommentReply(
        csrfToken: String?,
        replyCommentId: String,
        text: String,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.commentService.postCommentReply(
                csrfToken, replyCommentId, text
            )
        }
    ) {
        Logger.d(tag = "post_comment_reply_body") { it }
        return@websiteIOFlow WebsiteState.Success(Unit)
    }

    fun likeComment(
        csrfToken: String?,
        commentPlace: CommentPlace,
        foreignId: String?,
        isPositive: Boolean, // 你選擇的是讚還是踩，1是讚，0是踩
        likeUserId: String?,
        commentLikesCount: Int,
        commentLikesSum: Int,
        likeCommentStatus: Boolean, // 你之前有沒有點過讚，1是0否
        unlikeCommentStatus: Boolean, // 你之前有沒有點過踩，1是0否
        commentPosition: Int, comment: VideoComments.VideoComment,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.commentService.likeComment(
                csrfToken, commentPlace.value, foreignId,
                if (isPositive) 1 else 0,
                likeUserId, commentLikesCount, commentLikesSum,
                if (likeCommentStatus) 1 else 0,
                if (unlikeCommentStatus) 1 else 0
            )
        }
    ) {
        Logger.d(tag = "like_comment_body") { it }
        return@websiteIOFlow WebsiteState.Success(
            VideoCommentArgs(
                commentPosition, isPositive, comment
            )
        )
    }

    fun reportComment(
        csrfToken: String?,
        reason: String,
        currentUserId: String?,
        redirectUrl: String,
        reportableType: String?,
        reportableId: String?
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.commentService.submitReport(
                // Ktorfit 不允许 @Path 可空，判空提到这里。
                userId = requireNotNull(currentUserId) { "currentUserId is null" },
                csrfToken = csrfToken,
                redirectUrl = redirectUrl,
                reportableId = reportableId,
                reportableType = reportableType,
                reason = reason
            )
        },
        action = Parser::reportCommentResponse
    )

    //</editor-fold>

    //<editor-fold desc="Subscription">

    fun subscribeArtist(
        csrfToken: String?,
        userId: String,
        artistId: String,
        // 这里表示目标状态
        status: Boolean,
    ) = websiteIOFlow(
        request = {
            HanimeNetwork.subscriptionService.subscribeArtist(
                csrfToken, userId, artistId,
                if (status) "" else "1"
            )
        }
    ) {
        Logger.d(tag = "subscribe_artist_body") { it }
        return@websiteIOFlow WebsiteState.Success(status)
    }

    //</editor-fold>

    //<editor-fold desc="Base">

    fun login(email: String, password: String) = flow {
        emit(WebsiteState.Loading)
        // 首先获取token
        val loginPage = HanimeNetwork.hanimeService.getLoginPage()
        val token = Parser.extractTokenFromLoginPage(loginPage.bodyAsText())
        val req = HanimeNetwork.hanimeService.login(token, email, password)
        if (req.status.isSuccess()) {
            // 再次获取登录页面，如果失败则返回 cookie
            // 因为登录成功再次访问 login 会 404，这是判断是否登录成功的方法
            val loginPageAgain = HanimeNetwork.hanimeService.getLoginPage()
            if (loginPageAgain.status.value == 404) {
                // Cookie 會返回 XSRF-TOKEN 和 hanime1_session，我們只需要後者
                // 错误的，还需要 remember_web 字段！但我没找到！
                Logger.d(tag = "login_headers") { req.headers.entries().toString() }
                emit(WebsiteState.Success(req.headers.getAll(HttpHeaders.SetCookie).orEmpty()))
            } else {
                emit(WebsiteState.Error(LocalizedStateException(Res.string.account_or_password_wrong)))
            }
        } else {
            // 雙重保險
            emit(WebsiteState.Error(LocalizedStateException(Res.string.account_or_password_wrong)))
        }
    }.catch { e ->
        emit(WebsiteState.Error(handleException(e)))
    }.flowOn(Dispatchers.IO)

    /**
     * 用于单网页的情况
     *
     * @param permittedSuccessCode 用于处理特殊情况，比如[NetworkRepo.modifyPlaylist]需要302成功
     */
    private fun <T> websiteIOFlow(
        request: suspend () -> HttpResponse,
        permittedSuccessCode: IntArray? = null,
        action: (String) -> WebsiteState<T>,
    ) = flow {
        val requestResult = request.invoke()
        val resultBody = requestResult.bodyAsText()
        val permitted = permittedSuccessCode?.contains(requestResult.status.value) == true
        if ((permitted || requestResult.status.isSuccess())) {
            emit(action.invoke(resultBody))
        } else {
            requestResult.throwRequestException()
        }
    }.catch { e ->
        emit(WebsiteState.Error(handleException(e)))
    }.flowOn(Dispatchers.IO)

    /**
     * 用于有page分页的情况
     */
    private fun <T> pageIOFlow(
        request: suspend () -> HttpResponse,
        action: (String) -> PageLoadingState<T>,
    ) = flow {
        val requestResult = request.invoke()
        if (requestResult.status.isSuccess()) {
            emit(action.invoke(requestResult.bodyAsText()))
        } else {
            requestResult.throwRequestException()
        }
    }.catch { e ->
        emit(PageLoadingState.Error(handleException(e)))
    }.flowOn(Dispatchers.IO)

    /**
     * 用于影片界面
     */
    private fun <T> videoIOFlow(
        request: suspend () -> HttpResponse,
        action: (String) -> VideoLoadingState<T>,
    ) = flow {
        val requestResult = request.invoke()
        if (requestResult.status.isSuccess()) {
            emit(action.invoke(requestResult.bodyAsText()))
        } else {
            requestResult.throwRequestException()
        }
    }.catch { e ->
        emit(VideoLoadingState.Error(handleException(e)))
    }.flowOn(Dispatchers.IO)

    internal suspend fun HttpResponse.throwRequestException(): Nothing {
        val body = bodyAsText()
        when (val code = status.value) {
            403 -> if (body.isNotBlank()) {
                when {
                    "you have been blocked" in body ->
                        throw IPBlockedException(Res.string.cloudflare_ip_block_warning)

                    "Just a moment" in body ->
                        throw CloudFlareBlockedException(Res.string.cloudflare_network_mismatch)

                    else ->
                        throw HanimeNotFoundException(Res.string.video_might_not_exist) // 主要出現在影片界面，當你v數不大時會報403
                }
            } else throw IllegalStateException("$code ${status.description}")

            500 -> throw HanimeNotFoundException(Res.string.video_might_not_exist) // 主要出現在影片界面，當你v數很大時會報500

            404 -> if (!isAlreadyLogin) {
                throw LocalizedStateException(Res.string.not_logged_in_currently)
            } else {
                throw IllegalStateException("$code ${status.description}")
            }

            else -> throw IllegalStateException("$code ${status.description}")
        }
    }

    internal fun handleException(e: Throwable): Throwable {
        return when {
            e is CancellationException -> throw e
            e is ParseException -> {
                e.printStackTrace()
                ParseException("parse failed")
            }

            // SSLHandshakeException 是 JVM 专属类型，改用 expect/actual 判别，
            // iOS 那边对应的是 NSURLErrorDomain 下的一组 TLS 错误码。
            e.isSslHandshakeError() -> {
                e.printStackTrace()
                LocalizedStateException(Res.string.ssl_handshake_error, "ssl handshake failed")
            }

            else -> {
                e.printStackTrace()
                e
            }
        }
    }

    //</editor-fold>
}
