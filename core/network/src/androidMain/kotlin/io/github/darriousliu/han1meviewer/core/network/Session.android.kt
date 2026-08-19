package io.github.darriousliu.han1meviewer.core.network

import android.webkit.CookieManager

actual fun removeAllCookies() {
    CookieManager.getInstance().removeAllCookies(null)
}