package io.github.darriousliu.han1meviewer.core.network

import java.net.CookieHandler
import java.net.CookieManager

actual fun removeAllCookies() {
    (CookieHandler.getDefault() as? CookieManager)?.cookieStore?.removeAll()
}
