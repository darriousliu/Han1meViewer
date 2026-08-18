package io.github.darriousliu.han1meviewer.ui.screen.home.homepage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.repository.DatabaseRepo
import io.github.darriousliu.han1meviewer.core.repository.NetworkRepo
import io.github.darriousliu.han1meviewer.core.storage.entity.HKeyframeEntity
import io.github.darriousliu.han1meviewer.core.storage.entity.WatchHistoryEntity
import io.github.darriousliu.han1meviewer.core.common.exception.LoginStateExpiredException
import io.github.darriousliu.han1meviewer.core.model.Announcement
import io.github.darriousliu.han1meviewer.core.network.fetchPlatformAnnouncements
import io.github.darriousliu.han1meviewer.core.common.state.PageState
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.logout
import io.github.darriousliu.han1meviewer.ui.viewmodel.CsrfTokenStore
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.login_state_expired
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.StringResource

private val logger = Logger.withTag("HomePageViewModel")

class HomePageViewModel: ViewModel() {
    data class SessionExpiredMessage(
        val message: String?,
        val fallbackRes: StringResource,
    )

    private val _homePageFlow = MutableStateFlow<PageState<HomeData>>(PageState.Loading)
    val homePageFlow = _homePageFlow.asStateFlow()

    private val _sessionExpiredMessage = MutableSharedFlow<SessionExpiredMessage>()
    val sessionExpiredMessage = _sessionExpiredMessage

    private var homePageJob: Job? = null

    init {
        viewModelScope.launch {
            // 初始化默认已下载分组，防止[FOREIGN KEY constraint failed]
            DatabaseRepo.HanimeDownload.insertDefaultGroup()
        }
    }

    fun getHomePage(isRefresh: Boolean = false){
        homePageJob?.cancel()
        homePageJob = viewModelScope.launch {
            val current = _homePageFlow.value
            // cachedInfo 是 T?，`!= null` 之后并不会把 T? 收窄成 T（T 自己就可能是可空
            // 类型），跨模块之后编译器不再替我们做这一步，所以先取成局部变量。
            val cachedInfo = (current as? PageState.Error)?.cachedInfo
            if (isRefresh && current is PageState.Success) {
                _homePageFlow.value = current.copy(isRefreshing = true)
            } else if (isRefresh && cachedInfo != null) {
                _homePageFlow.value = PageState.Success(info = cachedInfo, isRefreshing = true)
            } else if (!isRefresh && current !is PageState.Success){
                _homePageFlow.value = PageState.Loading
            }
            val announcementsDeferred = async(Dispatchers.IO) {
                withTimeoutOrNull(ANNOUNCEMENTS_TIMEOUT_MILLIS.milliseconds) {
                    fetchAnnouncements()
                }.orEmpty()
            }
            NetworkRepo.getHomePage().collect { networkState ->
                when (networkState){
                    is WebsiteState.Error -> {
                        announcementsDeferred.cancel()
                        if (networkState.throwable is LoginStateExpiredException) {
                            logout()
                            _sessionExpiredMessage.emit(
                                SessionExpiredMessage(
                                    message = networkState.throwable.message,
                                    fallbackRes = Res.string.login_state_expired,
                                )
                            )
                        }
                        val previousData = (_homePageFlow.value as? PageState.Success)?.info
                        _homePageFlow.value = PageState.Error(networkState.throwable, cachedInfo = previousData)
                    }
                    is WebsiteState.Success -> {
                        val currentAnnouncements = announcementsDeferred.await()
                        CsrfTokenStore.csrfToken = networkState.info.csrfToken
                        networkState.info.userId.takeIf { it.isNotEmpty() }?.let { userId ->
                            Preferences.savedUserId = userId
                        }
                        val homeData = HomeData(page = networkState.info, announcements = currentAnnouncements)
                        _homePageFlow.value = PageState.Success(info = homeData, isRefreshing = false)
                    }
                    is WebsiteState.Loading -> { }
                }
            }
        }
    }
    /**
     * 「关掉公告后 24 小时内不再弹」这条策略留在 common，平台侧只负责把公告取回来。
     * 取不到（平台没有来源 / 抛异常）一律当空列表——公告不该阻塞首页。
     */
    private suspend fun fetchAnnouncements(): List<Announcement> {
        val lastDismissTime = Preferences.lastDismissTime
        val now = Clock.System.now().toEpochMilliseconds()
        if (now - lastDismissTime <= 24 * 60 * 60 * 1000L) return emptyList()

        return runCatching { fetchPlatformAnnouncements() }
            .onFailure { logger.e(it) { "公告读取失败" } }
            .getOrDefault(emptyList())
            .filter { it.isActive }
            .sortedBy { it.priority }
    }

    private companion object {
        const val ANNOUNCEMENTS_TIMEOUT_MILLIS = 5_000L
    }

    fun dismissAnnouncements(){
        Preferences.lastDismissTime = Clock.System.now().toEpochMilliseconds()
        val current = _homePageFlow.value
        if (current is PageState.Success) {
            _homePageFlow.value = current.copy(info = current.info.copy(announcements = emptyList()))
        }
    }

    fun deleteWatchHistory(history: WatchHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.WatchHistory.delete(history)
            logger.d { "$history DONE!" }
        }
    }

    fun deleteAllWatchHistories() {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.WatchHistory.deleteAll()
            logger.d { "delete all watch history DONE!" }
        }
    }

    fun loadAllWatchHistories() =
        DatabaseRepo.WatchHistory.loadAll()
            .catch { e -> e.printStackTrace() }
            .flowOn(Dispatchers.IO)
    private val _modifyHKeyframeFlow = MutableSharedFlow<Boolean>()
    fun removeHKeyframe(videoCode: String, hKeyframe: HKeyframeEntity.Keyframe) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.removeKeyframe(videoCode, hKeyframe)
            logger.d { "removeHKeyframe:$hKeyframe DONE!" }
            _modifyHKeyframeFlow.emit(true)
        }
    }
    fun modifyHKeyframe(
        videoCode: String,
        oldKeyframe: HKeyframeEntity.Keyframe, keyframe: HKeyframeEntity.Keyframe,
    ) {
        viewModelScope.launch {
            DatabaseRepo.HKeyframe.modifyKeyframe(videoCode, oldKeyframe, keyframe)
            logger.d { "modifyHKeyframe:$keyframe DONE!" }
            _modifyHKeyframeFlow.emit(true)
        }
    }
    fun deleteHKeyframes(entity: HKeyframeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.delete(entity)
        }
    }

    fun updateHKeyframes(entity: HKeyframeEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            DatabaseRepo.HKeyframe.update(entity)
        }
    }
}
