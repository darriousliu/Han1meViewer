package io.github.darriousliu.han1meviewer

import platform.Foundation.NSHTTPCookie
import platform.Foundation.NSHTTPCookieStorage
import platform.WebKit.WKWebsiteDataStore

actual fun removeAllCookies() {
    val sharedCookieStorage = NSHTTPCookieStorage.sharedHTTPCookieStorage
    sharedCookieStorage.cookies
        .orEmpty()
        .filterIsInstance<NSHTTPCookie>()
        .forEach(sharedCookieStorage::deleteCookie)

    val webCookieStore = WKWebsiteDataStore.defaultDataStore().httpCookieStore
    webCookieStore.getAllCookies { cookies ->
        cookies
            .orEmpty()
            .filterIsInstance<NSHTTPCookie>()
            .forEach { cookie ->
                webCookieStore.deleteCookie(cookie, completionHandler = null)
            }
    }
}
