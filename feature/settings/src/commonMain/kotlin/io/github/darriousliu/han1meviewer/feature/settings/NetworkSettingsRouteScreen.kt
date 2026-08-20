package io.github.darriousliu.han1meviewer.feature.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import co.touchlab.kermit.Logger
import io.github.darriousliu.han1meviewer.core.common.EMPTY_STRING
import io.github.darriousliu.han1meviewer.core.common.ProxyType
import io.github.darriousliu.han1meviewer.core.common.state.WebsiteState
import io.github.darriousliu.han1meviewer.core.network.DohConfig
import io.github.darriousliu.han1meviewer.core.network.HanimeNetwork
import io.github.darriousliu.han1meviewer.core.network.logout
import io.github.darriousliu.han1meviewer.core.parse.Parser
import io.github.darriousliu.han1meviewer.core.resource.Res
import io.github.darriousliu.han1meviewer.core.resource.attention
import io.github.darriousliu.han1meviewer.core.resource.cancel
import io.github.darriousliu.han1meviewer.core.resource.confirm
import io.github.darriousliu.han1meviewer.core.resource.custom
import io.github.darriousliu.han1meviewer.core.resource.custom_mirror_site_invalid
import io.github.darriousliu.han1meviewer.core.resource.custom_mirror_site_test_failed
import io.github.darriousliu.han1meviewer.core.resource.custom_mirror_site_test_failed_http
import io.github.darriousliu.han1meviewer.core.resource.custom_mirror_site_test_parse_failed
import io.github.darriousliu.han1meviewer.core.resource.custom_mirror_site_test_partial_success
import io.github.darriousliu.han1meviewer.core.resource.custom_mirror_site_test_success
import io.github.darriousliu.han1meviewer.core.resource.custom_mirror_site_testing
import io.github.darriousliu.han1meviewer.core.resource.custom_mirror_site_warning
import io.github.darriousliu.han1meviewer.core.resource.custom_mirror_site_watch_test_failed
import io.github.darriousliu.han1meviewer.core.resource.custom_mirror_site_watch_test_failed_http
import io.github.darriousliu.han1meviewer.core.resource.direct
import io.github.darriousliu.han1meviewer.core.resource.doh_conflict_message
import io.github.darriousliu.han1meviewer.core.resource.doh_disabled_summary
import io.github.darriousliu.han1meviewer.core.resource.domain_change_tips
import io.github.darriousliu.han1meviewer.core.resource.http_proxy
import io.github.darriousliu.han1meviewer.core.resource.invalid_ip_or_port
import io.github.darriousliu.han1meviewer.core.resource.loading
import io.github.darriousliu.han1meviewer.core.resource.mpv_socks5_warning
import io.github.darriousliu.han1meviewer.core.resource.network_timeout_text
import io.github.darriousliu.han1meviewer.core.resource.node_latency_sum
import io.github.darriousliu.han1meviewer.core.resource.restart_or_not_working
import io.github.darriousliu.han1meviewer.core.resource.socks_proxy
import io.github.darriousliu.han1meviewer.core.resource.system_proxy
import io.github.darriousliu.han1meviewer.core.resource.unknow
import io.github.darriousliu.han1meviewer.core.resource.warning
import io.github.darriousliu.han1meviewer.core.storage.Preferences
import io.github.darriousliu.han1meviewer.core.ui.component.ConfirmDialog
import io.github.darriousliu.han1meviewer.core.ui.component.LocalToaster
import io.github.darriousliu.han1meviewer.core.ui.component.showShort
import io.github.darriousliu.han1meviewer.feature.main.LocalMainHostActions
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import kotlin.time.TimeSource

private enum class DohConflictTarget {
    EnableDoH,
    EnableBuiltInHosts,
}

