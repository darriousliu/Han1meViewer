package io.github.darriousliu.han1meviewer.core.common.util

import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

/**
 * 分支顺序不能改：`ConnectException` 是 `SocketException` 的子类，
 * 换了顺序连接失败会被误判成连接中断。
 */
actual fun Throwable.networkExceptionKind(): NetworkExceptionKind = when (this) {
    is UnknownHostException -> NetworkExceptionKind.UNKNOWN_HOST
    is SocketTimeoutException -> NetworkExceptionKind.TIMEOUT
    is SSLHandshakeException -> NetworkExceptionKind.SSL_HANDSHAKE
    is ConnectException -> NetworkExceptionKind.CONNECT
    is SocketException -> NetworkExceptionKind.SOCKET
    else -> NetworkExceptionKind.OTHER
}
