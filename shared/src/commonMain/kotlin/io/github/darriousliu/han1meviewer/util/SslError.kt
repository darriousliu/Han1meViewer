package io.github.darriousliu.han1meviewer.util

/**
 * 是不是 TLS 握手失败。
 *
 * JVM 上有 `javax.net.ssl.SSLHandshakeException` 这个明确类型，Kotlin/Native 上没有——
 * Darwin engine 抛出来的是包着 `NSError` 的异常，只能看 domain/code。
 * 数据层要据此换成一句友好的提示（见 `NetworkRepo.handleException`），所以做成 expect/actual。
 */
expect fun Throwable.isSslHandshakeError(): Boolean
