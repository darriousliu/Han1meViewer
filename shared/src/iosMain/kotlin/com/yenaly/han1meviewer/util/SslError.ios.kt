package com.yenaly.han1meviewer.util

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import platform.Foundation.NSURLErrorClientCertificateRejected
import platform.Foundation.NSURLErrorClientCertificateRequired
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorSecureConnectionFailed
import platform.Foundation.NSURLErrorServerCertificateHasBadDate
import platform.Foundation.NSURLErrorServerCertificateHasUnknownRoot
import platform.Foundation.NSURLErrorServerCertificateNotYetValid
import platform.Foundation.NSURLErrorServerCertificateUntrusted

/**
 * Darwin engine 抛的是 [DarwinHttpRequestException]，里面包着 `NSError`，
 * 没有 JVM 那种 `SSLHandshakeException` 明确类型，只能按 `NSURLErrorDomain` 下的
 * TLS 相关 code 判。
 */
private val tlsErrorCodes = setOf(
    NSURLErrorSecureConnectionFailed,
    NSURLErrorServerCertificateHasBadDate,
    NSURLErrorServerCertificateUntrusted,
    NSURLErrorServerCertificateHasUnknownRoot,
    NSURLErrorServerCertificateNotYetValid,
    NSURLErrorClientCertificateRejected,
    NSURLErrorClientCertificateRequired,
)

actual fun Throwable.isSslHandshakeError(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        val origin = (current as? DarwinHttpRequestException)?.origin
        if (origin != null && origin.domain == NSURLErrorDomain && origin.code in tlsErrorCodes) {
            return true
        }
        val next = current.cause
        current = if (next === current) null else next
    }
    return false
}
