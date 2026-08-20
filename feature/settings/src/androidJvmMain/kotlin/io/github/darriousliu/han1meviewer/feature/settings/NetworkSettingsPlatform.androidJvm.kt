package io.github.darriousliu.han1meviewer.feature.settings

import io.github.darriousliu.han1meviewer.core.network.HDns
import io.github.darriousliu.han1meviewer.core.network.HProxySelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

actual val networkSettingsCapabilities = NetworkSettingsCapabilities(
    proxy = true,
    hosts = true,
    doh = true,
    latencyTest = true,
)

actual val networkDiagnostics: NetworkDiagnostics = object : NetworkDiagnostics {

    override suspend fun lookupByDoHOnly(host: String): List<String> =
        withContext(Dispatchers.IO) {
            HDns().lookupByDoHOnly(host).mapNotNull { it.hostAddress }.distinct()
        }

    override suspend fun cdnList(host: String): List<String> =
        withContext(Dispatchers.IO) { HDns().getCDNList(host) }

    override suspend fun measureDelay(ip: String): Int = withContext(Dispatchers.IO) {
        try {
            val start = System.currentTimeMillis()
            val reachable = InetAddress.getByName(ip).isReachable(2000)
            if (reachable) (System.currentTimeMillis() - start).toInt() else -1
        } catch (_: Exception) {
            -1
        }
    }

    override fun validateCustomHosts(raw: String): List<String> =
        HDns.validateCustomHosts(raw)

    override fun rebuildProxySelector() = HProxySelector.rebuildNetwork()
}
