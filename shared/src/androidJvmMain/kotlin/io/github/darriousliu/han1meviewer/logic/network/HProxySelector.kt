package io.github.darriousliu.han1meviewer.logic.network

import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.common.ProxyType
import okhttp3.internal.proxy.NullProxySelector
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

/**
 * 受 [EhViewer_CN_SXJ 中 EhProxySelector](https://github.com/xiaojieonly/Ehviewer_CN_SXJ/blob/BiLi_PC_Gamer/app/src/main/java/com/hippo/ehviewer/EhProxySelector.java)
 * 的启发，Han1meViewer 也将使用 [HProxySelector] 来实现代理功能。
 *
 * @project Han1meViewer
 * @author Yenaly Liew
 * @time 2023/10/07 007 17:32
 */
// #issue-15: 添加系统代理功能
class HProxySelector : ProxySelector() {

    private var delegation: ProxySelector? = null
    private val alternative: ProxySelector = getDefault() ?: NullProxySelector

    init {
        updateProxy()
    }

    companion object {
        // 真正的定义已抽到 commonMain 的 ProxyType（Preferences 上移 commonMain 需要），
        // 这里保留别名，现有调用点不用改。
        const val TYPE_DIRECT = ProxyType.DIRECT
        const val TYPE_SYSTEM = ProxyType.SYSTEM
        const val TYPE_HTTP = ProxyType.HTTP
        const val TYPE_SOCKS = ProxyType.SOCKS

        private val ipv4Regex =
            Regex("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")

        fun validateIp(ip: String): Boolean {
            return ipv4Regex.matches(ip)
        }

        fun validatePort(port: Int): Boolean {
            return port in 0..65535
        }

        // #issue-39: 代理沒有應用到 WebView 上，只能通過此種方式來全局代理。
        fun rebuildNetwork() {
            val properties = System.getProperties()
            when (Preferences.proxyType) {
                TYPE_HTTP, TYPE_SOCKS -> {
                    properties["proxySet"] = true.toString()
                    properties["proxyHost"] = Preferences.proxyIp
                    properties["proxyPort"] = Preferences.proxyPort.toString()
                }

                else -> {
                    properties["proxySet"] = false.toString()
                    properties["proxyHost"] = ""
                    properties["proxyPort"] = ""
                }
            }
        }
    }

    private fun updateProxy() {
        delegation = when (Preferences.proxyType) {
            TYPE_DIRECT -> NullProxySelector
            TYPE_SYSTEM -> alternative
            TYPE_HTTP, TYPE_SOCKS -> null
            else -> NullProxySelector
        }
    }

    override fun select(uri: URI?): MutableList<Proxy> {
        val type = Preferences.proxyType
        if (type == TYPE_HTTP || type == TYPE_SOCKS) {
            val ip = Preferences.proxyIp
            val port = Preferences.proxyPort
            if (ip.isNotBlank() && port != -1) {
                val inetAddress = InetAddress.getByName(ip)
                val socketAddress = InetSocketAddress(inetAddress, port)
                return mutableListOf(
                    Proxy(
                        if (type == TYPE_HTTP) Proxy.Type.HTTP else Proxy.Type.SOCKS,
                        socketAddress
                    )
                )
            }
        }

        return delegation?.select(uri) ?: alternative.select(uri)
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        delegation?.select(uri)
    }
}