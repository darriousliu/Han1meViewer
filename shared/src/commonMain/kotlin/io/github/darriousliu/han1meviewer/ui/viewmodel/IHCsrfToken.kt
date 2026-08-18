package io.github.darriousliu.han1meviewer.ui.viewmodel

interface IHCsrfToken {
    var csrfToken: String?
}

/**
 * 当前登录会话的 CSRF token。
 *
 * token 由首页或影片页刷新；独立于 Android 的 [AppViewModel] 后，commonMain 的
 * ViewModel 不需要再反向依赖平台层。
 */
object CsrfTokenStore : IHCsrfToken {
    override var csrfToken: String? = null
}
