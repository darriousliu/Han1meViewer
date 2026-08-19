package io.github.darriousliu.han1meviewer.feature.home

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 首页会话信息的进程级共享点。
 *
 * [HomePageViewModel] 是 NavEntry 作用域(跟随首页 entry),而抽屉头部与宿主
 * (登出确认框)活在导航图之外——两边共享的头像/用户名/会话过期提示/刷新请求
 * 经这里流转,谁也不用拿到对方的 ViewModel 实例。
 *
 * 导航图**之内**的刷新(登录成功/改头像)仍走 ResultEventBus;这里的
 * [refreshRequests] 只服务导航图之外的写入方(宿主登出)。
 */
object HomeSessionStore {

    /** 抽屉头部要展示的首页解析结果切片。是否登录由 `Preferences.loginStateFlow` 单独判断。 */
    data class Header(
        val avatarUrl: String? = null,
        val username: String? = null,
        val isLoading: Boolean = false,
    )

    private val _header = MutableStateFlow(Header())
    val header: StateFlow<Header> = _header.asStateFlow()

    private val _sessionExpired =
        MutableSharedFlow<HomePageViewModel.SessionExpiredMessage>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<HomePageViewModel.SessionExpiredMessage> =
        _sessionExpired.asSharedFlow()

    private val _refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshRequests: SharedFlow<Unit> = _refreshRequests.asSharedFlow()

    fun updateHeader(header: Header) {
        _header.value = header
    }

    fun emitSessionExpired(message: HomePageViewModel.SessionExpiredMessage) {
        _sessionExpired.tryEmit(message)
    }

    /** 宿主侧(登出等)请求首页刷新;首页 VM 在世期间收到即重拉。 */
    fun requestRefresh() {
        _refreshRequests.tryEmit(Unit)
    }
}
