package com.yenaly.han1meviewer.util

/**
 * Darwin engine 不抛 `java.net` 那套异常，能靠类型判出来的只有 TLS 握手失败
 * （[isSslHandshakeError] 里按 `NSURLErrorDomain` 的 code 判）。
 * 其余情况回落到 OTHER，由 [toNetworkErrorMessage] 的 message 关键字匹配兜底。
 */
actual fun Throwable.networkExceptionKind(): NetworkExceptionKind =
    if (isSslHandshakeError()) NetworkExceptionKind.SSL_HANDSHAKE else NetworkExceptionKind.OTHER
