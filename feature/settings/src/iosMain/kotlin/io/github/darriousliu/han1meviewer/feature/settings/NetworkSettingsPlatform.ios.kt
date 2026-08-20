package io.github.darriousliu.han1meviewer.feature.settings

// NSURLSession 没有自定义 DNS/DoH/ProxySelector 的钩子,相关设置项整体不展示。
actual val networkSettingsCapabilities = NetworkSettingsCapabilities(
    proxy = false,
    hosts = false,
    doh = false,
    latencyTest = false,
)

actual val networkDiagnostics: NetworkDiagnostics = object : NetworkDiagnostics {}