@Composable
fun NetworkSettingsRouteScreen() {
    val hostActions = LocalMainHostActions.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val capabilities = networkSettingsCapabilities
    val diagnostics = networkDiagnostics

    var refreshKey by remember { mutableIntStateOf(0) }
    var currentHost by remember { mutableStateOf(Preferences.baseUrl) }
    var isDelayTesting by remember { mutableStateOf(false) }
    var isDohTesting by remember { mutableStateOf(false) }
    var isCustomMirrorTesting by remember { mutableStateOf(false) }
    var customMirrorTestResult by remember { mutableStateOf<String?>(null) }
    var showDomainRestartConfirm by remember { mutableStateOf(false) }
    var showHostsRestartConfirm by remember { mutableStateOf(false) }
    var showCustomHostsValidationError by remember { mutableStateOf<List<String>?>(null) }
    var showCustomMirrorValidationError by remember { mutableStateOf(false) }
    var showCustomMirrorWarningConfirm by remember { mutableStateOf(false) }
    var showDohConflictConfirm by remember { mutableStateOf(false) }
    var showSocksWarning by remember { mutableStateOf(false) }
    var pendingDomainValue by remember { mutableStateOf("") }
    var pendingUseCustomMirrorSite by remember { mutableStateOf(Preferences.useCustomMirrorSite) }
    var pendingCustomMirrorSite by remember { mutableStateOf(Preferences.customMirrorSite) }
    var pendingAppendCustomMirrorPath by remember { mutableStateOf(Preferences.appendCustomMirrorPath) }
    var pendingDohConflictTarget by remember { mutableStateOf(DohConflictTarget.EnableDoH) }
    var pendingDohEnabled by remember { mutableStateOf(Preferences.useDoH) }
    var pendingDohPreset by remember { mutableStateOf(Preferences.dohPreset) }
    var pendingDohCustomUrl by remember { mutableStateOf(Preferences.dohCustomUrl) }
    var pendingDohBootstrapIps by remember { mutableStateOf(Preferences.dohBootstrapIps) }
    var pendingDohTimeoutSeconds by remember { mutableIntStateOf(Preferences.dohTimeoutSeconds) }
    val delayResults = remember { mutableStateListOf<DelayResultUi>() }
    val dohTestResults = remember { mutableStateListOf<DohTestResultUi>() }
    // 测试协程随组合退出自动取消;stopXxx 只负责用户手动关 dialog
    var delayJob by remember { mutableStateOf<Job?>(null) }
    var dohJob by remember { mutableStateOf<Job?>(null) }
    val uiState = buildNetworkSettingsUiState(refreshKey)

    fun stopDelayTest() {
        isDelayTesting = false
        delayJob?.cancel()
        delayJob = null
    }

    fun stopDohTest() {
        isDohTesting = false
        dohJob?.cancel()
        dohJob = null
    }

    fun runDohTest() {
        if (isDohTesting) return
        currentHost = Preferences.baseUrl
        dohTestResults.clear()
        isDohTesting = true
        dohJob = scope.launch {
            val host = currentHostName()
            val networkTimeoutText = getString(Res.string.network_timeout_text)
            val mark = TimeSource.Monotonic.markNow()
            val result = try {
                Result.success(diagnostics.lookupByDoHOnly(host))
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Result.failure(t)
            }
            val delayMs = mark.elapsedNow().inWholeMilliseconds.toInt()
            dohTestResults.clear()
            result.onSuccess { ips ->
                dohTestResults.add(
                    DohTestResultUi(
                        host = host,
                        ips = ips,
                        delay = delayMs,
                        message = "",
                    )
                )
            }.onFailure { throwable ->
                Logger.w("DOH_TEST") { "lookup failed for $host: ${throwable.message}" }
                dohTestResults.add(
                    DohTestResultUi(
                        host = host,
                        ips = emptyList(),
                        delay = -1,
                        message = throwable.message?.ifBlank { networkTimeoutText }
                            ?: networkTimeoutText,
                    )
                )
            }
        }
    }

    NetworkSettingsScreen(
        state = uiState,
        domainOptions = buildDomainOptions(),
        currentHost = currentHost,
        delayResults = delayResults,
        dohTestResults = dohTestResults,
        isDelayTesting = isDelayTesting,
        isDohTesting = isDohTesting,
        proxyType = Preferences.proxyType,
        proxyIp = Preferences.proxyIp,
        proxyPort = Preferences.proxyPort,
        dohEnabled = Preferences.useDoH,
        dohPreset = Preferences.dohPreset,
        dohCustomUrl = Preferences.dohCustomUrl,
        dohBootstrapIps = Preferences.dohBootstrapIps,
        dohTimeoutSeconds = Preferences.dohTimeoutSeconds,
        useCustomMirrorSite = Preferences.useCustomMirrorSite,
        customMirrorSite = Preferences.customMirrorSite,
        appendCustomMirrorPath = Preferences.appendCustomMirrorPath,
        customMirrorTestResult = customMirrorTestResult,
        isCustomMirrorTesting = isCustomMirrorTesting,
        showProxy = capabilities.proxy,
        showHosts = capabilities.hosts,
        showDoh = capabilities.doh,
        showLatencyTest = capabilities.latencyTest,
        onDomainChange = { newValue ->
            val origin = Preferences.baseUrl
            if (newValue != origin) {
                pendingDomainValue = newValue
                pendingUseCustomMirrorSite = false
                pendingCustomMirrorSite = Preferences.customMirrorSite
                pendingAppendCustomMirrorPath = Preferences.appendCustomMirrorPath
                showDomainRestartConfirm = true
            }
        },
        onSaveCustomMirrorSite = { enabled, url, appendPath ->
            val normalizedUrl = normalizeCustomMirrorSite(url)
            if (enabled && normalizedUrl == null) {
                showCustomMirrorValidationError = true
                return@NetworkSettingsScreen
            }
            val customMirrorSite = normalizedUrl.orEmpty()
            if (enabled != Preferences.useCustomMirrorSite ||
                customMirrorSite != Preferences.customMirrorSite ||
                appendPath != Preferences.appendCustomMirrorPath
            ) {
                pendingUseCustomMirrorSite = enabled
                pendingCustomMirrorSite = customMirrorSite
                pendingAppendCustomMirrorPath = appendPath
                if (enabled) {
                    showCustomMirrorWarningConfirm = true
                } else {
                    showDomainRestartConfirm = true
                }
            }
        },
        onTestCustomMirrorSite = { url, appendPath ->
            val normalizedUrl = normalizeCustomMirrorSite(url)
            if (normalizedUrl == null) {
                scope.launch {
                    customMirrorTestResult = getString(Res.string.custom_mirror_site_invalid)
                }
            } else if (!isCustomMirrorTesting) {
                isCustomMirrorTesting = true
                scope.launch {
                    customMirrorTestResult = getString(Res.string.custom_mirror_site_testing)
                    customMirrorTestResult = testCustomMirrorSite(normalizedUrl, appendPath)
                    isCustomMirrorTesting = false
                }
            }
        },
        onUseBuiltInHostsChange = { value ->
            if (value && Preferences.useDoH) {
                showDohConflictConfirm = true
                pendingDohConflictTarget = DohConflictTarget.EnableBuiltInHosts
                return@NetworkSettingsScreen
            }
            Preferences.useBuiltInHosts = value
            refreshKey++
            showHostsRestartConfirm = true
        },
        onSaveCustomHosts = { data ->
            val errors = diagnostics.validateCustomHosts(data)
            if (errors.isNotEmpty()) {
                showCustomHostsValidationError = errors
                return@NetworkSettingsScreen
            }
            Preferences.customHostsData = data
            refreshKey++
            if (Preferences.useBuiltInHosts) {
                HanimeNetwork.rebuildNetwork()
            }
        },
        customHostsData = Preferences.customHostsData,
        onSaveDohSettings = { enabled, preset, url, bootstrapIps, timeoutSeconds ->
            pendingDohEnabled = enabled
            pendingDohPreset = preset
            pendingDohCustomUrl = url
            pendingDohBootstrapIps = bootstrapIps
            pendingDohTimeoutSeconds = timeoutSeconds
            if (enabled && Preferences.useBuiltInHosts) {
                showDohConflictConfirm = true
                pendingDohConflictTarget = DohConflictTarget.EnableDoH
                return@NetworkSettingsScreen
            }
            Preferences.useDoH = enabled
            Preferences.dohPreset = preset
            Preferences.dohCustomUrl = url
            Preferences.dohBootstrapIps = bootstrapIps
            Preferences.dohTimeoutSeconds = timeoutSeconds.coerceIn(1, 60)
            currentHost = Preferences.baseUrl
            refreshKey++
            HanimeNetwork.rebuildNetwork()
        },
        onOpenDelayTest = {
            currentHost = Preferences.baseUrl
            delayResults.clear()
            isDelayTesting = true
            delayJob?.cancel()
            delayJob = scope.launch {
                val host = currentHostName()
                val ipList = diagnostics.cdnList(host)
                delayResults.clear()
                delayResults.addAll(ipList.map { DelayResultUi(it, -1) })
                // 原实现的节拍:每 2 秒对全部节点发一轮测试,不等上一轮结束
                while (isActive) {
                    ipList.forEach { ip ->
                        launch {
                            val delayMs = diagnostics.measureDelay(ip)
                            val index = delayResults.indexOfFirst { it.ip == ip }
                            if (index >= 0) {
                                delayResults[index] = DelayResultUi(ip, delayMs)
                            }
                        }
                    }
                    delay(2000)
                }
            }
        },
        onOpenDohTest = { runDohTest() },
        onDismissDelayTest = { stopDelayTest() },
        onDismissDohTest = { stopDohTest() },
        onApplyProxy = { type, ip, port ->
            val valid = when (type) {
                ProxyType.DIRECT, ProxyType.SYSTEM -> true
                ProxyType.HTTP, ProxyType.SOCKS -> validateIp(ip) && validatePort(port)
                else -> false
            }
            if (!valid) {
                scope.launch { toaster.showShort(getString(Res.string.invalid_ip_or_port)) }
                return@NetworkSettingsScreen
            }
            if (type == ProxyType.SOCKS) {
                showSocksWarning = true
            }
            Preferences.proxyType = type
            Preferences.proxyIp = ip
            Preferences.proxyPort = port
            diagnostics.rebuildProxySelector()
            HanimeNetwork.rebuildNetwork()
            refreshKey++
        },
    )

    ConfirmDialog(
        visible = showDomainRestartConfirm,
        title = stringResource(Res.string.attention),
        message = stringResource(Res.string.domain_change_tips).trimIndent(),
        confirmText = stringResource(Res.string.confirm),
        dismissText = stringResource(Res.string.cancel),
        cancelable = false,
        onConfirm = {
            if (pendingDomainValue.isNotEmpty()) {
                Preferences.domainName = pendingDomainValue
                Preferences.selectedBaseUrl = pendingDomainValue
            }
            Preferences.useCustomMirrorSite = pendingUseCustomMirrorSite
            Preferences.customMirrorSite = pendingCustomMirrorSite
            Preferences.appendCustomMirrorPath = pendingAppendCustomMirrorPath
            logout()
            hostActions.onRestartApp()
        },
        onDismiss = {
            pendingDomainValue = ""
            pendingUseCustomMirrorSite = Preferences.useCustomMirrorSite
            pendingCustomMirrorSite = Preferences.customMirrorSite
            pendingAppendCustomMirrorPath = Preferences.appendCustomMirrorPath
            showDomainRestartConfirm = false
        },
    )

    if (showCustomMirrorValidationError) {
        AlertDialog(
            onDismissRequest = { showCustomMirrorValidationError = false },
            title = { Text(stringResource(Res.string.attention)) },
            text = { Text(stringResource(Res.string.custom_mirror_site_invalid)) },
            confirmButton = {
                TextButton(onClick = { showCustomMirrorValidationError = false }) {
                    Text(stringResource(Res.string.confirm))
                }
            },
        )
    }

    ConfirmDialog(
        visible = showHostsRestartConfirm,
        title = stringResource(Res.string.attention),
        message = stringResource(Res.string.restart_or_not_working, EMPTY_STRING),
        confirmText = stringResource(Res.string.confirm),
        dismissText = stringResource(Res.string.cancel),
        cancelable = false,
        onConfirm = { hostActions.onRestartApp() },
        onDismiss = { showHostsRestartConfirm = false },
    )

    val validationErrors = showCustomHostsValidationError
    if (validationErrors != null) {
        AlertDialog(
            onDismissRequest = { showCustomHostsValidationError = null },
            title = { Text(stringResource(Res.string.attention)) },
            text = { Text(validationErrors.joinToString("\n")) },
            confirmButton = {
                TextButton(onClick = { showCustomHostsValidationError = null }) {
                    Text(stringResource(Res.string.confirm))
                }
            },
        )
    }

    ConfirmDialog(
        visible = showCustomMirrorWarningConfirm,
        title = stringResource(Res.string.attention),
        message = stringResource(Res.string.custom_mirror_site_warning),
        confirmText = stringResource(Res.string.confirm),
        dismissText = stringResource(Res.string.cancel),
        cancelable = false,
        onConfirm = {
            showCustomMirrorWarningConfirm = false
            showDomainRestartConfirm = true
        },
        onDismiss = {
            pendingUseCustomMirrorSite = Preferences.useCustomMirrorSite
            pendingCustomMirrorSite = Preferences.customMirrorSite
            pendingAppendCustomMirrorPath = Preferences.appendCustomMirrorPath
            showCustomMirrorWarningConfirm = false
        },
    )

    ConfirmDialog(
        visible = showDohConflictConfirm,
        title = stringResource(Res.string.attention),
        message = stringResource(Res.string.doh_conflict_message),
        confirmText = stringResource(Res.string.confirm),
        dismissText = stringResource(Res.string.cancel),
        cancelable = false,
        onConfirm = {
            when (pendingDohConflictTarget) {
                DohConflictTarget.EnableDoH -> {
                    Preferences.useBuiltInHosts = false
                    Preferences.useDoH = pendingDohEnabled
                    Preferences.dohPreset = pendingDohPreset
                    Preferences.dohCustomUrl = pendingDohCustomUrl
                    Preferences.dohBootstrapIps = pendingDohBootstrapIps
                    Preferences.dohTimeoutSeconds = pendingDohTimeoutSeconds.coerceIn(1, 60)
                }

                DohConflictTarget.EnableBuiltInHosts -> {
                    Preferences.useDoH = false
                    Preferences.useBuiltInHosts = true
                }
            }
            showDohConflictConfirm = false
            refreshKey++
            HanimeNetwork.rebuildNetwork()
        },
        onDismiss = { showDohConflictConfirm = false },
    )

    if (showSocksWarning) {
        AlertDialog(
            onDismissRequest = { showSocksWarning = false },
            title = { Text(stringResource(Res.string.warning)) },
            text = { Text(stringResource(Res.string.mpv_socks5_warning)) },
            confirmButton = {
                TextButton(onClick = { showSocksWarning = false }) {
                    Text(stringResource(Res.string.confirm))
                }
            },
        )
    }
}

