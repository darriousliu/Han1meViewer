package io.github.darriousliu.han1meviewer.util

import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.home_error_connect
import io.github.darriousliu.han1meviewer.core.resource.home_error_connection_interrupted
import io.github.darriousliu.han1meviewer.core.resource.home_error_connection_reset
import io.github.darriousliu.han1meviewer.core.resource.home_error_dns
import io.github.darriousliu.han1meviewer.core.resource.home_error_forbidden
import io.github.darriousliu.han1meviewer.core.resource.home_error_generic
import io.github.darriousliu.han1meviewer.core.resource.home_error_not_found
import io.github.darriousliu.han1meviewer.core.resource.home_error_server_unavailable
import io.github.darriousliu.han1meviewer.core.resource.home_error_ssl
import io.github.darriousliu.han1meviewer.core.resource.home_error_timeout
import org.jetbrains.compose.resources.StringResource

/**
 * 网络异常的粗分类。
 *
 * JVM 上能靠 `java.net` 的具体异常类型判断，Kotlin/Native 上没有这些类型，
 * 所以把「看类型」这一步做成 expect/actual，「看 message 关键字」那一步留在 common。
 * 和 [isSslHandshakeError] 是同一个思路。
 */
enum class NetworkExceptionKind {
    UNKNOWN_HOST,
    TIMEOUT,
    SSL_HANDSHAKE,
    CONNECT,
    SOCKET,
    OTHER,
}

/**
 * 判定顺序要和 [toNetworkErrorMessage] 的分支顺序对齐：
 * JVM 上 `ConnectException` 是 `SocketException` 的子类，必须先判前者。
 */
expect fun Throwable.networkExceptionKind(): NetworkExceptionKind

/**
 * 把首页加载异常映射成给用户看的提示文案。
 *
 * 优先按异常类型判断常见网络问题，判不出来再回退到 message 里的关键字匹配。
 */
fun Throwable.toNetworkErrorMessage(): StringResource {
    val rawMessage = message.orEmpty().lowercase()
    val kind = networkExceptionKind()
    return when {
        kind == NetworkExceptionKind.UNKNOWN_HOST ||
                rawMessage.contains("unable to resolve host") ||
                rawMessage.contains("no address associated with hostname") -> {
            Res.string.home_error_dns
        }

        kind == NetworkExceptionKind.TIMEOUT || rawMessage.contains("timeout") -> {
            Res.string.home_error_timeout
        }

        kind == NetworkExceptionKind.SSL_HANDSHAKE ||
                rawMessage.contains("ssl") ||
                rawMessage.contains("certificate") -> {
            Res.string.home_error_ssl
        }

        kind == NetworkExceptionKind.CONNECT || rawMessage.contains("failed to connect") -> {
            Res.string.home_error_connect
        }

        kind == NetworkExceptionKind.SOCKET && rawMessage.contains("connection reset") -> {
            Res.string.home_error_connection_interrupted
        }

        rawMessage.contains("connection reset") -> {
            Res.string.home_error_connection_reset
        }

        rawMessage.contains("403") -> {
            Res.string.home_error_forbidden
        }

        rawMessage.contains("404") -> {
            Res.string.home_error_not_found
        }

        rawMessage.contains("500") || rawMessage.contains("502") ||
                rawMessage.contains("503") || rawMessage.contains("504") -> {
            Res.string.home_error_server_unavailable
        }

        else -> {
            Res.string.home_error_generic
        }
    }
}
