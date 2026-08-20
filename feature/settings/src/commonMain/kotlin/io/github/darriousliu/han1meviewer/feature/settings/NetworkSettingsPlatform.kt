package io.github.darriousliu.han1meviewer.feature.settings

/**
 * 网络设置的平台能力。这些能力全部建立在 okhttp 侧的钩子上
 * (自定义 DNS/DoH/ProxySelector),iOS 的 NSURLSession 没有对应物,
 * 对应设置项直接不展示,避免「显示但静默无效」。
 */
data class NetworkSettingsCapabilities(
    /** 代理设置(HProxySelector)。 */
    val proxy: Boolean,
    /** 内置/自定义 hosts(HDns)。 */
    val hosts: Boolean,
    /** DoH 设置与 DoH 测试。 */
    val doh: Boolean,
    /** 节点延迟测试(InetAddress.isReachable)。 */
    val latencyTest: Boolean,
)

expect val networkSettingsCapabilities: NetworkSettingsCapabilities

/**
 * 网络诊断与 DNS 操作。入口已被 [networkSettingsCapabilities] 门控,
 * 全默认实现的平台(iOS)不会被调用到,默认值只是兜底。
 */
interface NetworkDiagnostics {
    /** 仅走 DoH 解析 [host],返回 IP 文本;失败抛异常(上层展示 message)。 */
    suspend fun lookupByDoHOnly(host: String): List<String> = emptyList()

    /** [host] 的内置 CDN 节点列表。 */
    suspend fun cdnList(host: String): List<String> = emptyList()

    /** ping [ip],返回毫秒;不可达/失败返回 -1。 */
    suspend fun measureDelay(ip: String): Int = -1

    /** 校验自定义 hosts 文本,返回错误行描述;空列表=合法。 */
    fun validateCustomHosts(raw: String): List<String> = emptyList()

    /** 代理配置落库后重建全局 ProxySelector(WebView 也吃这份,#issue-39)。 */
    fun rebuildProxySelector() {}
}

expect val networkDiagnostics: NetworkDiagnostics