/** 当前站点域名;解析不出来时退回「未知」文案。 */
private suspend fun currentHostName(): String =
    runCatching { Url(Preferences.baseUrl).host }.getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: getString(Res.string.unknow)

/**
 * @param refreshKey 只用来触发重算——`Preferences` 不是可观察状态，
 *   改完得靠它把这个 composable 拉一遍。
 */
@Composable
private fun buildNetworkSettingsUiState(refreshKey: Int): NetworkSettingsUiState {
    @Suppress("UNUSED_EXPRESSION") refreshKey
    return NetworkSettingsUiState(
        domainName = Preferences.baseUrl,
        domainDisplay = buildDomainOptions().firstOrNull { it.second == Preferences.baseUrl }?.first
            ?: Preferences.baseUrl,
        proxySummary = when (Preferences.proxyType) {
            ProxyType.DIRECT -> stringResource(Res.string.direct)
            ProxyType.SYSTEM -> stringResource(Res.string.system_proxy)
            ProxyType.HTTP -> stringResource(
                Res.string.http_proxy,
                Preferences.proxyIp,
                Preferences.proxyPort,
            )

            ProxyType.SOCKS -> stringResource(
                Res.string.socks_proxy,
                Preferences.proxyIp,
                Preferences.proxyPort,
            )

            else -> stringResource(Res.string.direct)
        },
        useBuiltInHosts = Preferences.useBuiltInHosts,
        useCustomMirrorSite = Preferences.useCustomMirrorSite,
        customMirrorSite = Preferences.customMirrorSite,
        appendCustomMirrorPath = Preferences.appendCustomMirrorPath,
        useDoH = Preferences.useDoH,
        dohSummary = buildDohSummary(),
        delaySummary = stringResource(Res.string.node_latency_sum),
    )
}

