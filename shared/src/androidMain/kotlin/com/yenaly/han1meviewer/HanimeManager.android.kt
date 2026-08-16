package com.yenaly.han1meviewer

import android.webkit.CookieManager

actual fun removeAllCookies() {
    CookieManager.getInstance().removeAllCookies(null)
}