package com.yenaly.han1meviewer.util

import javax.net.ssl.SSLHandshakeException

actual fun Throwable.isSslHandshakeError(): Boolean =
    generateSequence(this) { it.cause?.takeIf { cause -> cause !== it } }
        .any { it is SSLHandshakeException }