// 与 HProxySelector 里的校验一致;复制到 commonMain 是为了让校验先于落库、
// 不必进平台层。
private val ipv4Regex =
    Regex("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")

private fun validateIp(ip: String): Boolean = ipv4Regex.matches(ip)

private fun validatePort(port: Int): Boolean = port in 0..65535

private fun normalizeCustomMirrorSite(url: String): String? {
    val trimmed = url.trim().trimEnd('/')
    val parsed = runCatching { Url(trimmed) }.getOrNull() ?: return null
    if (parsed.protocol.name != "https" || parsed.host.isBlank()) return null
    if (parsed.encodedQuery.isNotBlank() || parsed.encodedFragment.isNotBlank()) return null
    return url.trim()
}

private suspend fun testCustomMirrorSite(homeUrl: String, appendPath: Boolean): String {
    return try {
        val response = HanimeNetwork.hClient.get(homeUrl)
        val finalUrl = response.request.url.toString()
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            return getString(
                Res.string.custom_mirror_site_test_failed_http,
                response.status.value,
                finalUrl,
            )
        }

        val apiBaseUrl = buildCustomMirrorApiBaseUrl(homeUrl, appendPath)
        val watchTestResult = testCustomMirrorWatchUrl(apiBaseUrl)
        val parseResult = withContext(Dispatchers.Default) { Parser.homePageVer2(body) }
        when (parseResult) {
            is WebsiteState.Success -> if (watchTestResult == null) {
                getString(
                    Res.string.custom_mirror_site_test_success,
                    finalUrl,
                    apiBaseUrl,
                )
            } else {
                getString(
                    Res.string.custom_mirror_site_test_partial_success,
                    finalUrl,
                    apiBaseUrl,
                    watchTestResult,
                )
            }

            is WebsiteState.Error -> getString(
                Res.string.custom_mirror_site_test_parse_failed,
                finalUrl,
                parseResult.throwable.message ?: parseResult.throwable::class.simpleName.orEmpty(),
            )

            WebsiteState.Loading -> getString(
                Res.string.custom_mirror_site_test_parse_failed,
                finalUrl,
                getString(Res.string.loading),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (throwable: Throwable) {
        getString(
            Res.string.custom_mirror_site_test_failed,
            throwable.message ?: throwable::class.simpleName.orEmpty(),
        )
    }
}

private suspend fun testCustomMirrorWatchUrl(apiBaseUrl: String): String? {
    return try {
        val response = HanimeNetwork.hClient.get(apiBaseUrl + "search")
        if (response.status.isSuccess()) {
            null
        } else {
            getString(
                Res.string.custom_mirror_site_watch_test_failed_http,
                response.status.value,
                response.request.url.toString(),
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (throwable: Throwable) {
        getString(
            Res.string.custom_mirror_site_watch_test_failed,
            throwable.message ?: throwable::class.simpleName.orEmpty(),
        )
    }
}

private fun buildCustomMirrorApiBaseUrl(homeUrl: String, appendPath: Boolean): String {
    val url = if (appendPath) homeUrl else {
        val u = Url(homeUrl)
        val port = u.specifiedPort.takeIf { it != 0 && it != u.protocol.defaultPort }
        "${u.protocol.name}://${u.host}" + (port?.let { ":$it" } ?: "")
    }
    return if (url.endsWith('/')) url else "$url/"
}

@Composable
private fun buildDohSummary(): String {
    if (!Preferences.useDoH) return stringResource(Res.string.doh_disabled_summary)
    if (Preferences.useBuiltInHosts) return stringResource(Res.string.doh_conflict_message)
    val core = if (Preferences.dohPreset == "custom") {
        Preferences.dohCustomUrl.ifBlank { stringResource(Res.string.custom) }
    } else {
        DohConfig.selectedPreset().title
    }
    val bootstrap = DohConfig.bootstrapIps().takeIf { it.isNotEmpty() }?.joinToString()
    return if (bootstrap != null) "$core\nBootstrap: $bootstrap" else core
}
